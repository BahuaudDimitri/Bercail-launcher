# :feature:search

La recherche de Bercail : la liste qui remplit le tiroir quand on touche la barre « Chercher ».

- **Expose** : `SearchRoute` et `SearchList` (la liste : toutes les apps de A à Z avec l'alphabet, ou les résultats par famille), `SearchViewModel`, `AppIcons` (les icônes des apps, dessinées une fois et gardées).
- **Ne doit pas** : connaître l'accueil (c'est l'app qui glisse cette liste dans le tiroir), ni une vraie source : il ne voit que les interfaces du domaine. Les règles de recherche sont dans `:core:domain`.
- **Batterie** : apps, gens, maison et agenda ne sont écoutés que lorsque la recherche est ouverte ; la dernière liste connue est gardée pour s'afficher tout de suite la fois suivante.
- **Tests** : `SearchViewModelTest`, `SearchListUiTest`, `SearchScreenshotTest`.
