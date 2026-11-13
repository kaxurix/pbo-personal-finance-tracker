package id.aruskas.service;

import id.aruskas.domain.*;
import id.aruskas.repository.Database;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class DompetServiceTest {
    @TempDir Path folder;
    private Database database;
    private DompetService service;
    private long masuk;
    private long keluar;
    private static final LocalDate HARI = LocalDate.of(2026, 11, 12);
    private static final YearMonth BULAN = YearMonth.of(2026, 11);
    private static final Clock JAM = Clock.fixed(Instant.parse("2026-11-12T05:00:00Z"), ZoneId.of("Asia/Jakarta"));

    @BeforeEach void siapkan() throws Exception {
        database = new Database(folder.resolve("kas.db"));
        database.inisialisasi();
        service = new DompetService(database, JAM);
        var kategori = service.muat(FilterRiwayat.semua()).kategori();
        masuk = kategori.stream().filter(k -> k.jenis() == JenisTransaksi.PEMASUKAN).findFirst().orElseThrow().id();
        keluar = kategori.stream().filter(k -> k.jenis() == JenisTransaksi.PENGELUARAN).findFirst().orElseThrow().id();
    }

    private HasilPencatatan masuk(long nilai) throws Exception { return service.catat(JenisTransaksi.PEMASUKAN, "" + nilai, masuk, HARI, "Uang saku"); }
    private HasilPencatatan keluar(long nilai, LocalDate hari) throws Exception { return service.catat(JenisTransaksi.PENGELUARAN, "" + nilai, keluar, hari, "Makan"); }
    private DataTampilan data() throws Exception { return service.muat(FilterRiwayat.semua()); }

    @Test void databaseKosongSaldoNolDanBelumDiatur() throws Exception {
        assertEquals(new RingkasanDompet(0, 0, 0), data().dompet());
        assertEquals(StatusAnggaran.BELUM_DIATUR, data().bulanBerjalan().status());
        assertTrue(data().riwayat().transaksi().isEmpty());
    }

    @Test void dataKategoriAnggaranDanTransaksiTetapAdaSetelahDibukaUlang() throws Exception {
        masuk(300_000); keluar(50_000, HARI);
        Kategori baru = service.tambahKategori("Buku Kuliah", JenisTransaksi.PENGELUARAN);
        service.simpanAnggaran(BULAN, "100000");
        database = new Database(folder.resolve("kas.db"));
        database.inisialisasi();
        service = new DompetService(database, JAM);
        assertEquals(new RingkasanDompet(300_000, 50_000, 250_000), data().dompet());
        assertEquals(2, data().jumlahTransaksi());
        assertTrue(data().kategori().contains(baru));
        assertEquals(100_000L, data().bulanBerjalan().batas());
        assertEquals(StatusAnggaran.DALAM_BATAS, data().bulanBerjalan().status());
    }

    @Test void saldoKurangDitolakDanPengeluaranTepatSaldoDiterima() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> keluar(1, HARI));
        masuk(100_000);
        var gagal = assertThrows(IllegalArgumentException.class, () -> keluar(100_001, HARI));
        assertTrue(gagal.getMessage().contains("Saldo tidak cukup"));
        assertEquals(1, data().jumlahTransaksi());
        keluar(100_000, HARI);
        assertEquals(0, data().dompet().saldo());
    }

    @Test void tepatBatasDanLewatBatasTetapTersimpanSaatSaldoCukup() throws Exception {
        masuk(500_000);
        service.simpanAnggaran(BULAN, "100000");
        assertEquals(StatusAnggaran.DALAM_BATAS, keluar(40_000, HARI).anggaran().status());
        var tepat = keluar(60_000, HARI).anggaran();
        assertEquals(StatusAnggaran.MENCAPAI_BATAS, tepat.status());
        assertEquals(0, tepat.sisaAtauKelebihan());
        assertEquals(100, tepat.persentase().orElseThrow());
        var lebih = keluar(1, HARI).anggaran();
        assertEquals(StatusAnggaran.TERLAMPAUI, lebih.status());
        assertEquals(1, lebih.sisaAtauKelebihan());
        assertEquals(4, data().jumlahTransaksi());
    }

    @Test void batasNolTidakMemilikiPersentaseDanDapatDiperbarui() throws Exception {
        var nol = service.simpanAnggaran(BULAN, "0");
        assertEquals(StatusAnggaran.MENCAPAI_BATAS, nol.status());
        assertTrue(nol.persentase().isEmpty());
        masuk(10);
        var lewat = keluar(1, HARI).anggaran();
        assertEquals(StatusAnggaran.TERLAMPAUI, lewat.status());
        assertTrue(lewat.persentase().isEmpty());
        var ganti = service.simpanAnggaran(BULAN, "5");
        assertEquals(StatusAnggaran.DALAM_BATAS, ganti.status());
        try (var koneksi = database.buka(); var sql = koneksi.prepareStatement("SELECT COUNT(*) FROM anggaran_bulanan"); var hasil = sql.executeQuery()) {
            assertEquals(1, hasil.getInt(1));
        }
    }

    @Test void pengeluaranTanpaAnggaranTetapDisimpan() throws Exception {
        masuk(100);
        assertEquals(StatusAnggaran.BELUM_DIATUR, keluar(50, HARI).anggaran().status());
        assertEquals(50, data().dompet().saldo());
    }

    @Test void periodeMengikutiTanggalTransaksiTermasukTahunYangBerbeda() throws Exception {
        masuk(500_000);
        service.simpanAnggaran(YearMonth.of(2025, 11), "20000");
        service.simpanAnggaran(YearMonth.of(2026, 10), "30000");
        service.simpanAnggaran(BULAN, "100000");
        assertEquals(StatusAnggaran.MENCAPAI_BATAS, keluar(20_000, LocalDate.of(2025, 11, 30)).anggaran().status());
        var bulanLain = keluar(30_001, LocalDate.of(2026, 10, 31)).anggaran();
        assertEquals(YearMonth.of(2026, 10), bulanLain.periode());
        assertEquals(StatusAnggaran.TERLAMPAUI, bulanLain.status());
        assertEquals(BULAN, data().bulanBerjalan().periode());
        assertEquals(0, data().bulanBerjalan().realisasi());
        keluar(50_000, HARI);
        assertEquals(50_000, data().bulanBerjalan().realisasi());
    }

    @Test void kategoriBaruDapatDipakaiDanKategoriTipeLainDitolak() throws Exception {
        Kategori buku = service.tambahKategori("Buku", JenisTransaksi.PENGELUARAN);
        masuk(100);
        service.catat(JenisTransaksi.PENGELUARAN, "25", buku.id(), HARI, "");
        assertEquals(buku, data().riwayat().transaksi().getFirst().getKategori());
        assertThrows(IllegalArgumentException.class, () -> service.catat(JenisTransaksi.PEMASUKAN, "25", buku.id(), HARI, ""));
        assertThrows(IllegalArgumentException.class, () -> service.tambahKategori(" buku ", JenisTransaksi.PENGELUARAN));
        assertDoesNotThrow(() -> service.tambahKategori("Buku", JenisTransaksi.PEMASUKAN));
    }

    @ParameterizedTest @NullAndEmptySource
    @ValueSource(strings = {" ", "0", "-1", "1.5", "1,5", "abc", "1.000", "Rp100", "+1", "9223372036854775808"})
    void nominalTransaksiTidakValidDitolak(String nilai) throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.catat(JenisTransaksi.PEMASUKAN, nilai, masuk, HARI, ""));
        assertEquals(0, data().jumlahTransaksi());
    }

    @Test void filterKategoriDanTanggalDigabungDenganBatasInklusifDanEksporSama() throws Exception {
        masuk(1_000);
        Kategori pilihan = service.tambahKategori("Buku <baru> & \"alat\"", JenisTransaksi.PENGELUARAN);
        service.simpanAnggaran(BULAN, "30");
        LocalDate awal = LocalDate.of(2026, 11, 1), akhir = LocalDate.of(2026, 11, 30);
        service.catat(JenisTransaksi.PENGELUARAN, "10", pilihan.id(), awal, "<script>alert('x')</script> & é");
        service.catat(JenisTransaksi.PENGELUARAN, "20", pilihan.id(), akhir, "batas akhir");
        service.catat(JenisTransaksi.PENGELUARAN, "30", pilihan.id(), akhir.plusDays(1), "DI LUAR RENTANG");
        keluar(5, HARI); // Kategori lain, tetap masuk realisasi anggaran November.
        FilterRiwayat filter = new FilterRiwayat(pilihan.id(), awal, akhir);
        LaporanRiwayat laporan = service.muat(filter).riwayat();
        assertEquals(2, laporan.transaksi().size());
        assertEquals(new RingkasanDompet(0, 30, -30), laporan.total());
        assertEquals(35, laporan.anggaran().getFirst().realisasi());
        Path html = folder.resolve("laporan.html");
        Exportable eksportir = new EksporHtml();
        eksportir.ekspor(laporan, html);
        String isi = Files.readString(html, StandardCharsets.UTF_8);
        assertTrue(isi.contains("charset=\"UTF-8\""));
        assertTrue(isi.contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; é"));
        assertTrue(isi.contains("Buku &lt;baru&gt; &amp; &quot;alat&quot;"));
        assertFalse(isi.contains("<script>"));
        assertFalse(isi.contains("DI LUAR RENTANG"));
        assertTrue(isi.contains("Tanggal awal: 01-11-2026"));
        assertTrue(isi.contains("Tanggal akhir: 30-11-2026"));
        assertTrue(isi.contains("Selisih hasil filter: −Rp30"));
        assertTrue(isi.contains("Terlampaui"));
        assertEquals(5, service.muat(FilterRiwayat.semua()).riwayat().transaksi().size());
        assertThrows(IllegalArgumentException.class, () -> new FilterRiwayat(null, akhir, awal));
    }

    @Test void eksporBeberapaBulanMenampilkanSetiapBatasDanStatus() throws Exception {
        masuk(1_000);
        service.simpanAnggaran(YearMonth.of(2026, 10), "10");
        service.simpanAnggaran(BULAN, "0");
        keluar(10, HARI.minusMonths(1)); keluar(1, HARI);
        var laporan = data().riwayat();
        assertEquals(2, laporan.anggaran().size());
        Path html = folder.resolve("lintas-bulan.html");
        new EksporHtml().ekspor(laporan, html);
        String isi = Files.readString(html);
        assertTrue(isi.contains("Oktober 2026 · Mencapai Batas"));
        assertTrue(isi.contains("November 2026 · Terlampaui"));
        assertTrue(isi.contains("Batas Rp10"));
        assertTrue(isi.contains("Batas Rp0"));
        String november = isi.substring(isi.indexOf("November 2026 ·"));
        assertFalse(november.contains("%"));
    }

    @Test void rollbackMembatalkanTulisanJikaOperasiGagal() throws Exception {
        assertThrows(SQLException.class, () -> database.dalamTransaksi(koneksi -> {
            try (var sql = koneksi.prepareStatement("INSERT INTO kategori(nama,jenis) VALUES ('Harus rollback','PEMASUKAN')")) { sql.executeUpdate(); }
            throw new SQLException("Simulasi kegagalan");
        }));
        assertTrue(data().kategori().stream().noneMatch(k -> k.nama().equals("Harus rollback")));
    }

    @Test void foreignKeyAktifDanNominalBenarBenarInteger() throws Exception {
        masuk(125);
        try (var koneksi = database.buka()) {
            try (var sql = koneksi.prepareStatement("PRAGMA foreign_keys"); var hasil = sql.executeQuery()) { assertEquals(1, hasil.getInt(1)); }
            try (var sql = koneksi.prepareStatement("SELECT typeof(nominal) FROM transaksi"); var hasil = sql.executeQuery()) { assertEquals("integer", hasil.getString(1)); }
            try (var sql = koneksi.prepareStatement("INSERT INTO transaksi(kategori_id,nominal,tanggal) VALUES (-1,10,'2026-11-12')")) {
                assertThrows(SQLException.class, sql::executeUpdate);
            }
        }
    }

    @Test void duaPengeluaranSerentakTidakDapatMenghabiskanSaldoYangSama() throws Exception {
        masuk(100);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch mulai = new CountDownLatch(1);
            Callable<Boolean> tugas = () -> {
                mulai.await();
                try { keluar(80, HARI); return true; }
                catch (IllegalArgumentException gagal) { return false; }
            };
            var pertama = executor.submit(tugas);
            var kedua = executor.submit(tugas);
            mulai.countDown();
            assertNotEquals(pertama.get(10, TimeUnit.SECONDS), kedua.get(10, TimeUnit.SECONDS));
        }
        assertEquals(20, data().dompet().saldo());
        assertEquals(2, data().jumlahTransaksi());
    }

    @Test void overflowDitolakTanpaMenyimpanDanPolimorfismeBerfungsi() throws Exception {
        masuk(Long.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> masuk(1));
        assertEquals(1, data().jumlahTransaksi());
        Transaksi pemasukan = data().riwayat().transaksi().getFirst();
        assertInstanceOf(Pemasukan.class, pemasukan);
        assertEquals(Long.MAX_VALUE, pemasukan.dampakSaldo());
        keluar(Long.MAX_VALUE, HARI);
        Transaksi pengeluaran = data().riwayat().transaksi().getFirst();
        assertInstanceOf(Pengeluaran.class, pengeluaran);
        assertEquals(-Long.MAX_VALUE, pengeluaran.dampakSaldo());
        assertEquals(0, data().dompet().saldo());
    }

    @Test void tanggalDanPeriodeTidakValidDitolak() {
        assertThrows(IllegalArgumentException.class, () -> Validasi.tanggal("31-02-2026"));
        assertThrows(IllegalArgumentException.class, () -> Validasi.tanggal("01-01-0000"));
        assertThrows(IllegalArgumentException.class, () -> Validasi.periode("2026-13"));
        assertEquals(LocalDate.of(2024, 2, 29), Validasi.tanggal("29-02-2024"));
        assertNull(Validasi.tanggalOpsional(" "));
    }
}
