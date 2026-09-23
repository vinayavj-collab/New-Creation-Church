import os
import subprocess
import re

classes_root = "app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"
src_root = "app/src/main/java"
cfr_jar = "/tmp/cfr.jar"

if not os.path.exists(src_root):
    os.makedirs(src_root)

print("Starting codebase recovery using CFR...")

count = 0
for root, dirs, files in os.walk(classes_root):
    for f in files:
        if f.endswith(".class") and "$" not in f:
            class_path = os.path.join(root, f)
            print(f"Decompiling {class_path}...")
            
            # Run CFR
            try:
                res = subprocess.run(
                    ["java", "-jar", cfr_jar, class_path],
                    capture_output=True,
                    text=True,
                    check=True
                )
                decompiled_code = res.stdout
                
                # Determine package from code or path
                pkg_match = re.search(r"package\s+([\w\.]+);", decompiled_code)
                if pkg_match:
                    pkg_name = pkg_match.group(1)
                else:
                    # Fallback to path-based package
                    rel_path = os.path.relpath(root, classes_root)
                    pkg_name = rel_path.replace(os.sep, ".")
                
                # Build target path
                pkg_dir = os.path.join(src_root, pkg_name.replace(".", os.sep))
                if not os.path.exists(pkg_dir):
                    os.makedirs(pkg_dir)
                
                kotlin_filename = f.replace(".class", ".kt")
                target_file = os.path.join(pkg_dir, kotlin_filename)
                
                with open(target_file, "w") as out:
                    out.write(decompiled_code)
                
                count += 1
            except Exception as e:
                print(f"Failed to decompile {class_path}: {e}")

print(f"Successfully recovered {count} source files!")
