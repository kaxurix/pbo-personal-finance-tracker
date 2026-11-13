package id.aruskas.service;

import id.aruskas.domain.Transaksi;
import id.aruskas.domain.RingkasanAnggaran;

/** Anggaran bernilai null untuk pemasukan. Hasil hanya dikembalikan setelah commit. */
public record HasilPencatatan(Transaksi transaksi, RingkasanAnggaran anggaran) { }
