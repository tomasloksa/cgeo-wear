package io.github.tomasloksa.cgeowear.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import io.github.tomasloksa.cgeowear.common.NavCodec
import io.github.tomasloksa.cgeowear.common.NavState
import io.github.tomasloksa.cgeowear.common.NavTarget
import io.github.tomasloksa.cgeowear.common.NavTick
import io.github.tomasloksa.cgeowear.common.WearPaths
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Navigation state from the phone bridge: target via DataClient (durable), ticks via MessageClient. */
class DataLayerNavigationSource(context: Context) : NavigationSource {

    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val messageClient: MessageClient = Wearable.getMessageClient(context)

    override val state: Flow<NavState> = callbackFlow {
        var target: NavTarget? = null
        var tick: NavTick? = null

        fun emitIfReady() {
            val currentTarget = target
            val currentTick = tick
            if (currentTarget != null && currentTick != null) {
                Log.d(TAG, "emitting NavState: ${currentTarget.name} ${currentTick.distanceMeters}m")
                trySend(NavState(currentTarget, currentTick))
            } else {
                Log.d(TAG, "not ready yet: target=${currentTarget != null}, tick=${currentTick != null}")
            }
        }

        val dataListener = DataClient.OnDataChangedListener { events ->
            for (event in events) {
                if (event.type == DataEvent.TYPE_CHANGED &&
                    event.dataItem.uri.path == WearPaths.TARGET
                ) {
                    event.dataItem.data?.let {
                        target = NavCodec.decodeTarget(it)
                        Log.d(TAG, "target via DataClient change: ${target?.name}")
                        emitIfReady()
                    }
                }
            }
            events.release()
        }

        val messageListener = MessageClient.OnMessageReceivedListener { event ->
            if (event.path == WearPaths.TICK) {
                tick = NavCodec.decodeTick(event.data)
                Log.d(TAG, "tick via MessageClient: ${tick?.distanceMeters}m ${tick?.bearingDeg}deg")
                emitIfReady()
            }
        }

        Log.d(TAG, "registering Data Layer listeners")
        dataClient.addListener(dataListener)
        messageClient.addListener(messageListener)

        dataClient.dataItems.addOnSuccessListener { buffer ->
            Log.d(TAG, "initial data items: ${buffer.count}")
            for (item in buffer) {
                if (item.uri.path == WearPaths.TARGET) {
                    item.data?.let {
                        target = NavCodec.decodeTarget(it)
                        Log.d(TAG, "target from initial data items: ${target?.name}")
                        emitIfReady()
                    }
                }
            }
            buffer.release()
        }

        awaitClose {
            Log.d(TAG, "removing Data Layer listeners")
            dataClient.removeListener(dataListener)
            messageClient.removeListener(messageListener)
        }
    }

    private companion object {
        const val TAG = "DataLayerNav"
    }
}
