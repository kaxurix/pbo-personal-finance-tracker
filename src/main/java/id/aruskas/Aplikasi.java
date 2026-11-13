package id.aruskas;

import id.aruskas.repository.Database;
import id.aruskas.service.DompetService;
import id.aruskas.ui.*;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

public final class Aplikasi {
    private Aplikasi() { }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Arus Kas Harian memerlukan lingkungan desktop. Jalankan id.aruskas.Aplikasi dari IntelliJ dengan JDK 21.");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            Gaya.pasang();
            Path lokasi = Path.of(System.getProperty("aruskas.db", "data/arus-kas.db"));
            Database database = new Database(lokasi);
            JDialog memuat = new JDialog((Frame) null, "Arus Kas Harian");
            JPanel panel = new JPanel(new BorderLayout(12, 12));
            panel.setBorder(Gaya.tepi(24));
            panel.add(new JLabel("Menyiapkan database lokal…"), BorderLayout.CENTER);
            JProgressBar progres = new JProgressBar(); progres.setIndeterminate(true);
            panel.add(progres, BorderLayout.SOUTH);
            memuat.setContentPane(panel); memuat.pack(); memuat.setLocationRelativeTo(null);
            memuat.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            memuat.setVisible(true);
            Pekerja.jalankan(memuat, () -> { database.inisialisasi(); return new DompetService(database); }, service -> {
                memuat.dispose();
                new JendelaUtama(service, database.lokasi()).setVisible(true);
            }, memuat::dispose);
        });
    }
}
