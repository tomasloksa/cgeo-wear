package io.github.tomasloksa.cgeowear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.tomasloksa.cgeowear.data.DataLayerNavigationSource
import io.github.tomasloksa.cgeowear.sensor.HeadingProvider
import io.github.tomasloksa.cgeowear.ui.compassScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navigationSource = DataLayerNavigationSource(this)
        val headingProvider = HeadingProvider(this)

        setContent {
            val navState by navigationSource.state.collectAsStateWithLifecycle(initialValue = null)
            val heading by headingProvider.heading.collectAsStateWithLifecycle(initialValue = null)
            compassScreen(state = navState, heading = heading)
        }
    }
}
