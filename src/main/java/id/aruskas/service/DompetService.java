package id.aruskas.service;

import id.aruskas.domain.*;
import id.aruskas.repository.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Satu pengguna, satu dompet, saldo awal Rp0. Semua aturan finansial terpusat di sini. */
public final class DompetService {
    private final Database database;
    private final Clock jam;
    private final RepositoryTransaksi transaksi = new RepositoryTransaksi();
    private final RepositoryKategori kategori = new RepositoryKategori();
    private final RepositoryAnggaran anggaran = new RepositoryAnggaran();

    public DompetService(Database database) { this(database, Clock.systemDefaultZone()); }
    public DompetService(Database database, Clock jam) { this.database = database; this.jam = jam; }

    public HasilPencatatan catat(JenisTransaksi jenis, String nominal, long kategoriId,
                                 LocalDate tanggal, String catatan) throws SQLException {
        if (jenis == null) throw new IllegalArgumentException("Jenis transaksi wajib dipilih.");
        long nilai = Validasi.nominal(nominal, false);
        return database.dalamTransaksi(koneksi -> {
            Kategori pilihan = kategori.cari(koneksi, kategoriId);
            if (pilihan.jenis() != jenis) throw new IllegalArgumentException("Kategori tidak sesuai jenis transaksi.");
            Transaksi baru = RepositoryTransaksi.buat(0, nilai, pilihan, tanggal, catatan);
            List<Transaksi> tersimpan = transaksi.cari(koneksi, FilterRiwayat.semua());
            RingkasanDompet dompet = ringkas(tersimpan);
            // Pemanggilan virtual melalui referensi Transaksi, bukan percabangan nominal di UI.
            long saldoCalon = jumlah(dompet.saldo(), baru.dampakSaldo());
            if (saldoCalon < 0) throw new IllegalArgumentException("Saldo tidak cukup. Saldo tersedia " + FormatData.rupiah(dompet.saldo()) + ".");
            // Batasi total per jenis agar semua ringkasan tetap dapat direpresentasikan long.
            jumlah(jenis == JenisTransaksi.PEMASUKAN ? dompet.pemasukan() : dompet.pengeluaran(), nilai);
            RingkasanAnggaran calon = null;
            if (jenis == JenisTransaksi.PENGELUARAN) {
                YearMonth periode = YearMonth.from(tanggal);
                RingkasanAnggaran saatIni = ringkasAnggaran(koneksi, periode, tersimpan);
                calon = new RingkasanAnggaran(periode, saatIni.batas(), jumlah(saatIni.realisasi(), nilai));
            }
            Transaksi hasil = transaksi.simpan(koneksi, baru);
            return new HasilPencatatan(hasil, calon);
        });
    }

    public Kategori tambahKategori(String nama, JenisTransaksi jenis) throws SQLException {
        Kategori baru = new Kategori(0, nama, jenis);
        return database.dalamTransaksi(koneksi -> {
            boolean duplikat = kategori.semua(koneksi).stream().anyMatch(k -> k.jenis() == jenis && k.nama().equalsIgnoreCase(baru.nama()));
            if (duplikat) throw new IllegalArgumentException("Nama kategori untuk jenis tersebut sudah ada.");
            return kategori.tambah(koneksi, baru.nama(), jenis);
        });
    }

    public RingkasanAnggaran simpanAnggaran(YearMonth periode, String nominal) throws SQLException {
        AnggaranBulanan batas = new AnggaranBulanan(periode, Validasi.nominal(nominal, true));
        return database.dalamTransaksi(koneksi -> {
            anggaran.simpan(koneksi, batas);
            return ringkasAnggaran(koneksi, periode, transaksi.cari(koneksi, FilterRiwayat.semua()));
        });
    }

    public RingkasanAnggaran lihatAnggaran(YearMonth periode) throws SQLException {
        return database.dalamTransaksi(koneksi -> ringkasAnggaran(koneksi, periode, transaksi.cari(koneksi, FilterRiwayat.semua())));
    }

    public DataTampilan muat(FilterRiwayat filter) throws SQLException {
        return database.dalamTransaksi(koneksi -> {
            List<Transaksi> semua = transaksi.cari(koneksi, FilterRiwayat.semua());
            List<Transaksi> hasil = transaksi.cari(koneksi, filter);
            List<YearMonth> periode = hasil.stream().map(t -> YearMonth.from(t.getTanggal())).distinct().sorted().toList();
            var status = new java.util.ArrayList<RingkasanAnggaran>();
            for (YearMonth bulan : periode) status.add(ringkasAnggaran(koneksi, bulan, semua));
            String nama = filter.kategoriId() == null ? "Semua kategori" : kategori.cari(koneksi, filter.kategoriId()).nama()
                    + " (" + kategori.cari(koneksi, filter.kategoriId()).jenis() + ")";
            LaporanRiwayat laporan = new LaporanRiwayat(filter, nama, hasil, ringkas(hasil), status);
            return new DataTampilan(ringkas(semua), ringkasAnggaran(koneksi, YearMonth.now(jam), semua),
                    kategori.semua(koneksi), laporan, semua.size());
        });
    }

    private RingkasanAnggaran ringkasAnggaran(Connection koneksi, YearMonth periode, List<Transaksi> semua) throws SQLException {
        long realisasi = 0;
        for (Transaksi item : semua) {
            if (item.getJenis() == JenisTransaksi.PENGELUARAN && YearMonth.from(item.getTanggal()).equals(periode)) {
                realisasi = jumlah(realisasi, item.getNominal());
            }
        }
        Long batas = anggaran.cari(koneksi, periode).map(AnggaranBulanan::batas).orElse(null);
        return new RingkasanAnggaran(periode, batas, realisasi);
    }

    private RingkasanDompet ringkas(List<Transaksi> semua) {
        try { return RingkasanDompet.dari(semua); }
        catch (ArithmeticException gagal) { throw new IllegalArgumentException("Total transaksi melebihi kapasitas nominal rupiah."); }
    }

    private long jumlah(long a, long b) {
        try { return Math.addExact(a, b); }
        catch (ArithmeticException gagal) { throw new IllegalArgumentException("Total transaksi melebihi kapasitas nominal rupiah."); }
    }
}
