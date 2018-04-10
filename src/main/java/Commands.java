import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.OrderedMenu;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavaplayer.CustomYoutubeAudioSourceManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;


public class Commands {

    private final AudioPlayerManager playerManager;
    private final EventWaiter eventWaiter;
    private final Main main;
    private final Color color = new Color(251, 252, 254);
    private HashMap<Guild, GuildAudioWrapper> guildAudioWrappers = new HashMap<>();

    public Commands(EventWaiter eventWaiter, Main main) {
        playerManager = new DefaultAudioPlayerManager();
        playerManager.registerSourceManager(new CustomYoutubeAudioSourceManager(true));
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        this.eventWaiter = eventWaiter;
        this.main = main;
    }

    public void onHelpCommand(TextChannel channel) {
        //TODO: use embed
        channel.sendMessage(
                "**I'm Sola - a Bot created specifically for listening to livestreams.**\n" +
                "My prefix is `sola`. The following Commands are available:\n\n" +
                "`help` : Displays this Message.\n" +
                "`ping` : Shows the time of the last Hearbeat in ms, which is roughly my Ping.\n\n" +
                "`play <query>` : Plays the lifestream <query>, where <query> can be a link or a search term.\n" +
                "`stop` : Stops Playback and disconnects from the Voice Channel.\n" +
                "\nIf you require assistance, please contact M\\*C\\*O#9635!"
        ).queue();
    }

    public void onPingCommand(TextChannel channel) {
        //TODO: use embed
        channel.sendMessage("Last Heartbeat took " + channel.getJDA().getPing() + "ms.").queue();
    }

    public void onPlayCommand(Guild guild, TextChannel channel, Member member, String[] args) {
        if (member.getVoiceState().getChannel() == null) return;
        GuildAudioWrapper wrapper = guildAudioWrappers.get(guild);
        if (wrapper == null) {
            wrapper = new GuildAudioWrapper(guild, this, playerManager);
            guildAudioWrappers.put(guild, wrapper);
        }
        if( (!wrapper.isConnected() || channel.getId().equals(wrapper.channelId))) {
            String query = String.join(" ", args);
            if (!query.toLowerCase().startsWith("http://") && !query.toLowerCase().startsWith("https://"))
                query = "ytsearch:" + query;
            joinVoiceChannel(channel, member, wrapper);
            loadAndPlay(query, channel);
        }
    }

    public void onUpdateCommand(TextChannel channel, User author) {
        if(!author.getId().equals("177733454824341505")) return;
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
        //TODO: use embed
        GuildAudioWrapper wrapper = guildAudioWrappers.get(guild);
        if(wrapper.isConnected() && channel.getId().equals(wrapper.channelId)) {
            String voiceName = guild.getAudioManager().getConnectedChannel().getName();
            wrapper.stop(channel.getJDA());
            guildAudioWrappers.remove(guild);
            channel.sendMessage("Unbound " + channel.getAsMention() + " and disconnected from channel " + voiceName + ".").queue();
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
            guildAudioWrappers.get(channel.getGuild()).play(track);
            if(!silent) {
                EmbedBuilder eb = new EmbedBuilder()
                        .setThumbnail("https://i.ytimg.com/vi/" + track.getIdentifier() + "/maxresdefault.jpg")
                        .setDescription("Now Playing\n**\"" + track.getInfo().title + "\"**[\uD83D\uDD17 ](" + track.getInfo().uri + ")\nby " + track.getInfo().author + ".")
                        .setColor(color);

                channel.sendMessage(eb.build()).queue();
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

    private void joinVoiceChannel(TextChannel channel, Member member, GuildAudioWrapper wrapper) {
        VoiceChannel voice = member.getVoiceState().getChannel();
        if(!wrapper.isConnected() && voice != null) {
            wrapper.connect(voice, channel);
            channel.sendMessage("Joined " + voice.getName() + " and bound to " + channel.getAsMention()).queue();
        }
    }

    public void onStreamEnd(AudioTrack track, String channelId) {
        main.jda.getTextChannelById(channelId).sendMessage("The Stream **" + track.getInfo().title + "** has ended, please select a new one.").queue();
    }

    public void onLoadFailed(AudioTrack track, String channelId) {
        main.jda.getTextChannelById(channelId).sendMessage("Couldn't load stream **" + track.getInfo().title + "**, perhaps it's being broadcasted with an unsupported codec.").queue();
    }
}
