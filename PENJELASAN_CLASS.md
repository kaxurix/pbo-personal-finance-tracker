# Penjelasan Class dan File Proyek Arus Kas Harian

## 1. Gambaran umum

Arus Kas Harian adalah aplikasi desktop untuk mencatat pemasukan, pengeluaran, kategori, dan anggaran bulanan. Aplikasi menggunakan Java 21, Swing, FlatLaf, serta SQLite. Data disimpan secara lokal untuk satu pengguna dan satu dompet dengan saldo awal Rp0.

Kode utama terdiri dari **31 file Java**. Pengujiannya terdiri dari **2 file Java**, sehingga total ada **33 file Java** yang dijelaskan di dokumen ini.

### Jenis deklarasi yang digunakan

| Jenis | Jumlah pada tingkat teratas di kode utama | Penjelasan |
| --- | ---: | --- |
| Class konkret | 18 | Class yang dapat memiliki implementasi lengkap, misalnya `DompetService` dan `JendelaUtama`. Beberapa merupakan class utilitas dengan constructor privat. |
| Class abstrak | 2 | Class dasar yang tidak dapat dibuat objeknya secara langsung: `Transaksi` dan `DialogForm`. |
| Record | 8 | Class khusus untuk membawa data, misalnya `Kategori` dan `DataTampilan`. Java menyediakan accessor, `equals()`, `hashCode()`, dan `toString()` secara otomatis, kecuali dioverride. |
| Enum | 2 | Tipe dengan pilihan nilai tetap: `JenisTransaksi` dan `StatusAnggaran`. |
| Interface | 1 | Kontrak perilaku ekspor: `Exportable`. |

Selain deklarasi tingkat teratas tersebut, `Database.java` memiliki interface bersarang `Database.Pekerjaan<T>`. Beberapa file UI juga membuat class anonim untuk `SwingWorker`, renderer, dan listener. Kedua file pengujian masing-masing mendefinisikan satu class pengujian.

### Pembagian package

| Lokasi | Tanggung jawab |
| --- | --- |
| `id.aruskas` | Menjalankan aplikasi dan menyiapkan komponen awal. |
| `id.aruskas.domain` | Menyimpan data dan perilaku dasar transaksi, kategori, filter, serta ringkasan. |
| `id.aruskas.repository` | Menjalankan SQL dan memetakan data SQLite menjadi objek Java. |
| `id.aruskas.service` | Mengatur aturan keuangan, validasi input, pemuatan data, dan ekspor. |
| `id.aruskas.ui` | Menampilkan jendela, dialog, tabel, dan menjalankan pekerjaan latar belakang. |

Alur utama aplikasi:

```text
Aplikasi.main()
    -> Database.inisialisasi()
    -> DompetService
    -> JendelaUtama dan dialog
        -> DompetService
            -> Database.dalamTransaksi()
                -> Repository
                    -> SQLite
```

Objek dari package `domain` dipakai oleh repository, service, dan UI. Ekspor memiliki alur `JendelaUtama -> Exportable -> EksporHtml -> file HTML`.

## 2. File titik masuk aplikasi

### 2.1. `Aplikasi.java`

**Lokasi:** [src/main/java/id/aruskas/Aplikasi.java](src/main/java/id/aruskas/Aplikasi.java)  
**Deklarasi:** `public final class Aplikasi`

Class ini menjadi titik masuk program melalui `main(String[] args)`. Constructor dibuat privat karena class ini hanya digunakan untuk menjalankan aplikasi.

Urutan kerjanya:

1. Memastikan lingkungan mendukung tampilan desktop; jika headless, menampilkan pesan ke terminal dan berhenti.
2. Menjadwalkan penyiapan UI melalui `SwingUtilities.invokeLater()`.
3. Memasang gaya tampilan melalui `Gaya.pasang()`.
4. Menentukan lokasi database dari properti `aruskas.db`, dengan nilai awal `data/arus-kas.db`.
5. Menampilkan dialog pemuatan dan menjalankan inisialisasi database melalui `Pekerja`.
6. Membuat `DompetService`, menutup dialog pemuatan, dan membuka `JendelaUtama` setelah berhasil.

## 3. Package `domain`

Package ini mendefinisikan bentuk data dan perilaku dasar aplikasi, tanpa menjalankan SQL atau membuat komponen Swing.

### 3.1. `Transaksi.java`

**Lokasi:** [src/main/java/id/aruskas/domain/Transaksi.java](src/main/java/id/aruskas/domain/Transaksi.java)  
**Deklarasi:** `public abstract class Transaksi`

`Transaksi` adalah induk untuk pemasukan dan pengeluaran. Objek transaksi selalu berupa subclass, yaitu `Pemasukan` atau `Pengeluaran`.

| Atribut privat `final` | Kegunaan |
| --- | --- |
| `id` | Identitas transaksi di database. |
| `nominal` | Nilai rupiah sebagai `long`. |
| `kategori` | Kategori sekaligus sumber informasi jenis transaksi. |
| `tanggal` | Tanggal transaksi sebagai `LocalDate`. |
| `catatan` | Catatan tambahan pengguna. |

Constructor `protected` memeriksa nominal lebih dari nol, kategori dan tanggal tidak kosong, jenis kategori sesuai transaksi, tahun 0001–9999, serta catatan maksimal 1.000 karakter. Catatan `null` diubah menjadi string kosong dan spasi di tepinya dibuang.

