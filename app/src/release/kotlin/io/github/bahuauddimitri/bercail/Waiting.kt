package io.github.bahuauddimitri.bercail

import io.github.bahuauddimitri.bercail.core.domain.Unconnected

/** In the released app, a source that is not plugged in yet shows its empty state, never made-up data. */
internal fun waiting() = Waiting(home = Unconnected.home)
