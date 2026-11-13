package id.aruskas.service;

import id.aruskas.domain.RingkasanAnggaran;
import id.aruskas.domain.StatusAnggaran;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class FormatData {
    private static final Locale INDONESIA = Locale.forLanguageTag("id-ID");
    private FormatData() { }
    public static String rupiah(long nilai) { return (nilai < 0 ? "−Rp" : "Rp") + NumberFormat.getIntegerInstance(INDONESIA).format(nilai < 0 ? -nilai : nilai); }
    public static String tanggal(LocalDate tanggal) { return tanggal.format(DateTimeFormatter.ofPattern("dd-MM-uuuu")); }
    public static String periode(YearMonth periode) { return periode.format(DateTimeFormatter.ofPattern("MMMM uuuu", INDONESIA)); }
    public static String detailAnggaran(RingkasanAnggaran ringkasan) {
        if (ringkasan.batas() == null) return "Realisasi " + rupiah(ringkasan.realisasi()) + " · Batas belum diatur";
        String selisih = ringkasan.status() == StatusAnggaran.TERLAMPAUI ? "Kelebihan " : "Sisa ";
        return "Realisasi " + rupiah(ringkasan.realisasi()) + " · Batas " + rupiah(ringkasan.batas())
                + " · " + selisih + rupiah(ringkasan.sisaAtauKelebihan());
    }
}
