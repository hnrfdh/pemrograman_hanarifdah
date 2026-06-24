package optik.kacamata;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataTransaksi extends JInternalFrame {

    private static final Logger logger = Logger.getLogger(DataTransaksi.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Form fields
    private JTextField kodeField, tanggalField, hargaField, totalField, keteranganField;
    private JSpinner jumlahSpinner;
    private JComboBox<ComboItem> pelangganCombo, kacamataCombo;
    private JButton btnTambah, btnSimpan, btnHapus, btnBatal;

    // Table
    private JTable table;
    private DefaultTableModel tableModel;

    // State
    private int selectedId      = -1;
    private int oldKacamataId   = -1;
    private int oldJumlah       = 0;

    public DataTransaksi() {
        super("Data Transaksi", true, true, true, true);
        initComponents();
        setSize(860, 540);
        setLocation(40, 40);
        loadData();
    }

    // -------------------------------------------------------------------------
    // UI setup
    // -------------------------------------------------------------------------

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Form Transaksi Penjualan"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets  = new Insets(4, 8, 4, 8);
        g.anchor  = GridBagConstraints.WEST;
        g.fill    = GridBagConstraints.HORIZONTAL;

        // --- Row 0: Kode | Tanggal ---
        kodeField    = new JTextField(14); kodeField.setEditable(false);
        tanggalField = new JTextField(14);

        g.gridy = 0; g.gridx = 0; formPanel.add(new JLabel("Kode Transaksi :"), g);
        g.gridx = 1; formPanel.add(kodeField, g);
        g.gridx = 2; formPanel.add(new JLabel("Tanggal (yyyy-MM-dd) :"), g);
        g.gridx = 3; formPanel.add(tanggalField, g);

        // --- Row 1: Pelanggan (spans 3 cols) ---
        pelangganCombo = new JComboBox<>();
        pelangganCombo.setPreferredSize(new Dimension(400, 24));

        g.gridy = 1; g.gridx = 0; formPanel.add(new JLabel("Pelanggan :"), g);
        g.gridx = 1; g.gridwidth = 3; formPanel.add(pelangganCombo, g);
        g.gridwidth = 1;

        // --- Row 2: Kacamata (spans 3 cols) ---
        kacamataCombo = new JComboBox<>();
        kacamataCombo.setPreferredSize(new Dimension(400, 24));

        g.gridy = 2; g.gridx = 0; formPanel.add(new JLabel("Kacamata :"), g);
        g.gridx = 1; g.gridwidth = 3; formPanel.add(kacamataCombo, g);
        g.gridwidth = 1;

        // --- Row 3: Jumlah | Harga Satuan | Total ---
        jumlahSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        hargaField    = new JTextField(12); hargaField.setEditable(false);
        totalField    = new JTextField(12); totalField.setEditable(false);

        g.gridy = 3; g.gridx = 0; formPanel.add(new JLabel("Jumlah :"), g);
        g.gridx = 1; formPanel.add(jumlahSpinner, g);
        g.gridx = 2; formPanel.add(new JLabel("Harga Satuan (Rp) :"), g);
        g.gridx = 3; formPanel.add(hargaField, g);

        // --- Row 4: Total | Keterangan ---
        keteranganField = new JTextField(14);

        g.gridy = 4; g.gridx = 0; formPanel.add(new JLabel("Total (Rp) :"), g);
        g.gridx = 1; formPanel.add(totalField, g);
        g.gridx = 2; formPanel.add(new JLabel("Keterangan :"), g);
        g.gridx = 3; formPanel.add(keteranganField, g);

        // --- Buttons ---
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        btnTambah = new JButton("Tambah Baru");
        btnSimpan = new JButton("Simpan");
        btnHapus  = new JButton("Hapus");
        btnBatal  = new JButton("Batal");
        btnPanel.add(btnTambah); btnPanel.add(btnSimpan);
        btnPanel.add(btnHapus);  btnPanel.add(btnBatal);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(formPanel, BorderLayout.CENTER);
        topPanel.add(btnPanel,  BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // --- Table ---
        // hidden cols: 0=id, 1=pelanggan_id, 2=kacamata_id
        tableModel = new DefaultTableModel(
            new String[]{"id","pid","kid","Kode","Tanggal","Pelanggan","Kacamata",
                         "Jumlah","Harga Satuan","Total","Keterangan"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);
        hideColumns(0, 1, 2);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- Initial state ---
        setFormEnabled(false);
        loadCombos();

        // --- Listeners ---
        btnTambah.addActionListener(e -> mulaiTambah());
        btnSimpan.addActionListener(e -> simpanData());
        btnHapus.addActionListener(e  -> hapusData());
        btnBatal.addActionListener(e  -> {
            clearForm(); setFormEnabled(false); table.clearSelection();
        });

        kacamataCombo.addActionListener(e -> recalcTotal());
        jumlahSpinner.addChangeListener(e -> recalcTotal());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0)
                isiFormDariTable();
        });
    }

    private void hideColumns(int... cols) {
        for (int c : cols) {
            var col = table.getColumnModel().getColumn(c);
            col.setMinWidth(0); col.setMaxWidth(0); col.setPreferredWidth(0);
        }
    }

    // -------------------------------------------------------------------------
    // State management
    // -------------------------------------------------------------------------

    private void setFormEnabled(boolean on) {
        tanggalField.setEnabled(on);
        pelangganCombo.setEnabled(on);
        kacamataCombo.setEnabled(on);
        jumlahSpinner.setEnabled(on);
        keteranganField.setEnabled(on);
        btnSimpan.setEnabled(on);
        btnHapus.setEnabled(on && selectedId > 0);
        btnTambah.setEnabled(!on);
    }

    private void clearForm() {
        kodeField.setText("");
        tanggalField.setText("");
        hargaField.setText("");
        totalField.setText("");
        keteranganField.setText("");
        jumlahSpinner.setValue(1);
        if (pelangganCombo.getItemCount() > 0) pelangganCombo.setSelectedIndex(0);
        if (kacamataCombo.getItemCount()  > 0) kacamataCombo.setSelectedIndex(0);
        selectedId = -1; oldKacamataId = -1; oldJumlah = 0;
    }

    private void mulaiTambah() {
        selectedId = -1; oldKacamataId = -1; oldJumlah = 0;
        loadCombos();
        clearForm();
        kodeField.setText(generateKode());
        tanggalField.setText(LocalDate.now().format(DATE_FMT));
        setFormEnabled(true);
        recalcTotal();
        tanggalField.requestFocus();
    }

    private void isiFormDariTable() {
        int row = table.getSelectedRow();
        selectedId    = (int) tableModel.getValueAt(row, 0);
        int pelId     = (int) tableModel.getValueAt(row, 1);
        int kacId     = (int) tableModel.getValueAt(row, 2);
        oldKacamataId = kacId;
        oldJumlah     = (int) tableModel.getValueAt(row, 7);

        loadCombos();
        kodeField.setText(tableModel.getValueAt(row, 3).toString());
        tanggalField.setText(tableModel.getValueAt(row, 4).toString());
        selectComboById(pelangganCombo, pelId);
        selectComboById(kacamataCombo,  kacId);
        jumlahSpinner.setValue(oldJumlah);
        keteranganField.setText(nvl(tableModel.getValueAt(row, 10)));
        recalcTotal();
        setFormEnabled(true);
    }

    private void recalcTotal() {
        ComboItem kac = (ComboItem) kacamataCombo.getSelectedItem();
        if (kac == null) { hargaField.setText("0"); totalField.setText("0"); return; }
        int jumlah = (int) jumlahSpinner.getValue();
        hargaField.setText(String.format("%,.0f", kac.harga));
        totalField.setText(String.format("%,.0f", jumlah * kac.harga));
    }

    // -------------------------------------------------------------------------
    // Combo loading helpers
    // -------------------------------------------------------------------------

    private void loadCombos() {
        loadPelangganCombo();
        loadKacamataCombo();
    }

    private void loadPelangganCombo() {
        Object prev = pelangganCombo.getSelectedItem();
        pelangganCombo.removeAllItems();
        String sql = "SELECT id, kode, nama FROM pelanggan ORDER BY nama";
        try (Connection c = DatabaseConnection.getConnection();
             Statement s  = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next())
                pelangganCombo.addItem(new ComboItem(
                    rs.getInt("id"),
                    rs.getString("kode") + " – " + rs.getString("nama"), 0));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Gagal load pelanggan", e);
        }
        if (prev instanceof ComboItem p) selectComboById(pelangganCombo, p.id);
    }

    private void loadKacamataCombo() {
        Object prev = kacamataCombo.getSelectedItem();
        kacamataCombo.removeAllItems();
        String sql = "SELECT id, kode, nama, harga, stok FROM kacamata ORDER BY nama";
        try (Connection c = DatabaseConnection.getConnection();
             Statement s  = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next())
                kacamataCombo.addItem(new ComboItem(
                    rs.getInt("id"),
                    rs.getString("kode") + " – " + rs.getString("nama")
                        + "  [Stok: " + rs.getInt("stok") + "]",
                    rs.getDouble("harga")));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Gagal load kacamata", e);
        }
        if (prev instanceof ComboItem p) selectComboById(kacamataCombo, p.id);
    }

    private void selectComboById(JComboBox<ComboItem> combo, int id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).id == id) { combo.setSelectedIndex(i); return; }
        }
    }

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    private void loadData() {
        tableModel.setRowCount(0);
        String sql = """
                SELECT t.id, t.pelanggan_id, t.kacamata_id,
                       t.kode, t.tanggal,
                       p.nama AS nama_pelanggan,
                       k.nama AS nama_kacamata,
                       t.jumlah, t.harga_satuan, t.total, t.keterangan
                FROM transaksi t
                JOIN pelanggan p ON p.id = t.pelanggan_id
                JOIN kacamata  k ON k.id = t.kacamata_id
                ORDER BY t.tanggal DESC, t.kode DESC
                """;
        try (Connection c = DatabaseConnection.getConnection();
             Statement s  = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next())
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getInt("pelanggan_id"),
                    rs.getInt("kacamata_id"),
                    rs.getString("kode"),
                    rs.getString("tanggal"),
                    rs.getString("nama_pelanggan"),
                    rs.getString("nama_kacamata"),
                    rs.getInt("jumlah"),
                    rs.getDouble("harga_satuan"),
                    rs.getDouble("total"),
                    nvl(rs.getString("keterangan"))
                });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Gagal memuat data transaksi", e);
            JOptionPane.showMessageDialog(this,
                "Gagal memuat data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void simpanData() {
        // Validate inputs
        String kode = kodeField.getText().trim();
        String tanggalStr = tanggalField.getText().trim();
        ComboItem pelItem = (ComboItem) pelangganCombo.getSelectedItem();
        ComboItem kacItem = (ComboItem) kacamataCombo.getSelectedItem();
        int jumlah = (int) jumlahSpinner.getValue();
        String ket = keteranganField.getText().trim();

        if (pelItem == null || kacItem == null) {
            JOptionPane.showMessageDialog(this,
                "Pelanggan dan Kacamata harus dipilih!", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.sql.Date tanggal;
        try {
            tanggal = java.sql.Date.valueOf(LocalDate.parse(tanggalStr, DATE_FMT));
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                "Format tanggal salah! Gunakan: yyyy-MM-dd\nContoh: 2026-06-23",
                "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double harga = kacItem.harga;
        double total = jumlah * harga;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (selectedId < 0) {
                    insertTransaksi(conn, kode, tanggal, pelItem.id, kacItem.id,
                                    jumlah, harga, total, ket);
                } else {
                    updateTransaksi(conn, kode, tanggal, pelItem.id, kacItem.id,
                                    jumlah, harga, total, ket);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Gagal menyimpan transaksi", e);
            JOptionPane.showMessageDialog(this,
                "Gagal menyimpan: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
            "Transaksi berhasil disimpan!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
        clearForm();
        setFormEnabled(false);
        table.clearSelection();
        loadData();
        loadKacamataCombo(); // refresh stok
    }

    private void insertTransaksi(Connection conn, String kode, java.sql.Date tanggal,
                                  int pelId, int kacId, int jumlah,
                                  double harga, double total, String ket) throws SQLException {
        // Check stok
        int stok = getStok(conn, kacId);
        if (stok < jumlah) {
            throw new SQLException("Stok tidak cukup! Stok tersedia: " + stok + ", diminta: " + jumlah);
        }

        String ins = "INSERT INTO transaksi (kode,tanggal,pelanggan_id,kacamata_id,jumlah,harga_satuan,total,keterangan) "
                   + "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(ins)) {
            ps.setString(1, kode);
            ps.setDate(2, tanggal);
            ps.setInt(3, pelId);
            ps.setInt(4, kacId);
            ps.setInt(5, jumlah);
            ps.setDouble(6, harga);
            ps.setDouble(7, total);
            ps.setString(8, ket.isEmpty() ? null : ket);
            ps.executeUpdate();
        }
        kurangiStok(conn, kacId, jumlah);
    }

    private void updateTransaksi(Connection conn, String kode, java.sql.Date tanggal,
                                  int pelId, int kacId, int jumlah,
                                  double harga, double total, String ket) throws SQLException {
        // Restore stok lama dulu
        tambahStok(conn, oldKacamataId, oldJumlah);

        // Cek stok baru (setelah restore)
        int stok = getStok(conn, kacId);
        if (stok < jumlah) {
            // Rollback restore jika gagal validasi — lempar exception, conn.rollback() di caller
            throw new SQLException("Stok tidak cukup! Stok tersedia: " + stok + ", diminta: " + jumlah);
        }

        String upd = "UPDATE transaksi SET kode=?,tanggal=?,pelanggan_id=?,kacamata_id=?,"
                   + "jumlah=?,harga_satuan=?,total=?,keterangan=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(upd)) {
            ps.setString(1, kode);
            ps.setDate(2, tanggal);
            ps.setInt(3, pelId);
            ps.setInt(4, kacId);
            ps.setInt(5, jumlah);
            ps.setDouble(6, harga);
            ps.setDouble(7, total);
            ps.setString(8, ket.isEmpty() ? null : ket);
            ps.setInt(9, selectedId);
            ps.executeUpdate();
        }
        kurangiStok(conn, kacId, jumlah);
    }

    private void hapusData() {
        if (selectedId < 0) return;
        int confirm = JOptionPane.showConfirmDialog(this,
            "Yakin ingin menghapus transaksi ini?\nStok kacamata akan dikembalikan.",
            "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Ambil kacamata_id dan jumlah dari transaksi
                int[] info = getTransaksiKacamataJumlah(conn, selectedId);
                tambahStok(conn, info[0], info[1]);

                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM transaksi WHERE id=?")) {
                    ps.setInt(1, selectedId);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Gagal menghapus transaksi", e);
            JOptionPane.showMessageDialog(this,
                "Gagal menghapus: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
            "Transaksi berhasil dihapus!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
        clearForm();
        setFormEnabled(false);
        table.clearSelection();
        loadData();
        loadKacamataCombo();
    }

    // -------------------------------------------------------------------------
    // Stock helpers
    // -------------------------------------------------------------------------

    private int getStok(Connection conn, int kacamataId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT stok FROM kacamata WHERE id=?")) {
            ps.setInt(1, kacamataId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void kurangiStok(Connection conn, int kacamataId, int jumlah) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE kacamata SET stok = stok - ? WHERE id=?")) {
            ps.setInt(1, jumlah);
            ps.setInt(2, kacamataId);
            ps.executeUpdate();
        }
    }

    private void tambahStok(Connection conn, int kacamataId, int jumlah) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE kacamata SET stok = stok + ? WHERE id=?")) {
            ps.setInt(1, jumlah);
            ps.setInt(2, kacamataId);
            ps.executeUpdate();
        }
    }

    private int[] getTransaksiKacamataJumlah(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT kacamata_id, jumlah FROM transaksi WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return new int[]{rs.getInt("kacamata_id"), rs.getInt("jumlah")};
        }
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private String generateKode() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "TRX-" + today + "-";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT COUNT(*) FROM transaksi WHERE kode LIKE ?")) {
            ps.setString(1, prefix + "%");
            ResultSet rs = ps.executeQuery();
            rs.next();
            return String.format("%s%03d", prefix, rs.getInt(1) + 1);
        } catch (SQLException e) {
            return "TRX-" + System.currentTimeMillis();
        }
    }

    private String nvl(Object v) { return v == null ? "" : v.toString(); }

    // -------------------------------------------------------------------------
    // Inner class
    // -------------------------------------------------------------------------

    static class ComboItem {
        final int    id;
        final String label;
        final double harga;

        ComboItem(int id, String label, double harga) {
            this.id    = id;
            this.label = label;
            this.harga = harga;
        }

        @Override public String toString() { return label; }
    }
}
