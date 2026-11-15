package audio;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

/** Independent music/effect channels. Missing assets or devices degrade to silence. */
public final class AudioManager implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(AudioManager.class.getName());
    private static final int MAX_EFFECTS = 16;
    private final AudioBackend backend;
    private final Function<String, URL> resources;
    private final List<AudioBackend.Playback> effects = new ArrayList<>();
    private final Set<String> warned = new HashSet<>();
    private AudioBackend.Playback music;
    private String musicPath;
    private int musicVolume = 3;
    private int effectsVolume = 3;
    private boolean closed;

    public AudioManager() { this(new JavaSoundBackend(), AudioManager.class::getResource); }
    public AudioManager(AudioBackend backend, Function<String, URL> resources) {
        this.backend = Objects.requireNonNull(backend);
        this.resources = Objects.requireNonNull(resources);
    }

    public synchronized void playMusic(String path) {
        if (closed || Objects.equals(musicPath, path)) return;
        stopMusic();
        musicPath = path;
        music = open(path, musicVolume, true);
    }

    public synchronized void stopMusic() {
        if (music != null) music.close();
        music = null;
        musicPath = null;
    }

    public synchronized void playEffect(String path) {
        if (closed || effectsVolume == 0) return;
        effects.removeIf(effect -> {
            if (effect.isRunning()) return false;
            effect.close();
            return true;
        });
        if (effects.size() >= MAX_EFFECTS) effects.removeFirst().close();
        var effect = open(path, effectsVolume, false);
        if (effect != null) effects.add(effect);
    }

    private AudioBackend.Playback open(String path, int volume, boolean loop) {
        if (path == null) return null;
        AudioBackend.Playback playback = null;
        try {
            URL resource = resources.apply(path);
            if (resource == null) throw new IllegalArgumentException("Resource not found");
            playback = backend.open(resource);
            playback.setVolume(volume);
            playback.start(loop);
            return playback;
        } catch (Exception failure) {
            if (playback != null) playback.close();
            if (warned.add(path)) LOG.warning("Audio unavailable: " + path + " (" + failure.getMessage() + ")");
            return null;
        }
    }

    public synchronized int getMusicVolume() { return musicVolume; }
    public synchronized int getEffectsVolume() { return effectsVolume; }
    public synchronized void setMusicVolume(int level) {
        musicVolume = Math.clamp(level, 0, 5);
        if (music != null) music.setVolume(musicVolume);
    }
    public synchronized void setEffectsVolume(int level) {
        effectsVolume = Math.clamp(level, 0, 5);
        effects.forEach(effect -> effect.setVolume(effectsVolume));
    }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        stopMusic();
        effects.forEach(AudioBackend.Playback::close);
        effects.clear();
    }
}
