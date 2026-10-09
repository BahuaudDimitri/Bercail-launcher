# Bercail

Un launcher Android sombre et minimaliste, pensé pour quatre usages : les messages (via Beeper), la musique et la vidéo, la maison (lampes Philips Hue) et l'agenda. Conçu pour le Pixel 9 sous Android 17, avec une priorité absolue : ne pas peser sur la batterie.

Projet personnel, en cours de construction.

## Installer

Télécharger le dernier `bercail-x.y.z.apk` depuis la page [Releases](https://github.com/BahuaudDimitri/Bercail-launcher/releases), l'installer, puis choisir Bercail dans Paramètres → Applications → Applications par défaut → Application d'écran d'accueil.

## Développer

- Les règles du projet : [`CLAUDE.md`](CLAUDE.md)
- Où est quoi : [`docs/CATALOG.md`](docs/CATALOG.md)
- Les décisions de conception : [`docs/handoff.md`](docs/handoff.md)
- Les prototypes visuels : [`design/prototype/`](design/prototype/)

Prérequis : Android Studio (son JDK suffit) et le SDK Android 37. Puis :

```bash
./gradlew check
```