Method getter menyediakan akses baca terhadap atribut. `getJenis()` mengambil jenis dari kategori. Method abstrak `dampakSaldo()` wajib diimplementasikan subclass untuk menentukan perubahan saldo.

### 3.2. `Pemasukan.java`

**Lokasi:** [src/main/java/id/aruskas/domain/Pemasukan.java](src/main/java/id/aruskas/domain/Pemasukan.java)  
**Deklarasi:** `public final class Pemasukan extends Transaksi`

Class ini mewakili uang masuk. Constructornya meneruskan data ke constructor `Transaksi` dengan jenis `PEMASUKAN`, sehingga kategori pengeluaran tidak dapat dipakai.

Method `dampakSaldo()` mengembalikan `getNominal()` sebagai nilai positif. Contohnya, pemasukan Rp100.000 memberikan perubahan saldo `+100000`. Class ini digunakan ketika repository membentuk objek transaksi pemasukan.

### 3.3. `Pengeluaran.java`

**Lokasi:** [src/main/java/id/aruskas/domain/Pengeluaran.java](src/main/java/id/aruskas/domain/Pengeluaran.java)  
**Deklarasi:** `public final class Pengeluaran extends Transaksi`

Class ini mewakili uang keluar. Constructornya meneruskan data ke `Transaksi` dengan jenis `PENGELUARAN`.

Method `dampakSaldo()` mengembalikan `-getNominal()`. Contohnya, pengeluaran Rp25.000 memberikan perubahan saldo `-25000`. Nominal yang disimpan tetap positif; tanda negatif digunakan saat menghitung saldo. Pemeriksaan kecukupan saldo dilakukan oleh `DompetService`.

### 3.4. `Kategori.java`

**Lokasi:** [src/main/java/id/aruskas/domain/Kategori.java](src/main/java/id/aruskas/domain/Kategori.java)  
**Deklarasi:** `public record Kategori(long id, String nama, JenisTransaksi jenis)`

Record ini menyimpan identitas, nama, dan jenis kategori. Contohnya adalah kategori Gaji untuk pemasukan atau Transportasi untuk pengeluaran.

Constructor memastikan nama dan jenis tidak `null`, menghapus spasi di tepi nama, dan membatasi panjang nama menjadi 1–60 karakter. Accessor datanya adalah `id()`, `nama()`, dan `jenis()`.

`toString()` dioverride agar mengembalikan nama kategori saat ditampilkan pada komponen pilihan. Pemeriksaan nama duplikat pada jenis yang sama dilakukan oleh service dan didukung constraint database.

### 3.5. `JenisTransaksi.java`

**Lokasi:** [src/main/java/id/aruskas/domain/JenisTransaksi.java](src/main/java/id/aruskas/domain/JenisTransaksi.java)  
**Deklarasi:** `public enum JenisTransaksi`

Enum ini membatasi jenis transaksi menjadi `PEMASUKAN` dan `PENGELUARAN`. Atribut `label` menyimpan teks untuk pengguna, sedangkan `toString()` mengembalikan label tersebut.

Sebagai contoh, `PEMASUKAN.name()` menghasilkan `PEMASUKAN` untuk penyimpanan database, sementara `PEMASUKAN.toString()` menghasilkan `Pemasukan` untuk tampilan.

### 3.6. `AnggaranBulanan.java`

**Lokasi:** [src/main/java/id/aruskas/domain/AnggaranBulanan.java](src/main/java/id/aruskas/domain/AnggaranBulanan.java)  
**Deklarasi:** `public record AnggaranBulanan(YearMonth periode, long batas)`

Record ini menyimpan batas pengeluaran untuk satu bulan dan tahun. `periode` menggunakan `YearMonth`, sedangkan `batas` menggunakan `long`.

Constructor memastikan periode tidak `null`, tahun berada pada 0001–9999, dan batas tidak negatif. Batas Rp0 diperbolehkan. Record ini digunakan oleh `RepositoryAnggaran` dan `DompetService` ketika membaca atau menyimpan batas anggaran.

### 3.7. `FilterRiwayat.java`

**Lokasi:** [src/main/java/id/aruskas/domain/FilterRiwayat.java](src/main/java/id/aruskas/domain/FilterRiwayat.java)  
**Deklarasi:** `public record FilterRiwayat(Long kategoriId, LocalDate tanggalAwal, LocalDate tanggalAkhir)`

Record ini membawa kriteria penyaringan transaksi. Masing-masing nilai boleh `null`, yang berarti kriteria tersebut tidak dibatasi. Tipe `Long` pada `kategoriId` memungkinkan nilai `null` untuk pilihan semua kategori.

Constructor menolak tanggal awal yang melewati tanggal akhir. Method statis `semua()` membuat filter tanpa batas kategori maupun tanggal. Repository menerapkan batas tanggal secara inklusif: transaksi tepat pada tanggal awal dan akhir ikut ditampilkan.

### 3.8. `RingkasanDompet.java`

**Lokasi:** [src/main/java/id/aruskas/domain/RingkasanDompet.java](src/main/java/id/aruskas/domain/RingkasanDompet.java)  
**Deklarasi:** `public record RingkasanDompet(long pemasukan, long pengeluaran, long saldo)`

Record ini membawa total pemasukan, total pengeluaran, dan saldo atau selisih dari sekumpulan transaksi.

Method statis `dari(List<Transaksi> transaksi)` menjumlahkan nominal per jenis dan memanggil `item.dampakSaldo()` melalui referensi `Transaksi`. Ini merupakan penggunaan polimorfisme karena perilaku yang dijalankan mengikuti subclass objeknya.

