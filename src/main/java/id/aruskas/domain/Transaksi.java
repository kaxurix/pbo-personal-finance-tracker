package id.aruskas.domain;

import java.time.LocalDate;
import java.util.Objects;

/** Enkapsulasi data bersama; subclass menentukan dampaknya pada saldo. */
public abstract class Transaksi {
    private final long id;
    private final long nominal;
    private final Kategori kategori;
    private final LocalDate tanggal;
    private final String catatan;

    protected Transaksi(long id, long nominal, Kategori kategori, LocalDate tanggal,
                        String catatan, JenisTransaksi jenis) {
        if (nominal <= 0) throw new IllegalArgumentException("Nominal harus lebih dari Rp0.");
        this.kategori = Objects.requireNonNull(kategori, "Kategori wajib dipilih.");
        if (kategori.jenis() != jenis) throw new IllegalArgumentException("Kategori tidak sesuai jenis transaksi.");
        this.tanggal = Objects.requireNonNull(tanggal, "Tanggal wajib diisi.");
        if (tanggal.getYear() < 1 || tanggal.getYear() > 9999) {
            throw new IllegalArgumentException("Tahun harus berada antara 0001 dan 9999.");
        }
        this.catatan = catatan == null ? "" : catatan.strip();
        if (this.catatan.length() > 1000) throw new IllegalArgumentException("Catatan maksimal 1.000 karakter.");
        this.id = id;
        this.nominal = nominal;
    }

    public long getId() { return id; }
    public long getNominal() { return nominal; }
    public Kategori getKategori() { return kategori; }
    public LocalDate getTanggal() { return tanggal; }
    public String getCatatan() { return catatan; }
    public JenisTransaksi getJenis() { return kategori.jenis(); }
    public abstract long dampakSaldo();
}
