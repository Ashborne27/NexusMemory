#!/data/data/com.termux/files/usr/bin/bash
set -e
cd ~/nexus-core
if [ -f "build.gradle.kts" ]; then
    echo "--- 🔍 Analyse du fichier build.gradle.kts racine ---"
    grep -n -E "versionCode|versionName" build.gradle.kts || true
    head -n 40 build.gradle.kts
else
    echo "❌ build.gradle.kts introuvable."
    exit 1
fi
