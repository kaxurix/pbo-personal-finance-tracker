package id.aruskas.ui;

import id.aruskas.domain.*;
import id.aruskas.service.DompetService;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

final class DialogKategori extends DialogForm {
    DialogKategori(Window induk, DompetService service, Runnable setelahSimpan) {
        this(induk, service, kategori -> setelahSimpan.run(), JenisTransaksi.PEMASUKAN);
    }

    DialogKategori(Window induk, DompetService service, Consumer<Kategori> setelahSimpan, JenisTransaksi pilihanAwal) {
        super(induk, "Tambah Kategori");
        JTextField nama = new JTextField();
        JComboBox<JenisTransaksi> jenis = new JComboBox<>(JenisTransaksi.values());
        jenis.setSelectedItem(pilihanAwal);
        tambah("Nama kategori · maksimal 60 karakter", nama);
        tambah("Jenis transaksi", jenis);
        petunjuk.setText("Kategori baru langsung tersedia untuk jenis transaksi yang dipilih.");
        simpan.addActionListener(event -> {
            try {
                Kategori baru = new Kategori(0, nama.getText(), (JenisTransaksi) jenis.getSelectedItem());
                kerjakan(() -> service.tambahKategori(baru.nama(), baru.jenis()), hasil -> {
                    dispose();
                    setelahSimpan.accept(hasil);
                    Gaya.pesan(induk, "Kategori tersimpan", "Kategori “" + hasil.nama() + "” berhasil ditambahkan.", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (IllegalArgumentException gagal) { validasiGagal(gagal); }
        });
        tampilkanUkuran(410);
    }
}
