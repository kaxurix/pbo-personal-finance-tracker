package id.aruskas.domain;

import java.time.LocalDate;

public final class Pengeluaran extends Transaksi {
    public Pengeluaran(long id, long nominal, Kategori kategori, LocalDate tanggal, String catatan) {
        super(id, nominal, kategori, tanggal, catatan, JenisTransaksi.PENGELUARAN);
    }

    @Override public long dampakSaldo() { return -getNominal(); }
}