Penjumlahan menggunakan `Math.addExact()` agar overflow terdeteksi. Ringkasan seluruh transaksi digunakan sebagai saldo dompet, sedangkan ringkasan hasil filter digunakan sebagai selisih laporan. Selisih hasil filter dapat negatif walaupun saldo dompet tidak negatif.

### 3.9. `RingkasanAnggaran.java`

**Lokasi:** [src/main/java/id/aruskas/domain/RingkasanAnggaran.java](src/main/java/id/aruskas/domain/RingkasanAnggaran.java)  
**Deklarasi:** `public record RingkasanAnggaran(YearMonth periode, Long batas, long realisasi)`

Record ini menggabungkan periode, batas anggaran, dan jumlah pengeluaran aktual. `batas == null` berarti anggaran belum diatur; kondisi ini berbeda dari anggaran Rp0.

| Method | Fungsi |
| --- | --- |
| `status()` | Menentukan status berdasarkan ketersediaan batas dan perbandingan realisasi terhadap batas. |
| `sisaAtauKelebihan()` | Mengembalikan selisih absolut batas dan realisasi, atau nol jika belum diatur. Status digunakan untuk membedakan sisa dan kelebihan. |
| `persentase()` | Mengembalikan `OptionalDouble` berisi persentase realisasi. Hasil kosong jika batas belum diatur atau bernilai nol. |

Record ini digunakan oleh dashboard, pemberitahuan transaksi, dialog anggaran, dan laporan HTML.

### 3.10. `StatusAnggaran.java`

**Lokasi:** [src/main/java/id/aruskas/domain/StatusAnggaran.java](src/main/java/id/aruskas/domain/StatusAnggaran.java)  
**Deklarasi:** `public enum StatusAnggaran`

Enum ini mendefinisikan empat keadaan anggaran:

| Nilai | Arti |
| --- | --- |
| `BELUM_DIATUR` | Belum ada batas untuk periode tersebut. |
| `DALAM_BATAS` | Realisasi lebih kecil daripada batas. |
| `MENCAPAI_BATAS` | Realisasi sama dengan batas. |
| `TERLAMPAUI` | Realisasi lebih besar daripada batas. |

Atribut `label` dan override `toString()` menyediakan teks yang mudah dibaca pengguna. UI menggunakan status ini untuk menentukan warna dan jenis pemberitahuan.

## 4. Package `repository`

Repository menerima `Connection` dari pemanggil. Dengan demikian, beberapa operasi SQL dapat memakai koneksi dan transaksi database yang sama.

### 4.1. `Database.java`

**Lokasi:** [src/main/java/id/aruskas/repository/Database.java](src/main/java/id/aruskas/repository/Database.java)  
**Deklarasi utama:** `public final class Database`  
**Deklarasi bersarang:** `public interface Pekerjaan<T>` dengan anotasi `@FunctionalInterface`

Class ini menyimpan lokasi database dalam atribut privat `lokasi`, membuka koneksi SQLite, menyiapkan tabel, dan mengendalikan commit serta rollback.

| Method | Fungsi |
| --- | --- |
| `Database(Path lokasi)` | Mengubah lokasi menjadi path absolut yang dinormalisasi. |
| `lokasi()` | Mengembalikan lokasi database. |
| `buka()` | Membuka koneksi dengan foreign key aktif, waktu tunggu kunci 5.000 ms, dan mode transaksi `IMMEDIATE`. |
| `dalamTransaksi(Pekerjaan<T> pekerjaan)` | Menjalankan pekerjaan dalam satu koneksi, melakukan commit setelah berhasil, dan rollback jika terjadi `SQLException` atau `RuntimeException`. |
| `inisialisasi()` | Membuat folder, tabel, indeks, dan kategori bawaan jika diperlukan. |

Interface `Pekerjaan<T>` memiliki method `jalankan(Connection koneksi)`. Interface ini memungkinkan service mengirim lambda berisi operasi database dan menerima hasil bertipe `T`. Hasil baru dikembalikan setelah commit berhasil.

Tabel yang dibuat adalah `kategori`, `transaksi`, dan `anggaran_bulanan`. Ada indeks tanggal dan kategori transaksi. Kategori bawaan meliputi Gaji, Uang Saku, Lainnya, Makan & Minum, Transportasi, dan Belanja; Lainnya tersedia pada kedua jenis transaksi.

Mode `IMMEDIATE` memperoleh kunci penulisan sebelum saldo dibaca dalam transaksi, sehingga dua pencatatan pengeluaran tidak dapat memakai saldo lama yang sama secara bersamaan.

### 4.2. `RepositoryKategori.java`

**Lokasi:** [src/main/java/id/aruskas/repository/RepositoryKategori.java](src/main/java/id/aruskas/repository/RepositoryKategori.java)  
**Deklarasi:** `public final class RepositoryKategori`

Class ini membaca dan menambahkan kategori ke tabel `kategori` menggunakan prepared statement.

| Method | Fungsi |
| --- | --- |
| `semua(Connection)` | Mengambil kategori dengan urutan jenis dan nama, lalu mengembalikan salinan list yang tidak dapat dimodifikasi. |
| `cari(Connection, long id)` | Mengambil kategori berdasarkan ID dan menolak ID yang tidak ditemukan. |
| `tambah(Connection, String nama, JenisTransaksi jenis)` | Menyimpan kategori dan mengambil ID hasil insert melalui `last_insert_rowid()`. |
| `tambahBawaan(Connection, String nama, String jenis)` | Menambahkan kategori awal dengan `INSERT OR IGNORE`; aksesnya terbatas pada package. |
| `baca(ResultSet)` | Method privat untuk mengubah satu baris SQL menjadi `Kategori`. |

