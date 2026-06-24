package optik.kacamata;

import javax.swing.*;
import java.awt.*;

public class FormUtama extends JFrame {

    private JDesktopPane desktopPane;

    public FormUtama() {
        super("Sistem Informasi Toko Optik Kacamata");
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 640);

        desktopPane = new JDesktopPane();
        desktopPane.setBackground(new Color(30, 80, 130));
        add(desktopPane, BorderLayout.CENTER);

        JLabel titleLabel = new JLabel("TOKO OPTIK KACAMATA", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setOpaque(true);
        titleLabel.setBackground(new Color(20, 60, 100));
        titleLabel.setPreferredSize(new Dimension(0, 40));
        add(titleLabel, BorderLayout.NORTH);

        JMenuBar menuBar = new JMenuBar();

        JMenu masterMenu = new JMenu("Master Data");
        masterMenu.setMnemonic('M');

        JMenuItem kacamataItem = new JMenuItem("Data Kacamata");
        kacamataItem.setMnemonic('K');
        kacamataItem.addActionListener(e -> bukaDataKacamata());

        JMenuItem pelangganItem = new JMenuItem("Data Pelanggan");
        pelangganItem.setMnemonic('P');
        pelangganItem.addActionListener(e -> bukaDataPelanggan());

        masterMenu.add(kacamataItem);
        masterMenu.add(pelangganItem);
        menuBar.add(masterMenu);

        JMenu transaksiMenu = new JMenu("Transaksi");
        transaksiMenu.setMnemonic('T');
        JMenuItem transaksiItem = new JMenuItem("Data Transaksi");
        transaksiItem.setMnemonic('T');
        transaksiItem.addActionListener(e -> bukaDataTransaksi());
        transaksiMenu.add(transaksiItem);
        menuBar.add(transaksiMenu);

        JMenu helpMenu = new JMenu("Bantuan");
        JMenuItem tentangItem = new JMenuItem("Tentang Aplikasi");
        tentangItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Sistem Informasi Toko Optik Kacamata\nVersi 1.0\n\nFitur:\n- Manajemen Data Kacamata\n- Manajemen Data Pelanggan",
                "Tentang", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(tentangItem);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void bukaDataKacamata() {
        for (JInternalFrame f : desktopPane.getAllFrames()) {
            if (f instanceof DataKacamata) {
                f.toFront();
                try { f.setSelected(true); } catch (Exception ignored) {}
                return;
            }
        }
        DataKacamata frame = new DataKacamata();
        desktopPane.add(frame);
        frame.setVisible(true);
        try { frame.setSelected(true); } catch (Exception ignored) {}
    }

    private void bukaDataPelanggan() {
        for (JInternalFrame f : desktopPane.getAllFrames()) {
            if (f instanceof DataPelanggan) {
                f.toFront();
                try { f.setSelected(true); } catch (Exception ignored) {}
                return;
            }
        }
        DataPelanggan frame = new DataPelanggan();
        desktopPane.add(frame);
        frame.setVisible(true);
        try { frame.setSelected(true); } catch (Exception ignored) {}
    }

    private void bukaDataTransaksi() {
        for (JInternalFrame f : desktopPane.getAllFrames()) {
            if (f instanceof DataTransaksi) {
                f.toFront();
                try { f.setSelected(true); } catch (Exception ignored) {}
                return;
            }
        }
        DataTransaksi frame = new DataTransaksi();
        desktopPane.add(frame);
        frame.setVisible(true);
        try { frame.setSelected(true); } catch (Exception ignored) {}
    }
}
