# CollecBox — Application Android Native

> Application Android native développée pour le TP M2 Informatique (Université de Corse, 2026).
> Port de la PWA CollecBox en application native Kotlin + Jetpack Compose.

---

## 🏗️ Architecture et Choix Techniques

| Couche | Technologie | Rôle |
|---|---|---|
| **UI** | Jetpack Compose + Material 3 | Interface réactive déclarative, thème sombre moderne, pas d'ANR. |
| **État** | `ViewModel` + `StateFlow` | Survie aux rotations et aux recompositions, état hoisté. |
| **Base Locale** | **Room Database** (`@Entity`, `@Dao`) | Source unique de vérité Local-First (`Flow<List<ItemEntity>>`). 3 graines initiales. |
| **Préférences** | **DataStore (Preferences)** | Remplace `localStorage` pour les catégories sélectionnées et le jeton FCM. |
| **Arrière-plan** | **WorkManager (`SyncWorker`)** | Remplace `Background Sync` : vidange de l'Outbox avec contrainte réseau `CONNECTED`. |
| **Notifications** | **Firebase Cloud Messaging (FCM)** | Alertes push même application tuée, avec canal dédié et `PendingIntent`. |
| **App Link** | `https://collecbox.netlify.app/` | `autoVerify="true"` via `/.well-known/assetlinks.json`. |

---

## 🚀 Fonctionnalités Natives (Impossibles en PWA)

1. **Widget d'accueil interactif (Jetpack Glance)** :
   - Présent directement sur le launcher Android.
   - Affiche en direct le nombre total d'objets, la dernière acquisition et un bouton d'action rapide `+ Scanner / Ajouter`.
2. **Reconnaissance de texte par caméra (CameraX + ML Kit Text Recognition on-device)** :
   - Analyse vidéo en temps réel à 30 fps sans latence.
   - Détection automatique des titres et des tomes (ex: *"Tome 12"*, *"Vol. 3"*) pour pré-remplir la fiche.
   - **100% hors-ligne**, sans appel API distant et sans dépendance réseau.

---

## 📊 Analytics (Les 3 événements obligatoires)

- `item_created` (**Activation**) : Enregistrement du premier objet dans la collection.
- `collection_filtered` (**Rétention**) : Filtrage par catégorie lors d'une session ultérieure.
- `camera_ocr_scanned` (**Le natif**) : Utilisation du scanner IA temps réel ML Kit.

---

## 🛠️ Configuration Firebase (Étape par étape)

1. Aller sur [Firebase Console](https://console.firebase.google.com/) et créer un projet gratuit (ex: `collecbox-android`).
2. Ajouter une application Android avec le package : `corsica.universita.collecbox`.
3. Télécharger le fichier **`google-services.json`** et le placer dans le dossier `native/app/`.
4. Activer **Cloud Messaging (FCM)** et **Google Analytics**.
5. Pour tester les notifications :
   - Lancez l'application sur le téléphone.
   - Cliquez sur l'icône de cloche en haut à droite : le jeton FCM est copié dans le presse-papiers.
   - Dans la console Firebase → **Messaging** → **Créer votre première campagne** (ou *Envoyer un message de test*), collez le jeton.
   - L'application tuée reçoit la notification et un clic ouvre l'application !

---

## 🧪 Commandes de vérification pour la Démo

```bash
# 1. Vérifier l'App Link
adb shell pm verify-app-links --re-verify corsica.universita.collecbox
adb shell pm get-app-links corsica.universita.collecbox
# Résultat attendu : verified

# 2. Simuler la mort du process en arrière-plan
adb shell am kill corsica.universita.collecbox

# 3. Simuler le mode Doze (veille batterie)
adb shell dumpsys deviceidle force-idle

# 4. Ouvrir un lien deep link
adb shell am start -a android.intent.action.VIEW -d "https://collecbox.netlify.app/item/1"
```

