package id.aruskas.domain;

import java.time.YearMonth;
import java.util.Objects;

public record AnggaranBulanan(YearMonth periode, long batas) {
    public AnggaranBulanan {
        Objects.requireNonNull(periode, "Periode wajib diisi.");
        if (periode.getYear() < 1 || periode.getYear() > 9999) {
            throw new IllegalArgumentException("Tahun harus berada antara 0001 dan 9999.");
        }
        if (batas < 0) throw new IllegalArgumentException("Batas anggaran tidak boleh negatif.");
    }
}
