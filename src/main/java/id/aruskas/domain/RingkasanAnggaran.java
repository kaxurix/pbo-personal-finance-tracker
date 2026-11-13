package id.aruskas.domain;

import java.time.YearMonth;
import java.util.OptionalDouble;

/** batas null berarti anggaran belum diatur, berbeda dari batas Rp0. */
public record RingkasanAnggaran(YearMonth periode, Long batas, long realisasi) {
    public StatusAnggaran status() {
        if (batas == null) return StatusAnggaran.BELUM_DIATUR;
        if (realisasi < batas) return StatusAnggaran.DALAM_BATAS;
        if (realisasi == batas) return StatusAnggaran.MENCAPAI_BATAS;
        return StatusAnggaran.TERLAMPAUI;
    }

    public long sisaAtauKelebihan() { return batas == null ? 0 : Math.abs(batas - realisasi); }

    public OptionalDouble persentase() {
        return batas == null || batas == 0 ? OptionalDouble.empty()
                : OptionalDouble.of((double) realisasi / batas * 100);
    }
}
