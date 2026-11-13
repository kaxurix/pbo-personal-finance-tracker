package id.aruskas.repository;

import id.aruskas.domain.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Repository hanya memetakan SQL dan objek; aturan keuangan berada di service. */
public final class RepositoryTransaksi {
    public List<Transaksi> cari(Connection koneksi, FilterRiwayat filter) throws SQLException {
        StringBuilder query = new StringBuilder("""
            SELECT t.id,t.nominal,t.tanggal,t.catatan,k.id AS kategori_id,k.nama,k.jenis
            FROM transaksi t JOIN kategori k ON k.id=t.kategori_id WHERE 1=1
            """);
        if (filter.kategoriId() != null) query.append(" AND t.kategori_id=?");
        if (filter.tanggalAwal() != null) query.append(" AND t.tanggal>=?");
        if (filter.tanggalAkhir() != null) query.append(" AND t.tanggal<=?");
        query.append(" ORDER BY t.tanggal DESC,t.id DESC");
        List<Transaksi> hasil = new ArrayList<>();
        try (PreparedStatement sql = koneksi.prepareStatement(query.toString())) {
            int indeks = 1;
            if (filter.kategoriId() != null) sql.setLong(indeks++, filter.kategoriId());
            if (filter.tanggalAwal() != null) sql.setString(indeks++, filter.tanggalAwal().toString());
            if (filter.tanggalAkhir() != null) sql.setString(indeks, filter.tanggalAkhir().toString());
            try (ResultSet baris = sql.executeQuery()) {
                while (baris.next()) {
                    Kategori kategori = new Kategori(baris.getLong("kategori_id"), baris.getString("nama"),
                            JenisTransaksi.valueOf(baris.getString("jenis")));
                    hasil.add(buat(baris.getLong("id"), baris.getLong("nominal"), kategori,
                            LocalDate.parse(baris.getString("tanggal")), baris.getString("catatan")));
                }
            }
        }
        return List.copyOf(hasil);
    }

    public Transaksi simpan(Connection koneksi, Transaksi transaksi) throws SQLException {
        try (PreparedStatement sql = koneksi.prepareStatement("INSERT INTO transaksi(kategori_id,nominal,tanggal,catatan) VALUES (?,?,?,?)")) {
            sql.setLong(1, transaksi.getKategori().id());
            sql.setLong(2, transaksi.getNominal());
            sql.setString(3, transaksi.getTanggal().toString());
            sql.setString(4, transaksi.getCatatan());
            sql.executeUpdate();
        }
        try (PreparedStatement sql = koneksi.prepareStatement("SELECT last_insert_rowid()"); ResultSet baris = sql.executeQuery()) {
            baris.next();
            return buat(baris.getLong(1), transaksi.getNominal(), transaksi.getKategori(), transaksi.getTanggal(), transaksi.getCatatan());
        }
    }

    public static Transaksi buat(long id, long nominal, Kategori kategori, LocalDate tanggal, String catatan) {
        return kategori.jenis() == JenisTransaksi.PEMASUKAN
                ? new Pemasukan(id, nominal, kategori, tanggal, catatan)
                : new Pengeluaran(id, nominal, kategori, tanggal, catatan);
    }
}
