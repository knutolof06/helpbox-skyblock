package com.knutolof.helpbox.inventory.buttons.model;

import java.util.UUID;

/**
 * Envanter ekranının üzerine render edilen, tıklanınca komut çalıştıran buton.
 *
 * <p>Koordinatlar envanter kenarlarına göre (anchor) saklanır:
 * <ul>
 *   <li>{@code anchorRight=false}: {@code x} = butonun sol kenarı ile envanterin sol kenarı arasındaki fark</li>
 *   <li>{@code anchorRight=true}: {@code x} = butonun sol kenarı ile envanterin sağ kenarı arasındaki fark</li>
 *   <li>{@code anchorBottom=false}: {@code y} = butonun üst kenarı ile envanterin üst kenarı arasındaki fark</li>
 *   <li>{@code anchorBottom=true}: {@code y} = butonun üst kenarı ile envanterin alt kenarı arasındaki fark</li>
 * </ul>
 * Bu sayede envanter boyutu değişse bile butonlar kenarlara göre sabit konumda kalır.</p>
 */
public class InventoryButton {

    /** Kararlı kimlik — silme/güncelleme için UUID bazlı referans. */
    public String id = UUID.randomUUID().toString();

    /** Yatay ofseti (soldan ya da sağdan, anchorRight'a göre). */
    public int x;

    /** Dikey ofseti (yukarıdan ya da aşağıdan, anchorBottom'a göre). */
    public int y;

    /** true → x, envanterin sağ kenarından ölçülür. */
    public boolean anchorRight = false;

    /** true → y, envanterin alt kenarından ölçülür. */
    public boolean anchorBottom = false;

    /** İkon string'i. */
    public String icon = null;

    /**
     * Opsiyonel: tam item bileşen SNBT (resource pack CIT için).
     * null ise sadece icon string kullanılır.
     */
    public String iconSnbt = null;

    /** Çalıştırılacak komut string'i. */
    public String command = null;

    /** true → 32×32 px ikonu; false → 16×16 px (normal boyut). */
    public boolean isGigantic = false;

    /** true → Kaynak paketi (CIT / custom model data) uyumlu; false → düz vanilla Minecraft dokusu. */
    public boolean useResourcePack = true;

    // ── Yardımcı metodlar ─────────────────────────────────────────────────────

    public boolean isValid() {
        return icon != null && !icon.isBlank()
            && command != null && !command.isBlank();
    }

    public int getSize() {
        return isGigantic ? 32 : 16;
    }

    /**
     * Envanter boyutları verildiğinde butonun sol-üst piksel konumunu hesaplar.
     */
    public int[] resolvePosition(int invLeft, int invTop, int invWidth, int invHeight) {
        int px = anchorRight  ? invLeft + invWidth  + x : invLeft + x;
        int py = anchorBottom ? invTop  + invHeight + y : invTop  + y;
        return new int[]{ px, py };
    }

    /**
     * Butonun hit-test sınırlarını döndürür: [x, y, w, h]
     */
    public int[] getBounds(int invLeft, int invTop, int invWidth, int invHeight) {
        int[] pos = resolvePosition(invLeft, invTop, invWidth, invHeight);
        return new int[]{ pos[0], pos[1], getSize(), getSize() };
    }
}
