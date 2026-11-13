package id.aruskas.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public final class Validasi {
    private static final DateTimeFormatter TANGGAL = DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT);
    private Validasi() { }

    public static long nominal(String teks, boolean bolehNol) {
        if (teks == null || !teks.strip().matches("[0-9]+")) {
            throw new IllegalArgumentException("Isi nominal dengan angka rupiah bulat tanpa titik, koma, atau tanda. Contoh: 300000.");
        }
        try {
            long nilai = Long.parseLong(teks.strip());
            if (!bolehNol && nilai == 0) throw new IllegalArgumentException("Nominal transaksi harus lebih dari Rp0.");
            return nilai;
        } catch (NumberFormatException gagal) {
            throw new IllegalArgumentException("Nominal terlalu besar. Maksimal 9.223.372.036.854.775.807 rupiah.");
        }
    }

    public static LocalDate tanggal(String teks) {
        try {
            if (teks == null || !teks.strip().matches("[0-9]{2}-[0-9]{2}-[0-9]{4}")) throw new DateTimeParseException("", "", 0);
            LocalDate tanggal = LocalDate.parse(teks.strip(), TANGGAL);
            if (tanggal.getYear() < 1) throw new DateTimeParseException("", "", 0);
            return tanggal;
        } catch (DateTimeParseException gagal) {
            throw new IllegalArgumentException("Isi tanggal yang valid dengan format DD-MM-YYYY. Contoh: 12-11-2026.");
        }
    }

    public static LocalDate tanggalOpsional(String teks) { return teks == null || teks.isBlank() ? null : tanggal(teks); }

    public static YearMonth periode(String teks) {
        try {
            if (teks == null || !teks.strip().matches("[0-9]{4}-[0-9]{2}")) throw new DateTimeParseException("", "", 0);
            YearMonth periode = YearMonth.parse(teks.strip());
            if (periode.getYear() < 1) throw new DateTimeParseException("", "", 0);
            return periode;
        } catch (DateTimeParseException gagal) {
            throw new IllegalArgumentException("Isi periode dengan format YYYY-MM. Contoh: 2026-11.");
        }
    }
}
