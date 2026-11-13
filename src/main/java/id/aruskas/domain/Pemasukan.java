package id.aruskas.domain;

import java.time.LocalDate;

public final class Pemasukan extends Transaksi {
    public Pemasukan(long id, long nominal, Kategori kategori, LocalDate tanggal, String catatan) {
        super(id, nominal, kategori, tanggal, catatan, JenisTransaksi.PEMASUKAN);
    }

    @Override public long dampakSaldo() { return getNominal(); }
}
