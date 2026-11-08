# rezz store - Tugas Praktikum 3 Pemrograman Perangkat Bergerak (Full CRUD)

Aplikasi mobile katalog produk berbasis **Kotlin** dan **Jetpack Compose** (Android Studio) yang telah mengimplementasikan **Full Functional CRUD (Create, Read, Update, Delete)** terintegrasi langsung dengan REST API Backend [DummyJSON Products](https://dummyjson.com/docs/products).

---

## 🚀 Fitur CRUD (Terkoneksi Backend):

### 1. **C (Create - Tambah Produk Baru)**
* Terdapat tombol **Floating Action Button (+)** di pojok kanan bawah.
* Membuka form dialog untuk memasukkan: *Nama Produk*, *Harga*, *Kategori*, dan *Deskripsi*.
* Mengirimkan request `POST https://dummyjson.com/products/add`.
* Data baru langsung tampil secara realtime di daftar produk paling atas disertai notifikasi berhasil.

### 2. **R (Read - Menampilkan & Mencari Produk)**
* Mengambil data seluruh produk dari backend via `GET https://dummyjson.com/products?limit=100`.
* Menampilkan katalog produk dengan skema kartu warna pastel yang elegan.
* Fitur **Live Search** terintegrasi dengan endpoint `GET https://dummyjson.com/products/search?q={query}`.
* Filter kategori interaktif (beauty, fragrances, furniture, groceries, dll.).
* Halaman rincian detail produk lengkap saat kartu produk diklik.

### 3. **U (Update - Edit Produk)**
* Terdapat tombol **Edit (ikon pensil ✏️)** pada setiap kartu produk dan di dalam dialog detail produk.
* Membuka form dialog yang terisi otomatis dengan data produk yang dipilih.
* Mengirimkan request `PUT https://dummyjson.com/products/{id}` ke backend.
* Data produk di layar langsung ter-update secara otomatis.

### 4. **D (Delete - Hapus Produk)**
* Terdapat tombol **Hapus (ikon tempat sampah 🗑️)** pada kartu produk dan halaman detail.
* Menampilkan dialog konfirmasi sebelum menghapus agar tidak terjadi kesalahan.
* Mengirimkan request `DELETE https://dummyjson.com/products/{id}` ke backend.
* Produk langsung dihapus dari daftar katalog di layar.

---

## 🛠️ Teknologi yang Digunakan
* **Bahasa Pemrograman:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **Image Loader:** Coil Compose
* **Networking / REST API:** HttpURLConnection & Coroutines (Dispatchers.IO)
* **Backend:** DummyJSON Products REST API (`https://dummyjson.com/docs/products`)

---

## 📱 Panduan Menjalankan Aplikasi
1. Buka folder project ini di **Android Studio**.
2. Pastikan emulator Android (misal: *Pixel 4a*) sudah terpilih di toolbar atas.
3. Klik tombol **Run ▶** (atau tekan `Shift + F10`).
4. Aplikasi **rezz store** akan otomatis terbuka di emulator dengan seluruh fitur CRUD yang siap diuji.
