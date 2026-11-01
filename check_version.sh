cat << 'EOF' > deploy_nexus.sh
#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "=== 🏛️ DÉPLOIEMENT SOUVERAIN (NEXUSMEMORY v9.0) ==="
cd ~/nexus-core

# 1. Synchronisation propre du dépôt
echo "=== 📥 SYNCHRONISATION DU CODE SOURCE ==="
git pull origin main

# 2. Vérification et compilation locale via Gradle
if [ -f "./gradlew" ]; then
    chmod +x gradlew
    echo "=== 🚀 COMPILATION LOCALE DE L'APK ==="
    ./gradlew clean assembleDebug
    
    # Localisation atomique de l'APK généré
    RAW_APK=$(find ./app/build/outputs/apk/debug -name "*.apk" | head -n 1)
else
    echo "❌ Erreur : Le fichier gradlew est introuvable."
    exit 1
fi

# 3. Copie sécurisée vers le stockage partagé visible
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
EOF

chmod +x deploy_nexus.sh
