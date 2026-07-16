package com.example.scanapp

import androidx.activity.ComponentActivity

// Holds the currently visible KuiklyRenderActivity so shared (commonMain) UI code
// can trigger Android-specific actions (e.g. runtime permission requests) that need
// a ComponentActivity context. Set on each activity resume, cleared on pause/destroy.
object ActivityHolder {
    @Volatile
    var currentActivity: ComponentActivity? = null
        private set

    fun set(activity: ComponentActivity) {
        currentActivity = activity
    }

    fun clear(activity: ComponentActivity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }
}
