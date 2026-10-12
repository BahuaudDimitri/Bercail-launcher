# :data:messages

Les conversations de toutes les messageries, lues dans Beeper sur le téléphone, derrière l'interface `MessagesSource` du domaine.

- **Expose** : `BeeperMessages` (les conversations, la plus récente en tête, avec leur réseau, leurs non-lus et leur sourdine ; l'ouverture de Beeper), `BeeperAccess` (les deux droits que Beeper donne à une autre app, lire et envoyer, et s'ils sont accordés).
- **Ne doit pas** : lire quoi que ce soit avant l'accord de l'utilisateur, ni rien envoyer hors du téléphone. Beeper est interrogé sur place, par son fournisseur de contenu `com.beeper.api` (interface expérimentale : si elle échoue, la liste est vide et l'accueil continue).
- **Règles** : cent conversations au plus, sans les archivées ni celles que l'utilisateur a rangées en « moins importantes » ; une conversation sans titre s'appelle « Sans nom » ; chaque réseau porte son nom courant (`networkName`).
- **Batterie** : Beeper prévient quand la liste des conversations change : ni minuterie ni boucle. Rien n'est observé quand personne n'écoute (accueil caché). L'historique d'une conversation, lent à lire, n'est jamais lu pour l'accueil.
- **Tests** : `BeeperMessagesUiTest` (Robolectric), contre `FakeBeeper`, un faux Beeper qui répond à la même adresse et avec les mêmes colonnes que le vrai.
