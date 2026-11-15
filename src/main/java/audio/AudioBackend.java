package audio;

import java.net.URL;

/** Device boundary: game policy can be tested without a sound card. */
@FunctionalInterface
public interface AudioBackend {
    Playback open(URL resource) throws Exception;

    interface Playback extends AutoCloseable {
        void start(boolean loop);
        void setVolume(int level);
        boolean isRunning();
        @Override void close();
    }
}
