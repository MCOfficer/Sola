import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavaplayer.AudioPlayerSendHandler;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;

public class GuildAudioWrapper {

    public final String guildId;
    public final AudioManager audioManager;
    public AudioTrack track;
    public final AudioPlayer player;
    public String channelId;
    public final Commands commands;

    public GuildAudioWrapper(Guild guild, Commands commands, AudioPlayerManager playerManager) {
        guildId = guild.getId();
        this.commands = commands;
        this.player = playerManager.createPlayer();
        player.addListener(new TrackEventListener(this));
        audioManager = guild.getAudioManager();
    }

    public void connect(VoiceChannel voice, TextChannel bindTo) {
        audioManager.setSendingHandler(new AudioPlayerSendHandler(player));
        audioManager.openAudioConnection(voice);
        channelId = bindTo.getId();
    }

    public void play(AudioTrack track) {
        this.track = track;
        player.startTrack(track, false);
    }

    public void stop(JDA jda) {
        track = null;
        jda.getGuildById(guildId).getAudioManager().closeAudioConnection();
    }

    public boolean isConnected() {
        return audioManager.isConnected() || audioManager.isAttemptingToConnect();
    }
}
