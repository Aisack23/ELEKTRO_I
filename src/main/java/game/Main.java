package game;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Desktop entry point; all Swing construction runs on the event dispatch thread. */
public final class Main {
    public static JFrame window;
    private Main() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            window = new JFrame("ELEKTRO");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(false);
            GamePanel panel = new GamePanel();
            window.add(panel);
            window.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent event) { panel.shutdown(); }
            });
            window.pack();
            window.setLocationRelativeTo(null);
            panel.setUpGame();
            window.setVisible(true);
            panel.requestFocusInWindow();
            panel.startGameThread();
        });
    }
}
