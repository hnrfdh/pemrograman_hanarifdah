package optik.kacamata;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataKacamata extends JInternalFrame {

    private static final Logger logger = Logger.getLogger(DataKacamata.class.getName());

    private JTextField kodeField, namaField, merekField, hargaField, stokField;
    private JComboBox<String> jenisCombo;
    private JButton btnTambah, btnSimpan, btnHapus, btnBatal;
    private JTable table;
    private DefaultTableModel tableModel;
    private int selectedId = -1;

    public DataKacamata() {
        super("Data Kacamata", true, true, true, true);
        initComponents();
        setSize(780, 520);
        setLocation(20, 20);
        loadData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));

        // ---- Form panel ----
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Form Data Kacamata"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 4, 8);
        g.anchor = GridBagConstraints.WEST;

        kodeField  = new JTextField(14);
        namaField  = new JTextField(14);
        merekField = new JTextField(14);
        jenisCombo = new JComboBox<>(new String[]{
            "Frame", "Lensa", "Kacamata Lengkap",
            "Kacamata Minus", "Kacamata Plus", "Kacamata Silinder"
        });
        hargaField = new JTextField(14);
        stokField  = new JTextField(14);

        addFormRow(formPanel, g, 0, "Kode Kacamata :", kodeField,  "Nama Produk :", namaField);
        addFormRow(formPanel, g, 1, "Merek :",          merekField, "Jenis :",       jenisCombo);
        addFormRow(formPanel, g, 2, "Harga (Rp) :",    hargaField, "Stok :",        stokField);

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
            new String[]{"id", "Kode", "Nama Produk", "Merek", "Jenis", "Harga", "Stok"}, 0) {
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
        merekField.setEnabled(enabled);
        jenisCombo.setEnabled(enabled);
        hargaField.setEnabled(enabled);
        stokField.setEnabled(enabled);
        btnSimpan.setEnabled(enabled);
        btnHapus.setEnabled(enabled && selectedId > 0);
        btnTambah.setEnabled(!enabled);
    }

    private void clearForm() {
        kodeField.setText("");
        namaField.setText("");
        merekField.setText("");
        jenisCombo.setSelectedIndex(0);
        hargaField.setText("");
        stokField.setText("");
    }

    private void isiFormDariTable() {
        int row = table.getSelectedRow();
        selectedId = (int) tableModel.getValueAt(row, 0);
        kodeField.setText(tableModel.getValueAt(row, 1).toString());
        namaField.setText(tableModel.getValueAt(row, 2).toString());
        merekField.setText(tableModel.getValueAt(row, 3).toString());
        jenisCombo.setSelectedItem(tableModel.getValueAt(row, 4));
        hargaField.setText(tableModel.getValueAt(row, 5).toString());
        stokField.setText(tableModel.getValueAt(row, 6).toString());
        setFormEnabled(true);
    }

    private void loadData() {
        tableModel.setRowCount(0);
        String sql = "SELECT id, kode, nama, merek, jenis, harga, stok FROM kacamata ORDER BY kode";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("kode"),
                    rs.getString("nama"),
                    rs.getString("merek"),
                    rs.getString("jenis"),
                    rs.getDouble("harga"),
                    rs.getInt("stok")
                });
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Gagal memuat data kacamata", e);
            JOptionPane.showMessageDialog(this,
                "Gagal memuat data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void simpanData() {
        String kode  = kodeField.getText().trim();
        String nama  = namaField.getText().trim();
        String merek = merekField.getText().trim();
        String jenis = (String) jenisCombo.getSelectedItem();
        String hargaStr = hargaField.getText().trim();
        String stokStr  = stokField.getText().trim();

        if (kode.isEmpty() || nama.isEmpty() || hargaStr.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Kode, Nama Produk, dan Harga wajib diisi!", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double harga;
        int stok;
        try {
            harga = Double.parseDouble(hargaStr);
            stok  = stokStr.isEmpty() ? 0 : Integer.parseInt(stokStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Harga dan Stok harus berupa angka!", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = selectedId < 0
            ? "INSERT INTO kacamata (kode, nama, merek, jenis, harga, stok) VALUES (?,?,?,?,?,?)"
            : "UPDATE kacamata SET kode=?, nama=?, merek=?, jenis=?, harga=?, stok=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kode);
            ps.setString(2, nama);
            ps.setString(3, merek);
            ps.setString(4, jenis);
            ps.setDouble(5, harga);
            ps.setInt(6, stok);
            if (selectedId > 0) ps.setInt(7, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this,
                "Data berhasil disimpan!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            setFormEnabled(false);
            table.clearSelection();
            loadData();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Gagal menyimpan data kacamata", e);
            JOptionPane.showMessageDialog(this,
                "Gagal menyimpan: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void hapusData() {
        if (selectedId < 0) return;
        int confirm = JOptionPane.showConfirmDialog(this,
            "Yakin ingin menghapus data kacamata ini?", "Konfirmasi Hapus",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM kacamata WHERE id=?")) {
            ps.setInt(1, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this,
                "Data berhasil dihapus!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            setFormEnabled(false);
            table.clearSelection();
            loadData();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Gagal menghapus data kacamata", e);
            JOptionPane.showMessageDialog(this,
                "Gagal menghapus: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
