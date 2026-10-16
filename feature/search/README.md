# :feature:search

La recherche de Bercail : la liste qui remplit le tiroir quand on touche la barre « Chercher ».

- **Expose** : `SearchRoute` et `SearchList` (la liste : toutes les apps de A à Z avec l'alphabet, ou les résultats par famille), `SearchViewModel`, `AppIcons` (les icônes des apps en une seule couleur, demandées pour les lignes affichées, dessinées une fois et gardées).
- **Ne doit pas** : connaître l'accueil (c'est l'app qui glisse cette liste dans le tiroir), ni une vraie source : il ne voit que les interfaces du domaine. Les règles de recherche sont dans `:core:domain`.
- **Batterie** : apps, gens, maison et agenda ne sont écoutés que lorsque la recherche est ouverte ; la dernière liste connue est gardée pour s'afficher tout de suite la fois suivante.
- **Tests** : `SearchViewModelTest`, `SearchListUiTest`, `SearchScreenshotTest`.
- **Messages** : à partir de trois lettres, et une fois la frappe en pause, la messagerie est interrogée sur les messages qui contiennent ce qui est tapé ; les cinq plus récents s'affichent sous « Messages », chacun sous le nom de sa conversation, et l'ouvrent d'un toucher.
