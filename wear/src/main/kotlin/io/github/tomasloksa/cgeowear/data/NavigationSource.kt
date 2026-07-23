package io.github.tomasloksa.cgeowear.data

import io.github.tomasloksa.cgeowear.common.NavState
import kotlinx.coroutines.flow.Flow

/**
 * Where navigation state comes from. M1 uses [FakeNavigationSource];
 * M2 swaps in a Data-Layer-backed implementation fed by the phone bridge,
 * and a watch-GPS implementation stays possible as a v2 option.
 */
interface NavigationSource {
    val state: Flow<NavState>
}
