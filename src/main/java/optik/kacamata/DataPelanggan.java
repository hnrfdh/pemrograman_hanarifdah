package optik.kacamata;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataPelanggan extends JInternalFrame {

    private static final Logger logger = Logger.getLogger(DataPelanggan.class.getName());

    private JTextField kodeField, namaField, alamatField, noTelpField,
                       resepKananField, resepKiriField;
    private JButton btnTambah, btnSimpan, btnHapus, btnBatal;
    private JTable table;
    private DefaultTableModel tableModel;
    private int selectedId = -1;

    public DataPelanggan() {
        super("Data Pelanggan", true, true, true, true);
        initComponents();
        setSize(820, 520);
        setLocation(60, 60);
        loadData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));

        // ---- Form panel ----
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Form Data Pelanggan"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 4, 8);
        g.anchor = GridBagConstraints.WEST;

        kodeField       = new JTextField(14);
        namaField       = new JTextField(14);
        alamatField     = new JTextField(14);
        noTelpField     = new JTextField(14);
        resepKananField = new JTextField(14);
        resepKiriField  = new JTextField(14);

        addFormRow(formPanel, g, 0, "Kode Pelanggan :", kodeField,       "Nama Pelanggan :", namaField);
        addFormRow(formPanel, g, 1, "Alamat :",          alamatField,     "No. Telepon :",   noTelpField);
        addFormRow(formPanel, g, 2, "Resep Mata Kanan (R) :", resepKananField,
                                    "Resep Mata Kiri (L) :",  resepKiriField);

        JLabel resepHint = new JLabel("  * Contoh resep: -1.50 / +2.25 / S-1.00 C-0.50");
        resepHint.setFont(resepHint.getFont().deriveFont(Font.ITALIC, 11f));
        resepHint.setForeground(Color.GRAY);
        GridBagConstraints gh = new GridBagConstraints();
        gh.gridx = 0; gh.gridy = 3; gh.gridwidth = 4; gh.anchor = GridBagConstraints.WEST;
        gh.insets = new Insets(0, 8, 4, 8);
        formPanel.add(resepHint, gh);

        // ---- Button panel ----
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        btnTambah = new JButton("Tambah Baru");
        btnSimpan = new JButton("Simpan");
        btnHapus  = new JButton("Hapus");
        btnBatal  = new JButton("Batal");
        buttonPanel.add(btnTambah);
        buttonPanel.add(btnSimpan);
        buttonPanel.add(btnHapus);
        buttonPanel.add(btnBatal);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(formPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // ---- Table ----
        tableModel = new DefaultTableModel(
            new String[]{"id", "Kode", "Nama", "Alamat", "No. Telp", "Resep Kanan (R)", "Resep Kiri (L)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);
        hideIdColumn();
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ---- Initial state ----
        setFormEnabled(false);

        // ---- Listeners ----
        btnTambah.addActionListener(e -> {
            selectedId = -1;
            clearForm();
            setFormEnabled(true);
            kodeField.requestFocus();
        });

        btnSimpan.addActionListener(e -> simpanData());
        btnHapus.addActionListener(e -> hapusData());

        btnBatal.addActionListener(e -> {
            clearForm();
            setFormEnabled(false);
            table.clearSelection();
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                isiFormDariTable();
            }
        });
    }

    private void addFormRow(JPanel p, GridBagConstraints g,
                            int row, String label1, Component comp1,
                            String label2, Component comp2) {
        g.gridy = row;
        g.gridx = 0; p.add(new JLabel(label1), g);
        g.gridx = 1; p.add(comp1, g);
        g.gridx = 2; p.add(new JLabel(label2), g);
        g.gridx = 3; p.add(comp2, g);
    }

    private void hideIdColumn() {
        var col = table.getColumnModel().getColumn(0);
        col.setMinWidth(0); col.setMaxWidth(0); col.setPreferredWidth(0);
    }

    private void setFormEnabled(boolean enabled) {
        kodeField.setEnabled(enabled);
        namaField.setEnabled(enabled);
        alamatField.setEnabled(enabled);
        noTelpField.setEnabled(enabled);
        resepKananField.setEnabled(enabled);
        resepKiriField.setEnabled(enabled);
        btnSimpan.setEnabled(enabled);
        btnHapus.setEnabled(enabled && selectedId > 0);
        btnTambah.setEnabled(!enabled);
    }

    private void clearForm() {
        kodeField.setText("");
        namaField.setText("");
        alamatField.setText("");
        noTelpField.setText("");
        resepKananField.setText("");
        resepKiriField.setText("");
    }

    private void isiFormDariTable() {
        int row = table.getSelectedRow();
        selectedId = (int) tableModel.getValueAt(row, 0);
        kodeField.setText(tableModel.getValueAt(row, 1).toString());
        namaField.setText(tableModel.getValueAt(row, 2).toString());
        alamatField.setText(nvl(tableModel.getValueAt(row, 3)));
        noTelpField.setText(nvl(tableModel.getValueAt(row, 4)));
        resepKananField.setText(nvl(tableModel.getValueAt(row, 5)));
        resepKiriField.setText(nvl(tableModel.getValueAt(row, 6)));
        setFormEnabled(true);
    }

    private String nvl(Object val) {
        return val == null ? "" : val.toString();
    }

    private void loadData() {
        tableModel.setRowCount(0);
        String sql = "SELECT id, kode, nama, alamat, no_telp, resep_kanan, resep_kiri "
                   + "FROM pelanggan ORDER BY kode";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("kode"),
                    rs.getString("nama"),
                    rs.getString("alamat"),
                    rs.getString("no_telp"),
                    rs.getString("resep_kanan"),
                    rs.getString("resep_kiri")
                });
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Gagal memuat data pelanggan", e);
            JOptionPane.showMessageDialog(this,
                "Gagal memuat data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void simpanData() {
        String kode       = kodeField.getText().trim();
        String nama       = namaField.getText().trim();
        String alamat     = alamatField.getText().trim();
        String noTelp     = noTelpField.getText().trim();
        String resepKanan = resepKananField.getText().trim();
        String resepKiri  = resepKiriField.getText().trim();

        if (kode.isEmpty() || nama.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Kode dan Nama Pelanggan wajib diisi!", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = selectedId < 0
            ? "INSERT INTO pelanggan (kode, nama, alamat, no_telp, resep_kanan, resep_kiri) VALUES (?,?,?,?,?,?)"
            : "UPDATE pelanggan SET kode=?, nama=?, alamat=?, no_telp=?, resep_kanan=?, resep_kiri=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kode);
            ps.setString(2, nama);
            ps.setString(3, alamat.isEmpty() ? null : alamat);
            ps.setString(4, noTelp.isEmpty() ? null : noTelp);
            ps.setString(5, resepKanan.isEmpty() ? null : resepKanan);
            ps.setString(6, resepKiri.isEmpty() ? null : resepKiri);
            if (selectedId > 0) ps.setInt(7, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this,
                "Data berhasil disimpan!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            setFormEnabled(false);
            table.clearSelection();
            loadData();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Gagal menyimpan data pelanggan", e);
            JOptionPane.showMessageDialog(this,
                "Gagal menyimpan: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void hapusData() {
        if (selectedId < 0) return;
        int confirm = JOptionPane.showConfirmDialog(this,
            "Yakin ingin menghapus data pelanggan ini?", "Konfirmasi Hapus",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM pelanggan WHERE id=?")) {
            ps.setInt(1, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this,
                "Data berhasil dihapus!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            setFormEnabled(false);
            table.clearSelection();
            loadData();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Gagal menghapus data pelanggan", e);
            JOptionPane.showMessageDialog(this,
                "Gagal menghapus: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
