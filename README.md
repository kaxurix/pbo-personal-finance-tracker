# Arus Kas Harian

Aplikasi desktop proyek PBO: **Java 21, Maven, Swing + FlatLaf, SQLite JDBC**. Satu pengguna, satu dompet lokal, saldo awal Rp0. Tidak memerlukan server atau login.

## Menjalankan dari IntelliJ IDEA

1. Pilih **File → Open**, buka `pom.xml` di folder ini sebagai proyek Maven. Jika folder sudah terbuka, klik kanan `pom.xml` → **Add as Maven Project**, lalu **Reload All Maven Projects**.
2. Buka **File → Project Structure → Project SDK**, pilih **JDK 21**. Pada **Settings → Build Tools → Maven → Runner**, pilih JDK 21 dan Maven bawaan IntelliJ. Tunggu impor dependensi selesai.
3. Buka `src/main/java/id/aruskas/Aplikasi.java`, klik segitiga hijau di sebelah `main()`, lalu **Run 'Aplikasi.main()'**. Gunakan folder proyek sebagai **Working directory**.
4. Database otomatis dibuat pada **`data/arus-kas.db`** relatif terhadap working directory. Lokasi lengkap juga tersedia pada tooltip keterangan di bagian bawah jendela. Jangan menghapus file ini jika ingin mempertahankan data.

Konfigurasi Maven memakai cache proyek `.tools/m2` melalui `.mvn/maven.config`. Dependensi yang sudah diunduh tersedia di cache lokal proyek ini; jika cache belum ada, Maven mengunduhnya dari Maven Central. Cache tidak perlu dikumpulkan sebagai kode sumber. Jika jaringan IntelliJ dibatasi, gunakan cache yang sudah tersedia dan aktifkan **Work offline** pada panel Maven.

Alternatif melalui terminal dengan Java 21 dan Maven di PATH:

```sh
mvn test
mvn package
java -jar target/arus-kas-harian.jar
```

`mvn exec:java` juga dapat dipakai. JAR hasil `package` sudah menyertakan dependensi. Untuk lokasi database lain, tambahkan VM option `-Daruskas.db=/lokasi/pilihan/kas.db` (sebelum `-jar` jika memakai terminal).

## Petunjuk penggunaan

- Mulai dengan **Tambah Transaksi → Pemasukan**. Nominal menggunakan angka bulat tanpa pemisah, misalnya `300000`; aplikasi menampilkan `Rp300.000`. Tanggal menggunakan `DD-MM-YYYY`.
- Pengeluaran ditolak jika melebihi saldo tersedia; nominal yang sama dengan saldo diterima. Saldo menggunakan seluruh transaksi tersimpan, termasuk transaksi yang tanggalnya berada pada bulan lain.
- Tombol **Kategori**, atau **+ Kategori** dalam formulir, menambahkan kategori pemasukan/pengeluaran. Nama maksimal 60 karakter; nama yang sama boleh ada pada jenis berbeda.
- **Atur Anggaran** menerima periode `YYYY-MM`. Klik **Lihat Periode** untuk membaca batas tersimpan, lalu **Simpan** untuk menetapkan/memperbaruinya. Batas boleh `0`. Anggaran hanya memberi pemberitahuan; selama saldo cukup, pengeluaran tetap disimpan.
- Status: **Belum Diatur**, **Dalam Batas**, **Mencapai Batas**, dan **Terlampaui**. Anggaran Rp0 tidak menampilkan persentase. Hasil pencatatan mengikuti periode tanggal transaksi; dashboard selalu mengikuti bulan berjalan.
- Di riwayat, pilih kategori dan/atau tanggal awal–akhir, lalu **Terapkan**. Keduanya dapat digabung; tanggal batas ikut dihitung. Kolom tanggal boleh kosong. **Reset** mengembalikan semua transaksi.
- **Ekspor HTML** menyimpan laporan UTF-8 melalui dialog simpan, dengan baris dan urutan yang sama seperti riwayat yang terakhir diterapkan. Perubahan isian filter baru berlaku setelah **Terapkan**. Laporan berisi kriteria, total pemasukan/pengeluaran, selisih, rincian, dan status anggaran setiap periode transaksi. Realisasi anggaran tetap memakai seluruh pengeluaran periode tersebut, termasuk kategori/tanggal di luar filter.

