package id.aruskas.domain;

import java.time.LocalDate;

/** Batas tanggal bersifat inklusif. null berarti kriteria itu tidak dibatasi. */
public record FilterRiwayat(Long kategoriId, LocalDate tanggalAwal, LocalDate tanggalAkhir) {
    public FilterRiwayat {
        if (tanggalAwal != null && tanggalAkhir != null && tanggalAwal.isAfter(tanggalAkhir)) {
            throw new IllegalArgumentException("Tanggal awal tidak boleh melebihi tanggal akhir.");
        }
    }
    public static FilterRiwayat semua() { return new FilterRiwayat(null, null, null); }
}
