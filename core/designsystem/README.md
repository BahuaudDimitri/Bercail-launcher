# :core:designsystem

Le design system de Bercail : jetons (couleurs, police Outfit, espacements, formes, mouvements), `BcTheme` et les composants `Bc…`. C'est la seule porte vers Compose pour les écrans.

- **Expose** : les composants publics `Bc…`, chacun inscrit au catalogue (`docs/CATALOG.md`).
- **Ne doit pas** : connaître le domaine ni les sources de données.
- **Tests** : chaque composant a ses tests de comportement et une capture de référence par état (Roborazzi). Construit en vague 2.
