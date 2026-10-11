package io.github.bahuauddimitri.bercail.core.testing

import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.access.AccessSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** What the user agreed to, set by hand. Everything by default: the tests that need a refusal ask for it. */
class FakeAccess(granted: Set<Access> = Access.entries.toSet()) : AccessSource {
    override val granted = MutableStateFlow(granted)

    fun grant(access: Access) = granted.update { it + access }

    fun refuse(access: Access) = granted.update { it - access }
}
