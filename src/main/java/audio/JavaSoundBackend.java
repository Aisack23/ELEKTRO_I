package audio;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.BooleanControl;
import java.net.URL;

/** Owns native audio resources and closes partial allocations on failure. */
public final class JavaSoundBackend implements AudioBackend {
    @Override
    public Playback open(URL resource) throws Exception {
        Clip clip = AudioSystem.getClip();
        try (var stream = AudioSystem.getAudioInputStream(resource)) {
            clip.open(stream);
            return new ClipPlayback(clip);
        } catch (Exception failure) {
            clip.close();
            throw failure;
        }
    }

    private record ClipPlayback(Clip clip) implements Playback {
        @Override public void start(boolean loop) {
            if (loop) clip.loop(Clip.LOOP_CONTINUOUSLY);
            else clip.start();
        }
        @Override public boolean isRunning() { return clip.isRunning(); }
        @Override public void setVolume(int level) {
            if (clip.isControlSupported(BooleanControl.Type.MUTE)) {
                ((BooleanControl) clip.getControl(BooleanControl.Type.MUTE)).setValue(level == 0);
            }
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                var gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float decibels = level == 0 ? gain.getMinimum() : (float) (20 * Math.log10(level / 5.0));
                gain.setValue(Math.clamp(decibels, gain.getMinimum(), gain.getMaximum()));
            }
        }
        @Override public void close() { clip.stop(); clip.close(); }
    }
}
