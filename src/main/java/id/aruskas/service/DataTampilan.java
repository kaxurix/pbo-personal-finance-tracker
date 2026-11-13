package id.aruskas.service;

import id.aruskas.domain.*;
import java.util.List;

public record DataTampilan(RingkasanDompet dompet, RingkasanAnggaran bulanBerjalan,
                            List<Kategori> kategori, LaporanRiwayat riwayat, int jumlahTransaksi) { }
