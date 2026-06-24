package optik.kacamata;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {

    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());

    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DB_NAME = "db_optik";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static final String BASE_URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/?useSSL=false&serverTimezone=Asia/Jakarta&allowPublicKeyRetrieval=true";
    private static final String DB_URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME + "?useSSL=false&serverTimezone=Asia/Jakarta&allowPublicKeyRetrieval=true";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(BASE_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE DATABASE IF NOT EXISTS " + DB_NAME
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

            logger.info("Database '" + DB_NAME + "' siap.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Gagal membuat database", e);
            throw new RuntimeException("Tidak dapat terhubung ke MySQL: " + e.getMessage(), e);
        }

        String createKacamata = """
                CREATE TABLE IF NOT EXISTS kacamata (
                    id       INT AUTO_INCREMENT PRIMARY KEY,
                    kode     VARCHAR(20)  UNIQUE NOT NULL,
                    nama     VARCHAR(100) NOT NULL,
                    merek    VARCHAR(50),
                    jenis    VARCHAR(50)  DEFAULT 'Kacamata Lengkap',
                    harga    DOUBLE       NOT NULL DEFAULT 0,
                    stok     INT          DEFAULT 0
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;

        String createPelanggan = """
                CREATE TABLE IF NOT EXISTS pelanggan (
                    id           INT AUTO_INCREMENT PRIMARY KEY,
                    kode         VARCHAR(20)  UNIQUE NOT NULL,
                    nama         VARCHAR(100) NOT NULL,
                    alamat       VARCHAR(200),
                    no_telp      VARCHAR(20),
                    resep_kanan  VARCHAR(50),
                    resep_kiri   VARCHAR(50)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;

        String createTransaksi = """
                CREATE TABLE IF NOT EXISTS transaksi (
                    id           INT AUTO_INCREMENT PRIMARY KEY,
                    kode         VARCHAR(30)  UNIQUE NOT NULL,
                    tanggal      DATE         NOT NULL,
                    pelanggan_id INT          NOT NULL,
                    kacamata_id  INT          NOT NULL,
                    jumlah       INT          NOT NULL DEFAULT 1,
                    harga_satuan DOUBLE       NOT NULL,
                    total        DOUBLE       NOT NULL,
                    keterangan   VARCHAR(200),
                    FOREIGN KEY (pelanggan_id) REFERENCES pelanggan(id),
                    FOREIGN KEY (kacamata_id)  REFERENCES kacamata(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createKacamata);
            stmt.execute(createPelanggan);
            stmt.execute(createTransaksi);
            logger.info("Tabel database siap.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Gagal membuat tabel", e);
            throw new RuntimeException("Gagal inisialisasi tabel: " + e.getMessage(), e);
        }
    }
}
