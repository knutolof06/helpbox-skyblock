# Hypixel HelpBox - Proje Kuralları (Project Rules)

## 1. Güncelleme Tamamlandı Kuralı
Kullanıcı **"güncelleme tamamlandı"** (veya benzeri bir onay) dediğinde:
1. Mod versiyonu bir sonraki sürüme geçirilir (örn. `gradle.properties` içindeki `mod_version` artırılır: `1.0.0` -> `1.0.1` vb.).
2. Değişiklikler tüm versiyonlara senkronize edilir (`sync_versions.py`).
3. Her üç versiyon da derlenir (`26.3`, `26.2`, `26.1.2`).
4. Oluşan yeni sürüm `.jar` dosyaları `C:\Users\burha\Desktop\helpbox-jar\` dizinine kopyalanır.
5. Modrinth profillerindeki (`Fabric 26.3`, `SkyBlock Enhanced`, `SkyBlock Enhanced (1)`) mod dizinlerine de kopyalanır.
6. Git commit & push yapılır.

## 2. Her Değişiklik Sonrası Soru Kuralı
Her değişiklik veya görev tamamlandıktan sonra asistandan gelen her cevabın sonunda **MUTLAKA** kullanıcıya şu soru sorulmalıdır:
> **"Güncelleme tamamlandı mı?"**
