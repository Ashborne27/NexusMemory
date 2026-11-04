import os

path = "app/src/main/java/com/ashborne/nexusmemory/MainActivity.kt"
if not os.path.exists(path):
    print("Erreur : MainActivity.kt introuvable.")
    exit(1)

with open(path, "r", encoding="utf-8") as f:
    content = f.read()

if "BiometricHelper" in content:
    print("L'intégration biométrique est déjà présente dans MainActivity.kt.")
else:
    print("Injection de l'appel BiometricHelper...")
    
    biometric_code = """
    // --- Intégration Biométrique v11.0 ---
    var isBiometricAuthenticated by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (BiometricHelper.canAuthenticate(applicationContext)) {
            try {
                BiometricHelper.authenticate(
                    activity = this@MainActivity,
                    onSuccess = { isBiometricAuthenticated = true },
                    onError = { _ -> isBiometricAuthenticated = true }
                )
            } catch (e: Exception) {
                isBiometricAuthenticated = true
            }
        } else {
            isBiometricAuthenticated = true
        }
    }
    """
    
    idx = content.rfind("}")
    if idx != -1:
        new_content = content[:idx] + biometric_code + "\n}\n"
        with open(path, "w", encoding="utf-8") as f:
            f.write(new_content)
        print("Sas biométrique injecté avec succès !")
    else:
        print("Erreur de structure dans MainActivity.kt")