Class ini dipakai oleh `DompetService` dan saat inisialisasi database.

### 4.3. `RepositoryTransaksi.java`

**Lokasi:** [src/main/java/id/aruskas/repository/RepositoryTransaksi.java](src/main/java/id/aruskas/repository/RepositoryTransaksi.java)  
**Deklarasi:** `public final class RepositoryTransaksi`

Class ini membaca dan menyimpan transaksi serta membentuk objek sesuai jenis kategorinya.

| Method | Fungsi |
| --- | --- |
| `cari(Connection, FilterRiwayat)` | Menggabungkan tabel transaksi dan kategori, menerapkan kriteria filter, lalu memetakan hasil menjadi objek. |
| `simpan(Connection, Transaksi)` | Menyimpan kategori, nominal, tanggal, dan catatan; mengembalikan objek dengan ID database yang baru. |
| `buat(long id, long nominal, Kategori kategori, LocalDate tanggal, String catatan)` | Method statis pembuat objek `Pemasukan` atau `Pengeluaran` berdasarkan jenis kategori. |

Hasil pencarian diurutkan menurut tanggal terbaru, lalu ID terbesar. Tanggal disimpan sebagai teks ISO dan dibaca kembali menjadi `LocalDate`. Pemeriksaan kecukupan saldo berada di service; repository berfokus pada pemetaan SQL dan objek.

### 4.4. `RepositoryAnggaran.java`

**Lokasi:** [src/main/java/id/aruskas/repository/RepositoryAnggaran.java](src/main/java/id/aruskas/repository/RepositoryAnggaran.java)  
**Deklarasi:** `public final class RepositoryAnggaran`

Class ini membaca dan menyimpan batas pada tabel `anggaran_bulanan`.

`cari(Connection, YearMonth)` mengembalikan `Optional<AnggaranBulanan>`, sehingga periode yang belum diatur diwakili `Optional.empty()`. `simpan(Connection, AnggaranBulanan)` menjalankan insert atau update melalui `ON CONFLICT(tahun,bulan)`.

Kombinasi tahun dan bulan menjadi primary key. Menyimpan anggaran untuk periode yang sama memperbarui batas pada baris yang sudah ada.

## 5. Package `service`

### 5.1. `DompetService.java`

**Lokasi:** [src/main/java/id/aruskas/service/DompetService.java](src/main/java/id/aruskas/service/DompetService.java)  
**Deklarasi:** `public final class DompetService`

Class ini menjadi pusat aturan keuangan dan penghubung antara UI dengan repository. Atributnya mencakup `Database`, `Clock`, `RepositoryTransaksi`, `RepositoryKategori`, dan `RepositoryAnggaran`.

Terdapat dua constructor: satu memakai jam sistem, sedangkan yang lain menerima `Clock` agar bulan berjalan dapat dikendalikan saat pengujian.

| Method publik | Fungsi |
| --- | --- |
| `catat(...)` | Memvalidasi transaksi, memeriksa kategori dan saldo, menghitung kondisi anggaran, lalu menyimpan transaksi. |
| `tambahKategori(...)` | Memvalidasi kategori dan menolak nama yang sama pada jenis yang sama tanpa membedakan huruf besar/kecil. |
| `simpanAnggaran(...)` | Menyimpan atau memperbarui batas dan mengembalikan ringkasan periode tersebut. |
| `lihatAnggaran(...)` | Membaca batas dan realisasi suatu periode. |
| `muat(FilterRiwayat)` | Menyiapkan ringkasan seluruh dompet, anggaran bulan berjalan, daftar kategori, dan laporan hasil filter. |

Alur `catat()` berjalan dalam satu transaksi database: membaca kategori, membentuk objek transaksi, menghitung saldo seluruh transaksi, menolak saldo calon yang negatif, memeriksa overflow, menghitung anggaran calon untuk pengeluaran, lalu melakukan insert.

Method privat `ringkasAnggaran()` menjumlahkan seluruh pengeluaran pada periode terkait. `ringkas()` menghitung ringkasan dompet, sedangkan `jumlah()` melakukan penjumlahan dengan pendeteksian overflow.

Aturan penting yang diterapkan:

- Saldo memakai seluruh transaksi tersimpan, termasuk transaksi pada bulan lain.
- Pengeluaran sebesar saldo tersedia diperbolehkan.
- Anggaran memberi informasi status; pengeluaran tetap boleh disimpan ketika anggaran terlampaui selama saldo cukup.
- Status pada hasil pencatatan mengikuti bulan tanggal transaksi, sedangkan dashboard mengikuti bulan berjalan.
- Realisasi anggaran memakai seluruh pengeluaran periode tersebut, termasuk transaksi di luar filter riwayat.

### 5.2. `Validasi.java`

**Lokasi:** [src/main/java/id/aruskas/service/Validasi.java](src/main/java/id/aruskas/service/Validasi.java)  
**Deklarasi:** `public final class Validasi`

Class utilitas ini mengubah teks input menjadi nilai yang dapat dipakai aplikasi. Constructor privat mencegah pembuatan objek utilitas.

