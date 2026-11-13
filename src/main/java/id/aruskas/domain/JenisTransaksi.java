package id.aruskas.domain;

public enum JenisTransaksi {
    PEMASUKAN("Pemasukan"), PENGELUARAN("Pengeluaran");

    private final String label;

    JenisTransaksi(String label) { this.label = label; }

    @Override public String toString() { return label; }
}
