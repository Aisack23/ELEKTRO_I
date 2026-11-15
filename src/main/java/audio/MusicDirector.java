package audio;

import enums.GameState;
import java.util.Objects;

/** Maps game context to assets; no map or battle class knows about audio devices. */
public final class MusicDirector {
    private final AudioManager audio;
    public MusicDirector(AudioManager audio) { this.audio = Objects.requireNonNull(audio); }

    public void update(GameState state, String mapName) {
        if (state == null) return;
        String track = switch (state) {
            case BATTLE_STATE -> "/audio/music/battle.wav";
            case TITLE_STATE, CHOOSE_LEVEL_STATE, CREDITS_STATE,
                 SECRET_PROJECTS_SEQUENCE_STATE, FIRST_PROJECT_STATE,
                 SECOND_PROJECT_STATE, THIRD_PROJECT_STATE, TO_BE_CONTINUED_STATE -> "/audio/music/title.wav";
            default -> mapName == null ? null : "/audio/music/maps/" + mapName + ".wav";
        };
        audio.playMusic(track);
    }
}
