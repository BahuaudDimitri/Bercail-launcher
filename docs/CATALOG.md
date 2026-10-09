# Catalogue de Bercail

Où est quoi. Une ligne par module, composant, écran, source de données et règle métier. Mis à jour dans le même commit que le code : le test `CatalogTest` (module `:architecture`) échoue si un module ou un composant public du design system manque, ou si un fichier cité n'existe plus.

Avant toute fonctionnalité : lire ce catalogue, chercher dans le code, puis n'ajouter que ce qui manque.

## Modules

| Module | Rôle | README |
|---|---|---|
| `:app` | L'application : écran d'accueil déclaré à Android, assemblage des modules | `app/README.md` |
| `:core:domain` | Règles métier et interfaces des sources de données, en Kotlin pur | `core/domain/README.md` |
| `:core:designsystem` | Design system : jetons et composants `Bc…`, seule porte vers Compose pour les écrans | `core/designsystem/README.md` |
| `:core:testing` | Faux (fakes) des sources du domaine, pour les tests et les aperçus | `core/testing/README.md` |
| `:architecture` | Tests d'architecture : « seulement le design system », interdits batterie, catalogue | `architecture/README.md` |

## Écrans

| Écran | Fichier | Tests |
|---|---|---|
| Accueil (vide, vague 1) | `app/src/main/kotlin/io/github/bahuauddimitri/bercail/HomeScreen.kt` | `app/src/test/kotlin/io/github/bahuauddimitri/bercail/LauncherUiTest.kt`, `app/src/test/kotlin/io/github/bahuauddimitri/bercail/HomeScreenshotTest.kt` |
| Activité launcher (écran d'accueil d'Android, retour bloqué, fond d'écran visible) | `app/src/main/kotlin/io/github/bahuauddimitri/bercail/MainActivity.kt` | `app/src/test/kotlin/io/github/bahuauddimitri/bercail/LauncherUiTest.kt`, `app/src/androidTest/kotlin/io/github/bahuauddimitri/bercail/LauncherDeviceTest.kt` |

## Design system

Jetons et thème (vague 2). Police Outfit sous licence SIL OFL (`core/designsystem/OFL-Outfit.txt`).

| Composant | Rôle | Fichier | Tests |
|---|---|---|---|
| `BcTheme` | Racine de chaque écran : vitesse des animations du téléphone, couleur et style de texte par défaut | `core/designsystem/src/main/kotlin/io/github/bahuauddimitri/bercail/core/designsystem/theme/BcTheme.kt` | `core/designsystem/src/test/kotlin/io/github/bahuauddimitri/bercail/core/designsystem/theme/BcThemeUiTest.kt` |
| `BcColors` | Palette sombre unique : fond de nuit, textes, verres, tuiles, 5 pastels, éteint ; contraste WCAG | `core/designsystem/src/main/kotlin/io/github/bahuauddimitri/bercail/core/designsystem/theme/BcColors.kt` | `core/designsystem/src/test/kotlin/io/github/bahuauddimitri/bercail/core/designsystem/theme/BcColorsTest.kt` |
| `BcType` | Police Outfit et styles nommés, jamais sous 10,5 sp | `core/designsystem/src/main/kotlin/io/github/bahuauddimitri/bercail/core/designsystem/theme/BcType.kt` | `core/designsystem/src/test/kotlin/io/github/bahuauddimitri/bercail/core/designsystem/theme/BcTypeTest.kt` |
| `BcSizes`, `BcSpacing`, `BcShapes` | Cibles tactiles (≥ 40 dp), espacements, rayons | `core/designsystem/src/main/kotlin/io/github/bahuauddimitri/bercail/core/designsystem/theme/BcSizes.kt` | `core/designsystem/src/test/kotlin/io/github/bahuauddimitri/bercail/core/designsystem/theme/BcSizesTest.kt` |
| `BcMotion` | Durées et courbe des animations, coupées si Android les supprime | `core/designsystem/src/main/kotlin/io/github/bahuauddimitri/bercail/core/designsystem/theme/BcMotion.kt` | `core/designsystem/src/test/kotlin/io/github/bahuauddimitri/bercail/core/designsystem/theme/BcMotionTest.kt` |

## Sources de données

Aucune pour l'instant.

## Règles d'architecture (vérifiées en CI)

| Règle | Tests |
|---|---|
| Seulement le design system dans les écrans | `architecture/src/test/kotlin/io/github/bahuauddimitri/bercail/architecture/DesignSystemOnlyTest.kt`, detekt `config/detekt/screens.yml` |
| Interdits batterie : verrou de réveil, service au premier plan, alarme exacte | `architecture/src/test/kotlin/io/github/bahuauddimitri/bercail/architecture/BatteryRulesTest.kt` |
| Domaine sans Android, un README par module | `architecture/src/test/kotlin/io/github/bahuauddimitri/bercail/architecture/ModuleRulesTest.kt` |
| Catalogue à jour | `architecture/src/test/kotlin/io/github/bahuauddimitri/bercail/architecture/CatalogTest.kt` |
