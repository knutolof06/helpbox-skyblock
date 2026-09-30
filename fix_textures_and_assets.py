import os
import shutil

# 1. Update StorageInitializer.java
init_path = "26.3/src/main/java/com/knutolof/helpbox/storage/StorageInitializer.java"
with open(init_path, "r", encoding="utf-8") as f:
    c = f.read()

if 'public static final String TEXTURE_NAMESPACE = "enhanced_storage";' not in c:
    c = c.replace(
        'public static final String MOD_ID = "helpbox_vault";',
        'public static final String MOD_ID = "helpbox_vault";\n    public static final String TEXTURE_NAMESPACE = "enhanced_storage";'
    )
    with open(init_path, "w", encoding="utf-8") as f:
        f.write(c)
    print("Updated StorageInitializer.java with TEXTURE_NAMESPACE")

# 2. Update all java files in 26.3/src/main/java/com/knutolof/helpbox/storage
target_files = [
    "26.3/src/main/java/com/knutolof/helpbox/storage/gui/StorageOverlayLayout.java",
    "26.3/src/main/java/com/knutolof/helpbox/storage/gui/SackOverlayLayout.java",
    "26.3/src/main/java/com/knutolof/helpbox/storage/gui/component/IconButtonComponent.java",
    "26.3/src/main/java/com/knutolof/helpbox/storage/gui/component/ItemButtonComponent.java",
    "26.3/src/main/java/com/knutolof/helpbox/storage/gui/component/PageCardComponent.java",
    "26.3/src/main/java/com/knutolof/helpbox/storage/gui/component/SackCardComponent.java",
]

for p in target_files:
    if not os.path.exists(p):
        continue
    with open(p, "r", encoding="utf-8") as f:
        content = f.read()
    
    # Replace Identifier.fromNamespaceAndPath(MOD_ID, with Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE,
    # or Identifier.fromNamespaceAndPath("enhanced_storage",
    content = content.replace("Identifier.fromNamespaceAndPath(MOD_ID,", "Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE,")
    
    with open(p, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"Updated textures in {p}")

# 3. Copy assets/enhanced_storage to assets/helpbox_vault in all versions
for v in ["26.3", "26.2", "26.1.2"]:
    src_assets = os.path.join(v, "src/main/resources/assets/enhanced_storage")
    dest_assets = os.path.join(v, "src/main/resources/assets/helpbox_vault")
    if os.path.exists(dest_assets):
        shutil.rmtree(dest_assets)
    shutil.copytree(src_assets, dest_assets)
    print(f"Copied assets/enhanced_storage -> {dest_assets}")

print("Texture and asset fix completed!")
