import javax.swing.SwingUtilities;
import ui.LoginFrame;
import util.UIUtil;

/** Entry point of the Hostel Room Allocation System. */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UIUtil.setupLookAndFeel();
            new LoginFrame().setVisible(true);
        });
    }
}
