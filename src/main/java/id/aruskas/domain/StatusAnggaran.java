package id.aruskas.domain;

public enum StatusAnggaran {
    BELUM_DIATUR("Belum Diatur"), DALAM_BATAS("Dalam Batas"),
    MENCAPAI_BATAS("Mencapai Batas"), TERLAMPAUI("Terlampaui");

    private final String label;
    StatusAnggaran(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
