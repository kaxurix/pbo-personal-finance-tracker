package id.aruskas.service;

import id.aruskas.domain.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class EksporHtml implements Exportable {
    @Override public void ekspor(LaporanRiwayat laporan, Path tujuan) throws IOException {
        StringBuilder html = new StringBuilder("""
            <!doctype html>
            <html lang="id"><head><meta charset="UTF-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <title>Laporan Arus Kas Harian</title>
            <style>
            body{font:15px/1.6 system-ui,sans-serif;color:#1c2d45;background:#f4f6fa;margin:0;padding:40px}
            main{max-width:1100px;margin:auto;background:white;padding:32px;border-radius:12px}
            h1{color:#142f56}h2{margin-top:32px}table{width:100%;border-collapse:collapse}
            th,td{padding:12px;text-align:left;border-bottom:1px solid #dde4ee;vertical-align:top}
            th{background:#edf2fa}td{white-space:pre-wrap;overflow-wrap:anywhere}.angka{text-align:right;white-space:nowrap}
            .total{background:#142f56;color:white;padding:18px;border-radius:8px}.tabel{overflow-x:auto}
            @media print{body{padding:0;background:white}main{padding:0}thead{display:table-header-group}}
            </style></head><body><main><h1>Arus Kas Harian</h1><p>Laporan transaksi · Satu dompet lokal</p>
            """);
        FilterRiwayat filter = laporan.filter();
        html.append("<h2>Kriteria filter</h2><p>Kategori: ").append(escape(laporan.namaKategori()))
                .append("<br>Tanggal awal: ").append(filter.tanggalAwal() == null ? "Tidak dibatasi" : FormatData.tanggal(filter.tanggalAwal()))
                .append("<br>Tanggal akhir: ").append(filter.tanggalAkhir() == null ? "Tidak dibatasi" : FormatData.tanggal(filter.tanggalAkhir()))
                .append("<br>Kedua tanggal batas disertakan. Jumlah transaksi: ").append(laporan.transaksi().size()).append("</p>");
        RingkasanDompet total = laporan.total();
        html.append("<div class=\"total\">Total pemasukan: ").append(FormatData.rupiah(total.pemasukan()))
                .append("<br>Total pengeluaran: ").append(FormatData.rupiah(total.pengeluaran()))
                .append("<br>Selisih hasil filter: ").append(FormatData.rupiah(total.saldo())).append("</div>")
                .append("<h2>Rincian transaksi</h2><div class=\"tabel\"><table><thead><tr><th>Tanggal</th><th>Jenis</th><th>Kategori</th><th class=\"angka\">Nominal</th><th>Catatan</th></tr></thead><tbody>");
        for (Transaksi transaksi : laporan.transaksi()) {
            html.append("<tr><td>").append(FormatData.tanggal(transaksi.getTanggal())).append("</td><td>")
                    .append(transaksi.getJenis()).append("</td><td>").append(escape(transaksi.getKategori().nama()))
                    .append("</td><td class=\"angka\">").append(FormatData.rupiah(transaksi.getNominal()))
                    .append("</td><td>").append(escape(transaksi.getCatatan())).append("</td></tr>\n");
        }
        if (laporan.transaksi().isEmpty()) html.append("<tr><td colspan=\"5\">Tidak ada transaksi yang sesuai filter.</td></tr>");
        html.append("</tbody></table></div><h2>Anggaran per periode</h2>")
                .append("<p>Periode mengikuti transaksi pada hasil filter. Realisasi anggaran memakai seluruh pengeluaran pada bulan dan tahun tersebut, termasuk yang berada di luar filter.</p>");
        if (laporan.anggaran().isEmpty()) html.append("<p>Tidak ada periode transaksi dalam hasil filter.</p>");
        for (RingkasanAnggaran anggaran : laporan.anggaran()) {
            html.append("<p><strong>").append(FormatData.periode(anggaran.periode())).append(" · ").append(anggaran.status())
                    .append("</strong><br>").append(FormatData.detailAnggaran(anggaran));
            if (anggaran.persentase().isPresent()) html.append(" · ").append(String.format(java.util.Locale.forLanguageTag("id-ID"), "%.1f%%", anggaran.persentase().getAsDouble()));
            html.append("</p>");
        }
        html.append("</main></body></html>");
        // Tulis ke file sementara dahulu agar laporan lama tidak rusak ketika penulisan gagal.
        Path absolut = tujuan.toAbsolutePath();
        Path sementara = Files.createTempFile(absolut.getParent(), ".arus-kas-", ".tmp");
        try {
            Files.writeString(sementara, html, StandardCharsets.UTF_8);
            try { Files.move(sementara, absolut, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException gagal) { Files.move(sementara, absolut, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(sementara); }
    }

    public static String escape(String teks) {
        return teks.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
