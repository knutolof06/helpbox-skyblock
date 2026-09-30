# 🌟 Hypixel HelpBox

<p align="center">
  <img src="26.3/src/main/resources/assets/helpbox/icon.png" alt="Hypixel HelpBox Logo" width="180" height="180" />
</p>

<p align="center">
  <strong>The Ultimate Client-Side Quality-of-Life Mod for Hypixel SkyBlock & Modern Minecraft</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Fabric-blue?style=for-the-badge&logo=fabric" alt="Fabric" />
  <img src="https://img.shields.io/badge/Minecraft-26.1.2%20|%2026.2%20|%2026.3-green?style=for-the-badge&logo=minecraft" alt="Minecraft Versions" />
  <img src="https://img.shields.io/badge/Side-100%25%20Client-orange?style=for-the-badge" alt="Client-Side" />
  <img src="https://img.shields.io/badge/License-MIT-purple?style=for-the-badge" alt="License" />
</p>

---

## 📖 Overview

**Hypixel HelpBox** is a modern, high-performance, client-side Quality-of-Life (QoL) mod tailored specifically for **Hypixel SkyBlock** and vanilla gameplay. Designed with a sleek Tailwind/Glassmorphism interface, HelpBox consolidates multiple essential tools into one cohesive, lightweight, and lag-free suite.

From **3D in-world GPS navigation** with relief-shaded minimaps to **Enhanced Vault & Sacks storage**, customizable **inventory macro buttons**, an **in-world block/skull raycast copier**, and an interactive **console history terminal**, HelpBox elevates your SkyBlock journey.

---

## ✨ Key Features

### 🗺️ 1. 3D GPS, Radar & Interactive Minimap
- **True In-World 3D Beacons:** Render vibrant waypoint markers, custom icons (★, ⚑, ♦, ●, ▲), distance readouts, and glowing path lines with configurable opacity.
- **Auto-Arrival Clear:** Automatically clears waypoint navigation once you reach your destination.
- **Relief-Shaded True-Color Minimap:** Features Minecraft map-engine block color mapping (`MapColor`) with realistic hill/sunlight shading (`High`, `Normal`, `Low` brightness).
- **Multiplayer & Hypixel Chunk Engine:** Direct chunk-section scanner ensures maps render accurately even on multiplayer servers without standard heightmap packets.
- **Interactive Zoom & Pan:**
  - 5 Zoom levels (`0.25x`, `0.5x`, `1.0x`, `2.0x`, `4.0x`).
  - Smooth mouse wheel scrolling and click-and-drag map panning.
  - Dedicated UI buttons: `[ ⟳ ] Refresh`, `[ - ] Zoom Out`, `[ + ] Zoom In`, and `[ ⌖ ] Recenter`.
  - Player heading arrow (yaw tracking) and compass directions (`N`, `S`, `W`, `E`).
  - Click to autofill coordinates or right-click to quick-save custom map locations.

### 📦 2. Enhanced Vault & Sacks Storage
- **Modern Storage Interface:** Clean, spacious grid overlay for Vaults, Ender Chests, and personal storage.
- **Independent Sacks System:** Fully separated and dedicated sacks manager with smart auto-refresh and 1-click batch insertion.
- **Mod Compatibility:** Works independently and seamlessly takes priority over third-party storage mods without conflicts.
- **Fast Search & Filter:** Instant item search, tooltip previewing, and rapid deposit actions.

### ⚡ 3. Custom Inventory Action Buttons
- **Drag-and-Drop Button Layout:** Place quick-action buttons directly on container screens and player inventory.
- **Extensive Icon Library:** Choose from custom game symbols and Minecraft block textures.
- **Interactive Popup Editor:** Full keyboard typing support inside popup settings to bind custom Hypixel commands (e.g. `/hub`, `/is`, `/warp dungeon`, `/wardrobe`).
- **Flexible Button Sizes:** Multiple dimensions to fit any screen resolution and layout preference.

### 🔍 4. In-World Block, Skull & Lore Copier
- **20-Block In-World Raycast:** Simply aim your crosshair at any block, skull, or NPC head in the world and press your keybind to instantly copy its name, texture, or NBT data!
- **Inventory Item Copier:** Fast copying of item display names, formatted lore, and skull skin values.
- **Universal Keybind Support:** Full support for both keyboard keys and mouse buttons (Left, Right, Middle Click, Mouse 4, Mouse 5).

### 💻 5. Game Terminal Console & Command History
- **Interactive In-Game Console:** Dedicated command terminal with command history recall.
- **Arrow-Key Navigation:** Browse previous commands with Up/Down arrow keys, just like a modern command line or PowerShell.

### 🎨 6. Modern Control Center (Ayarlar Merkezi)
- **Centralized Hub:** Press your configured shortcut key anytime in-game to open the unified HelpBox Control Center.
- **Tabbed Navigation:** Seamlessly switch between GPS, Custom Buttons, Storage, Sacks, Console, and Copier.
- **Multilingual Support:** Fully localized in English, Turkish, Chinese, Russian, and more.

---

## ⌨️ Default Keybindings

| Function | Default Key | Configurable |
| :--- | :---: | :---: |
| **Open HelpBox Control Center** | `Right Shift` | ✅ Yes (Options -> Controls) |
| **Open GPS & Waypoint Manager** | `M` | ✅ Yes (Options -> Controls) |
| **In-World / Item Name & Skull Copier** | `C` (or Mouse Button) | ✅ Yes (HelpBox Settings) |
| **Minimap Zoom In / Out** | `Mouse Wheel` / `UI Buttons` | ✅ Built-in |
| **Minimap Pan / Drag** | `Left Click + Drag` | ✅ Built-in |
| **Minimap Quick-Save Waypoint** | `Right Click on Map` | ✅ Built-in |

---

## 📥 Installation

1. Make sure you have **[Fabric Loader](https://fabricmc.net/)** installed for Minecraft **26.1.2**, **26.2**, or **26.3**.
2. Install **[Fabric API](https://modrinth.com/mod/fabric-api)**.
3. Download the matching **`hypixel-helpbox`** `.jar` file for your Minecraft version:
   - For Minecraft 26.3: `hypixel-helpbox-26.3-1.0.0.jar`
   - For Minecraft 26.2: `hypixel-helpbox-26.2-1.0.0.jar`
   - For Minecraft 26.1.2: `hypixel-helpbox-26.1.2-1.0.0.jar`
4. Drop the `.jar` into your `.minecraft/mods` folder.
5. Launch the game and enjoy!

---

## 🌐 Supported Versions

| Minecraft Version | Fabric Loader | Status | Mod Jar Name |
| :---: | :---: | :---: | :---: |
| **26.3** | `>= 0.16.x` | 🟢 Supported | `hypixel-helpbox-26.3-1.0.0.jar` |
| **26.2** | `>= 0.16.x` | 🟢 Supported | `hypixel-helpbox-26.2-1.0.0.jar` |
| **26.1.2** | `>= 0.16.x` | 🟢 Supported | `hypixel-helpbox-26.1.2-1.0.0.jar` |

---

## 📜 License & Open Source

This project is licensed under the **MIT License**. You are free to use, modify, and include it in modpacks.

---

<p align="center">
  <sub>Made with ❤️ for the Hypixel SkyBlock community.</sub>
</p>
