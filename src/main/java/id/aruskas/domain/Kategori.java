package id.aruskas.domain;

import java.util.Objects;

public record Kategori(long id, String nama, JenisTransaksi jenis) {
    public Kategori {
        Objects.requireNonNull(nama, "Nama kategori wajib diisi.");
        Objects.requireNonNull(jenis, "Jenis kategori wajib dipilih.");
        nama = nama.strip();
        if (nama.isEmpty() || nama.length() > 60) {
            throw new IllegalArgumentException("Nama kategori harus berisi 1–60 karakter.");
        }
    }

    @Override public String toString() { return nama; }
}
