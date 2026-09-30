# 🌟 Hypixel HelpBox (Türkçe Açıklama)

<p align="center">
  <img src="26.3/src/main/resources/assets/helpbox/icon.png" alt="Hypixel HelpBox Logo" width="180" height="180" />
</p>

<p align="center">
  <strong>Hypixel SkyBlock ve Modern Minecraft İçin Gelişmiş İstemci Taraflı (Client-Side) Yaşam Kalitesi (QoL) Modu</strong>
</p>

---

## 📖 Genel Bakış

**Hypixel HelpBox**, özellikle **Hypixel SkyBlock** oyuncuları ve modern Minecraft için tasarlanmış, tamamen istemci taraflı (client-side), hafif ve yüksek performanslı bir yaşam kalitesi (QoL) modudur. Modern Tailwind & Glassmorphism tarzı arayüzü sayesinde birden fazla eklenti ihtiyacını tek bir çatı altında toplar ve sıfır FPS kaybıyla çalışır.

3 boyutlu dünya içi GPS navigasyonundan gerçekçi gölgelendirmeli mini haritaya, bağımsız kasa ve sacks yönetiminden özelleştirilebilir arayüz makro butonlarına, 20 blok menzilli blok/kafa kopyalayıcıdan oyun içi konsol geçmişine kadar oyun deneyiminizi bir üst seviyeye taşır.

---

## ✨ Öne Çıkan Özellikler

### 🗺️ 1. 3D GPS Navigasyon, Radar ve İnteraktif Mini Harita
- **Dünya İçi 3D Işık Hüzmeleri (Beacons):** Özel ikonlar (★, ⚑, ♦, ●, ▲), mesafe göstergeleri ve saydamlığı ayarlanabilir ışık çizgileriyle hedefinizi dünya içinde görün.
- **Hedefe Varınca Otomatik Temizleme:** Belirlenen konuma ulaştığınızda navigasyon rotası otomatik olarak kapanır.
- **Gerçek Blok Renkleri ve 3D Gölgelendirmeli Harita:** Minecraft'ın kendi harita motoru blok renklerini (`MapColor`) ve arazi tepe/güneş ışıklandırmasını (`High`, `Normal`, `Low`) kullanarak gerçekçi 3 boyutlu derinlik sunar.
- **Hypixel & Çok Oyunculu Chunk Tarayıcı:** Sunucuların yükseklik haritası paketi göndermediği durumlarda bile doğrudan chunk kesitlerini tarayarak haritayı anında renkli ve eksiksiz çizer.
- **İnteraktif Yakınlaştırma & Haritada Gezinme:**
  - 5 farklı yakınlaştırma seviyesi (`0.25x`, `0.5x`, `1.0x`, `2.0x`, `4.0x`).
  - Fare tekerleğiyle hızlı zoom ve fare sol tıkıyla haritayı tutup serbestçe gezebilme (Pan/Drag).
  - Arayüz üstü butonlar: `[ ⟳ ] Yenile`, `[ - ] Uzaklaş`, `[ + ] Yakınlaş`, `[ ⌖ ] Oyuncuya Odaklan`.
  - Oyuncunun baktığı açıyı takip eden yön oku ve pusula yönleri (`N`, `S`, `W`, `E`).
  - Haritada tıklanan yerin koordinatlarını forma doldurma veya sağ tıkla anında özel harita noktası kaydetme.

### 📦 2. Gelişmiş Kasa (Vault) ve Sacks Depo Sistemi
- **Modern Depo Arayüzü:** Kasa, Ender Chest ve kişisel depolar için ferah, modern kart tasarımı.
- **Bağımsız Sacks (Torba) Paneli:** Depodan tamamen bağımsız, tek tıkla otomatik doldurma / getirme desteği sunan özel torba yönetim ekranı.
- **Mod Uyumluluğu:** Diğer üçüncü taraf depo modlarıyla çakışmaz; HelpBox yüklü olduğunda akıllıca önceliği alarak kendi kusursuz arayüzünü devreye sokar.
- **Hızlı Arama ve Filtreleme:** Anında eşya arama çubuğu ve hızlı aktarım butonları.

### ⚡ 3. Özel Envanter Butonları & Hızlı Eylemler
- **Sürükle-Bırak Buton Dizilimi:** Sandık ve oyuncu envanteri ekranlarına dilediğiniz yere özel butonlar ekleyin.
- **Geniş Simge Kütüphanesi:** Birçok özel sembol ve Minecraft blok simgeleri arasından seçim yapın.
- **Gelişmiş Popup Editörü:** Buton ayarları menüsünde tam klavye desteği ile Hypixel komutlarını (`/hub`, `/is`, `/warp dungeon`, `/wardrobe` vb.) kolayca bağlayın.
- **Esnek Boyut Seçenekleri:** Arayüzünüze en uygun buton boyutunu seçebilme imkanı.

