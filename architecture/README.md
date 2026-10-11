# :architecture

Tests d'architecture (Konsist) qui lisent tout le dépôt. Aucun code de production.

- `DesignSystemOnlyTest` : les modules d'écran n'utilisent ni Material, ni `BasicText`, ni couleur ou taille de texte en dur.
- `BatteryRulesTest` : ni verrou de réveil, ni service au premier plan, ni alarme exacte, ni travail programmé en arrière-plan ; réseau en https seulement.
- `ModuleRulesTest` : domaine sans Android, un README par module.
- `CatalogTest` : `docs/CATALOG.md` suit le code.