| Method statis | Fungsi |
| --- | --- |
| `nominal(String teks, boolean bolehNol)` | Menerima angka rupiah bulat tanpa pemisah atau tanda, memeriksa kapasitas `long`, dan mengatur apakah nol diperbolehkan. |
| `tanggal(String teks)` | Membaca `DD-MM-YYYY` dengan pemeriksaan tanggal ketat, termasuk tanggal kabisat dan tahun mulai 0001. |
| `tanggalOpsional(String teks)` | Mengembalikan `null` untuk teks kosong; selain itu memanggil `tanggal()`. |
| `periode(String teks)` | Membaca bulan dan tahun berformat `YYYY-MM`. |

Input yang tidak valid menghasilkan `IllegalArgumentException` dengan pesan berbahasa Indonesia.

### 5.3. `FormatData.java`

**Lokasi:** [src/main/java/id/aruskas/service/FormatData.java](src/main/java/id/aruskas/service/FormatData.java)  
**Deklarasi:** `public final class FormatData`

Class utilitas ini menyeragamkan teks keluaran. Atribut statis `INDONESIA` menyimpan locale `id-ID`.

Method `rupiah(long)` menghasilkan teks seperti `Rp300.000`; `tanggal(LocalDate)` menghasilkan format `dd-MM-uuuu`; `periode(YearMonth)` menghasilkan nama bulan dan tahun dalam bahasa Indonesia; `detailAnggaran(RingkasanAnggaran)` menyusun informasi realisasi, batas, serta sisa atau kelebihan.

UI dan eksportir memakai class yang sama agar format tampilan dan laporan konsisten.

### 5.4. `DataTampilan.java`

**Lokasi:** [src/main/java/id/aruskas/service/DataTampilan.java](src/main/java/id/aruskas/service/DataTampilan.java)  
**Deklarasi:** `public record DataTampilan(...)`

Record ini membawa hasil `DompetService.muat()` ke `JendelaUtama` dalam satu objek.

| Komponen | Isi |
| --- | --- |
| `dompet` | Ringkasan seluruh transaksi. |
| `bulanBerjalan` | Status anggaran bulan berjalan. |
| `kategori` | Daftar kategori. |
| `riwayat` | Laporan transaksi berdasarkan filter. |
| `jumlahTransaksi` | Jumlah seluruh transaksi untuk membedakan database kosong dari hasil filter kosong. |

File ini hanya mendefinisikan pembawa data dan tidak menjalankan operasi database sendiri.

### 5.5. `HasilPencatatan.java`

**Lokasi:** [src/main/java/id/aruskas/service/HasilPencatatan.java](src/main/java/id/aruskas/service/HasilPencatatan.java)  
**Deklarasi:** `public record HasilPencatatan(Transaksi transaksi, RingkasanAnggaran anggaran)`

Record ini menjadi hasil dari `DompetService.catat()`. Komponen `transaksi` berisi objek yang telah disimpan beserta ID-nya. Komponen `anggaran` berisi ringkasan periode pengeluaran, atau `null` untuk pemasukan.

UI menggunakan hasil ini untuk menampilkan pemberitahuan transaksi dan status anggaran. Hasil diterima pemanggil setelah transaksi database berhasil di-commit.

### 5.6. `LaporanRiwayat.java`

**Lokasi:** [src/main/java/id/aruskas/service/LaporanRiwayat.java](src/main/java/id/aruskas/service/LaporanRiwayat.java)  
**Deklarasi:** `public record LaporanRiwayat(...)`

Record ini menyimpan snapshot laporan berupa `filter`, `namaKategori`, daftar `transaksi`, `total` hasil filter, dan daftar ringkasan `anggaran` per periode transaksi.

Constructor menggunakan `List.copyOf()` untuk daftar transaksi dan anggaran agar kedua list tidak dapat dimodifikasi melalui hasil record. Tabel UI dan eksportir memakai snapshot yang sama, sehingga baris serta urutan laporan mengikuti riwayat yang sudah dimuat. Perubahan isian filter belum memengaruhi snapshot sebelum tombol Terapkan dijalankan dan pemuatan berhasil.

### 5.7. `Exportable.java`

**Lokasi:** [src/main/java/id/aruskas/service/Exportable.java](src/main/java/id/aruskas/service/Exportable.java)  
**Deklarasi:** `public interface Exportable`

Interface ini menetapkan kontrak ekspor melalui method:

```java
void ekspor(LaporanRiwayat laporan, Path tujuan) throws IOException;
```

Implementasi harus dapat menerima laporan dan lokasi file tujuan. Dalam proyek ini, kontrak tersebut diimplementasikan oleh `EksporHtml`. `JendelaUtama` memanggil ekspor melalui referensi `Exportable`, sehingga pemanggilan bergantung pada kontrak ekspor.

### 5.8. `EksporHtml.java`

**Lokasi:** [src/main/java/id/aruskas/service/EksporHtml.java](src/main/java/id/aruskas/service/EksporHtml.java)  
**Deklarasi:** `public final class EksporHtml implements Exportable`

Class ini mengubah `LaporanRiwayat` menjadi dokumen HTML UTF-8. Method `ekspor()` menyusun kriteria filter, total pemasukan dan pengeluaran, selisih, tabel transaksi, serta status anggaran per periode. File ini juga memuat CSS laporan di dalam string HTML.

Method statis `escape(String)` mengubah karakter `&`, `<`, `>`, tanda kutip ganda, dan tanda kutip tunggal menjadi entitas HTML. Dengan demikian, nama kategori dan catatan ditampilkan sebagai teks.

