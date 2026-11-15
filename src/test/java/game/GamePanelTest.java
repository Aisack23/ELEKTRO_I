package game;

import audio.AudioManager;
import enums.GameState;
import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class GamePanelTest {
    @Test void initializesRendersAndTraversesEveryMapWithoutASoundDevice() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var panel = new GamePanel(new AudioManager(url -> { throw new IllegalArgumentException("No sound device"); }, GamePanelTest.class::getResource));
            try {
                panel.setUpGame();
                assertEquals(GameState.TITLE_STATE, panel.getGameState());
                panel.setSize(panel.getPreferredSize());
                BufferedImage screen = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
                var graphics = screen.createGraphics();
                try { panel.paint(graphics); } finally { graphics.dispose(); }
                assertNotEquals(0, screen.getRGB(10, 10));
                panel.getCurrentLevel().play();
                panel.setGameState(GameState.PLAY_STATE);
                var visited = new ArrayList<String>();
                for (int i = 0; i < 7; i++) {
                    visited.add(panel.getCurrentMap().getMapName());
                    panel.setGameState(GameState.BATTLE_STATE);
                    panel.setGameState(GameState.PLAY_STATE);
                    panel.finishGame();
                }
                assertEquals(java.util.List.of("house", "origen", "desert", "mall", "parkinglot", "forest", "cave"), visited);
                assertEquals(GameState.FINAL_CINEMATIC_FINISHED_STATE, panel.getGameState());
            } finally { panel.shutdown(); }
        });
    }
}
