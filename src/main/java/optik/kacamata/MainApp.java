package optik.kacamata;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainApp {

    private static final Logger logger = Logger.getLogger(MainApp.class.getName());

    public static void main(String[] args) {
        try {
            DatabaseConnection.initializeDatabase();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(null,
                    "Gagal terhubung ke database MySQL!\n\n" + e.getMessage()
                    + "\n\nPastikan MySQL berjalan dan konfigurasi koneksi benar.",
                    "Error Database", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Nimbus L&F tidak tersedia, menggunakan default.", e);
        }

        SwingUtilities.invokeLater(() -> {
            FormUtama form = new FormUtama();
            form.setLocationRelativeTo(null);
            form.setVisible(true);
        });
    }
}