### 🔍 4. Dünyadan Blok, Kafa ve Eşya Kopyalama (Raycast Copier)
- **20 Blok Menzilli Hedef Kopyalama:** Oyundayken baktığınız herhangi bir bloğa veya oyuncu kafasına nişan alıp kısayol tuşuna basarak ismini, dokusunu veya NBT bilgisini anında panoya kopyalayın!
- **Envanter Eşyası Kopyalama:** Eşya adlarını, renk kodlu açıklamaları (lore) ve kafa doku kodlarını anında kopyalama.
- **Evrensel Tuş Atama:** Klavyenin tüm tuşlarının yanı sıra fare tuşlarını da (Sol Tık, Sağ Tık, Orta Tık, Mouse 4, Mouse 5) kısayol olarak atayabilme.

### 💻 5. Oyun Terminali Konsolu & Komut Geçmişi
- **Etkileşimli Konsol Ekranı:** Oyun içi komutlar için terminal penceresi.
- **Yukarı/Aşağı Ok ile Geçmişi Gezme:** Tıpkı Windows PowerShell veya Linux terminalinde olduğu gibi daha önce girdiğiniz komutları yukarı/aşağı yön tuşlarıyla anında geri çağırın.

### 🎨 6. HelpBox Kontrol & Ayar Merkezi
- **Merkezi Yönetim:** Oyun içinde tek bir tuşla açılan, tüm mod özelliklerini tek ekranda toplayan şık yönetim merkezi.
- **Sol Kenar Çubuğu:** GPS, Butonlar, Depo, Torbalar, Konsol ve Kopyalayıcı sekmeleri arasında anında geçiş.
- **Çoklu Dil Desteği:** Türkçe, İngilizce, Çince, Rusça ve daha birçok dil desteği.

---

## ⌨️ Varsayılan Kısayol Tuşları

| Özellik | Varsayılan Tuş | Özelleştirilebilir |
| :--- | :---: | :---: |
| **HelpBox Ayarlar Merkezi** | `Sağ Shift` | ✅ Evet (Seçenekler -> Tuş Atamaları) |
| **GPS & Hedef Yöneticisi** | `M` | ✅ Evet (Seçenekler -> Tuş Atamaları) |
| **Blok / İsim / Kafa Kopyalama** | `C` (veya Fare Tuşu) | ✅ Evet (HelpBox Ayarları) |
| **Haritada Yakınlaşma / Uzaklaşma** | `Fare Tekerleği` / `Ekran Butonları` | ✅ Entegre |
| **Haritada Gezinme (Pan)** | `Sol Tık + Sürükle` | ✅ Entegre |
| **Haritada Hızlı Nokta Kaydetme** | `Haritaya Sağ Tık` | ✅ Entegre |

---

## 📥 Kurulum Rehberi

1. Minecraft sürümünüze uygun **[Fabric Loader](https://fabricmc.net/)** kurun (**26.1.2**, **26.2** veya **26.3**).
2. **[Fabric API](https://modrinth.com/mod/fabric-api)** modunu indirin ve `mods` klasörüne atın.
3. Sürümünüze uygun **`hypixel-helpbox`** `.jar` dosyasını `mods` klasörüne ekleyin:
   - Minecraft 26.3 için: `hypixel-helpbox-26.3-1.0.0.jar`
   - Minecraft 26.2 için: `hypixel-helpbox-26.2-1.0.0.jar`
   - Minecraft 26.1.2 için: `hypixel-helpbox-26.1.2-1.0.0.jar`
4. Oyunu başlatın ve keyfini çıkarın!

---

## 🌐 Desteklenen Sürümler

| Minecraft Sürümü | Fabric Loader | Durum | Mod Jar Dosyası |
| :---: | :---: | :---: | :---: |
| **26.3** | `>= 0.16.x` | 🟢 Destekleniyor | `hypixel-helpbox-26.3-1.0.0.jar` |
| **26.2** | `>= 0.16.x` | 🟢 Destekleniyor | `hypixel-helpbox-26.2-1.0.0.jar` |
| **26.1.2** | `>= 0.16.x` | 🟢 Destekleniyor | `hypixel-helpbox-26.1.2-1.0.0.jar` |

---

## 📜 Lisans & Açık Kaynak

Bu proje **MIT Lisansı** ile lisanslanmıştır. Kendi mod paketlerinize ekleyebilir, dilediğiniz gibi geliştirebilir ve kullanabilirsiniz.

---

<p align="center">
  <sub>Hypixel SkyBlock topluluğu için ❤️ ile geliştirilmiştir.</sub>
</p>
