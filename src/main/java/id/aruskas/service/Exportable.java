package id.aruskas.service;

import java.io.IOException;
import java.nio.file.Path;

public interface Exportable {
    void ekspor(LaporanRiwayat laporan, Path tujuan) throws IOException;
}
