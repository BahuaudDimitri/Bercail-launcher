# :feature:home

L'accueil de Bercail : ses deux écrans (Accueil et Écoute), son tiroir en verre et la feuille « Aujourd'hui », assemblés uniquement avec les composants `Bc…` du design system.

- **Expose** : `HomeRoute` (l'écran branché sur son `HomeViewModel`), `HomeScreen` (l'écran sans état, pour les tests et les captures), `HomeViewModel`.
- **Ne doit pas** : contenir de règle métier (elles sont dans `:core:domain`), ni de couleur, de taille de texte ou de composant Material (vérifié par `DesignSystemOnlyTest`), ni connaître une vraie source de données : il ne voit que les interfaces du domaine.
- **Agenda** : sans le droit de lire l'agenda, l'en-tête propose de le relier et la feuille de la journée offre le bouton qui pose la question d'Android (`HomeLinks.onAskAccess`) ; rien de l'agenda n'est montré avant l'accord.
- **Batterie** : l'écran n'écoute ses sources que lorsqu'il est visible ; la position du morceau n'est suivie que sur l'écran Écoute.
- **Tests** : `HomeViewModelTest` (ce que l'écran expose selon les événements), `HomeScreenUiTest` (gestes et états affichés), `HomeScreenshotTest` (une capture par moment).
