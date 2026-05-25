# ⚡ ProPortal — Freelancer Mini İş & Zaman Yöneticisi

Görsel Programlama dersi kapsamında geliştirilen, serbest çalışanların görev, proje ve zamanlarını yönetmelerine yardımcı olan masaüstü uygulaması.

**Geliştirici:** Ceren Altıntaş  
**Ders:** Görsel Programlama (2025–2026)  
**Teknoloji:** Java 11 · Swing · SQLite · FlatLaf · iTextPDF  

---

## Özellikler

| Modül | Açıklama |
|---|---|
| 📊 Dashboard | Toplam / Bekleyen / Devam Eden / Tamamlanan görev sayıları ve toplam bütçe özeti |
| ✅ Görev Yönetimi | Ekle, düzenle, sil; kategori (Okul/İş/Kişisel/Genel), öncelik, etiket, teslim tarihi |
| 🔍 Arama & Filtreleme | Gerçek zamanlı metin arama + kategori & öncelik filtresi |
| 📑 PDF Raporu | Tüm görevleri iTextPDF ile dışa aktar |
| 📊 Gelir Grafiği | Aylık bazda İş kategorisi gelirlerini gösteren çubuk grafik |
| 📅 Gantt Görünümü | Görevleri zaman çizelgesinde renkli şeritlerle görselleştirir |
| ⏱ Pomodoro | 25 dk odaklanma sayacı; tamamlanan oturumlar SQLite'a kaydedilir |
| 📈 Pomodoro İstatistik | Son 7 günlük oturum geçmişi |
| 📝 Haftalık Özet | Her Pazartesi otomatik açılan haftalık rapor popup'ı |
| 🌙 Dark Mode | Tek tıkla aydınlık / karanlık tema değiştirme |

---

## Kurulum & Çalıştırma

### Gereksinimler
- Java 11+
- Maven 3.6+

### Derleme

```bash
mvn package -DskipTests
```

### Çalıştırma

```bash
java -jar target/FreelancerManager-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Veritabanı (`freelancer_manager.db`) ilk çalıştırmada otomatik oluşturulur.

---

## Proje Yapısı

```
src/main/java/com/freelancer/manager/
├── Main.java                  # Uygulama giriş noktası
├── ui/
│   └── MainFrame.java         # Ana pencere ve tüm UI bileşenleri
├── dao/
│   └── GorevDAO.java          # Veritabanı erişim katmanı
├── model/
│   └── Gorev.java             # Görev veri modeli
└── db/
    └── DatabaseConnection.java # SQLite bağlantı yönetimi
```

---

## Ekran Görüntüsü

![ProPortal Dashboard](screenshot_final.png)

---

## Kullanılan Teknolojiler

- **Java Swing** — masaüstü arayüz
- **FlatLaf 3.4** — modern Light/Dark tema
- **SQLite JDBC 3.42** — yerel veritabanı
- **iTextPDF 5.5** — PDF rapor oluşturma
- **Maven Assembly Plugin** — bağımlılıkları içeren tek JAR
