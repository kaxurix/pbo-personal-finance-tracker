package id.aruskas.ui;

import id.aruskas.domain.RingkasanAnggaran;
import id.aruskas.service.*;

import javax.swing.*;
import java.awt.*;
import java.time.YearMonth;

final class DialogAnggaran extends DialogForm {
    private final JTextField periode = new JTextField(YearMonth.now().toString());
    private final JTextField nominal = new JTextField();
    private final JTextArea informasi = new JTextArea(4, 30);

    DialogAnggaran(Window induk, DompetService service, Runnable setelahSimpan) {
        super(induk, "Anggaran Bulanan");
        JPanel pemilih = new JPanel(new BorderLayout(8, 0));
        pemilih.setOpaque(false);
        JButton lihat = Gaya.tombol("Lihat Periode", false);
        pemilih.add(periode, BorderLayout.CENTER); pemilih.add(lihat, BorderLayout.EAST);
        tambah("Tahun dan bulan · YYYY-MM", pemilih);
        nominal.putClientProperty("JTextField.placeholderText", "Contoh: 1000000; boleh 0");
        tambah("Batas rupiah · angka bulat tanpa pemisah", nominal);
        informasi.setEditable(false); informasi.setLineWrap(true); informasi.setWrapStyleWord(true);
        informasi.setOpaque(false); informasi.setForeground(Gaya.REDUP);
        tambah("Status periode", informasi);
        petunjuk.setText("Simpan memperbarui batas periode yang diisi. Batas boleh Rp0.");
        lihat.addActionListener(event -> lihat(service));
        simpan.addActionListener(event -> {
            try {
                YearMonth bulan = Validasi.periode(periode.getText());
                String batas = nominal.getText();
                Validasi.nominal(batas, true);
                kerjakan(() -> service.simpanAnggaran(bulan, batas), hasil -> {
                    dispose(); setelahSimpan.run();
                    Gaya.pesan(induk, "Anggaran tersimpan", "Batas anggaran " + FormatData.periode(bulan) + " berhasil disimpan.\n\n"
                            + hasil.status() + "\n" + FormatData.detailAnggaran(hasil), JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (IllegalArgumentException gagal) { validasiGagal(gagal); }
        });
        tampilkanUkuran(490);
        lihat(service);
    }

    private void lihat(DompetService service) {
        try {
            YearMonth bulan = Validasi.periode(periode.getText());
            kerjakan(() -> service.lihatAnggaran(bulan), hasil -> {
                nominal.setText(hasil.batas() == null ? "" : hasil.batas().toString());
                informasi.setText(FormatData.periode(bulan) + " · " + hasil.status() + "\n" + FormatData.detailAnggaran(hasil));
            });
        } catch (IllegalArgumentException gagal) { validasiGagal(gagal); }
    }
}
