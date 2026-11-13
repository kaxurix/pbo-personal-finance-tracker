package id.aruskas.repository;

import id.aruskas.domain.AnggaranBulanan;

import java.sql.*;
import java.time.YearMonth;
import java.util.Optional;

public final class RepositoryAnggaran {
    public Optional<AnggaranBulanan> cari(Connection koneksi, YearMonth periode) throws SQLException {
        try (PreparedStatement sql = koneksi.prepareStatement("SELECT batas FROM anggaran_bulanan WHERE tahun=? AND bulan=?")) {
            sql.setInt(1, periode.getYear());
            sql.setInt(2, periode.getMonthValue());
            try (ResultSet baris = sql.executeQuery()) {
                return baris.next() ? Optional.of(new AnggaranBulanan(periode, baris.getLong("batas"))) : Optional.empty();
            }
        }
    }

    public void simpan(Connection koneksi, AnggaranBulanan anggaran) throws SQLException {
        try (PreparedStatement sql = koneksi.prepareStatement("""
            INSERT INTO anggaran_bulanan(tahun,bulan,batas) VALUES (?,?,?)
            ON CONFLICT(tahun,bulan) DO UPDATE SET batas=excluded.batas
            """)) {
            sql.setInt(1, anggaran.periode().getYear());
            sql.setInt(2, anggaran.periode().getMonthValue());
            sql.setLong(3, anggaran.batas());
            sql.executeUpdate();
        }
    }
}
