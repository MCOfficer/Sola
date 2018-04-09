import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.OrderedMenu;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavaplayer.AudioPlayerSendHandler;
import lavaplayer.CustomYoutubeAudioSourceManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.AudioManager;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class Commands {

    private final AudioPlayerManager playerManager;
    private final AudioPlayer player;
    private final EventWaiter eventWaiter;
    private final Color color = new Color(251, 252, 254);
    private TextChannel channel;

    public Commands(EventWaiter eventWaiter) {
        playerManager = new DefaultAudioPlayerManager();
        playerManager.registerSourceManager(new CustomYoutubeAudioSourceManager(true));
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        player = playerManager.createPlayer();
        this.eventWaiter = eventWaiter;
    }

    public void onHelpCommand(TextChannel channel) {
        channel.sendMessage(
                "**I'm Sola - a Bot created specifically for listening to livestreams.**\n" +
                "The following Commands are available:\n\n" +
                "`-help` : Displays this Message.\n" +
                "`-ping` : Shows the time of the last Hearbeat in ms, which is roughly my Ping.\n" +
                "`-play <query>` : Plays the lifestream <query>, where <query> can be a link or a search term.\n" +
                "`-stop` : Stops Playback and disconnects from the Voice Channel.\n" +
                "`-update` : Makes me restart and perform an auto-update.\n" +
                "\nIf you require assistance, please contact M\\*C\\*O#9635!"
        ).queue();
    }

    public void onPingCommand(TextChannel channel) {
        channel.sendMessage("Last Heartbeat took " + channel.getJDA().getPing() + "ms.").queue();
    }

    public void onPlayCommand(Guild guild, TextChannel channel, Member member, String[] args) {
        if(!isConnected(guild.getAudioManager()) || channel == this.channel) {
            String query = String.join(" ", args);
            if (!query.toLowerCase().startsWith("http://") && !query.toLowerCase().startsWith("https://"))
                query = "ytsearch:" + query;
            joinVoiceChannel(channel, member);
            loadAndPlay(query, channel);
        }
    }

    public void onUpdateCommand(TextChannel channel) {
        channel.sendMessage("Restarting...").complete();
        try {
            Path file = Paths.get(".solarestart");
            Files.write(file, channel.getId().getBytes());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(133);
    }

    public void onStopCommand(Guild guild, TextChannel channel) {
        if(isConnected(guild.getAudioManager()) && channel == this.channel) {
            player.stopTrack();
            AudioManager am = guild.getAudioManager();
            String voiceName = am.getConnectedChannel().getName();
            guild.getAudioManager().closeAudioConnection();
            channel.sendMessage("Unbound " + channel.getAsMention() + " and disconnected from channel " + voiceName + ".").queue();
            this.channel = null;
        }
    }

    public void loadAndPlay(String query, TextChannel channel) {
        playerManager.loadItem(query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                startTrack(track, channel, false);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if(playlist.isSearchResult())
                    displaySearchResult(playlist, channel);
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Couldn't find any streams for `" + query.substring(9) + "`!").queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                if (e.severity.equals(FriendlyException.Severity.COMMON))
                    channel.sendMessage("The Track could not be loaded, presumably because it's blocked in my country.").queue();
                e.printStackTrace();
            }
        });
    }

    public void startTrack(AudioTrack track, TextChannel channel, boolean silent) {
        if (track.getInfo().isStream) {
            player.startTrack(track, false);
            if(!silent) {
                EmbedBuilder eb = new EmbedBuilder()
                        .setThumbnail("https://i.ytimg.com/vi/" + track.getIdentifier() + "/maxresdefault.jpg")
                        .setDescription("Now Playing\n**\"" + track.getInfo().title + "\"**[\uD83D\uDD17 ](" + track.getInfo().uri + ")\nby " + track.getInfo().author + ".")
                        .setColor(color);

                channel.sendMessage(eb.build()).queue();
                channel.getJDA().getPresence().setGame(Game.playing(track.getInfo().title));
            }
        }
        else if (!silent)
            channel.sendMessage("Feed me with Lifestreams, not just lame Tracks ;-;").queue();
    }

    public void displaySearchResult(AudioPlaylist playlist, TextChannel channel) {
        List<AudioTrack> results = playlist.getTracks().subList(0, playlist.getTracks().size() < 10 ? playlist.getTracks().size() : 10);
        if (results.size() > 1) {
            OrderedMenu.Builder builder = new OrderedMenu.Builder()
                    .setSelection((message, integer) -> startTrack(results.get(integer - 1), channel, false))
                    .setEventWaiter(eventWaiter)
                    .setDescription("**I could find the following streams:**")
                    .setColor(color);
            results.forEach(audioTrack -> builder.addChoice(audioTrack.getInfo().title + "\n"));
            builder.build().display(channel);
        }
        else
            startTrack(results.get(0), channel, false);
    }

    private void joinVoiceChannel(TextChannel channel, Member member) {
        AudioManager am = channel.getGuild().getAudioManager();
        if(!isConnected(am) && member.getVoiceState().getChannel() != null) {
            am.setSendingHandler(new AudioPlayerSendHandler(player));
            VoiceChannel voice = member.getVoiceState().getChannel();
            am.openAudioConnection(voice);
            this.channel = channel;
            channel.sendMessage("Joined " + voice.getName() + " and bound to " + channel.getAsMention()).queue();
        }
    }

    private boolean isConnected(AudioManager audioManager) {
        return audioManager.isConnected() || audioManager.isAttemptingToConnect();
    }

    public void onStreamEnd(AudioTrack track) {
        channel.sendMessage("The Stream **" + track.getInfo().title + "** has ended, please select a new one.").queue();
    }

    public void onLoadFailed(AudioTrack track) {
        channel.sendMessage("Couldn't load stream **" + track.getInfo().title + "**, perhaps it's being broadcasted with an unsupported codec.").queue();
    }
}
