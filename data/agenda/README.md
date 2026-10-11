# :data:agenda

L'agenda du téléphone (tous les comptes que l'utilisateur y a mis), derrière l'interface `AgendaSource` du domaine.

- **Expose** : `DeviceAgenda` (les rendez-vous d'une journée lus dans `CalendarContract`, l'ouverture d'un rendez-vous ou d'un jour dans l'app agenda du téléphone).
- **Ne doit pas** : écrire dans l'agenda (seule l'autorisation de lecture est déclarée), ni lire quoi que ce soit avant que l'utilisateur ait donné son accord.
- **Règles** : seulement les rendez-vous qui commencent ce jour-là ; les journées entières à part (Android les range en temps universel) ; ni refusés, ni annulés, ni ceux d'un agenda masqué ; un identifiant par occurrence d'un rendez-vous qui se répète.
- **Batterie** : Android n'est interrogé que lorsque quelqu'un écoute (accueil affiché ou recherche ouverte), puis prévient lui-même des changements ; une rafale de changements ne compte qu'une fois ; rien ne reste enregistré ensuite. Lecture hors du fil principal.
- **Tests** : `DeviceAgendaUiTest` (Robolectric), contre `FakeCalendar`, un faux agenda Android qui répond à la même adresse avec les mêmes colonnes.