Penulisan dilakukan ke file sementara terlebih dahulu, lalu dipindahkan ke tujuan. Pemindahan atomik digunakan jika didukung, dengan alternatif pemindahan biasa jika tidak didukung. File sementara dibersihkan pada blok `finally`.

## 6. Package `ui`

UI menggunakan Event Dispatch Thread atau EDT untuk mengelola komponen Swing. Operasi database dan penulisan laporan dijalankan melalui `Pekerja` di latar belakang agar jendela tetap dapat merespons.

### 6.1. `JendelaUtama.java`

**Lokasi:** [src/main/java/id/aruskas/ui/JendelaUtama.java](src/main/java/id/aruskas/ui/JendelaUtama.java)  
**Deklarasi:** `public final class JendelaUtama extends JFrame`

Class ini membuat dashboard utama: kartu total pemasukan, total pengeluaran, saldo, anggaran bulan berjalan, filter kategori dan tanggal, tabel riwayat, serta tombol aksi.

Atribut pentingnya adalah `service`, lokasi `database`, `ModelTabelTransaksi model`, `filterAktif`, snapshot `laporan`, `daftarKategori`, dan penanda `sibuk`. Atribut lain menyimpan komponen Swing yang diperbarui saat data berubah.

| Method | Fungsi |
| --- | --- |
| `bagianAtas()` dan `kartuNominal(...)` | Membuat judul, tombol utama, kartu ringkasan, dan bagian anggaran. |
| `bagianRiwayat()` | Membuat filter, tabel, tampilan kosong, dan ringkasan hasil filter. |
| `renderer()` | Memformat tanggal dan rupiah serta memberi warna teks jenis transaksi. |
| `pasangAksi()` | Menghubungkan tombol ke dialog, filter, reset, muat ulang, dan ekspor. |
| `muat()` dan `muat(FilterRiwayat)` | Meminta `DataTampilan` dari service melalui `Pekerja`, lalu memperbarui komponen. |
| `sibuk(boolean)` | Mengendalikan tombol, input, cursor, dan penutupan jendela selama operasi berlangsung. |
| `ekspor()` | Membuka pemilih file dan mengekspor snapshot laporan melalui `Exportable`. |

Constructor juga memasang timer setiap 60 detik untuk memeriksa pergantian bulan dan memuat ulang ketika diperlukan. Tabel mempertahankan urutan hasil repository agar sesuai dengan ekspor. Class anonim dipakai untuk renderer daftar, renderer tabel, dan listener penutupan jendela.

### 6.2. `DialogForm.java`

**Lokasi:** [src/main/java/id/aruskas/ui/DialogForm.java](src/main/java/id/aruskas/ui/DialogForm.java)  
**Deklarasi:** `abstract class DialogForm extends JDialog`

Class abstrak ini menjadi kerangka bersama dialog transaksi, kategori, dan anggaran. Akses class terbatas pada package `ui`.

Atribut `protected` berupa panel `bidang`, tombol `simpan`, tombol `batal`, dan label `petunjuk` dapat digunakan subclass. Atribut privat `baris` mengatur posisi isian berikutnya.

Method `tambah()` menambahkan komponen beserta labelnya, `tampilkanUkuran()` mengatur ukuran dan posisi dialog, `kerjakan()` menjalankan tugas melalui `Pekerja`, `sibuk()` mengatur status komponen, dan `validasiGagal()` menampilkan pesan pemeriksaan input. Method privat `aktifkan()` mengubah status komponen secara rekursif.

Dialog bersifat modal. Tombol Enter menjalankan Simpan, sedangkan Escape dapat menutup dialog ketika tombol Batal aktif.

### 6.3. `DialogTransaksi.java`

**Lokasi:** [src/main/java/id/aruskas/ui/DialogTransaksi.java](src/main/java/id/aruskas/ui/DialogTransaksi.java)  
**Deklarasi:** `final class DialogTransaksi extends DialogForm`

Class ini menampilkan formulir jenis transaksi, nominal, kategori, tanggal, dan catatan. Atribut `daftar` menyimpan salinan kategori yang tersedia.

Method privat `muatKategori(Kategori pilihan)` mengisi daftar pilihan sesuai jenis transaksi. Tombol `+ Kategori` membuka `DialogKategori`; kategori yang baru dibuat langsung dapat dipilih pada formulir.

Saat disimpan, dialog memeriksa pilihan kategori, memvalidasi nominal dan tanggal, lalu memanggil `DompetService.catat()` melalui fasilitas `kerjakan()`. Setelah berhasil, dialog ditutup, dashboard dimuat ulang melalui callback `setelahSimpan`, dan pesan hasil ditampilkan. Pengeluaran yang mencapai atau melampaui batas anggaran memperoleh ikon peringatan.

### 6.4. `DialogKategori.java`

**Lokasi:** [src/main/java/id/aruskas/ui/DialogKategori.java](src/main/java/id/aruskas/ui/DialogKategori.java)  
**Deklarasi:** `final class DialogKategori extends DialogForm`

Class ini menampilkan isian nama dan jenis kategori. Terdapat dua constructor: versi dengan callback `Runnable` untuk pemuatan ulang, dan versi dengan `Consumer<Kategori>` serta jenis awal untuk mengembalikan kategori baru ke formulir transaksi.

Saat tombol Simpan dipilih, objek `Kategori` dibuat untuk memvalidasi input, lalu `DompetService.tambahKategori()` dijalankan di latar belakang. Setelah berhasil, dialog ditutup, callback dijalankan, dan pesan kategori tersimpan ditampilkan.

