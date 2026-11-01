#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "=== 🏛️ INITIALISATION DE LA MISE À JOUR SOUVERAINE ==="

cd ~/nexus-core

# 1. Récupération des dernières modifications du dépôt source
echo "=== 📥 SYNCHRONISATION AVEC LE DÉPÔT DISTANT ==="
git pull origin main

# 2. Vérification et incitation à la compilation locale ou via Gradle
echo "=== ⚙️ VÉRIFICATION DE LA VERSION ET GRADLE ==="
if [ -f "./gradlew" ]; then
    chmod +x gradlew
    echo "=== 🚀 LANCEMENT DE LA COMPILATION LOCALE DE L'APK ==="
    ./gradlew assembleDebug
    
    # Localisation du nouvel APK généré par Gradle
    RAW_APK=$(find ./app/build/outputs/apk/debug -name "*.apk" | head -n 1)
else
    echo "❌ Erreur : Gradlew introuvable dans ce répertoire."
    exit 1
fi

if [ -f "$RAW_APK" ]; then
    echo "=== 📦 APK COMPILÉ AVEC SUCCÈS : $RAW_APK ==="
    
    # 3. Copie vers le stockage partagé visible par l'explorateur de fichiers
    termux-setup-storage 2>/dev/null || true
    mkdir -p ~/storage/shared/Download
    
    TARGET_APK="$HOME/storage/shared/Download/NexusMemory-v9.0.apk"
    rm -f "$TARGET_APK"
    cp -f "$RAW_APK" "$TARGET_APK"
    
    echo "=== ✨ SUCCÈS ABSOLU : FICHIER PRÊT DANS LE DOSSIER DOWNLOAD ==="
    ls -lh "$TARGET_APK"
else
    echo "❌ Erreur critique : L'APK n'a pas pu être généré par la compilation."
    exit 1
fi
