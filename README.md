# Follow — App Android · Le Gypaète Parapente

App native Android (Kotlin) pour le module Follow.  
Appelle directement l'API PHP existante (`content/follow/api.php`).

---

## Architecture

```
LoginActivity        → saisie URL + cookie session
  └─ MainActivity    → tabs Déco / Atterro + polling
       └─ VolListFragment (×2)
            └─ VolAdapter   → cartes élèves
FollowViewModel      → logique, polling toutes 5s
FollowRepository     → appels Retrofit → api.php
ApiClient            → Retrofit singleton (cookie injecté)
```

## Flux complet

```
[App démarre]
  → LoginActivity (si pas de config sauvegardée)
      URL + PHPSESSID → SharedPreferences
  → MainActivity
      GET api.php?action=list&room=1
        → liste des vols du jour
      GET api.php?action=poll&room=1&since_id=N  (toutes les 5s)
        → events depuis last seen
        → si events nouveaux → re-fetch list

[Mode DÉCO — assistant au décollage]
  Élèves en statut "en_attente"
  Bouton "Décollé" → dialog exercices FSVL (multi-select)
  POST api.php action=decolle + exercices[]

[Mode ATTERRO — instructeur en bas]
  Élèves en statut "decolle" (en l'air)
  Affiche les exercices assignés au déco
  Bouton "Posé" → POST api.php action=atterri
  Bouton "Transféré" → POST api.php action=transfere
```

---

## Mise en place

### 1. Ouvrir dans Android Studio
- File → Open → sélectionner le dossier `follow_app/`
- Laisser Gradle sync se terminer

### 2. Obtenir le cookie de session
Le module Follow utilise l'auth PHP du site.  
Sur ton navigateur (connecté au site) :
1. Ouvrir les Outils développeur (F12)
2. Onglet **Application** → **Cookies** → ton domaine
3. Copier la valeur de `PHPSESSID`

Format à entrer dans l'app :
```
PHPSESSID=abc123def456...
```

### 3. Configurer l'app
Au premier lancement :
- **URL** : `https://ton-ecole.ch/content/follow/`  
  (avec le `/` final — important pour Retrofit)
- **Cookie** : la valeur PHPSESSID copiée

### 4. Compiler et installer
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Évolutions possibles

| Fonctionnalité | Effort |
|---|---|
| Authentification par token Bearer (évite le cookie) | Moyen — nécessite un endpoint login côté PHP |
| Son/vibration à chaque événement poll | Facile — MediaPlayer ou Vibrator dans FollowViewModel |
| Support multi-rooms (sélecteur) | Facile — spinner dans MainActivity |
| Mode hors-ligne (cache local Room DB) | Moyen |
| Notification push (FCM) | Long |

---

## Note sécurité

Le cookie PHPSESSID est stocké en `SharedPreferences` en clair.  
Pour une app de prod, utiliser **EncryptedSharedPreferences** (Jetpack Security).  
Ajouter dans `build.gradle` :
```
implementation 'androidx.security:security-crypto:1.1.0-alpha06'
```
Et remplacer `getSharedPreferences(...)` par :
```kotlin
EncryptedSharedPreferences.create(
    this, "follow_secure",
    MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```
