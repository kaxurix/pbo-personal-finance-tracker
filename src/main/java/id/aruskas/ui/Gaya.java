package id.aruskas.ui;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public final class Gaya {
    public static final Color BIRU = new Color(22, 49, 85);
    public static final Color LATAR = new Color(244, 247, 251);
    public static final Color TEKS = new Color(31, 46, 65);
    public static final Color REDUP = new Color(91, 108, 129);
    public static final Color HIJAU = new Color(15, 112, 83);
    public static final Color MERAH = new Color(177, 45, 56);
    public static final Color KUNING = new Color(137, 91, 9);
    private Gaya() { }

    public static void pasang() {
        FlatLightLaf.setup();
        UIManager.put("defaultFont", new Font("SansSerif", Font.PLAIN, 14));
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Component.focusColor", BIRU);
        UIManager.put("Table.rowHeight", 38);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.gridColor", new Color(235, 239, 245));
        UIManager.put("Table.selectionBackground", new Color(221, 232, 247));
        UIManager.put("Table.selectionForeground", TEKS);
        UIManager.put("OptionPane.yesButtonText", "Ya");
        UIManager.put("OptionPane.noButtonText", "Tidak");
        UIManager.put("OptionPane.cancelButtonText", "Batal");
        UIManager.put("OptionPane.okButtonText", "Tutup");
        UIManager.put("FileChooser.saveButtonText", "Simpan");
        UIManager.put("FileChooser.cancelButtonText", "Batal");
        UIManager.put("FileChooser.fileNameLabelText", "Nama berkas:");
        UIManager.put("FileChooser.filesOfTypeLabelText", "Jenis berkas:");
        UIManager.put("FileChooser.lookInLabelText", "Lihat di:");
        UIManager.put("FileChooser.saveInLabelText", "Simpan di:");
    }

    public static JLabel label(String teks, int ukuran, Color warna, boolean tebal) {
        JLabel label = new JLabel(teks);
        label.setFont(label.getFont().deriveFont(tebal ? Font.BOLD : Font.PLAIN, ukuran));
        label.setForeground(warna);
        return label;
    }

    public static JButton tombol(String teks, boolean utama) {
        JButton tombol = new JButton(teks);
        tombol.setMargin(new Insets(8, 15, 8, 15));
        if (utama) { tombol.setBackground(BIRU); tombol.setForeground(Color.WHITE); }
        return tombol;
    }

    public static Border tepi(int ruang) { return BorderFactory.createEmptyBorder(ruang, ruang, ruang, ruang); }

    public static JPanel kartu() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(224, 231, 241)), tepi(16)));
        return panel;
    }

    public static JPanel kolom(Component... komponen) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < komponen.length; i++) {
            c.gridy = i;
            c.insets = new Insets(0, 0, i == komponen.length - 1 ? 0 : 6, 0);
            panel.add(komponen[i], c);
        }
        return panel;
    }

    public static JPanel bidang(String teks, JComponent komponen) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        JLabel label = label(teks, 13, TEKS, true);
        label.setLabelFor(komponen);
        panel.add(label, BorderLayout.NORTH);
        panel.add(komponen, BorderLayout.CENTER);
        return panel;
    }

    public static void pesan(Component induk, String judul, String teks, int jenis) {
        // JTextArea mencegah nama/catatan pengguna ditafsirkan sebagai HTML oleh JLabel.
        JTextArea isi = new JTextArea(teks);
        isi.setEditable(false);
        isi.setOpaque(false);
        isi.setLineWrap(true);
        isi.setWrapStyleWord(true);
        isi.setColumns(42);
        isi.setFont(UIManager.getFont("Label.font"));
        JOptionPane.showMessageDialog(induk, isi, judul, jenis);
    }
}
