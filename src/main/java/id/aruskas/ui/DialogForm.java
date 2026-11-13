package id.aruskas.ui;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Kerangka kecil untuk formulir modal dan status penyimpanan. */
abstract class DialogForm extends JDialog {
    protected final JPanel bidang = new JPanel(new GridBagLayout());
    protected final JButton simpan = Gaya.tombol("Simpan", true);
    protected final JButton batal = Gaya.tombol("Batal", false);
    protected final JLabel petunjuk = Gaya.label(" ", 12, Gaya.REDUP, false);
    private int baris;

    DialogForm(Window induk, String judul) {
        super(induk, judul, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel akar = new JPanel(new BorderLayout(0, 18));
        akar.setBackground(Gaya.LATAR);
        akar.setBorder(Gaya.tepi(24));
        akar.add(Gaya.label(judul, 24, Gaya.BIRU, true), BorderLayout.NORTH);
        bidang.setOpaque(false);
        JScrollPane gulir = new JScrollPane(bidang);
        gulir.setBorder(BorderFactory.createEmptyBorder());
        gulir.getVerticalScrollBar().setUnitIncrement(16);
        akar.add(gulir, BorderLayout.CENTER);
        JPanel bawah = new JPanel(new BorderLayout(0, 12));
        bawah.setOpaque(false);
        bawah.add(petunjuk, BorderLayout.NORTH);
        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tombol.setOpaque(false);
        tombol.add(batal); tombol.add(simpan);
        bawah.add(tombol, BorderLayout.SOUTH);
        akar.add(bawah, BorderLayout.SOUTH);
        setContentPane(akar);
        batal.addActionListener(event -> dispose());
        getRootPane().setDefaultButton(simpan);
        getRootPane().registerKeyboardAction(event -> { if (batal.isEnabled()) dispose(); },
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setMinimumSize(new Dimension(470, 400));
    }

    protected void tambah(String label, JComponent komponen) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = baris++; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 14, 0);
        c.anchor = GridBagConstraints.NORTH;
        bidang.add(Gaya.bidang(label, komponen), c);
    }

    protected void tampilkanUkuran(int tinggi) {
        setSize(540, tinggi);
        setLocationRelativeTo(getOwner());
    }

    protected <T> void kerjakan(Callable<T> tugas, Consumer<T> berhasil) {
        sibuk(true);
        Pekerja.jalankan(this, tugas, berhasil, () -> sibuk(false));
    }

    protected void sibuk(boolean nilai) {
        simpan.setEnabled(!nilai); batal.setEnabled(!nilai);
        setDefaultCloseOperation(nilai ? DO_NOTHING_ON_CLOSE : DISPOSE_ON_CLOSE);
        for (Component komponen : bidang.getComponents()) aktifkan(komponen, !nilai);
        setCursor(Cursor.getPredefinedCursor(nilai ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void aktifkan(Component komponen, boolean nilai) {
        komponen.setEnabled(nilai);
        if (komponen instanceof Container kontainer) for (Component anak : kontainer.getComponents()) aktifkan(anak, nilai);
    }

    protected void validasiGagal(IllegalArgumentException gagal) {
        Gaya.pesan(this, "Periksa isian", gagal.getMessage(), JOptionPane.WARNING_MESSAGE);
    }
}
