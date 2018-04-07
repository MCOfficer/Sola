import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.AudioManager;


public class Commands {

    private final AudioPlayerManager playerManager;
    private final AudioPlayer player;

    public Commands() {
        playerManager = new DefaultAudioPlayerManager();
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        AudioSourceManagers.registerRemoteSources(playerManager);
        player = playerManager.createPlayer();
    }

    public void onHelpCommand(TextChannel channel) {
        channel.sendMessage(
                "**I'm Sola - a Bot created specifically for listening to livestreams.**\n" +
                "The following Commands are available:\n\n" +
                "`-help` : Displays this Message.\n" +
                "`-ping` : Shows the time of the last Hearbeat in ms, which is roughly my Ping.\n" +
                "`-stop` : Stops Playback and disconnects from the Voice Channel.\n" +
                "`-play <query>` : Plays the lifestream <query>, where <query> can be a link or a search term.\n" +
                "\nIf you require assistance, please contact M\\*C\\*O#9635!"
        ).queue();
    }

    public void onPingCommand(TextChannel channel) {
        channel.sendMessage("Last Heartbeat took " + channel.getJDA().getPing() + "ms.").queue();
    }

    public void onPlayCommand(Guild guild, TextChannel channel, Member member, String[] args) {
        String query = String.join(" ", args);
        if (!query.toLowerCase().startsWith("http://") && !query.toLowerCase().startsWith("https://"))
            query = "ytsearch:" + query;
        joinVoiceChannel(guild, member);
        loadAndPlay(query, channel);
    }

    public void onStopCommand(Guild guild, TextChannel channel) {
        player.stopTrack();
        AudioManager am = guild.getAudioManager();
        String voiceName = am.getConnectedChannel().getName();
        guild.getAudioManager().closeAudioConnection();
        channel.sendMessage("Stopped Playback and disconnected from channel " + voiceName + ".").queue();
    }

    public void loadAndPlay(String query, TextChannel channel) {
        playerManager.loadItem(query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                startTrack(track, channel);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if(playlist.isSearchResult())
                    for (AudioTrack track : playlist.getTracks())
                        if (track.getInfo().isStream) {
                            startTrack(track, channel);
                            break;
                        }
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Couldn't find anything for `" + query.substring(9) + "`!").queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                if (e.severity.equals(FriendlyException.Severity.COMMON))
                    channel.sendMessage("The Track could not be loaded, presumably because it's blocked in my country.").queue();
                e.printStackTrace();
            }
        });
    }

    public void startTrack(AudioTrack track, TextChannel channel) {
        if (track.getInfo().isStream) {
            player.startTrack(track, false);
            channel.sendMessage("Now Playing \"" + track.getInfo().title + "\" by " + track.getInfo().author + ".").queue();
            channel.getJDA().getPresence().setGame(Game.playing(track.getInfo().title));
        }
        else {
            channel.sendMessage("Feed me with Lifestreams, not just lame Tracks ;-;").queue();
        }
    }

    private void joinVoiceChannel(Guild guild, Member member) {
        AudioManager am = guild.getAudioManager();
        if(!am.isConnected() && !am.isAttemptingToConnect() && member.getVoiceState().getChannel() != null) {
            am.setSendingHandler(new AudioPlayerSendHandler(player));
            am.openAudioConnection(member.getVoiceState().getChannel());
        }
    }
}
