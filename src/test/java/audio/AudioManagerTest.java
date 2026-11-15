package audio;

import enums.GameState;
import org.junit.jupiter.api.Test;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AudioManagerTest {
    static class FakePlayback implements AudioBackend.Playback {
        boolean loop, closed, running;
        int volume;
        public void start(boolean loop) { this.loop = loop; running = true; }
        public void setVolume(int level) { volume = level; }
        public boolean isRunning() { return running; }
        public void close() { closed = true; running = false; }
    }
    static final class Backend implements AudioBackend {
        final List<String> paths = new ArrayList<>();
        final List<FakePlayback> clips = new ArrayList<>();
        public FakePlayback open(URL resource) {
            paths.add(resource.getPath());
            FakePlayback clip = new FakePlayback();
            clips.add(clip);
            return clip;
        }
    }
    static URL resource(String path) {
        try { return new URL("file:" + path); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    @Test void mapBattleReturnAndMenusSelectExclusiveTracks() {
        Backend backend = new Backend();
        try (var audio = new AudioManager(backend, AudioManagerTest::resource)) {
            var director = new MusicDirector(audio);
            director.update(GameState.TITLE_STATE, null);
            director.update(GameState.PLAY_STATE, "forest");
            director.update(GameState.OPTIONS_STATE, "forest"); // no restart
            assertEquals(2, backend.clips.size());
            director.update(GameState.BATTLE_STATE, "forest");
            director.update(GameState.BATTLE_STATE, "cave"); // combat wins over map changes
            director.update(GameState.CUTSCENE_STATE, "cave");
            director.update(GameState.TITLE_STATE, "cave");
            assertEquals(List.of("/audio/music/title.wav", "/audio/music/maps/forest.wav",
                    "/audio/music/battle.wav", "/audio/music/maps/cave.wav", "/audio/music/title.wav"), backend.paths);
            assertTrue(backend.clips.stream().allMatch(p -> p.loop));
            assertTrue(backend.clips.subList(0, 4).stream().allMatch(p -> p.closed));
        }
        assertTrue(backend.clips.getLast().closed);
    }

    @Test void everyMapHasADistinctConventionalTrack() {
        Backend backend = new Backend();
        try (var audio = new AudioManager(backend, AudioManagerTest::resource)) {
            var director = new MusicDirector(audio);
            for (String map : List.of("house", "origen", "desert", "mall", "parkinglot", "forest", "cave")) {
                director.update(GameState.PLAY_STATE, map);
            }
            assertEquals(7, backend.paths.stream().distinct().count());
        }
    }

    @Test void effectsOverlapAndTheirVolumeDoesNotChangeMusic() {
        Backend backend = new Backend();
        var audio = new AudioManager(backend, AudioManagerTest::resource);
        audio.playMusic("/music.wav");
        audio.playEffect("/coin.wav");
        audio.playEffect("/hit.wav");
        audio.setMusicVolume(99);
        audio.setEffectsVolume(-1);
        assertEquals(5, backend.clips.getFirst().volume);
        assertEquals(0, backend.clips.get(1).volume);
        assertEquals(0, backend.clips.get(2).volume);
        assertFalse(backend.clips.get(1).closed);
        assertFalse(backend.clips.get(1).loop);
        audio.close();
        audio.close();
        audio.playMusic("/after-close.wav");
        assertEquals(3, backend.clips.size());
        assertTrue(backend.clips.stream().allMatch(p -> p.closed));
    }

    @Test void completedEffectsAreClosedAndSimultaneousEffectsAreBounded() {
        Backend backend = new Backend();
        try (var audio = new AudioManager(backend, AudioManagerTest::resource)) {
            audio.playEffect("/one.wav");
            backend.clips.getFirst().running = false;
            audio.playEffect("/two.wav");
            assertTrue(backend.clips.getFirst().closed);
            for (int i = 0; i < 30; i++) audio.playEffect("/effect.wav");
            assertEquals(16, backend.clips.stream().filter(p -> !p.closed).count());
        }
    }

    @Test void missingFilesAndUnavailableDevicesNeverCrashGameplay() {
        try (var missing = new AudioManager(url -> { fail("No device access for missing assets"); return null; }, path -> null)) {
            assertDoesNotThrow(() -> missing.playMusic("/missing.wav"));
            assertDoesNotThrow(() -> missing.playEffect("/missing.wav"));
            assertDoesNotThrow(missing::stopMusic);
            assertDoesNotThrow(() -> missing.setMusicVolume(0));
        }
        try (var unavailable = new AudioManager(url -> { throw new IllegalArgumentException("No mixer"); }, AudioManagerTest::resource)) {
            assertDoesNotThrow(() -> unavailable.playMusic("/valid.wav"));
            assertDoesNotThrow(() -> unavailable.playEffect("/valid.wav"));
        }
    }

    @Test void startupFailureClosesTheAllocatedFakePlayback() {
        var allocated = new FakePlayback() {
            @Override public void start(boolean loop) { throw new IllegalStateException("Device disconnected"); }
        };
        try (var audio = new AudioManager(url -> allocated, AudioManagerTest::resource)) {
            assertDoesNotThrow(() -> audio.playMusic("/track.wav"));
            assertTrue(allocated.closed);
        }
    }
}