### 6.5. `DialogAnggaran.java`

**Lokasi:** [src/main/java/id/aruskas/ui/DialogAnggaran.java](src/main/java/id/aruskas/ui/DialogAnggaran.java)  
**Deklarasi:** `final class DialogAnggaran extends DialogForm`

Class ini menampilkan isian periode `YYYY-MM`, nominal batas, dan informasi status anggaran. Atribut utamanya adalah `periode`, `nominal`, dan `informasi`.

Method privat `lihat(DompetService)` memvalidasi periode, membaca anggaran melalui `lihatAnggaran()`, lalu mengisi nominal dan informasi status. Method ini dijalankan saat dialog dibuka dan ketika tombol Lihat Periode dipilih.

Tombol Simpan memvalidasi input dan memanggil `simpanAnggaran()`. Batas nol diperbolehkan. Setelah berhasil, dialog ditutup dan dashboard diperbarui melalui callback.

### 6.6. `ModelTabelTransaksi.java`

**Lokasi:** [src/main/java/id/aruskas/ui/ModelTabelTransaksi.java](src/main/java/id/aruskas/ui/ModelTabelTransaksi.java)  
**Deklarasi:** `public final class ModelTabelTransaksi extends AbstractTableModel`

Class ini menjadi penghubung daftar objek `Transaksi` dengan `JTable`. Atribut `KOLOM` menetapkan lima kolom: Tanggal, Jenis, Kategori, Nominal, dan Catatan. Atribut `baris` menyimpan data tabel.

`isi(List<Transaksi>)` mengganti data dengan `List.copyOf()` dan memanggil `fireTableDataChanged()`. Override `getRowCount()`, `getColumnCount()`, `getColumnName()`, `getColumnClass()`, serta `getValueAt()` menyediakan struktur dan nilai sel.

Tanggal dikembalikan sebagai `LocalDate`, nominal sebagai `Long`, dan kolom lain sebagai `String`. Pemformatan visual tanggal dan rupiah dilakukan oleh renderer di `JendelaUtama`.

### 6.7. `Gaya.java`

**Lokasi:** [src/main/java/id/aruskas/ui/Gaya.java](src/main/java/id/aruskas/ui/Gaya.java)  
**Deklarasi:** `public final class Gaya`

Class utilitas ini menyimpan konstanta warna dan fungsi pembuat komponen agar tampilan konsisten. Warna yang tersedia adalah `BIRU`, `LATAR`, `TEKS`, `REDUP`, `HIJAU`, `MERAH`, dan `KUNING`.

`pasang()` mengaktifkan `FlatLightLaf`, mengatur font, lengkungan komponen, tabel, serta teks dialog standar. Method `label()`, `tombol()`, `tepi()`, `kartu()`, `kolom()`, dan `bidang()` membuat komponen atau susunan yang dipakai berulang.

Method `pesan()` membuat dialog pesan dengan isi `JTextArea`. Teks pengguna ditampilkan sebagai teks biasa dan dapat dibungkus menjadi beberapa baris.

### 6.8. `Pekerja.java`

**Lokasi:** [src/main/java/id/aruskas/ui/Pekerja.java](src/main/java/id/aruskas/ui/Pekerja.java)  
**Deklarasi:** `public final class Pekerja`

Class utilitas ini menjalankan pekerjaan latar belakang melalui method generik statis `jalankan(Component induk, Callable<T> tugas, Consumer<T> berhasil, Runnable selesai)`.

Di dalamnya terdapat class anonim turunan `SwingWorker<T, Void>`. `doInBackground()` menjalankan `tugas.call()`, sedangkan `done()` mengambil hasil dan menjalankan callback pada EDT.

Callback `berhasil` menerima hasil tugas. Callback `selesai` selalu dipanggil melalui blok `finally`, misalnya untuk mengaktifkan kembali tombol. Kegagalan validasi, SQL, dan I/O diberi pesan yang sesuai. Jika terjadi interupsi ketika mengambil hasil, status interupsi thread dipulihkan.

## 7. File pengujian

Bagian ini menjelaskan cakupan pengujian berdasarkan kode sumber, bukan laporan eksekusi pengujian baru.

### 7.1. `DompetServiceTest.java`

**Lokasi:** [src/test/java/id/aruskas/service/DompetServiceTest.java](src/test/java/id/aruskas/service/DompetServiceTest.java)  
**Deklarasi:** `class DompetServiceTest`

Class JUnit ini menguji service, repository, dan SQLite dengan database sementara melalui `@TempDir`. Method `siapkan()` beranotasi `@BeforeEach` membuat database baru, memasang service dengan `Clock.fixed()`, dan mengambil ID kategori untuk pengujian.

Kasus yang dicakup meliputi:

- Saldo awal, persistensi kategori/transaksi/anggaran, serta database yang dibuka ulang.
- Saldo kurang, pengeluaran tepat saldo, dan overflow nominal maupun total.
- Status anggaran dalam batas, mencapai batas, terlampaui, belum diatur, dan batas nol.
- Transaksi lintas bulan/tahun, kategori berbeda jenis, dan duplikasi nama kategori.
- Filter kategori dan tanggal dengan batas inklusif, kesesuaian ekspor, escaping HTML, dan laporan beberapa periode.
- Rollback, foreign key, penyimpanan integer, dan dua pengeluaran serentak.
- Validasi nominal, tanggal, dan periode, serta perilaku polimorfisme transaksi.

