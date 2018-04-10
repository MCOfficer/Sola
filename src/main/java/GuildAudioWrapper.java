import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavaplayer.AudioPlayerSendHandler;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;

public class GuildAudioWrapper {

    //TODO: instead of straight up storing objects, store IDs
    public final Guild guild;
    public final AudioManager audioManager;
    public AudioTrack track;
    public final AudioPlayer player;
    public TextChannel channel = null;
    public final Commands commands;

    public GuildAudioWrapper(Guild guild, Commands commands, AudioPlayerManager playerManager) {
        this.guild = guild;
        this.commands = commands;
        this.player = playerManager.createPlayer();
        player.addListener(new TrackEventListener(this));
        audioManager = guild.getAudioManager();
    }

    public void connect(VoiceChannel voice, TextChannel bindTo) {
        audioManager.setSendingHandler(new AudioPlayerSendHandler(player));
        audioManager.openAudioConnection(voice);
        channel = bindTo;
    }

    public void play(AudioTrack track) {
        this.track = track;
        player.startTrack(track, false);
    }

    public void stop() {
        track = null;
        guild.getAudioManager().closeAudioConnection();
    }

    public boolean isConnected() {
        return audioManager.isConnected() || audioManager.isAttemptingToConnect();
    }
}
