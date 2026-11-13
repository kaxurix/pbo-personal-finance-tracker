package id.aruskas.domain;

import java.util.List;

public record RingkasanDompet(long pemasukan, long pengeluaran, long saldo) {
    public static RingkasanDompet dari(List<Transaksi> transaksi) {
        long masuk = 0, keluar = 0, saldo = 0;
        for (Transaksi item : transaksi) {
            // Polimorfisme: referensi Transaksi memanggil implementasi subclass.
            saldo = Math.addExact(saldo, item.dampakSaldo());
            if (item.getJenis() == JenisTransaksi.PEMASUKAN) masuk = Math.addExact(masuk, item.getNominal());
            else keluar = Math.addExact(keluar, item.getNominal());
        }
        return new RingkasanDompet(masuk, keluar, saldo);
    }
}
