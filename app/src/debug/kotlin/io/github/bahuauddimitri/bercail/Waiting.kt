package io.github.bahuauddimitri.bercail

import io.github.bahuauddimitri.bercail.core.testing.FakeWorld

/**
 * In the debug app, the source that is not plugged in yet shows the prototype's data: every state of the home
 * screen can be seen and tried on the phone. The released app shows an empty state instead.
 */
internal fun waiting(): Waiting = Waiting(home = FakeWorld().home)
