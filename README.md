# CollecBox — Application Android Native

> Application Android native développée pour le TP M2 Informatique (Université de Corse, 2026).  
> Sujet : **"CollecBox" — Gestionnaire de collection nomade (Vinyles, Mangas, Jeux vidéo, BD)**.  
> Port de la PWA CollecBox en application native Kotlin + Jetpack Compose.

---

## 🚀 Comment Construire et Lancer l'Application

### Prérequis
- **JDK 21** ou supérieur.
- **Android Studio** (version Koala / Ladybug 2024.1+ recommandée) ou le SDK Android (API 35, build-tools 35.0.0).
- Un appareil physique Android (avec Débogage USB activé) ou un Émulateur Android (AVD avec Google Play Services, API 26+).

---

### Méthode 1 : Avec Android Studio (Recommandé)

1. **Ouvrir le projet** :
   - Lancez Android Studio.
   - Cliquez sur **File** → **Open...** et sélectionnez le dossier **`native/`** de ce dépôt.
2. **Synchroniser Gradle** :
   - Android Studio synchronise automatiquement les dépendances définies dans `build.gradle.kts`.
   - Si besoin, cliquez sur l'icône de synchronisation (l'éléphant *"Sync Project with Gradle Files"*).
3. **Lancer l'application** :
   - Sélectionnez votre appareil connecté dans la liste déroulante en haut.
   - Cliquez sur le bouton vert **Run ▶️** (ou utilisez le raccourci `Shift + F10`).
   - L'application se compile, s'installe et s'ouvre automatiquement sur votre écran !

---

### Méthode 2 : En Ligne de Commande (Terminal)

Depuis le dossier `native/` :

```bash
cd native

# 1. Compiler l'APK Debug
./gradlew assembleDebug

# 2. Installer directement l'APK sur l'appareil connecté via adb
./gradlew installDebug
# (ou alternativement : adb install -r app/build/intermediates/apk/debug/app-debug.apk)

# 3. Lancer l'application sur le téléphone
adb shell am start -n corsica.universita.collecbox/.MainActivity
```

---

## 🏗️ Architecture et Choix Techniques

| Couche | Technologie | Rôle & Justification architecturale |
|---|---|---|
| **UI** | Jetpack Compose + Material 3 | Interface déclarative réactive, thème sombre moderne, pas d'ANR. |
| **État** | `ViewModel` + `StateFlow` + `SavedStateHandle` | Survie aux rotations et à la destruction du processus par l'OS (`am kill`). |
| **Base Locale** | **Room Database** (`@Entity`, `@Dao`) | Source unique de vérité Local-First (`Flow<List<ItemEntity>>`). 3 graines initiales. |
| **Préférences** | **DataStore (Preferences)** | Remplace `localStorage` pour les catégories et le jeton FCM de manière asynchrone sur `Dispatchers.IO`. |
| **Arrière-plan** | **WorkManager (`SyncWorker`)** | Remplace `Background Sync` : vidange de l'Outbox avec contrainte réseau `NetworkType.CONNECTED`. |
| **Notifications** | **Firebase Cloud Messaging (FCM)** | Réception des alertes même application tuée, canal haute priorité et `PendingIntent`. |
| **App Link** | `https://collecbox.netlify.app/` | `autoVerify="true"` via `/.well-known/assetlinks.json`. |

---

## 🚀 Fonctionnalités Natives (Impossibles en PWA)

1. **Widget d'accueil interactif (Jetpack Glance)** :
   - Installé directement sur le launcher Android.
   - Affiche les statistiques en direct de la collection et la dernière pièce ajoutée.
   - Un clic sur le widget ouvre immédiatement le formulaire d'ajout d'objet.
2. **Scanner de Code-barres / EAN (CameraX + ML Kit On-Device)** :
   - Intégré directement à l'intérieur du champ Code-barres.
   - Analyse vidéo temps réel à 30 fps via le modèle ML Kit embarqué (100% hors-ligne, accéléré par le GPU/NPU).
   - Détection automatique avec retour haptique (vibration) et injection directe des chiffres dans le formulaire.

---

## 📊 Analytics (Les 3 événements obligatoires)

Visualisables en direct dans la console Firebase via **Analytics** → **DebugView** :
- `item_created` (**Activation**) : Premier geste utile (création d'un objet).
- `collection_filtered` (**Rétention**) : Filtrage de la collection lors d'une session ultérieure.
- `barcode_scanned` (**Le natif**) : Utilisation du scanner CameraX + ML Kit.

Pour activer le mode debug temps réel :
```bash
adb shell setprop debug.firebase.analytics.app corsica.universita.collecbox
```

---

## 🧪 Commandes de vérification pour la Démo

```bash
# 1. Vérifier l'App Link
adb shell pm get-app-links corsica.universita.collecbox
# Résultat attendu : collecbox.netlify.app: verified

# 2. Ouvrir directement un lien d'objet via App Link
adb shell am start -a android.intent.action.VIEW -d "https://collecbox.netlify.app/item/1"

# 3. Simuler la mort du process en arrière-plan (SavedStateHandle)
adb shell am kill corsica.universita.collecbox

# 4. Forcer l'arrêt complet de l'application
adb shell am force-stop corsica.universita.collecbox

# 5. Simuler le mode Doze (veille batterie)
adb shell dumpsys deviceidle force-idle

# 6. Révoquer la permission caméra pour tester le flux de refus
adb shell pm revoke corsica.universita.collecbox android.permission.CAMERA
```
