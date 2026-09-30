import os
import shutil

SRC_DIR = "26.3"
DEST_DIRS = ["26.2", "26.1.2"]

for dest_dir in DEST_DIRS:
    print(f"\n--- Syncing to {dest_dir} ---")
    
    # 1. Clean up old com/github in destination
    old_github = os.path.join(dest_dir, "src/main/java/com/github")
    if os.path.exists(old_github):
        shutil.rmtree(old_github)
        print(f"Removed old {old_github}")
        
    # Remove old HelpBoxStorageMixinPlugin if exists in destination
    old_plugin = os.path.join(dest_dir, "src/main/java/com/knutolof/helpbox/mixin/HelpBoxStorageMixinPlugin.java")
    if os.path.exists(old_plugin):
        os.remove(old_plugin)
        print(f"Removed {old_plugin}")

    # Remove old helpbox.storage.mixins.json if exists in destination
    old_storage_mixins = os.path.join(dest_dir, "src/main/resources/helpbox.storage.mixins.json")
    if os.path.exists(old_storage_mixins):
        os.remove(old_storage_mixins)
        print(f"Removed {old_storage_mixins}")

    # 2. Copy storage tree
    src_storage = os.path.join(SRC_DIR, "src/main/java/com/knutolof/helpbox/storage")
    dest_storage = os.path.join(dest_dir, "src/main/java/com/knutolof/helpbox/storage")
    if os.path.exists(dest_storage):
        shutil.rmtree(dest_storage)
    shutil.copytree(src_storage, dest_storage)
    print(f"Copied storage package -> {dest_storage}")

    # 3. Copy lang files
    src_lang = os.path.join(SRC_DIR, "src/main/resources/assets/helpbox/lang")
    dest_lang = os.path.join(dest_dir, "src/main/resources/assets/helpbox/lang")
    if os.path.exists(dest_lang):
        shutil.rmtree(dest_lang)
    shutil.copytree(src_lang, dest_lang)
    print(f"Copied lang package -> {dest_lang}")

    # 4. Specific files to sync
    files = [
        "src/main/java/com/knutolof/helpbox/HelpBoxMod.java",
        "src/main/java/com/knutolof/helpbox/features/ItemNameCopier.java",
        "src/main/java/com/knutolof/helpbox/inventory/buttons/PopupEditor.java",
        "src/main/java/com/knutolof/helpbox/inventory/buttons/ButtonRenderer.java",
        "src/main/java/com/knutolof/helpbox/inventory/buttons/ButtonStore.java",
        "src/main/java/com/knutolof/helpbox/inventory/buttons/ButtonEditorScreen.java",
        "src/main/java/com/knutolof/helpbox/inventory/buttons/model/InventoryButton.java",
        "src/main/java/com/knutolof/helpbox/mixin/HelpBoxScreenSwapMixin.java",
        "src/main/java/com/knutolof/helpbox/mixin/ClientPacketListenerMixin.java",
        "src/main/java/com/knutolof/helpbox/mixin/ContainerScreenHighlightClipMixin.java",
        "src/main/java/com/knutolof/helpbox/mixin/AbstractContainerScreenAccessor.java",
        "src/main/java/com/knutolof/helpbox/mixin/MixinContainerScreen.java",
        "src/main/java/com/knutolof/helpbox/mixin/MixinKeyboardHandler.java",
        "src/main/java/com/knutolof/helpbox/ui/HelpBoxControlCenterScreen.java",
        "src/main/java/com/knutolof/helpbox/navigation/ui/NavigationScreen.java",
        "src/main/java/com/knutolof/helpbox/console/ConsoleHistory.java",
        "src/main/resources/assets/helpbox/icon.png",
        "src/main/resources/assets/helpbox/textures/gui/logo.png",
        "src/main/resources/helpbox.mixins.json",
        "src/main/resources/fabric.mod.json"
    ]

    for rel_path in files:
        src_file = os.path.join(SRC_DIR, rel_path)
        dest_file = os.path.join(dest_dir, rel_path)
        os.makedirs(os.path.dirname(dest_file), exist_ok=True)
        shutil.copyfile(src_file, dest_file)
        print(f"Synced {rel_path}")

print("\nSync completed successfully for all versions!")