Pengujian nominal menggunakan `@ParameterizedTest` agar satu method dijalankan untuk beberapa input salah. Method pembantu `masuk()`, `keluar()`, dan `data()` menyederhanakan persiapan skenario.

### 7.2. `AntarmukaTest.java`

**Lokasi:** [src/test/java/id/aruskas/ui/AntarmukaTest.java](src/test/java/id/aruskas/ui/AntarmukaTest.java)  
**Deklarasi:** `class AntarmukaTest`

Class ini menjalankan smoke test alur Swing dengan database sementara. Method pengujian `formulirDashboardFilterDanUbahUkuranBerjalan()` membuka jendela, menambahkan kategori dan pemasukan, mengatur anggaran, mencatat pengeluaran, memeriksa pemberitahuan, menerapkan filter, mengekspor HTML, melakukan reset, dan membuka ulang jendela untuk memeriksa persistensi.

Pengujian juga merender jendela pada ukuran 1200 × 900 dan 1000 × 760, kemudian menyimpan gambar ke `target/ui-review/`. Jika lingkungan headless, pengujian dilewati melalui asumsi JUnit.

Method pembantunya meliputi `edt()` untuk menjalankan akses UI pada EDT, `komponen()` untuk mencari komponen secara rekursif, `dialog()` dan `tombol()` untuk menemukan elemen, `klik()` untuk menjalankan aksi tombol, `tunggu()` untuk menunggu kondisi dengan batas waktu, serta `rekam()` untuk menyimpan gambar jendela.

## 8. File pendukung

| File | Penjelasan |
| --- | --- |
| [pom.xml](pom.xml) | Konfigurasi Maven: identitas proyek, Java 21, encoding UTF-8, dependensi FlatLaf/SQLite JDBC/SLF4J/JUnit, serta plugin kompilasi, pengujian, eksekusi, dan pengemasan. Maven Shade Plugin membentuk JAR beserta dependensinya dengan entry point `Aplikasi`. |
| [README.md](README.md) | Panduan menjalankan aplikasi, penggunaan fitur, struktur package, dan catatan verifikasi proyek. |
| [.mvn/maven.config](.mvn/maven.config) | Mengarahkan repository dependensi Maven lokal ke `.tools/m2`. |
| [.gitignore](.gitignore) | Mengatur file yang diabaikan Git, termasuk hasil build, cache alat, database lokal, dan beberapa berkas IDE. |

Folder `data/` digunakan untuk database pengguna, `target/` untuk hasil build dan pengujian, `.tools/` untuk alat atau cache lokal, serta `.idea/` untuk konfigurasi IntelliJ. Folder tersebut bukan package class aplikasi.

## 9. Hubungan class dan penerapan PBO

### Pewarisan dan implementasi interface

```text
Transaksi (abstract)
├── Pemasukan
└── Pengeluaran

JDialog (Swing)
└── DialogForm (abstract)
    ├── DialogTransaksi
    ├── DialogKategori
    └── DialogAnggaran

JFrame (Swing)
└── JendelaUtama

AbstractTableModel (Swing)
└── ModelTabelTransaksi

Exportable (interface)
└── diimplementasikan oleh EksporHtml
```

### Contoh konsep PBO pada kode

| Konsep | Penerapan |
| --- | --- |
| Enkapsulasi | Atribut `Transaksi` bersifat `private final` dan dibaca melalui getter. Validasi constructor menjaga aturan dasar objek. |
| Abstraksi | `Transaksi` menyediakan kerangka transaksi dan kontrak `dampakSaldo()`; `Exportable` menyediakan kontrak ekspor. |
| Pewarisan | `Pemasukan` dan `Pengeluaran` mewarisi `Transaksi`; tiga dialog mewarisi `DialogForm`. |
| Polimorfisme | `RingkasanDompet` dan `DompetService` memanggil `dampakSaldo()` melalui referensi `Transaksi`. UI memanggil `ekspor()` melalui referensi `Exportable`. |
| Overriding | `dampakSaldo()` diimplementasikan berbeda pada kedua subclass; model tabel mengoverride method `AbstractTableModel`. |
| Overloading | `DompetService` dan `DialogKategori` memiliki beberapa constructor; `JendelaUtama` memiliki variasi method `muat()`. |
| Hubungan antarobjek | `DompetService` memegang database dan repository; `JendelaUtama` memegang service, model tabel, dan komponen Swing. |
| Generics | `Database.Pekerjaan<T>` dan `Pekerja.jalankan()` mendukung hasil tugas dengan tipe berbeda. |

### Contoh alur pencatatan pengeluaran

1. Pengguna mengisi `DialogTransaksi` dan memilih Simpan.
2. `Validasi` membaca nominal dan tanggal dari teks.
3. `DialogForm.kerjakan()` meneruskan tugas ke `Pekerja`.
4. `DompetService.catat()` membuka unit transaksi melalui `Database.dalamTransaksi()`.
5. `RepositoryKategori` membaca kategori, lalu `RepositoryTransaksi.buat()` membentuk `Pengeluaran`.
6. Service memanggil `dampakSaldo()` untuk menghitung saldo calon dan menolak pencatatan jika saldo tidak cukup.
7. Service menghitung status anggaran periode transaksi dan meminta repository menyimpan transaksi.
8. Database melakukan commit, kemudian service mengembalikan `HasilPencatatan`.
9. Callback pada EDT memperbarui dashboard dan menampilkan pesan hasil kepada pengguna.
