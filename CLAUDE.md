# Bercail — règles pour les agents de développement

Bercail est un launcher Android (Kotlin + Jetpack Compose) pour le Pixel 9 (Android 17, appareil de référence). Toutes les décisions figées sont dans `docs/handoff.md` ; la section « Décisions de la phase de précisions » prime sur le reste. Le porteur du projet ne lit pas le Kotlin : les tests, en français, sont son outil de contrôle.

## Avant toute fonctionnalité

1. Lire `docs/CATALOG.md`, puis chercher dans le code : réutiliser avant de créer.
2. Écrire dans le plan ce qui est réutilisé et ce qui manque. On ne crée que ce qui manque.

## Méthode

- **TDD d'abord** : un test qui échoue pour la bonne raison, le minimum de code, puis refactor. Pas de code de production sans test.
- **Tout sur `main`**, sans branche ni pull request. Seule exception : Renovate.
- **Commits courts** en Conventional Commits, en anglais (`feat:`, `fix:`, `test:`, `refactor:`, `chore:`, `docs:`, `ci:`, `build:`). `feat:` publie une version mineure, `fix:` un correctif ; les autres ne publient rien.
- **Langue** : code, commentaires et commits en anglais ; noms de tests en phrases françaises entre backticks (`` `le tiroir s'ouvre après 24 dp` ``) ; textes de l'interface en français.
- **Catalogue** : `docs/CATALOG.md` est mis à jour dans le même commit que le code (`CatalogTest` le vérifie). Un `README.md` court par module.
- **CI rouge sur `main` = priorité absolue** : corriger ou annuler le commit tout de suite.

## Design system d'abord

Les écrans sont assemblés uniquement avec les composants `Bc…` de `:core:designsystem`. Hors de `:app` et `:core:designsystem` : ni Compose Material, ni `BasicText`/`BasicTextField`, ni couleur (`Color(0x…)`) ni taille de texte (`12.sp`) en dur. Vérifié par `DesignSystemOnlyTest` et par detekt (`config/detekt/screens.yml`). Un besoin non couvert : on ajoute d'abord le composant au design system, avec ses tests et sa capture.

## Batterie (priorité absolue)

Budget : moins de 10 mAh/jour. Écran éteint ou accueil caché : zéro travail. Ni verrou de réveil, ni service au premier plan, ni alarme exacte, ni interrogation en boucle (vérifié par `BatteryRulesTest`). Tout est événementiel ; tout s'arrête quand l'accueil n'est pas visible. Animations à 20 images/s au plus.

## Sécurité (dépôt public)

Aucun secret dans le dépôt. La clé de signature vit dans les secrets GitHub (`BERCAIL_KEYSTORE_BASE64`, `BERCAIL_KEYSTORE_PASSWORD`, `BERCAIL_KEY_ALIAS`, `BERCAIL_KEY_PASSWORD`) et dans 1Password. Les clés Hue sont saisies dans l'app et chiffrées sur le téléphone.

## Commandes

| But | Commande |
|---|---|
| Tout vérifier (comme le hook avant push) | `./gradlew check` |
| Formater le code | `./gradlew spotlessApply` |
| Tests unitaires / d'interface / captures | `./gradlew test -Pbercail.suite=unit` (ou `ui`, `screenshots`) |
| Enregistrer de nouvelles captures de référence | `./gradlew recordRoborazziDebug` |
| Tests sur le téléphone branché en USB | `./gradlew connectedCheck` |
| Installer l'app de debug sur le téléphone | `./gradlew :app:installDebug` |
| Mesurer l'énergie de Brume sur le Pixel 9 | `bash scripts/measure-brume.sh` (voir l'en-tête du script) |
| Vérifier l'app sur le téléphone branché, sans toucher à l'aveugle | `source scripts/phone-ui.sh`, puis `tap_label "Chercher"` (voir l'en-tête du script) |

Noms des tests instrumentés (`androidTest/`) : seulement des lettres, des chiffres, des espaces, le tiret et l'apostrophe typographique `’`. Le format DEX d'Android refuse le reste (`'`, virgule, deux-points…) ; vérifié par `ModuleRulesTest`, et `./gradlew check` construit l'APK de ces tests.

Suites : les classes `*UiTest` sont les tests d'interface (Robolectric), `*ScreenshotTest` les captures (Roborazzi), les autres les tests unitaires ; `androidTest/` contient les tests instrumentés.

## Modules

`:app` (launcher, branchement des sources), `:core:domain` (Kotlin pur, couverture 90 %), `:core:designsystem`, `:core:testing` (faux ; seule la version debug de l'app les affiche, la version publiée montre des états vides), `:feature:home` (l'accueil et l'écran Écoute), `:feature:search` (la recherche du tiroir), `:feature:settings` (Réglages de Bercail), `:data:apps` (apps installées, liens vers le téléphone), `:data:settings` (réglages enregistrés), `:architecture` (tests Konsist). Un écran = un module `:feature:…` qui ne voit que le domaine et le design system. Les plugins de convention sont dans `build-logic/`, les versions dans `gradle/libs.versions.toml` (mises à jour par Renovate).
