package optik.kacamata;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Screenshot generator — runs entirely on the EDT via javax.swing.Timer.
 * Jalankan: mvn exec:java -Dexec.mainClass=optik.kacamata.ScreenshotGen
 */
public class ScreenshotGen {

    static final String OUTDIR = System.getProperty("user.dir") + "/target/screenshots/";
    static FormUtama   mainForm;
    static JDesktopPane desktop;

    public static void main(String[] args) throws Exception {
        DatabaseConnection.initializeDatabase();
        new File(OUTDIR).mkdirs();

        // Set LookAndFeel before anything (can be done on any thread)
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        // Launch everything from EDT using invokeLater (same as MainApp)
        SwingUtilities.invokeLater(ScreenshotGen::startCapture);
    }

    static void startCapture() {
        // Create and show main form
        mainForm = new FormUtama();
        mainForm.setLocation(60, 40);
        mainForm.setVisible(true);
        desktop = findDesktopPane(mainForm);

        // Steps: each step shows a different form, waits 800ms, captures
        AtomicInteger step = new AtomicInteger(0);
        Timer timer = new Timer(900, null);
        timer.addActionListener((ActionEvent e) -> {
            try {
                switch (step.getAndIncrement()) {
                    case 0 -> {
                        // Form Utama - already visible
                        capture("01_form_utama");
                    }
                    case 1 -> {
                        // Data Kacamata - list
                        clearDesktop();
                        DataKacamata dk = new DataKacamata();
                        dk.setLocation(5, 5);
                        desktop.add(dk); dk.setVisible(true);
                        try { dk.setSelected(true); } catch (Exception ex) {}
                    }
                    case 2 -> capture("02_data_kacamata_list")  ;
                    case 3 -> {
                        // Data Kacamata - tambah baru
                        JButton btn = findButton(desktop, "Tambah Baru");
                        if (btn != null) btn.doClick();
                    }
                    case 4 -> capture("03_data_kacamata_tambah");
                    case 5 -> {
                        // Data Pelanggan - list
                        clearDesktop();
                        DataPelanggan dp = new DataPelanggan();
                        dp.setLocation(5, 5);
                        desktop.add(dp); dp.setVisible(true);
                        try { dp.setSelected(true); } catch (Exception ex) {}
                    }
                    case 6 -> capture("04_data_pelanggan_list");
                    case 7 -> {
                        // Data Transaksi - list
                        clearDesktop();
                        DataTransaksi dt = new DataTransaksi();
                        dt.setLocation(5, 5);
                        desktop.add(dt); dt.setVisible(true);
                        try { dt.setSelected(true); } catch (Exception ex) {}
                    }
                    case 8 -> capture("05_data_transaksi_list");
                    case 9 -> {
                        // Data Transaksi - tambah baru
                        JButton btn = findButton(desktop, "Tambah Baru");
                        if (btn != null) btn.doClick();
                    }
                    case 10 -> {
                        capture("06_data_transaksi_tambah");
                        timer.stop();
                        System.err.println("=== Semua screenshot selesai ===");
                        File dir = new File(OUTDIR);
                        File[] files = dir.listFiles((d, n) -> n.endsWith(".png"));
                        if (files != null)
                            for (File f : files)
                                System.err.println("  " + f.getName() + " (" + f.length() + " bytes)");
                        System.exit(0);
                    }
                }
            } catch (Exception ex) {
                System.err.println("Timer error step " + step + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        });
        timer.setRepeats(true);
        timer.start();
    }

    // ── capture via paintAll ──────────────────────────────────────────────────

    static void capture(String name) {
        mainForm.repaint();
        int w = mainForm.getWidth();
        int h = mainForm.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        mainForm.paintAll(g);
        g.dispose();
        try {
            File out = new File(OUTDIR + name + ".png");
            ImageIO.write(img, "png", out);
            System.err.println("Saved: " + name + " (" + out.length() + " bytes)");
        } catch (Exception e) {
            System.err.println("Save error " + name + ": " + e.getMessage());
        }
    }

    static void clearDesktop() {
        for (JInternalFrame f : desktop.getAllFrames()) {
            f.setVisible(false);
            desktop.remove(f);
        }
        desktop.repaint();
    }

    static JDesktopPane findDesktopPane(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JDesktopPane dp) return dp;
            if (comp instanceof Container sub) {
                JDesktopPane found = findDesktopPane(sub);
                if (found != null) return found;
            }
        }
        return null;
    }

    static JButton findButton(Container c, String text) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JButton btn && text.equals(btn.getText())) return btn;
            if (comp instanceof Container sub) {
                JButton f = findButton(sub, text);
                if (f != null) return f;
            }
        }
        return null;
    }
}
