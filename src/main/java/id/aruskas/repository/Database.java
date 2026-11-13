package id.aruskas.repository;

import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Membuka koneksi singkat; seluruh operasi service memakai satu unit transaksi. */
public final class Database {
    private final Path lokasi;

    public Database(Path lokasi) { this.lokasi = lokasi.toAbsolutePath().normalize(); }
    public Path lokasi() { return lokasi; }

    public Connection buka() throws SQLException {
        SQLiteConfig konfigurasi = new SQLiteConfig();
        konfigurasi.enforceForeignKeys(true);
        konfigurasi.setBusyTimeout(5000);
        // Kunci penulis diperoleh sebelum membaca saldo, termasuk antar-instance aplikasi.
        konfigurasi.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
        return DriverManager.getConnection("jdbc:sqlite:" + lokasi, konfigurasi.toProperties());
    }

    @FunctionalInterface
    public interface Pekerjaan<T> { T jalankan(Connection koneksi) throws SQLException; }

    public <T> T dalamTransaksi(Pekerjaan<T> pekerjaan) throws SQLException {
        try (Connection koneksi = buka()) {
            koneksi.setAutoCommit(false);
            try {
                T hasil = pekerjaan.jalankan(koneksi);
                koneksi.commit();
                return hasil;
            } catch (SQLException | RuntimeException gagal) {
                try { koneksi.rollback(); } catch (SQLException rollback) { gagal.addSuppressed(rollback); }
                throw gagal;
            }
        }
    }

    public void inisialisasi() throws IOException, SQLException {
        Files.createDirectories(lokasi.getParent());
        dalamTransaksi(koneksi -> {
            try (Statement sql = koneksi.createStatement()) {
                sql.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS kategori (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        nama TEXT NOT NULL COLLATE NOCASE CHECK(length(trim(nama)) BETWEEN 1 AND 60),
                        jenis TEXT NOT NULL CHECK(jenis IN ('PEMASUKAN','PENGELUARAN')),
                        UNIQUE(nama, jenis)
                    )
                    """);
                sql.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS transaksi (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        kategori_id INTEGER NOT NULL REFERENCES kategori(id) ON DELETE RESTRICT,
                        nominal INTEGER NOT NULL CHECK(typeof(nominal) = 'integer' AND nominal > 0),
                        tanggal TEXT NOT NULL CHECK(length(tanggal) = 10),
                        catatan TEXT NOT NULL DEFAULT '' CHECK(length(catatan) <= 1000)
                    )
                    """);
                sql.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS anggaran_bulanan (
                        tahun INTEGER NOT NULL CHECK(tahun BETWEEN 1 AND 9999),
                        bulan INTEGER NOT NULL CHECK(bulan BETWEEN 1 AND 12),
                        batas INTEGER NOT NULL CHECK(typeof(batas) = 'integer' AND batas >= 0),
                        PRIMARY KEY(tahun, bulan)
                    )
                    """);
                sql.executeUpdate("CREATE INDEX IF NOT EXISTS idx_transaksi_tanggal ON transaksi(tanggal)");
                sql.executeUpdate("CREATE INDEX IF NOT EXISTS idx_transaksi_kategori ON transaksi(kategori_id)");
            }
            RepositoryKategori kategori = new RepositoryKategori();
            kategori.tambahBawaan(koneksi, "Gaji", "PEMASUKAN");
            kategori.tambahBawaan(koneksi, "Uang Saku", "PEMASUKAN");
            kategori.tambahBawaan(koneksi, "Lainnya", "PEMASUKAN");
            kategori.tambahBawaan(koneksi, "Makan & Minum", "PENGELUARAN");
            kategori.tambahBawaan(koneksi, "Transportasi", "PENGELUARAN");
            kategori.tambahBawaan(koneksi, "Belanja", "PENGELUARAN");
            kategori.tambahBawaan(koneksi, "Lainnya", "PENGELUARAN");
            return null;
        });
    }
}
