import os
import shutil
import re

base_dir = "26.3/src/main/java"
old_storage_dir = os.path.join(base_dir, "com/github/kdgaming0/enhancedstorage")
new_storage_dir = os.path.join(base_dir, "com/knutolof/helpbox/storage")

# 1. Remove unwanted files before moving
unwanted = [
    os.path.join(old_storage_dir, "mixin/MinecraftMixin.java"),
    os.path.join(old_storage_dir, "mixin/EnhancedStorageMixinPlugin.java"),
]
for f in unwanted:
    if os.path.exists(f):
        os.remove(f)
        print(f"Removed {f}")

# Remove old HelpBoxStorageMixinPlugin if exists
old_plugin = os.path.join(base_dir, "com/knutolof/helpbox/mixin/HelpBoxStorageMixinPlugin.java")
if os.path.exists(old_plugin):
    os.remove(old_plugin)
    print(f"Removed {old_plugin}")

# 2. Move directory
if os.path.exists(new_storage_dir):
    shutil.rmtree(new_storage_dir)

os.makedirs(os.path.dirname(new_storage_dir), exist_ok=True)
shutil.move(old_storage_dir, new_storage_dir)
print(f"Moved {old_storage_dir} -> {new_storage_dir}")

# Remove empty com/github directory
shutil.rmtree(os.path.join(base_dir, "com/github"), ignore_errors=True)

# 3. Rename EnhancedStorage.java to StorageInitializer.java
old_entrypoint = os.path.join(new_storage_dir, "EnhancedStorage.java")
new_entrypoint = os.path.join(new_storage_dir, "StorageInitializer.java")
if os.path.exists(old_entrypoint):
    os.rename(old_entrypoint, new_entrypoint)
    print(f"Renamed EnhancedStorage.java -> StorageInitializer.java")

# 4. Update all java files in 26.3/src/main/java
for root, dirs, files in os.walk(base_dir):
    for f in files:
        if not f.endswith(".java"):
            continue
        p = os.path.join(root, f)
        with open(p, "r", encoding="utf-8") as fp:
            content = fp.read()

        # Replace package and imports
        content = content.replace("package com.github.kdgaming0.enhancedstorage", "package com.knutolof.helpbox.storage")
        content = content.replace("import com.github.kdgaming0.enhancedstorage.", "import com.knutolof.helpbox.storage.")
        content = content.replace("com.github.kdgaming0.enhancedstorage.", "com.knutolof.helpbox.storage.")

        # If it's StorageInitializer.java
        if f == "StorageInitializer.java":
            content = content.replace("public class EnhancedStorage implements ClientModInitializer", "public class StorageInitializer")
            content = content.replace("public static final String MOD_ID = \"enhanced_storage\";", "public static final String MOD_ID = \"helpbox_vault\";")
            content = content.replace("public static final String SACK_MOD_ID = \"enhanced_storage_sacks\";", "public static final String SACK_MOD_ID = \"helpbox_sacks\";")
            content = content.replace("public void onInitializeClient()", "public static void init()")
            content = content.replace("EnhancedStorage.bypassNextSackOverlay", "StorageInitializer.bypassNextSackOverlay")

        # Replace references to EnhancedStorage class
        content = re.sub(r'\bEnhancedStorage\.MOD_ID\b', 'StorageInitializer.MOD_ID', content)
        content = re.sub(r'\bEnhancedStorage\.SACK_MOD_ID\b', 'StorageInitializer.SACK_MOD_ID', content)
        content = re.sub(r'\bEnhancedStorage\.bypassNextSackOverlay\b', 'StorageInitializer.bypassNextSackOverlay', content)
        content = re.sub(r'\bEnhancedStorage\.LOGGER\b', 'StorageInitializer.LOGGER', content)

        # Replace accessor method names
        content = content.replace("enhancedstorage$setImageWidth", "helpbox$setImageWidth")
        content = content.replace("enhancedstorage$setImageHeight", "helpbox$setImageHeight")
        content = content.replace("enhancedstorage$getHighlightClip", "helpbox$getHighlightClip")
        content = content.replace("enhancedstorage$", "helpbox$")

        with open(p, "w", encoding="utf-8") as fp:
            fp.write(content)

print("Java files updated successfully.")