## Struktur untuk presentasi PBO

| Package | Tanggung jawab |
| --- | --- |
| `domain` | Entitas dan nilai: `Transaksi`, `Pemasukan`, `Pengeluaran`, `Kategori`, `AnggaranBulanan`, status, filter, dan ringkasan. |
| `service` | `DompetService` memvalidasi saldo/anggaran; `Validasi` membaca input; kontrak `Exportable` diimplementasikan `EksporHtml`. |
| `repository` | Koneksi/skema SQLite dan prepared statement pada `RepositoryTransaksi`, `RepositoryKategori`, `RepositoryAnggaran`. |
| `ui` | Formulir, dashboard, tabel, dialog simpan, dan `SwingWorker` untuk I/O di luar EDT. |

`Transaksi` abstrak mengenkapsulasi atribut privat. `Pemasukan.dampakSaldo()` positif dan `Pengeluaran.dampakSaldo()` negatif. `DompetService` dan `RingkasanDompet` memanggil metode tersebut melalui referensi **`Transaksi`**: contoh pewarisan dan polimorfisme yang benar-benar digunakan.

Tabel SQLite: `kategori`, `transaksi` (foreign key kategori), dan `anggaran_bulanan` (primary key gabungan tahun/bulan). Nominal disimpan sebagai integer 64 bit (`long`), tanggal sebagai teks ISO yang dipetakan ke `LocalDate`, dan periode ke `YearMonth`. Foreign key aktif pada setiap koneksi. SQLite `BEGIN IMMEDIATE` mengunci penulis **sebelum saldo dibaca**; validasi, perhitungan realisasi calon, dan insert memakai satu koneksi serta commit/rollback yang sama. Tampilan berubah setelah commit. Total yang melampaui kapasitas `long` ditolak.

## Verifikasi

Pengujian memakai file SQLite sementara terpisah dari data pengguna. Jalankan `mvn test`; laporan terdapat di `target/surefire-reports/`.

Hasil verifikasi pada lingkungan pengerjaan: **28 pengujian lulus, 0 gagal, 0 dilewati** (27 kasus service/SQLite dan 1 alur antarmuka Swing). Build Maven `package` berhasil; paket `target/arus-kas-harian.jar` juga berhasil membuka dashboard melalui entry point `Aplikasi.main`, memuat FlatLaf dan driver SQLite, membuat skema otomatis, serta menampilkan saldo awal Rp0. Data uji tidak dimasukkan ke database pengguna.

- Saldo Rp0, validasi nominal, saldo kurang, pengeluaran tepat saldo, dan overflow.
- Status anggaran tepat/di atas batas, batas Rp0, pembaruan satu periode, dan transaksi lintas bulan/tahun.
- Persistensi transaksi, kategori, dan anggaran setelah database dibuka ulang; foreign key, tipe integer, rollback, dan dua pengeluaran serentak.
- Filter gabungan dengan batas inklusif, reset, ekspor sesuai filter, escaping teks pengguna, dan anggaran beberapa periode.
- Smoke test Swing melalui formulir kategori/transaksi/anggaran, pemberitahuan **Mencapai Batas**, filter/reset, dialog ekspor, pembukaan ulang jendela, serta render ukuran 1200×900 dan 1000×760. Screenshot ada di `target/ui-review/`. Pengujian UI dilewati otomatis pada lingkungan headless; jalankan dari desktop IntelliJ untuk mencobanya.

Tampilan menggunakan latar terang, kartu saldo biru tua, dan status berupa teks beserta warna. Komponen dibuat langsung dengan Swing tanpa referensi visual eksternal. Dokumentasi dependensi: [FlatLaf](https://www.formdev.com/flatlaf/) dan [SQLite JDBC](https://github.com/xerial/sqlite-jdbc).
