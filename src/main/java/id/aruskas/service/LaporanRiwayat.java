package id.aruskas.service;

import id.aruskas.domain.*;
import java.util.List;

/** Snapshot yang sama dipakai tabel dan eksportir, sehingga baris tidak bergeser saat ekspor. */
public record LaporanRiwayat(FilterRiwayat filter, String namaKategori, List<Transaksi> transaksi,
                             RingkasanDompet total, List<RingkasanAnggaran> anggaran) {
    public LaporanRiwayat {
        transaksi = List.copyOf(transaksi);
        anggaran = List.copyOf(anggaran);
    }
}
