# :feature:home

L'accueil de Bercail : ses deux écrans (Accueil et Écoute) et son tiroir en verre, assemblés uniquement avec les composants `Bc…` du design system.

- **Expose** : `HomeRoute` (l'écran branché sur son `HomeViewModel`), `HomeScreen` (l'écran sans état, pour les tests et les captures), `HomeViewModel`.
- **Ne doit pas** : contenir de règle métier (elles sont dans `:core:domain`), ni de couleur, de taille de texte ou de composant Material (vérifié par `DesignSystemOnlyTest`), ni connaître une vraie source de données : il ne voit que les interfaces du domaine.
- **Batterie** : l'écran n'écoute ses sources que lorsqu'il est visible ; la position du morceau n'est suivie que sur l'écran Écoute.
- **Tests** : `HomeViewModelTest` (ce que l'écran expose selon les événements), `HomeScreenUiTest` (gestes et états affichés), `HomeScreenshotTest` (une capture par moment).
