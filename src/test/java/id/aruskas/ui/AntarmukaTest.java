package id.aruskas.ui;

import id.aruskas.domain.*;
import id.aruskas.repository.Database;
import id.aruskas.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** Smoke test Swing: isian UI -> SwingWorker -> service -> SQLite -> dashboard/riwayat. */
class AntarmukaTest {
    @TempDir Path folder;

    @Test void formulirDashboardFilterDanUbahUkuranBerjalan() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Memerlukan lingkungan desktop untuk Swing.");
        Database db = new Database(folder.resolve("ui.db"));
        db.inisialisasi();
        DompetService service = new DompetService(db);
        JendelaUtama frame = edt(() -> {
            Gaya.pasang();
            JendelaUtama hasil = new JendelaUtama(service, db.lokasi());
            hasil.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            hasil.setVisible(true);
            return hasil;
        });
        try {
            tunggu(() -> tombol(frame, "+ Tambah Transaksi").isEnabled());
            assertTrue(edt(() -> komponen(frame, JLabel.class).stream().anyMatch(l -> l.getText().contains("Belum ada transaksi"))));
            // Kategori dibuat melalui dialog; harus langsung tersedia di formulir.
            klik(frame, "Kategori");
            JDialog kategori = dialog("Tambah Kategori");
            edt(() -> { komponen(kategori, JTextField.class).getFirst().setText("Pekerjaan Kampus"); return null; });
            klik(kategori, "Simpan");
            tutupPesan("Kategori tersimpan");
            tunggu(() -> tombol(frame, "+ Tambah Transaksi").isEnabled());
            Kategori kategoriKampus = serviceKategori(service, "Pekerjaan Kampus");

            klik(frame, "+ Tambah Transaksi");
            JDialog transaksiMasuk = dialog("Tambah Transaksi");
            edt(() -> {
                komponen(transaksiMasuk, JTextField.class).getFirst().setText("300000");
                komponen(transaksiMasuk, JComboBox.class).get(1).setSelectedItem(kategoriKampus);
                return null;
            });
            klik(transaksiMasuk, "Simpan");
            tutupPesan("Transaksi tersimpan");
            tunggu(() -> tombol(frame, "+ Tambah Transaksi").isEnabled());

            klik(frame, "Atur Anggaran");
            JDialog anggaran = dialog("Anggaran Bulanan");
            tunggu(() -> tombol(anggaran, "Simpan").isEnabled());
            edt(() -> { komponen(anggaran, JTextField.class).get(1).setText("100000"); return null; });
            klik(anggaran, "Simpan");
            tutupPesan("Anggaran tersimpan");
            tunggu(() -> tombol(frame, "+ Tambah Transaksi").isEnabled());

            klik(frame, "+ Tambah Transaksi");
            JDialog isian = dialog("Tambah Transaksi");
            edt(() -> {
                komponen(isian, JComboBox.class).getFirst().setSelectedItem(JenisTransaksi.PENGELUARAN);
                komponen(isian, JTextField.class).getFirst().setText("100000");
                return null;
            });
            klik(isian, "Simpan");
            JDialog pemberitahuan = dialog("Transaksi tersimpan");
            assertTrue(edt(() -> komponen(pemberitahuan, JTextArea.class).stream().anyMatch(t -> t.getText().contains("Mencapai Batas"))));
            klik(pemberitahuan, "Tutup");
            tunggu(() -> tombol(frame, "+ Tambah Transaksi").isEnabled());
            assertEquals(200000, service.muat(FilterRiwayat.semua()).dompet().saldo());
            assertTrue(edt(() -> komponen(frame, JLabel.class).stream().anyMatch(l -> "Mencapai Batas".equals(l.getText()))));
            assertEquals(2, edt(() -> komponen(frame, JTable.class).getFirst().getRowCount()));

            edt(() -> { komponen(frame, JComboBox.class).getFirst().setSelectedItem(kategoriKampus); return null; });
            klik(frame, "Terapkan");
            tunggu(() -> tombol(frame, "Terapkan").isEnabled());
            assertEquals(1, edt(() -> komponen(frame, JTable.class).getFirst().getRowCount()));
            klik(frame, "Ekspor HTML");
            JDialog simpanLaporan = dialog("Simpan laporan HTML");
            Path html = folder.resolve("laporan-ui.html");
            edt(() -> {
                JFileChooser pemilih = komponen(simpanLaporan, JFileChooser.class).getFirst();
                pemilih.setSelectedFile(html.toFile());
                pemilih.approveSelection();
                return null;
            });
            tutupPesan("Ekspor berhasil");
            String isi = Files.readString(html);
            assertTrue(isi.contains("Jumlah transaksi: 1"));
            assertTrue(isi.contains("Total pemasukan: Rp300.000"));
            assertTrue(isi.contains("Total pengeluaran: Rp0"));
            assertTrue(isi.contains("Kategori: Pekerjaan Kampus (Pemasukan)"));
            klik(frame, "Reset");
            tunggu(() -> tombol(frame, "Reset").isEnabled());
            assertEquals(2, edt(() -> komponen(frame, JTable.class).getFirst().getRowCount()));

            // Render kedua ukuran untuk ditinjau tanpa menangkap desktop/aplikasi lain.
            rekam(frame, 1200, 900, "dashboard.png");
            rekam(frame, 1000, 760, "dashboard-minimum.png");
            assertTrue(edt(() -> komponen(frame, JTable.class).getFirst().getParent().getHeight() >= 76), "Tabel harus tetap menampilkan setidaknya dua baris pada ukuran minimum.");
            edt(() -> { frame.dispose(); return null; });
            Database dibukaLagi = new Database(folder.resolve("ui.db"));
            dibukaLagi.inisialisasi();
            JendelaUtama ulang = edt(() -> {
                JendelaUtama hasil = new JendelaUtama(new DompetService(dibukaLagi), dibukaLagi.lokasi());
                hasil.setVisible(true); return hasil;
            });
            tunggu(() -> tombol(ulang, "+ Tambah Transaksi").isEnabled());
            assertEquals(2, edt(() -> komponen(ulang, JTable.class).getFirst().getRowCount()));
            assertTrue(edt(() -> komponen(ulang, JLabel.class).stream().anyMatch(l -> "Rp200.000".equals(l.getText()))));
        } finally {
            edt(() -> { for (Window window : Window.getWindows()) window.dispose(); return null; });
        }
    }

    private static Kategori serviceKategori(DompetService service, String nama) throws Exception {
        // Hanya lookup objek untuk memilih combo di pengujian; produksi tidak melakukan I/O di EDT.
        return service.muat(FilterRiwayat.semua()).kategori().stream().filter(k -> k.nama().equals(nama)).findFirst().orElseThrow();
    }

    private static void rekam(JFrame frame, int lebar, int tinggi, String nama) throws Exception {
        BufferedImage gambar = edt(() -> {
            frame.setSize(lebar, tinggi); frame.validate();
            BufferedImage hasil = new BufferedImage(lebar, tinggi, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = hasil.createGraphics(); frame.paint(g); g.dispose();
            return hasil;
        });
        Path tujuan = Path.of("target", "ui-review", nama);
        Files.createDirectories(tujuan.getParent());
        ImageIO.write(gambar, "png", tujuan.toFile());
    }

    private static void tutupPesan(String judul) throws Exception { klik(dialog(judul), "Tutup"); }
    private static JDialog dialog(String judul) throws Exception {
        tunggu(() -> java.util.Arrays.stream(Window.getWindows()).anyMatch(w -> w instanceof JDialog d && d.isShowing() && d.getTitle().equals(judul)));
        return edt(() -> java.util.Arrays.stream(Window.getWindows()).filter(w -> w instanceof JDialog d && d.isShowing() && d.getTitle().equals(judul)).map(w -> (JDialog) w).findFirst().orElseThrow());
    }
    private static void klik(Container induk, String teks) { SwingUtilities.invokeLater(() -> tombol(induk, teks).doClick()); }
    private static JButton tombol(Container induk, String teks) { return komponen(induk, JButton.class).stream().filter(b -> teks.equals(b.getText())).findFirst().orElseThrow(); }
    private static <T> List<T> komponen(Container induk, Class<T> tipe) {
        List<T> hasil = new ArrayList<>();
        for (Component item : induk.getComponents()) {
            if (tipe.isInstance(item)) hasil.add(tipe.cast(item));
            if (item instanceof Container kontainer) hasil.addAll(komponen(kontainer, tipe));
        }
        return hasil;
    }
    private static void tunggu(BooleanSupplier kondisi) throws Exception {
        long batas = System.nanoTime() + 15_000_000_000L;
        while (System.nanoTime() < batas) {
            if (edt(kondisi::getAsBoolean)) return;
            Thread.sleep(50);
        }
        fail("Antarmuka tidak menyelesaikan operasi dalam 15 detik.");
    }
    private static <T> T edt(Callable<T> tugas) throws Exception {
        FutureTask<T> hasil = new FutureTask<>(tugas);
        SwingUtilities.invokeAndWait(hasil);
        return hasil.get();
    }
}
