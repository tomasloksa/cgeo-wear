package io.github.tomasloksa.cgeowear

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import io.github.tomasloksa.cgeowear.common.WearPaths

/** Launches the compass when the phone bridge publishes a navigation target. */
class NavigationListenerService : WearableListenerService() {

    override fun onDataChanged(events: DataEventBuffer) {
        val hasTarget = events.any {
            it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == WearPaths.TARGET
        }
        events.release()
        Log.d(TAG, "onDataChanged, hasTarget=$hasTarget")
        if (hasTarget) {
            startActivity(
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private companion object {
        const val TAG = "NavListenerSvc"
    }
}
