package id.aruskas.ui;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Semua I/O berjalan di SwingWorker; callback selalu di Event Dispatch Thread. */
public final class Pekerja {
    private Pekerja() { }
    public static <T> void jalankan(Component induk, Callable<T> tugas, Consumer<T> berhasil, Runnable selesai) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return tugas.call(); }
            @Override protected void done() {
                try { berhasil.accept(get()); }
                catch (InterruptedException gagal) { Thread.currentThread().interrupt(); }
                catch (ExecutionException gagal) {
                    Throwable sebab = gagal.getCause();
                    String pesan = sebab instanceof IllegalArgumentException ? sebab.getMessage()
                            : sebab instanceof SQLException ? "Operasi database gagal. Data tidak diubah oleh operasi yang gagal. Pastikan folder dapat ditulis dan coba lagi.\n\n" + sebab.getMessage()
                            : sebab instanceof IOException ? "Berkas tidak dapat dibaca atau ditulis. Periksa lokasi dan izin folder.\n\n" + sebab.getMessage()
                            : "Operasi gagal: " + sebab.getMessage();
                    Gaya.pesan(induk, "Operasi belum berhasil", pesan, JOptionPane.ERROR_MESSAGE);
                } finally { selesai.run(); }
            }
        }.execute();
    }
}
