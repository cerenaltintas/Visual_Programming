# Görsel Programlama: Freelancer Mini İş/Zaman Yöneticisi

## 📅 Proje Geliştirme Planı

**Aşama 1: Planlama ve Gereksinim Analizi (SRS Dokümanı) - İlk Yapılacaklar**
* Projenin amacının ve kapsamının netleştirilmesi.
* Fonksiyonel gereksinimlerin (proje ekleme, silme, güncelleme, listeleme) detaylandırılması.
* Kullanıcı işlemlerinin belirlenmesi.
* Use Case (Kullanım Senaryosu) diyagramlarının tasarlanması.
* *Çıktı: Kısa SRS Dokümanı taslağının oluşturulması.*

**Aşama 2: Geliştirme Ortamı ve Veritabanı Hazırlığı**
* Java IDE'sinde yeni bir projenin oluşturulması.
* Arayüz kütüphanesi seçimi (Java Swing).
* Projeye SQLite JDBC sürücüsünün (driver) eklenmesi.
* SQLite veritabanı dosyasının (`freelancer_manager.db`) ve `gorevler` tablosunun oluşturulması.
* Veritabanı bağlantısını sağlayacak `DatabaseConnection` sınıfının yazılması.

**Aşama 3: Model ve CRUD İşlemlerinin (Backend) Kodlanması**
* Veritabanı tablosuna karşılık gelen `Gorev` (Task) model sınıfının yazılması (`Proje_Adi`, `Musteri`, `Teslim_Tarihi`, `Durum`).
* Veritabanına kayıt ekleme (Create), okuma (Read), güncelleme (Update) ve silme (Delete) işlevlerini yerine getirecek SQL sorgularının Java metotlarına dönüştürülmesi (`GorevDAO` sınıfı).

**Aşama 4: Kullanıcı Arayüzü (UI) Tasarımı**
* **Ana Ekran:** Görevlerin listeleneceği bir tablo (`JTable`), "Yeni Görev Ekle", "Düzenle" ve "Sil" butonlarının yerleştirilmesi.
* **Ekleme/Düzenleme Formu:** Görev adı, müşteri, teslim tarihi ve durum (Açılır liste - ComboBox) bilgilerinin girilebileceği bir diyalog penceresinin tasarlanması.

**Aşama 5: Arayüz ve Veritabanı Entegrasyonu**
* Arayüzdeki butonlara tıklama olaylarının (Event Listener) eklenmesi.
* Tablo bileşeninin veritabanındaki kayıtlarla dinamik olarak doldurulması.
* Kullanıcının formdan girdiği verilerin SQLite veritabanına kaydedilmesi ve arayüzün anında güncellenmesi.

**Aşama 6: Test, Hata Ayıklama ve Teslimat**
* Uygulamanın çalıştırılarak tüm fonksiyonların test edilmesi.
* Hatalı girişlere karşı uyarı mesajlarının eklenmesi.
* Kodların temizlenmesi ve gerekli yorum satırlarının eklenmesi.
* Projenin ve SRS dokümanının teslim için paketlenmesi.

---

## 📝 Kısa SRS (Yazılım Gereksinim Analizi) Dokümanı

### 1. Sistemin Amacı
Freelancer Mini İş/Zaman Yöneticisi, serbest çalışanların veya öğrencilerin üstlendikleri projeleri, müşteri bilgilerini, teslim tarihlerini ve projelerin güncel durumlarını tek bir merkezden takip edebilmelerini sağlayan masaüstü tabanlı bir veritabanı uygulamasıdır.

### 2. Temel Gereksinimler (Fonksiyonel)
* **Sistem;** yeni bir proje/görev eklenmesine olanak tanımalıdır.
* **Sistem;** kayıtlı olan projelerin müşteri bilgisi, teslim tarihi ve durumlarını bir liste halinde göstermelidir.
* **Sistem;** mevcut bir projenin bilgilerinin (özellikle "Yapılacak", "Devam Ediyor", "Bitti" durumlarının) güncellenmesini sağlamalıdır.
* **Sistem;** tamamlanan veya iptal edilen projelerin veritabanından silinmesine izin vermelidir.

### 3. Kullanıcı İşlemleri
* **Görev Görüntüleme:** Kullanıcı, ana ekranda tüm görevlerini bir tabloda görüntüleyebilir.
* **Görev Ekleme:** Kullanıcı form aracılığıyla veri girerek veritabanına yeni görev kaydeder.
* **Görev Düzenleme:** Kullanıcı tablodan seçtiği bir görevin durumunu veya teslim tarihini günceller.
* **Görev Silme:** Kullanıcı tablodan seçtiği görevi kalıcı olarak siler.

### 4. Use Case (Kullanım Senaryosu) Diyagramı İçeriği
* **Aktör:** Kullanıcı (Freelancer/Öğrenci)
* **Senaryolar (Use Cases):**
  * Görevleri Listele
  * Yeni Görev Ekle
  * Görev Bilgilerini Düzenle
  * Görev Durumunu Güncelle
  * Görev Sil
* **Bağlantılar:** Aktör (Kullanıcı) ile tüm bu senaryolar arasında doğrudan ilişki çizgileri olmalıdır.
