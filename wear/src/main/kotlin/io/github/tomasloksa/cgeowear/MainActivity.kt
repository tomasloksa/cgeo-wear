package io.github.tomasloksa.cgeowear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.tomasloksa.cgeowear.data.FakeNavigationSource
import io.github.tomasloksa.cgeowear.sensor.HeadingProvider
import io.github.tomasloksa.cgeowear.ui.CompassScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // M1: fake data. M2 swaps this for a Data-Layer-backed source.
        val navigationSource = FakeNavigationSource()
        val headingProvider = HeadingProvider(this)

        setContent {
            // Lifecycle-aware collection: flows stop (and the sensor listener
            // unregisters) whenever the activity leaves STARTED.
            val navState by navigationSource.state.collectAsStateWithLifecycle(initialValue = null)
            val heading by headingProvider.heading.collectAsStateWithLifecycle(initialValue = null)
            CompassScreen(state = navState, heading = heading)
        }
    }
}
