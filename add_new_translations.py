import json
import os

NEW_TRANSLATIONS = {
    "en_us": {
        "helpbox.ui.sacks.settings_title": "✦ HelpBox Sacks Settings",
        "helpbox.ui.sacks.theme": "Appearance Theme:",
        "helpbox.ui.sacks.sacks_per_row": "Sacks per Row:",
        "helpbox.ui.sacks.card_spacing": "Card Spacing (px):",
        "helpbox.ui.sacks.auto_scroll": "Auto-Scroll:",
        "helpbox.ui.sacks.enable_sacks": "Enable Sacks Overlay",
        "helpbox.ui.sacks.only_official": "Only Official Sacks",
        "helpbox.ui.sacks.show_overview": "Show Overview Card",
        "helpbox.ui.sacks.save_close": "Save & Close",
        "helpbox.ui.picker.cat_all": "All",
        "helpbox.ui.picker.cat_combat": "Combat",
        "helpbox.ui.picker.cat_blocks": "Blocks",
        "helpbox.ui.picker.cat_items": "Items",
        "helpbox.ui.picker.cat_special": "Special",
        "helpbox.ui.copier.press_key": "> Press Key <",
        "helpbox.ui.copier.unbind": "✕ Unbind"
    },
    "tr_tr": {
        "helpbox.ui.sacks.settings_title": "✦ HelpBox Sacks Ayarları",
        "helpbox.ui.sacks.theme": "Görünüm Teması:",
        "helpbox.ui.sacks.sacks_per_row": "Satır Başına Sacks:",
        "helpbox.ui.sacks.card_spacing": "Kart Aralığı (px):",
        "helpbox.ui.sacks.auto_scroll": "Oto Kaydırma:",
        "helpbox.ui.sacks.enable_sacks": "Sacks Arayüzünü Etkinleştir",
        "helpbox.ui.sacks.only_official": "Yalnızca Resmi Sacks",
        "helpbox.ui.sacks.show_overview": "Genel Bakış Kartını Göster",
        "helpbox.ui.sacks.save_close": "Kaydet & Kapat",
        "helpbox.ui.picker.cat_all": "Tümü",
        "helpbox.ui.picker.cat_combat": "Savaş",
        "helpbox.ui.picker.cat_blocks": "Bloklar",
        "helpbox.ui.picker.cat_items": "Eşyalar",
        "helpbox.ui.picker.cat_special": "Özel",
        "helpbox.ui.copier.press_key": "> Tuşa Basın <",
        "helpbox.ui.copier.unbind": "✕ Sıfırla"
    },
    "de_de": {
        "helpbox.ui.sacks.settings_title": "✦ HelpBox Sack-Einstellungen",
        "helpbox.ui.sacks.theme": "Design-Thema:",
        "helpbox.ui.sacks.sacks_per_row": "Säcke pro Zeile:",
        "helpbox.ui.sacks.card_spacing": "Kartenabstand (px):",
        "helpbox.ui.sacks.auto_scroll": "Autom. Scrollen:",
        "helpbox.ui.sacks.enable_sacks": "Sack-Overlay aktivieren",
        "helpbox.ui.sacks.only_official": "Nur offizielle Säcke",
        "helpbox.ui.sacks.show_overview": "Übersichtskarte anzeigen",
        "helpbox.ui.sacks.save_close": "Speichern & Schließen",
        "helpbox.ui.picker.cat_all": "Alle",
        "helpbox.ui.picker.cat_combat": "Kampf",
        "helpbox.ui.picker.cat_blocks": "Blöcke",
        "helpbox.ui.picker.cat_items": "Gegenstände",
        "helpbox.ui.picker.cat_special": "Spezial",
        "helpbox.ui.copier.press_key": "> Taste Drücken <",
        "helpbox.ui.copier.unbind": "✕ Belegung Löschen"
    },
    "es_es": {
        "helpbox.ui.sacks.settings_title": "✦ Ajustes de Sacos HelpBox",
        "helpbox.ui.sacks.theme": "Tema Visual:",
        "helpbox.ui.sacks.sacks_per_row": "Sacos por Fila:",
        "helpbox.ui.sacks.card_spacing": "Espaciado de Tarjetas (px):",
        "helpbox.ui.sacks.auto_scroll": "Desplazamiento Automático:",
        "helpbox.ui.sacks.enable_sacks": "Activar Superposición de Sacos",
        "helpbox.ui.sacks.only_official": "Solo Sacos Oficiales",
        "helpbox.ui.sacks.show_overview": "Mostrar Tarjeta de Vista General",
        "helpbox.ui.sacks.save_close": "Guardar y Cerrar",
        "helpbox.ui.picker.cat_all": "Todo",
        "helpbox.ui.picker.cat_combat": "Combate",
        "helpbox.ui.picker.cat_blocks": "Bloques",
        "helpbox.ui.picker.cat_items": "Objetos",
        "helpbox.ui.picker.cat_special": "Especial",
        "helpbox.ui.copier.press_key": "> Presiona una Tecla <",
        "helpbox.ui.copier.unbind": "✕ Restablecer"
    },
    "fr_fr": {
        "helpbox.ui.sacks.settings_title": "✦ Paramètres des Sacs HelpBox",
        "helpbox.ui.sacks.theme": "Thème d'Apparence :",
        "helpbox.ui.sacks.sacks_per_row": "Sacs par Ligne :",
        "helpbox.ui.sacks.card_spacing": "Espacement des Cartes (px) :",
        "helpbox.ui.sacks.auto_scroll": "Défilement Auto :",
        "helpbox.ui.sacks.enable_sacks": "Activer l'Overlay des Sacs",
        "helpbox.ui.sacks.only_official": "Seulement les Sacs Officiels",
        "helpbox.ui.sacks.show_overview": "Afficher la Carte d'Aperçu",
        "helpbox.ui.sacks.save_close": "Enregistrer & Fermer",
        "helpbox.ui.picker.cat_all": "Tous",
        "helpbox.ui.picker.cat_combat": "Combat",
        "helpbox.ui.picker.cat_blocks": "Blocs",
        "helpbox.ui.picker.cat_items": "Objets",
        "helpbox.ui.picker.cat_special": "Spécial",
        "helpbox.ui.copier.press_key": "> Appuyez sur une Touche <",
        "helpbox.ui.copier.unbind": "✕ Dissocier"
    },
    "ar_sa": {
        "helpbox.ui.sacks.settings_title": "✦ إعدادات أكياس HelpBox",
        "helpbox.ui.sacks.theme": "سمة المظهر:",
        "helpbox.ui.sacks.sacks_per_row": "أكياس لكل صف:",
        "helpbox.ui.sacks.card_spacing": "تباعد البطاقات (بكسل):",
        "helpbox.ui.sacks.auto_scroll": "تمرير تلقائي:",
        "helpbox.ui.sacks.enable_sacks": "تفعيل واجهة الأكياس",
        "helpbox.ui.sacks.only_official": "الأكياس الرسمية فقط",
        "helpbox.ui.sacks.show_overview": "عرض بطاقة النظرة العامة",
        "helpbox.ui.sacks.save_close": "حفظ وإغلاق",
        "helpbox.ui.picker.cat_all": "الكل",
        "helpbox.ui.picker.cat_combat": "قتال",
        "helpbox.ui.picker.cat_blocks": "كتل",
        "helpbox.ui.picker.cat_items": "عناصر",
        "helpbox.ui.picker.cat_special": "خاص",
        "helpbox.ui.copier.press_key": "> اضغط على مفتاح <",
        "helpbox.ui.copier.unbind": "✕ إلغاء التعيين"
    },
    "hi_in": {
        "helpbox.ui.sacks.settings_title": "✦ HelpBox बोरियां सेटिंग्स",
        "helpbox.ui.sacks.theme": "थीम:",
        "helpbox.ui.sacks.sacks_per_row": "प्रति पंक्ति बोरियां:",
        "helpbox.ui.sacks.card_spacing": "कार्ड अंतर (px):",
        "helpbox.ui.sacks.auto_scroll": "ऑटो-स्क्रॉल:",
        "helpbox.ui.sacks.enable_sacks": "बोरी ओवरले सक्षम करें",
        "helpbox.ui.sacks.only_official": "केवल आधिकारिक बोरियां",
        "helpbox.ui.sacks.show_overview": "अवलोकन कार्ड दिखाएं",
        "helpbox.ui.sacks.save_close": "सहेजें और बंद करें",
        "helpbox.ui.picker.cat_all": "सभी",
        "helpbox.ui.picker.cat_combat": "युद्ध",
        "helpbox.ui.picker.cat_blocks": "ब्लॉक",
        "helpbox.ui.picker.cat_items": "वस्तुएं",
        "helpbox.ui.picker.cat_special": "विशेष",
        "helpbox.ui.copier.press_key": "> कुंजी दबाएं <",
        "helpbox.ui.copier.unbind": "✕ हटाएं"
    },
    "ja_jp": {
        "helpbox.ui.sacks.settings_title": "✦ HelpBox 袋設定",
        "helpbox.ui.sacks.theme": "外観テーマ:",
        "helpbox.ui.sacks.sacks_per_row": "行あたりの袋数:",
        "helpbox.ui.sacks.card_spacing": "カード間隔 (px):",
        "helpbox.ui.sacks.auto_scroll": "自動スクロール:",
        "helpbox.ui.sacks.enable_sacks": "袋オーバーレイを有効化",
        "helpbox.ui.sacks.only_official": "公式袋のみ",
        "helpbox.ui.sacks.show_overview": "概要カードを表示",
        "helpbox.ui.sacks.save_close": "保存して閉じる",
        "helpbox.ui.picker.cat_all": "すべて",
        "helpbox.ui.picker.cat_combat": "戦闘",
        "helpbox.ui.picker.cat_blocks": "ブロック",
        "helpbox.ui.picker.cat_items": "アイテム",
        "helpbox.ui.picker.cat_special": "特殊",
        "helpbox.ui.copier.press_key": "> キーを押してください <",
        "helpbox.ui.copier.unbind": "✕ 割り当て解除"
    },
    "pt_br": {
        "helpbox.ui.sacks.settings_title": "✦ Configurações de Sacos HelpBox",
        "helpbox.ui.sacks.theme": "Tema Visual:",
        "helpbox.ui.sacks.sacks_per_row": "Sacos por Linha:",
        "helpbox.ui.sacks.card_spacing": "Espaçamento dos Cards (px):",
        "helpbox.ui.sacks.auto_scroll": "Rolagem Automática:",
        "helpbox.ui.sacks.enable_sacks": "Ativar Overlay de Sacos",
        "helpbox.ui.sacks.only_official": "Apenas Sacos Oficiais",
        "helpbox.ui.sacks.show_overview": "Mostrar Card de Visão Geral",
        "helpbox.ui.sacks.save_close": "Salvar e Fechar",
        "helpbox.ui.picker.cat_all": "Tudo",
        "helpbox.ui.picker.cat_combat": "Combate",
        "helpbox.ui.picker.cat_blocks": "Blocos",
        "helpbox.ui.picker.cat_items": "Itens",
        "helpbox.ui.picker.cat_special": "Especial",
        "helpbox.ui.copier.press_key": "> Pressione uma Tecla <",
        "helpbox.ui.copier.unbind": "✕ Desvincular"
    },
    "ru_ru": {
        "helpbox.ui.sacks.settings_title": "✦ Настройки мешков HelpBox",
        "helpbox.ui.sacks.theme": "Тема оформления:",
        "helpbox.ui.sacks.sacks_per_row": "Мешков в строке:",
        "helpbox.ui.sacks.card_spacing": "Интервал карточек (px):",
        "helpbox.ui.sacks.auto_scroll": "Автопрокрутка:",
        "helpbox.ui.sacks.enable_sacks": "Включить оверлей мешков",
        "helpbox.ui.sacks.only_official": "Только официальные мешки",
        "helpbox.ui.sacks.show_overview": "Показывать карточку обзора"
    },
    "zh_cn": {
        "helpbox.ui.sacks.settings_title": "✦ HelpBox 物品袋设置",
        "helpbox.ui.sacks.theme": "外观主题：",
        "helpbox.ui.sacks.sacks_per_row": "每行袋数：",
        "helpbox.ui.sacks.card_spacing": "卡片间距 (px)：",
        "helpbox.ui.sacks.auto_scroll": "自动滚动：",
        "helpbox.ui.sacks.enable_sacks": "启用物品袋覆盖层",
        "helpbox.ui.sacks.only_official": "仅官方物品袋",
        "helpbox.ui.sacks.show_overview": "显示概览卡片",
        "helpbox.ui.sacks.save_close": "保存并关闭",
        "helpbox.ui.picker.cat_all": "全部",
        "helpbox.ui.picker.cat_combat": "战斗",
        "helpbox.ui.picker.cat_blocks": "方块",
        "helpbox.ui.picker.cat_items": "物品",
        "helpbox.ui.picker.cat_special": "特殊",
        "helpbox.ui.copier.press_key": "> 请按任意键 <",
        "helpbox.ui.copier.unbind": "✕ 解除绑定"
    }
}

MODULES = ["26.3", "26.2", "26.1.2"]

for mod in MODULES:
    lang_dir = os.path.join(mod, "src", "main", "resources", "assets", "helpbox", "lang")
    if not os.path.isdir(lang_dir):
        continue

    for lang_code, trans in NEW_TRANSLATIONS.items():
        file_path = os.path.join(lang_dir, f"{lang_code}.json")
        data = {}
        if os.path.exists(file_path):
            with open(file_path, "r", encoding="utf-8") as f:
                try:
                    data = json.load(f)
                except Exception:
                    data = {}

        for k, v in trans.items():
            data[k] = v

        with open(file_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
        print(f"Updated {file_path}")

print("All language files updated with new keys!")
