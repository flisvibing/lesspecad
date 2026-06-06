# Lesspecad Browser

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Database-Room%20DB-FFA000?style=for-the-badge&logo=sqlite&logoColor=white" alt="Room DB" />
</p>

Lesspecad is a lightweight, local-first, and modern web browser developed for Android devices (compiling to an APK size under 20 MB). It integrates clean usability, local privacy controls, hostname-based ad filtering, user-script extensions, and tab management into a single cohesive experience.

Built with modern native Android technologies adhering to Jetpack Compose and Material Design 3 guidelines.

---

## English Documentation

### Key Features

#### Privacy & Ad Blocker
- **Built-in AdBlocker:** Filters advertisements and tracking network domains out-of-the-box via a lightweight local host-matching filter, reducing page size and layout clutter without external addons.
- **Incognito Mode:** Browse without leaving traces; session cookies, web storage, and cache are kept temporary and cleared completely from memory when the tab is closed.
- **Incognito-by-Default Preference:** Option to start the browser in a sandboxed, clean-slate privacy session every time it launches.

#### Extension & Script Engine
- **In-App Extensions:** Customize webpages and inject self-executing JavaScript extensions to curate your own surfing experience.
- **Preconfigured Add-ons:** Pre-shipped with reading aids, Zoom Font capabilities, and highly optimized advanced Dark Mode injectors.

#### Tabs & Tab Groups
- **Visual Grid Hub:** Oversee open sessions inside a clean visual Card-based grid.
- **Tab Groups:** Separate work, research, and personal browsing with customizable group labels and colors.

#### Reader Mode
- Extract articles, news blogs, and essays, removing ads, sidebar clutter, and visual noise from any page for comfortable, high-readability text focuses.

#### Themes & Styling
- **Material 3 Dynamic System:** Dynamically reflects Android system palette accents.
- **Bilingual Interface:** Supports both English and Turkish natively with a language switcher accessible from the welcomes screen as well as interface settings.

#### Cloudless Backup & Sync
- Secure JSON exports and imports. Back up and synchronize all bookmarks, active tabs, groups, download history, and extension configurations on other devices without any telemetry.

---

### Architecture & Tech Stack

This browser implements industry-standard **MVVM (Model-View-ViewModel)** architectural blueprints:

- **Jetpack Compose:** Fully declarative UI ensuring smooth transition pipelines and responsive layouts.
- **Optimized WebView Pooling:** Recycles instances through custom lifecycle-receptive managers (`webViewPool`) to mitigate memory leaks and Android OOM crashes.
- **Room SQLite:** Secure, ACID-compliant local transactions for user preferences, extensions, downloads, and bookmarks.
- **Coroutines & flow:** Asynchronous and reactive state flows.

---

### Project Setup & Build

#### Prerequisites
- **JDK 17** or higher
- **Android Studio Jellyfish** (or newer)
- **Gradle 8.0+**

#### Building via Command Line

1. **Clone the repository:**
   ```bash
   git clone https://github.com/flisvibing/lesspecad.git
   cd lesspecad
   ```

2. **Assemble the Android Debug APK:**
   ```bash
   gradle assembleDebug
   ```

3. **Execute JVM Unit Tests:**
   ```bash
   gradle test
   ```

---

### Package Metadata
- **Application ID / Package Name:** `com.lesspecad.browser`

---

### License
This project is licensed under the **Mozilla Public License 2.0 (MPL 2.0)**. See the [LICENSE](LICENSE) file for details.

---

## Türkçe Dokümantasyon

### Öne Çıkan Özellikler

#### Gizlilik Tercihleri ve Reklam Filtresi
- **Dahili AdFilter:** Üçüncü şahıs eklentilerine ihtiyaç duymadan, yerel bir ana makine (hostname) engelleme filtresiyle yaygın reklam ağlarını ve izleyicileri otomatik engeller.
- **İz Bırakmayan Gizli Mod:** Web oturum çerezlerini, web depolamasını ve önbelleğini geçici oturum boyunca muhafaza eder ve gizli sekme kapatıldığında bellekten temizler.
- **Varsayılan Gizlilik Tercihi:** Uygulamanın her zaman korumalı ve iz bırakmayan gizli modda başlatılması seçeneği.

#### Eklenti ve Script Yönetimi
- **Dahili Eklenti Paneli:** Web sayfalarına özel JavaScript scriptleri tanımlama ve eklentileri tek tıkla açıp kapatabilme.
- **Kullanıma Hazır Eklentiler:** Gece okumasını kolaylaştıran gelişmiş koyu tema, metin büyütme ve veri tasarrufu eklentileri.

#### Gelişmiş Sekme ve Grup Yönetimi (Tab Groups)
- **Görsel Sekme Kontrolü:** Açık sayfaları şık kart tasarımlarıyla izleme ve yönetme.
- **Sekme Grupları:** Sekmelerinizi çalışma ve ilgi alanlarınıza göre adlandırıp renklendirerek düzenleme.

#### Okuma Modu (Reader Mode)
- Sayfalardaki dikkat dağıtıcı tüm reklamları, panelleri ve menüleri temizleyerek sadece içeriğe odaklanmanızı sağlayan temiz ekran.

#### Temalar ve Tasarım Kodları
- **Material 3 Dynamic System:** Cihazınızın sistem renk akortlarıyla uyumlu dinamik renkler.
- **Çift Dil Desteği:** Karşılama ekranında ve ayarlarda yer alan İngilizce / Türkçe dil seçenekleri.

#### Bulutsuz Yedekleme ve Senkronizasyon
- Yer imlerinizi, geçmişinizi, eklentilerinizi ve ayarlarınızı tek tıkla JSON formatında dışa aktarın veya içeri aktarın.

---

### Mimari ve Teknik Altyapı

Uygulama, Android ekosisteminin en güncel ve kararlı pratiklerine uygun olarak **MVVM (Model-View-ViewModel)** mimarisiyle tasarlanmıştır:

- **Jetpack Compose:** Bildirimsel (Declarative) modern arayüz tasarımı ve responsive ekran yerleşimleri.
- **WebView Havuz Yönetimi:** Compose yaşam döngüsüyle uyumlu, bellek tasarrufu sağlayan akıllı havuz (`webViewPool`) yapısı.
- **Room Database:** SQLite tabanlı hızlı yerel veritabanı persistansı.
- **Flow ve Coroutines:** Arka plan asenkron thread yönetimi ve reaktif veri akışları.

---

### Proje Kurulumu ve Derleme

#### Gereksinimler
- **JDK 17** veya üzeri
- **Android Studio Jellyfish** (veya daha yeni bir sürüm)
- **Gradle 8.0+**

#### CLI ile Derleme Adımları

1. **Depoyu klonlayın:**
   ```bash
   git clone https://github.com/flisvibing/lesspecad.git
   cd lesspecad
   ```

2. **Geliştirici (Debug) APK Dosyasını Derleyin:**
   ```bash
   gradle assembleDebug
   ```

3. **Birim (Unit) Testlerini Çalıştırın:**
   ```bash
   gradle test
   ```

---

### Paket Bilgisi
- **Application ID / Paket Adı:** `com.lesspecad.browser`

---

### Lisans
Bu proje **Mozilla Kamu Lisansı 2.0 (MPL 2.0)** altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına göz atabilirsiniz.

---

<p align="center">
  <i>Explore the digital world freely and securely with Lesspecad. / Lesspecad ile dijital dünyayı özgürce ve güvenle keşfedin.</i>
</p>
