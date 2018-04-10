import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

public class TrackEventListener extends AudioEventAdapter {

    public final GuildAudioWrapper wrapper;
    public final Commands commands;

    public TrackEventListener(GuildAudioWrapper wrapper) {
        this.wrapper = wrapper;
        this.commands = wrapper.commands;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if(endReason == AudioTrackEndReason.FINISHED)
            commands.onStreamEnd(track, wrapper.channelId);
        if(endReason == AudioTrackEndReason.LOAD_FAILED)
            commands.onLoadFailed(track, wrapper.channelId);
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        commands.startTrack(track, null, true); //channel can be null if silent is true
    }
}
