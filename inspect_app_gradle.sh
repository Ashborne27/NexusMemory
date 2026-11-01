#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "=== 🏛️ RECHERCHE ET INSPECTION DU FICHIER APP/BUILD.GRADLE.KTS ==="
cd ~/nexus-core

APP_GRADLE="app/build.gradle.kts"
if [ ! -f "$APP_GRADLE" ]; then
    APP_GRADLE=$(find . -path "*/app/build.gradle.kts" -o -path "*/app/build.gradle" | head -n 1)
fi

if [ -f "$APP_GRADLE" ]; then
    echo "📂 Fichier localisé avec succès : $APP_GRADLE"
    echo "--- 🔍 Paramètres de version actuels ---"
    grep -n -E "versionCode|versionName" "$APP_GRADLE" || echo "Propriétés non trouvées directement."
    echo "---"
    echo "✨ Inspection terminée."
else
    echo "❌ Erreur : Le fichier de configuration du module app est introuvable."
    exit 1
fi
