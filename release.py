import os
import re
import shutil
import subprocess
import sys

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DEST_JAR_DIR = r"C:\Users\burha\Desktop\helpbox-jar"
MODRINTH_DIR = r"C:\Users\burha\AppData\Roaming\ModrinthApp\profiles"

VERSIONS = ["26.3", "26.2", "26.1.2"]

MODRINTH_PROFILES = {
    "26.3": os.path.join(MODRINTH_DIR, "Fabric 26.3", "mods"),
    "26.2": os.path.join(MODRINTH_DIR, "SkyBlock Enhanced", "mods"),
    "26.1.2": os.path.join(MODRINTH_DIR, "SkyBlock Enhanced (1)", "mods"),
}

def get_current_version():
    props_path = os.path.join(BASE_DIR, "26.3", "gradle.properties")
    with open(props_path, "r", encoding="utf-8") as f:
        for line in f:
            if line.startswith("mod_version="):
                return line.strip().split("=")[1].strip()
    return "1.0.0"

def bump_version(ver_str):
    parts = ver_str.split(".")
    try:
        parts[-1] = str(int(parts[-1]) + 1)
        return ".".join(parts)
    except ValueError:
        return ver_str + ".1"

def set_version(new_version):
    for v in VERSIONS:
        props_path = os.path.join(BASE_DIR, v, "gradle.properties")
        if not os.path.exists(props_path):
            continue
        with open(props_path, "r", encoding="utf-8") as f:
            content = f.read()
        content = re.sub(r"mod_version=.*", f"mod_version={new_version}", content)
        with open(props_path, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"[{v}] Updated mod_version -> {new_version}")

def main():
    target_version = sys.argv[1] if len(sys.argv) > 1 else None
    current_ver = get_current_version()
    
    if not target_version:
        target_version = bump_version(current_ver)
    
    print(f"=== Releasing HelpBox Mod: {current_ver} -> {target_version} ===")
    
    set_version(target_version)
    
    # 1. Sync
    print("\n--- Running sync_versions.py ---")
    subprocess.run([sys.executable, os.path.join(BASE_DIR, "sync_versions.py")], check=True)
    
    os.makedirs(DEST_JAR_DIR, exist_ok=True)
    
    # 2. Build and copy
    built_jars = []
    for v in VERSIONS:
        print(f"\n--- Building {v} ---")
        v_dir = os.path.join(BASE_DIR, v)
        gradlew = os.path.join(v_dir, "gradlew.bat" if os.name == "nt" else "./gradlew")
        subprocess.run([gradlew, "build"], cwd=v_dir, check=True)
        
        libs_dir = os.path.join(v_dir, "build", "libs")
        for f in os.listdir(libs_dir):
            if f.endswith(f"-{target_version}.jar") and not f.endswith("-sources.jar"):
                src_jar = os.path.join(libs_dir, f)
                dest_jar = os.path.join(DEST_JAR_DIR, f)
                shutil.copyfile(src_jar, dest_jar)
                print(f"Copied to Desktop/helpbox-jar -> {f}")
                built_jars.append(dest_jar)
                
                # Copy to Modrinth profile
                mod_dest_dir = MODRINTH_PROFILES.get(v)
                if mod_dest_dir and os.path.exists(mod_dest_dir):
                    # remove older helpbox jars
                    for old_f in os.listdir(mod_dest_dir):
                        if "helpbox" in old_f.lower() and old_f.endswith(".jar"):
                            try:
                                os.remove(os.path.join(mod_dest_dir, old_f))
                            except Exception:
                                pass
                    mod_dest_jar = os.path.join(mod_dest_dir, f)
                    shutil.copyfile(src_jar, mod_dest_jar)
                    print(f"Copied to Modrinth {v} -> {mod_dest_jar}")

    # 3. Git commit & push
    print("\n--- Git Commit and Push ---")
    try:
        subprocess.run(["git", "add", "."], cwd=BASE_DIR, check=True)
        subprocess.run(["git", "commit", "-m", f"Release version {target_version}"], cwd=BASE_DIR, check=True)
        subprocess.run(["git", "push", "origin", "main"], cwd=BASE_DIR, check=True)
        print("Pushed to origin main!")
    except Exception as e:
        print(f"Git commit/push warning: {e}")
        
    print(f"\n=== Successfully released HelpBox v{target_version}! ===")

if __name__ == "__main__":
    main()
