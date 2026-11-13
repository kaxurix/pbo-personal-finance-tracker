package id.aruskas.ui;

import id.aruskas.domain.Transaksi;
import id.aruskas.service.FormatData;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDate;
import java.util.List;

public final class ModelTabelTransaksi extends AbstractTableModel {
    private static final String[] KOLOM = {"Tanggal", "Jenis", "Kategori", "Nominal", "Catatan"};
    private List<Transaksi> baris = List.of();
    public void isi(List<Transaksi> data) { baris = List.copyOf(data); fireTableDataChanged(); }
    @Override public int getRowCount() { return baris.size(); }
    @Override public int getColumnCount() { return KOLOM.length; }
    @Override public String getColumnName(int kolom) { return KOLOM[kolom]; }
    @Override public Class<?> getColumnClass(int kolom) { return kolom == 0 ? LocalDate.class : kolom == 3 ? Long.class : String.class; }
    @Override public Object getValueAt(int baris, int kolom) {
        Transaksi item = this.baris.get(baris);
        return switch (kolom) {
            case 0 -> item.getTanggal();
            case 1 -> item.getJenis().toString();
            case 2 -> item.getKategori().nama();
            case 3 -> item.getNominal();
            default -> item.getCatatan();
        };
    }
}
