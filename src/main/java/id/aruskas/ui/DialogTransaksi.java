package id.aruskas.ui;

import id.aruskas.domain.*;
import id.aruskas.service.*;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class DialogTransaksi extends DialogForm {
    private final JComboBox<JenisTransaksi> jenis = new JComboBox<>(JenisTransaksi.values());
    private final JComboBox<Kategori> kategori = new JComboBox<>();
    private final JTextField nominal = new JTextField();
    private final JTextField tanggal = new JTextField(FormatData.tanggal(LocalDate.now()));
    private final JTextArea catatan = new JTextArea(4, 30);
    private final List<Kategori> daftar;

    DialogTransaksi(Window induk, DompetService service, List<Kategori> daftar, Runnable setelahSimpan) {
        super(induk, "Tambah Transaksi");
        this.daftar = new ArrayList<>(daftar);
        nominal.putClientProperty("JTextField.placeholderText", "Contoh: 300000");
        catatan.setLineWrap(true); catatan.setWrapStyleWord(true);
        tambah("Jenis transaksi", jenis);
        tambah("Nominal rupiah · angka bulat tanpa pemisah", nominal);
        JPanel kategoriDanAksi = new JPanel(new BorderLayout(8, 0));
        kategoriDanAksi.setOpaque(false);
        kategoriDanAksi.add(kategori, BorderLayout.CENTER);
        JButton baru = Gaya.tombol("+ Kategori", false);
        kategoriDanAksi.add(baru, BorderLayout.EAST);
        tambah("Kategori", kategoriDanAksi);
        tambah("Tanggal · DD-MM-YYYY", tanggal);
        tambah("Catatan · opsional, maksimal 1.000 karakter", new JScrollPane(catatan));
        petunjuk.setText("Pengeluaran tetap dicatat jika saldo cukup, meski melebihi anggaran.");
        jenis.addActionListener(event -> muatKategori(null));
        kategori.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                putClientProperty("html.disable", true);
                return this;
            }
        });
        baru.addActionListener(event -> new DialogKategori(this, service, tambahan -> {
            this.daftar.add(tambahan);
            jenis.setSelectedItem(tambahan.jenis());
            muatKategori(tambahan);
            setelahSimpan.run();
        }, (JenisTransaksi) jenis.getSelectedItem()).setVisible(true));
        muatKategori(null);
        simpan.addActionListener(event -> {
            try {
                JenisTransaksi pilihanJenis = (JenisTransaksi) jenis.getSelectedItem();
                Kategori pilihanKategori = (Kategori) kategori.getSelectedItem();
                if (pilihanKategori == null) throw new IllegalArgumentException("Tambahkan atau pilih kategori terlebih dahulu.");
                String nilai = nominal.getText();
                Validasi.nominal(nilai, false);
                LocalDate hari = Validasi.tanggal(tanggal.getText());
                String teks = catatan.getText();
                kerjakan(() -> service.catat(pilihanJenis, nilai, pilihanKategori.id(), hari, teks), hasil -> {
                    dispose();
                    setelahSimpan.run();
                    String pesan = hasil.transaksi().getJenis() + " " + FormatData.rupiah(hasil.transaksi().getNominal()) + " berhasil disimpan.";
                    int ikon = JOptionPane.INFORMATION_MESSAGE;
                    if (hasil.anggaran() != null) {
                        RingkasanAnggaran anggaran = hasil.anggaran();
                        pesan += "\n\nAnggaran " + FormatData.periode(anggaran.periode()) + ": " + anggaran.status()
                                + "\n" + FormatData.detailAnggaran(anggaran);
                        if (anggaran.status() == StatusAnggaran.MENCAPAI_BATAS || anggaran.status() == StatusAnggaran.TERLAMPAUI) ikon = JOptionPane.WARNING_MESSAGE;
                    }
                    Gaya.pesan(induk, "Transaksi tersimpan", pesan, ikon);
                });
            } catch (IllegalArgumentException gagal) { validasiGagal(gagal); }
        });
        tampilkanUkuran(640);
    }

    private void muatKategori(Kategori pilihan) {
        kategori.removeAllItems();
        for (Kategori item : daftar) if (item.jenis() == jenis.getSelectedItem()) kategori.addItem(item);
        if (pilihan != null) kategori.setSelectedItem(pilihan);
    }
}
