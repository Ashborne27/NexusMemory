#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "=== 🏛️ INITIALISATION DU DÉPLOIEMENT SOUVERAIN (NEXUSMEMORY v9.0) ==="

cd ~/nexus-core

# 1. Synchronisation avec le dépôt distant
echo "=== 📥 SYNCHRONISATION DU CODE SOURCE ==="
git pull origin main

# 2. Vérification de l'environnement Java pour la compilation locale
if [ -z "$JAVA_HOME" ] && ! command -v java &> /dev/null; then
    echo "⚙️ Installation d'OpenJDK 17 pour la compilation locale..."
    pkg update -y && pkg install openjdk-17 -y
fi

# 3. Compilation propre de l'APK via Gradle
if [ -f "./gradlew" ]; then
    chmod +x gradlew
    echo "=== 🚀 COMPILATION DE L'APPLICATION (GRADLEW) ==="
    ./gradlew clean assembleDebug
    
    # Localisation atomique de l'APK généré
    RAW_APK=$(find ./app/build/outputs/apk/debug -name "*.apk" | head -n 1)
else
    echo "❌ Erreur : Le fichier gradlew est introuvable dans ce répertoire."
    exit 1
fi

# 4. Copie vers le stockage partagé pour l'explorateur de fichiers
if [ -f "$RAW_APK" ]; then
    echo "=== 📦 ARTEFACT GÉNÉRÉ : $RAW_APK ==="
    
    termux-setup-storage 2>/dev/null || true
    mkdir -p ~/storage/shared/Download
    
    TARGET_APK="$HOME/storage/shared/Download/NexusMemory-v9.0.apk"
    rm -f "$TARGET_APK"
    cp -f "$RAW_APK" "$TARGET_APK"
    
    echo "=== ✨ SUCCÈS ABSOLU : FICHIER PRÊT DANS LE DOSSIER DOWNLOAD ==="
    ls -lh "$TARGET_APK"
else
    echo "❌ Erreur critique : L'APK n'a pas pu être assemblé."
    exit 1
fi
