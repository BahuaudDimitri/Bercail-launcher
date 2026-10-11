package io.github.bahuauddimitri.bercail.core.domain.update

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

/** The update row of the settings: the version running, and one sentence that says where things stand. */
class UpdateLineTest {
    private val current = Version(0, 8, 1)
    private val next = Version(0, 9, 0)

    private fun line(status: UpdateStatus) = updateLine(current, status)

    @Test
    fun `la ligne donne la version installée`() {
        assertThat(line(UpdateStatus.UpToDate()).title).isEqualTo("Bercail 0.8.1")
    }

    @Test
    fun `à jour, la ligne propose de vérifier`() {
        assertThat(line(UpdateStatus.UpToDate()).detail).isEqualTo("À jour · toucher pour vérifier")
        assertThat(line(UpdateStatus.UpToDate()).touchable).isTrue()
    }

    @Test
    fun `quand les versions publiées n'ont pas pu être lues, la ligne propose de réessayer`() {
        assertThat(line(UpdateStatus.UpToDate(UpdateTrouble.Unreachable)).detail)
            .isEqualTo("Vérification impossible · toucher pour réessayer")
    }

    @Test
    fun `quand Android a refusé le paquet, la ligne le dit`() {
        assertThat(line(UpdateStatus.UpToDate(UpdateTrouble.Refused)).detail)
            .isEqualTo("La dernière version n'a pas pu s'installer · toucher pour réessayer")
    }

    @Test
    fun `pendant la recherche, le téléchargement et l'installation, la ligne ne se touche pas`() {
        assertThat(line(UpdateStatus.Checking).detail).isEqualTo("Recherche d'une mise à jour…")
        assertThat(line(UpdateStatus.Downloading(next)).detail).isEqualTo("Téléchargement de la version 0.9.0…")
        assertThat(line(UpdateStatus.Installing(next)).detail).isEqualTo("Installation de la version 0.9.0…")

        assertThat(line(UpdateStatus.Checking).touchable).isFalse()
        assertThat(line(UpdateStatus.Downloading(next)).touchable).isFalse()
        assertThat(line(UpdateStatus.Installing(next)).touchable).isFalse()
    }

    @Test
    fun `une version disponible mais pas encore téléchargée se télécharge au toucher`() {
        assertThat(line(UpdateStatus.Waiting(next)).detail)
            .isEqualTo("Version 0.9.0 disponible · toucher pour la télécharger")
        assertThat(line(UpdateStatus.Waiting(next)).touchable).isTrue()
    }

    @Test
    fun `une version prête annonce qu'elle s'installera quand on quittera l'accueil`() {
        assertThat(line(UpdateStatus.Ready(next)).detail)
            .isEqualTo("Version 0.9.0 prête · elle s'installera quand tu quitteras l'accueil")
        assertThat(line(UpdateStatus.Ready(next)).touchable).isTrue()
    }

    @Test
    fun `si Android veut un accord, la version prête s'installe au toucher`() {
        assertThat(line(UpdateStatus.Ready(next, needsUser = true)).detail)
            .isEqualTo("Version 0.9.0 prête · toucher pour l'installer")
    }

    @Test
    fun `la version de mise au point dit qu'elle ne se met pas à jour toute seule`() {
        val line = updateLine(null, UpdateStatus.Off)

        assertThat(line.title).isEqualTo("Bercail, version de mise au point")
        assertThat(line.detail).isEqualTo("Elle ne se met pas à jour toute seule")
        assertThat(line.touchable).isFalse()
    }
}
