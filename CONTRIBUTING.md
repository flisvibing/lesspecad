# Contributing to Lesspecad Browser / Lesspecad Browser'a Katkıda Bulunma

Thank you for your interest in contributing to Lesspecad Browser! / Lesspecad Browser projesine katkıda bulunmak istediğiniz için teşekkür ederiz!

---

## Language Navigation / Dil Seçimi
- [English](#english)
- [Türkçe](#türkçe)

---

<a name="english"></a>
# English

Lesspecad Browser is an ultra-lightweight, privacy-oriented, and customizable Android browser built with modern Kotlin and Jetpack Compose. We welcome all contributions, whether it's bug reports, feature requests, code improvements, localization, or documentation.

---

## Getting Started and Prerequisites

To develop or build Lesspecad Browser locally, ensure you have:
* Android Studio: Android Studio Ladybug (2024.2+) or newer.
* JDK: OpenJDK 17 or JDK 21.
* Android SDK: Compile SDK 35, Min SDK 24.
* Gradle and Kotlin: Kotlin 2.0+ with Jetpack Compose BOM.

### Repository Setup
1. Fork the repository on GitHub (`https://github.com/flisvibing/lesspecad`).
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/lesspecad.git
   cd lesspecad
   ```
3. Open the project in Android Studio and let Gradle sync.
4. Run the app on an Android device or emulator (Run > Run 'app').

---

## Branch and Git Workflow

We follow standard Git feature branching:

1. Create a branch from `main`:
   ```bash
   # For new features
   git checkout -b feature/find-in-page

   # For bug fixes
   git checkout -b fix/tab-crash-on-rotate

   # For documentation/localization
   git checkout -b docs/update-readme
   ```
2. Write clean, atomic commits with descriptive commit messages:
   ```bash
   git commit -m "feat(navigation): add find in page highlighting bar"
   git commit -m "fix(tabs): resolve memory leak when closing incognito tab"
   ```
3. Push your branch to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```
4. Open a Pull Request (PR) against the `main` branch of `flisvibing/lesspecad`.

---

## Architecture and Coding Guidelines

* Kotlin and Jetpack Compose: Write declarative UI using modern Jetpack Compose. Avoid XML layouts.
* Material Design 3 (M3): Utilize dynamic theming, `ColorScheme`, and accessibility standards (minimum touch target size of 48dp).
* State Management: Use `BrowserViewModel`, `StateFlow`, and Compose state (`remember`, `mutableStateOf`).
* Localization: When adding user-facing text, add both English (`en`) and Turkish (`tr`) strings in `Locales.kt`.
* Clean Code: Keep functions small, modular, and self-documenting.

---

## Pull Request (PR) Checklist

Before submitting your PR, make sure:
- [ ] The app builds cleanly without errors.
- [ ] Code adheres to Kotlin and Jetpack Compose conventions.
- [ ] No sensitive keys or unneeded build artifacts are committed.
- [ ] Localized strings are updated in `Locales.kt` if UI strings were added.
- [ ] The PR description clearly explains the problem and the solution.

---

## Reporting Bugs and Suggesting Features

* Bug Reports: Open an issue on `https://github.com/flisvibing/lesspecad/issues` detailing the device model, Android version, steps to reproduce, and screenshots or logcat output.
* Feature Requests: Describe the proposed feature, why it is useful, and potential user experience or privacy implications.

---

<a name="türkçe"></a>
# Türkçe

Lesspecad Browser projesine katkıda bulunmak istediğiniz için teşekkürler! Lesspecad, modern Kotlin ve Jetpack Compose teknolojileriyle geliştirilmiş, ultra hafif ve gizlilik odaklı bir Android web tarayıcısıdır.

Hata bildirimleri, yeni özellik önerileri, kod iyileştirmeleri, çeviriler veya dökümantasyon katkıları dahil olmak üzere topluluktan gelen her türlü desteğe açığız.

---

## Başlarken ve Gereksinimler

Projeyi yerel ortamınızda çalıştırmak ve geliştirmek için gerekenler:
* Android Studio: Android Studio Ladybug (2024.2+) veya daha yeni bir sürüm.
* JDK: OpenJDK 17 veya JDK 21.
* Android SDK: Compile SDK 35, Min SDK 24.
* Gradle ve Kotlin: Kotlin 2.0+ ve Jetpack Compose BOM.

### Proje Kurulumu
1. Projeyi GitHub üzerinden fork edin (`https://github.com/flisvibing/lesspecad`).
2. Fork'unuzu bilgisayarınıza klonlayın:
   ```bash
   git clone https://github.com/KULLANICI_ADINIZ/lesspecad.git
   cd lesspecad
   ```
3. Projeyi Android Studio ile açın ve Gradle senkronizasyonunun tamamlanmasını bekleyin.
4. Uygulamayı fiziksel bir Android cihazda veya emülatörde çalıştırın (Run > Run 'app').

---

## Git ve Branch İş Akışı

Geliştirmeleri düzenli tutmak için branch adlandırma kurallarımıza uyun:

1. `main` dalından yeni bir branch açın:
   ```bash
   # Yeni bir özellik ekliyorsanız
   git checkout -b feature/sayfada-bul

   # Bir hatayı düzeltiyorsanız
   git checkout -b fix/sekme-kapanma-hatasi

   # Dökümantasyon veya çeviri için
   git checkout -b docs/ceviri-guncellemesi
   ```
2. Açıklayıcı ve anlaşılır commit mesajları yazın:
   ```bash
   git commit -m "feat(ui): sayfada arama cubugu ve eslesme sayaci eklendi"
   git commit -m "fix(webview): gizli sekme bellek sizintisi giderildi"
   ```
3. Branch'inizi GitHub fork'unuza gönderin:
   ```bash
   git push origin feature/branch-adiniz
   ```
4. `flisvibing/lesspecad` deposunun `main` dalına bir Pull Request (PR) açın.

---

## Mimari ve Kodlama Standartları

* Kotlin ve Jetpack Compose: Tüm arayüz geliştirmelerinde Jetpack Compose kullanılmalıdır.
* Material Design 3 (M3): Dinamik renk paletleri ve erişilebilirlik standartlarına (en az 48dp dokunma alanı) dikkat edilmelidir.
* Durum Yönetimi (State Management): `BrowserViewModel`, `StateFlow` ve Compose durum değişkenleri (`remember`, `mutableStateOf`) kullanılmalıdır.
* Çoklu Dil Desteği (Yerelleştirme): Arayüze yeni bir metin eklendiğinde `Locales.kt` dosyasına hem Türkçe (`tr`) hem de İngilizce (`en`) karşılığı eklenmelidir.
* Temiz Kod: Fonksiyonlar kısa, modüler ve okunabilir olmalıdır.

---

## Pull Request (PR) Kontrol Listesi

PR göndermeden önce lütfen kontrol edin:
- [ ] Uygulama hatasız şekilde derleniyor.
- [ ] Kodlar Kotlin ve Jetpack Compose standartlarına uygun.
- [ ] Gereksiz build dosyaları veya hassas anahtarlar commit'e dahil edilmedi.
- [ ] Yeni bir arayüz metni eklendiyse `Locales.kt` güncellendi.
- [ ] PR açıklamasında neyin değiştirildiği ve neden yapıldığı açıkça belirtildi.

---

## Hata Bildirimi ve Özellik Önerisi

* Hata Bildirimi (Bug Report): Yaşanan sorunu cihaz modeli, Android sürümü, hatayı yeniden oluşturma adımları ve varsa ekran görüntüsü/logcat çıktısı ile birlikte `https://github.com/flisvibing/lesspecad/issues` üzerinden bir Issue olarak açın.
* Özellik Önerisi (Feature Request): Önerdiğiniz özelliğin tarayıcıya ne katacağını ve gizlilik/kullanılabilirlik açısından faydasını açıklayan bir Issue oluşturun.
