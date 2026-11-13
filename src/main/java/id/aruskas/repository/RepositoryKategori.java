package id.aruskas.repository;

import id.aruskas.domain.JenisTransaksi;
import id.aruskas.domain.Kategori;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class RepositoryKategori {
    public List<Kategori> semua(Connection koneksi) throws SQLException {
        List<Kategori> hasil = new ArrayList<>();
        try (PreparedStatement sql = koneksi.prepareStatement("SELECT id,nama,jenis FROM kategori ORDER BY jenis,nama COLLATE NOCASE");
             ResultSet baris = sql.executeQuery()) {
            while (baris.next()) hasil.add(baca(baris));
        }
        return List.copyOf(hasil);
    }

    public Kategori cari(Connection koneksi, long id) throws SQLException {
        try (PreparedStatement sql = koneksi.prepareStatement("SELECT id,nama,jenis FROM kategori WHERE id=?")) {
            sql.setLong(1, id);
            try (ResultSet baris = sql.executeQuery()) {
                if (!baris.next()) throw new IllegalArgumentException("Kategori tidak ditemukan. Pilih ulang kategori.");
                return baca(baris);
            }
        }
    }

    public Kategori tambah(Connection koneksi, String nama, JenisTransaksi jenis) throws SQLException {
        try (PreparedStatement sql = koneksi.prepareStatement("INSERT INTO kategori(nama,jenis) VALUES (?,?)")) {
            sql.setString(1, nama);
            sql.setString(2, jenis.name());
            sql.executeUpdate();
        }
        try (PreparedStatement sql = koneksi.prepareStatement("SELECT last_insert_rowid()"); ResultSet baris = sql.executeQuery()) {
            baris.next();
            return new Kategori(baris.getLong(1), nama, jenis);
        }
    }

    void tambahBawaan(Connection koneksi, String nama, String jenis) throws SQLException {
        try (PreparedStatement sql = koneksi.prepareStatement("INSERT OR IGNORE INTO kategori(nama,jenis) VALUES (?,?)")) {
            sql.setString(1, nama);
            sql.setString(2, jenis);
            sql.executeUpdate();
        }
    }

    private Kategori baca(ResultSet baris) throws SQLException {
        return new Kategori(baris.getLong("id"), baris.getString("nama"), JenisTransaksi.valueOf(baris.getString("jenis")));
    }
}
