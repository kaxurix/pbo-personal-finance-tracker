package id.aruskas.ui;

import id.aruskas.domain.*;
import id.aruskas.service.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public final class JendelaUtama extends JFrame {
    private final DompetService service;
    private final Path database;
    private final JLabel pemasukan = Gaya.label("Rp0", 26, Gaya.HIJAU, true);
    private final JLabel pengeluaran = Gaya.label("Rp0", 26, Gaya.MERAH, true);
    private final JLabel saldo = Gaya.label("Rp0", 28, Color.WHITE, true);
    private final JLabel periode = Gaya.label("Anggaran bulan berjalan", 16, Gaya.TEKS, true);
    private final JLabel statusAnggaran = Gaya.label("Memuat…", 15, Gaya.REDUP, true);
    private final JLabel realisasi = Gaya.label(" ", 14, Gaya.TEKS, false);
    private final JLabel sisa = Gaya.label(" ", 13, Gaya.REDUP, false);
    private final JProgressBar progres = new JProgressBar(0, 100);
    private final JLabel ringkasanFilter = Gaya.label("Memuat riwayat…", 13, Gaya.REDUP, false);
    private final JLabel informasi = Gaya.label("Memuat data lokal…", 13, Gaya.REDUP, false);
    private final JComboBox<Object> kategori = new JComboBox<>();
    private final JTextField awal = new JTextField(10);
    private final JTextField akhir = new JTextField(10);
    private final JButton tambah = Gaya.tombol("+ Tambah Transaksi", true);
    private final JButton kelolaKategori = Gaya.tombol("Kategori", false);
    private final JButton aturAnggaran = Gaya.tombol("Atur Anggaran", false);
    private final JButton terapkan = Gaya.tombol("Terapkan", true);
    private final JButton reset = Gaya.tombol("Reset", false);
    private final JButton ekspor = Gaya.tombol("Ekspor HTML", false);
    private final JButton segarkan = Gaya.tombol("Muat Ulang", false);
    private final ModelTabelTransaksi model = new ModelTabelTransaksi();
    private final JTable tabel = new JTable(model);
    private final JLabel kosong = Gaya.label(" ", 15, Gaya.REDUP, false);
    private final JPanel isiRiwayat = new JPanel(new CardLayout());
    private FilterRiwayat filterAktif = FilterRiwayat.semua();
    private LaporanRiwayat laporan;
    private List<Kategori> daftarKategori = List.of();
    private boolean sibuk;

    public JendelaUtama(DompetService service, Path database) {
        super("Arus Kas Harian");
        this.service = service;
        this.database = database;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 760));
        setSize(1200, 900);
        setLocationRelativeTo(null);
        JPanel akar = new JPanel(new BorderLayout(0, 16));
        akar.setBackground(Gaya.LATAR);
        akar.setBorder(Gaya.tepi(20));
        akar.add(bagianAtas(), BorderLayout.NORTH);
        akar.add(bagianRiwayat(), BorderLayout.CENTER);
        JPanel kaki = new JPanel(new BorderLayout());
        kaki.setOpaque(false);
        kaki.add(informasi, BorderLayout.CENTER);
        JLabel lokal = Gaya.label("Lokal · Satu dompet", 12, Gaya.REDUP, false);
        lokal.setToolTipText(database.toString());
        kaki.add(lokal, BorderLayout.EAST);
        akar.add(kaki, BorderLayout.SOUTH);
        setContentPane(akar);
        pasangAksi();
        kategori.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setText(value instanceof Kategori k ? k.nama() + " · " + k.jenis() : String.valueOf(value));
                putClientProperty("html.disable", true);
                return this;
            }
        });
        Timer pergantianBulan = new Timer(60_000, event -> {
            if (!sibuk && isEnabled() && laporan != null && !periode.getText().endsWith(FormatData.periode(YearMonth.now()))) muat();
        });
        pergantianBulan.start();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) { pergantianBulan.stop(); }
        });
        muat();
    }

    private JPanel bagianAtas() {
        JPanel atas = new JPanel(new BorderLayout(0, 16));
        atas.setOpaque(false);
        JPanel judul = new JPanel(new BorderLayout(16, 0));
        judul.setOpaque(false);
        judul.add(Gaya.kolom(Gaya.label("DOMPET PRIBADI", 11, Gaya.REDUP, true),
                Gaya.label("Arus Kas Harian", 30, Gaya.BIRU, true)), BorderLayout.CENTER);
        JPanel aksi = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        aksi.setOpaque(false);
        aksi.add(kelolaKategori);
        aksi.add(tambah);
        judul.add(aksi, BorderLayout.EAST);
        atas.add(judul, BorderLayout.NORTH);
        JPanel kartu = new JPanel(new GridLayout(1, 3, 16, 0));
        kartu.setOpaque(false);
        kartu.add(kartuNominal("TOTAL PEMASUKAN", pemasukan, "Seluruh transaksi tersimpan", false));
        kartu.add(kartuNominal("TOTAL PENGELUARAN", pengeluaran, "Seluruh transaksi tersimpan", false));
        kartu.add(kartuNominal("SALDO TERSEDIA", saldo, "Saldo awal Rp0", true));
        atas.add(kartu, BorderLayout.CENTER);
        JPanel anggaran = Gaya.kartu();
        anggaran.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(224, 231, 241)), Gaya.tepi(12)));
        JPanel kepala = new JPanel(new BorderLayout());
        kepala.setOpaque(false);
        kepala.add(periode, BorderLayout.WEST);
        kepala.add(aturAnggaran, BorderLayout.EAST);
        anggaran.add(kepala, BorderLayout.NORTH);
        progres.setStringPainted(true);
        progres.setPreferredSize(new Dimension(200, 14));
        JPanel barisStatus = new JPanel(new BorderLayout(20, 0));
        barisStatus.setOpaque(false);
        barisStatus.add(statusAnggaran, BorderLayout.WEST);
        barisStatus.add(realisasi, BorderLayout.CENTER);
        JPanel barisProgres = new JPanel(new BorderLayout(20, 0));
        barisProgres.setOpaque(false);
        barisProgres.add(progres, BorderLayout.CENTER);
        barisProgres.add(sisa, BorderLayout.EAST);
        anggaran.add(Gaya.kolom(barisStatus, barisProgres), BorderLayout.CENTER);
        atas.add(anggaran, BorderLayout.SOUTH);
        return atas;
    }

    private JPanel kartuNominal(String judul, JLabel nilai, String petunjuk, boolean gelap) {
        JPanel kartu = Gaya.kartu();
        if (gelap) kartu.setBackground(Gaya.BIRU);
        Color warna = gelap ? new Color(206, 220, 240) : Gaya.REDUP;
        kartu.add(Gaya.kolom(Gaya.label(judul, 11, warna, true), nilai,
                Gaya.label(petunjuk, 12, warna, false)), BorderLayout.CENTER);
        return kartu;
    }

    private JPanel bagianRiwayat() {
        JPanel riwayat = Gaya.kartu();
        JPanel kepala = new JPanel(new BorderLayout(12, 12));
        kepala.setOpaque(false);
        kepala.add(Gaya.label("Riwayat transaksi", 20, Gaya.BIRU, true), BorderLayout.WEST);
        JPanel aksi = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        aksi.setOpaque(false);
        aksi.add(segarkan);
        aksi.add(ekspor);
        kepala.add(aksi, BorderLayout.EAST);
        JPanel filter = new JPanel(new GridBagLayout());
        filter.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(0, 0, 0, 10);
        c.gridx = 0; c.weightx = 1; filter.add(Gaya.bidang("Kategori", kategori), c);
        awal.setToolTipText("DD-MM-YYYY, kosongkan untuk semua tanggal");
        akhir.setToolTipText("DD-MM-YYYY, kedua tanggal batas disertakan");
        awal.putClientProperty("JTextField.placeholderText", "DD-MM-YYYY");
        akhir.putClientProperty("JTextField.placeholderText", "DD-MM-YYYY");
        c.gridx = 1; c.weightx = 0.35; filter.add(Gaya.bidang("Tanggal awal", awal), c);
        c.gridx = 2; filter.add(Gaya.bidang("Tanggal akhir", akhir), c);
        c.gridx = 3; c.weightx = 0; c.anchor = GridBagConstraints.SOUTH; filter.add(terapkan, c);
        c.gridx = 4; c.insets = new Insets(0, 0, 0, 0); filter.add(reset, c);
        kepala.add(filter, BorderLayout.SOUTH);
        riwayat.add(kepala, BorderLayout.NORTH);
        tabel.setFillsViewportHeight(true);
        tabel.setAutoCreateRowSorter(false); // Urutan persis snapshot laporan ekspor.
        tabel.getTableHeader().setReorderingAllowed(false);
        tabel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabel.setRowHeight(38);
        tabel.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        int[] lebar = {110, 115, 180, 150, 300};
        for (int i = 0; i < lebar.length; i++) tabel.getColumnModel().getColumn(i).setPreferredWidth(lebar[i]);
        tabel.setDefaultRenderer(Object.class, renderer());
        tabel.setDefaultRenderer(String.class, renderer());
        tabel.setDefaultRenderer(LocalDate.class, renderer());
        tabel.setDefaultRenderer(Long.class, renderer());
        isiRiwayat.add(new JScrollPane(tabel), "tabel");
        JPanel petunjuk = new JPanel(new GridBagLayout());
        petunjuk.setBackground(Color.WHITE);
        kosong.setHorizontalAlignment(SwingConstants.CENTER);
        petunjuk.add(kosong);
        isiRiwayat.add(petunjuk, "kosong");
        riwayat.add(isiRiwayat, BorderLayout.CENTER);
        riwayat.add(ringkasanFilter, BorderLayout.SOUTH);
        return riwayat;
    }

    private DefaultTableCellRenderer renderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, col);
                putClientProperty("html.disable", true);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                setHorizontalAlignment(col == 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                if (value instanceof LocalDate tanggal) setText(FormatData.tanggal(tanggal));
                if (value instanceof Long nominal) setText(FormatData.rupiah(nominal));
                if (!selected) setForeground(col == 1 ? ("Pemasukan".equals(value) ? Gaya.HIJAU : Gaya.MERAH) : Gaya.TEKS);
                setToolTipText(value == null ? null : String.valueOf(value));
                return this;
            }
        };
    }

    private void pasangAksi() {
        tambah.addActionListener(event -> new DialogTransaksi(this, service, daftarKategori, this::muat).setVisible(true));
        kelolaKategori.addActionListener(event -> new DialogKategori(this, service, this::muat).setVisible(true));
        aturAnggaran.addActionListener(event -> new DialogAnggaran(this, service, this::muat).setVisible(true));
        terapkan.addActionListener(event -> {
            try {
                Kategori pilihan = kategori.getSelectedItem() instanceof Kategori k ? k : null;
                FilterRiwayat baru = new FilterRiwayat(pilihan == null ? null : pilihan.id(),
                        Validasi.tanggalOpsional(awal.getText()), Validasi.tanggalOpsional(akhir.getText()));
                muat(baru);
            } catch (IllegalArgumentException gagal) { Gaya.pesan(this, "Periksa filter", gagal.getMessage(), JOptionPane.WARNING_MESSAGE); }
        });
        reset.addActionListener(event -> muat(FilterRiwayat.semua()));
        segarkan.addActionListener(event -> muat());
        ekspor.addActionListener(event -> ekspor());
    }

    public void muat() { muat(filterAktif); }
    private void muat(FilterRiwayat filter) {
        if (sibuk) return;
        sibuk(true);
        informasi.setText("Memuat data lokal…");
        Pekerja.jalankan(this, () -> service.muat(filter), data -> {
            filterAktif = filter;
            laporan = data.riwayat();
            daftarKategori = data.kategori();
            pemasukan.setText(FormatData.rupiah(data.dompet().pemasukan()));
            pengeluaran.setText(FormatData.rupiah(data.dompet().pengeluaran()));
            saldo.setText(FormatData.rupiah(data.dompet().saldo()));
            periode.setText("Anggaran · " + FormatData.periode(data.bulanBerjalan().periode()));
            RingkasanAnggaran anggaran = data.bulanBerjalan();
            statusAnggaran.setText(anggaran.status().toString());
            Color warna = switch (anggaran.status()) {
                case BELUM_DIATUR -> Gaya.REDUP;
                case DALAM_BATAS -> Gaya.HIJAU;
                case MENCAPAI_BATAS -> Gaya.KUNING;
                case TERLAMPAUI -> Gaya.MERAH;
            };
            statusAnggaran.setForeground(warna);
            realisasi.setText("Realisasi " + FormatData.rupiah(anggaran.realisasi()) + "   /   Batas "
                    + (anggaran.batas() == null ? "belum diatur" : FormatData.rupiah(anggaran.batas())));
            sisa.setText(anggaran.batas() == null ? "Atur batas bulanan untuk memantau pengeluaranmu."
                    : (anggaran.status() == StatusAnggaran.TERLAMPAUI ? "Kelebihan " : "Sisa ") + FormatData.rupiah(anggaran.sisaAtauKelebihan()));
            progres.setVisible(anggaran.persentase().isPresent());
            progres.setForeground(warna);
            if (anggaran.persentase().isPresent()) {
                double persen = anggaran.persentase().getAsDouble();
                progres.setValue((int) Math.min(100, persen));
                progres.setString(String.format(java.util.Locale.forLanguageTag("id-ID"), "%.1f%%", persen));
            }
            kategori.removeAllItems();
            kategori.addItem("Semua kategori");
            for (Kategori item : daftarKategori) {
                kategori.addItem(item);
                if (filter.kategoriId() != null && item.id() == filter.kategoriId()) kategori.setSelectedItem(item);
            }
            awal.setText(filter.tanggalAwal() == null ? "" : FormatData.tanggal(filter.tanggalAwal()));
            akhir.setText(filter.tanggalAkhir() == null ? "" : FormatData.tanggal(filter.tanggalAkhir()));
            model.isi(laporan.transaksi());
            kosong.setText(data.jumlahTransaksi() == 0 ? "Belum ada transaksi. Klik Tambah Transaksi untuk memulai."
                    : "Tidak ada transaksi sesuai filter. Coba ubah filter atau klik Reset.");
            ((CardLayout) isiRiwayat.getLayout()).show(isiRiwayat, laporan.transaksi().isEmpty() ? "kosong" : "tabel");
            RingkasanDompet total = laporan.total();
            ringkasanFilter.setText(laporan.transaksi().size() + " transaksi  ·  Masuk " + FormatData.rupiah(total.pemasukan())
                    + "  ·  Keluar " + FormatData.rupiah(total.pengeluaran()) + "  ·  Selisih " + FormatData.rupiah(total.saldo()));
            informasi.setText("Data tersimpan lokal · Kedua tanggal batas filter disertakan.");
            informasi.setToolTipText(database.toString());
        }, () -> {
            sibuk(false);
            if (informasi.getText().equals("Memuat data lokal…")) informasi.setText("Gagal memuat. Klik Muat Ulang untuk mencoba kembali.");
        });
    }

    private void sibuk(boolean nilai) {
        sibuk = nilai;
        setDefaultCloseOperation(nilai ? JFrame.DO_NOTHING_ON_CLOSE : JFrame.EXIT_ON_CLOSE);
        for (JComponent item : new JComponent[]{tambah, kelolaKategori, aturAnggaran, terapkan, reset, segarkan, kategori, awal, akhir}) item.setEnabled(!nilai);
        ekspor.setEnabled(!nilai && laporan != null);
        setCursor(Cursor.getPredefinedCursor(nilai ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void ekspor() {
        if (laporan == null) return;
        JFileChooser dialog = new JFileChooser();
        dialog.setDialogTitle("Simpan laporan HTML");
        dialog.setFileFilter(new FileNameExtensionFilter("Laporan HTML (*.html)", "html"));
        dialog.setAcceptAllFileFilterUsed(false);
        dialog.setSelectedFile(new java.io.File("arus-kas-" + LocalDate.now() + ".html"));
        if (dialog.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path tujuan = dialog.getSelectedFile().toPath();
        if (!tujuan.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".html")) tujuan = tujuan.resolveSibling(tujuan.getFileName() + ".html");
        if (Files.exists(tujuan) && JOptionPane.showConfirmDialog(this, "Berkas sudah ada. Ganti dengan laporan ini?", "Ganti berkas", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        Path lokasi = tujuan;
        LaporanRiwayat snapshot = laporan;
        sibuk(true);
        informasi.setText("Mengekspor laporan…");
        Pekerja.jalankan(this, () -> {
            Exportable eksportir = new EksporHtml();
            eksportir.ekspor(snapshot, lokasi);
            return lokasi;
        }, hasil -> {
            informasi.setText("Laporan berhasil diekspor.");
            Gaya.pesan(this, "Ekspor berhasil", "Laporan HTML UTF-8 berhasil disimpan:\n" + hasil.toAbsolutePath()
                    + "\n\nBaris laporan mengikuti hasil filter yang sedang ditampilkan.", JOptionPane.INFORMATION_MESSAGE);
        }, () -> sibuk(false));
    }
}
