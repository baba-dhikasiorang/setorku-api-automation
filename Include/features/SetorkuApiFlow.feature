Feature: Tahap 1 - Biller Multifinance Input Data Tagihan
  Sebagai Biller Multifinance
  Saya ingin mengirimkan data tagihan pelanggan melalui API /SetorDTH
  Agar sistem Setorku dapat menerbitkan kode bayar untuk transaksi pelanggan.

  Scenario: TS-01 - Input Data Tagihan Berhasil (Positif)
    Given endpoint API "/SetorDTH" siap menerima request
    And header "Content-Type" bernilai "application/json"
    And payload memuat data tagihan valid beserta kalkulasi signature SHA1 yang benar
    When client mengirimkan request POST ke "/SetorDTH" dengan signature "VALID"
    Then response status code bernilai 200
    And response body mengembalikan rc "00" dan rcdesc "SUKSES"
    And response body memuat array "ls_kodebayar" berisi kode bayar yang berhasil diterbitkan

  Scenario: TS-02 - Input Data Tagihan Gagal Karena Signature Tidak Valid (Negatif)
    Given endpoint API "/SetorDTH" siap menerima request
    And payload request menggunakan signature SHA1 yang tidak sesuai
    When client mengirimkan request POST ke "/SetorDTH" dengan signature "INVALID"
    Then response body mengembalikan rc selain "00"
    And rcdesc menampilkan pesan kesalahan validasi signature

  Scenario: TS-03 - Input Data Tagihan Gagal Karena Parameter Wajib Kosong (Negatif)
    Given endpoint API "/SetorDTH" siap menerima request
    And payload request tidak menyertakan parameter mandatory seperti "trxid" atau "usercode"
    When client mengirimkan request POST ke "/SetorDTH" dengan signature "VALID"
    Then response status code mengembalikan status error validasi
    And response body menampilkan pesan bahwa parameter wajib belum terisi

Feature: Tahap 2 - Merchant Inquiry Process & Validasi Tagihan
  Sebagai Kasir / Merchant
  Saya ingin melakukan inquiry kode bayar melalui API /SetorInq
  Agar sistem dapat memvalidasi apakah tagihan ada (is tagihan exist) sebelum pembayaran diproses.

  Scenario: TS-04 - Inquiry Berhasil dan Tagihan Ditemukan / YES (Positif)
    Given kode bayar "EXISTING" terdaftar di DB Setorku dan berstatus belum dibayar
    And payload request inquiry disusun dengan partnerid "DMY", productid "STR", dan signature SHA1 valid
    When merchant mengirimkan request POST inquiry ke "/SetorInq" untuk kode bayar "EXISTING"
    Then response status code bernilai 200
    And response body mengembalikan rc "00" dan rcdesc "SUKSES"
    And response body menampilkan rincian total tagihan beserta detail pelanggan

  Scenario: TS-05 - Inquiry Gagal dan Tagihan Tidak Ditemukan / NO (Negatif)
    Given kode bayar "99900000000000000000" tidak terdaftar di sistem Setorku
    And payload request inquiry berisi kode bayar tersebut
    When merchant mengirimkan request POST inquiry ke "/SetorInq" untuk kode bayar "99900000000000000000"
    Then response body mengembalikan kode rc yang mengindikasikan tagihan tidak ditemukan
    And alur transaksi dihentikan dan tidak dapat dilanjutkan ke proses pembayaran

  Scenario: TS-06 - Inquiry Gagal Karena Ketidakcocokan Partner atau Product ID (Negatif)
    Given kode bayar "EXISTING" terdaftar untuk partnerid "DMY"
    And payload request inquiry dikirimkan menggunakan partnerid "UNKNOWN"
    When merchant mengirimkan request POST inquiry ke "/SetorInq" untuk kode bayar "EXISTING"
    Then response body mengembalikan pesan kesalahan verifikasi partner

Feature: Tahap 3 - Merchant Payment Process
  Sebagai Kasir / Merchant
  Saya ingin mengirimkan instruksi pembayaran melalui API /SetorPay
  Agar pembayaran tagihan pelanggan yang sudah divalidasi dapat diselesaikan.

  Scenario: TS-07 - Proses Pembayaran Tagihan Berhasil (Positif)
    Given proses inquiry untuk kode bayar "EXISTING" telah berhasil dilakukan
    And payload pembayaran dikirimkan dengan nominal yang sesuai dan signature valid
    When merchant mengirimkan request POST payment ke "/SetorPay" dengan nominal "869000"
    Then response status code bernilai 200
    And response body mengembalikan rc "00" dan rcdesc "SUKSES"

  Scenario: TS-08 - Pembayaran Gagal Karena Nominal Tidak Sesuai (Negatif)
    Given total tagihan pada kode bayar "EXISTING" adalah "869000"
    And merchant mengirimkan payload pembayaran dengan nominal "500000"
    When merchant mengirimkan request POST payment ke "/SetorPay" dengan nominal "500000"
    Then response body mengembalikan kode rc yang menandakan nominal pembayaran tidak cocok
    And status tagihan di DB Setorku tetap belum terbayar

  Scenario: TS-09 - Pembayaran Gagal Pada Kode Bayar yang Sudah Lunas (Negatif)
    Given kode bayar "EXISTING" telah sukses dibayar sebelumnya
    And merchant mencoba mengirimkan request pembayaran ulang untuk kode bayar tersebut
    When merchant mengirimkan request POST payment ke "/SetorPay" dengan nominal "869000"
    Then response body mengembalikan rc error yang menyatakan tagihan sudah pernah dibayar

Feature: Tahap 4 - Pendukung & Validasi DB (/SetorCheckStatus & /SetorCancel)
  Sebagai Sistem Integration
  Saya ingin memeriksa status transaksi atau membatalkan kode bayar
  Agar status tagihan pada database Setorku selalu konsisten dan akurat.

  Scenario: TS-10 - Cek Status Pembayaran untuk Transaksi Lunas (Positif)
    Given transaksi pembayaran untuk kode bayar "1701600419003" telah sukses diproses
    When client mengirimkan request POST ke "/SetorCheckStatus"
    Then response status code bernilai 200
    And response body mengembalikan status "PAID"

  Scenario: TS-11 - Cek Status Pembayaran untuk Transaksi Belum Dibayar (Negatif)
    Given kode bayar "90901061" belum pernah dilakukan pembayaran
    When client mengirimkan request POST ke "/SetorCheckStatus"
    Then response body mengembalikan status "UNPAID"

  Scenario: TS-12 - Pembatalan Kode Bayar Aktif Berhasil (Positif)
    Given kode bayar "90901061" dalam status aktif di DB Setorku
    When client mengirimkan request POST ke "/SetorCancel"
    Then response status code bernilai 200
    And response body mengembalikan rc "00" dan rcdesc "SUKSES"

  Scenario: TS-13 - Inquiry Ditolak pada Kode Bayar yang Sudah Dibatalkan (Negatif)
    Given kode bayar "90901061" telah sukses dibatalkan melalui API "/SetorCancel"
    When merchant mencoba melakukan inquiry untuk kode bayar "90901061" melalui "/SetorInq"
    Then response body mengembalikan rc error yang menginformasikan bahwa tagihan telah dibatalkan
