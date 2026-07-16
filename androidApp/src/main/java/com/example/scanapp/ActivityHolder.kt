package com.example.scanapp

import android.app.Activity

// Holds the currently visible KuiklyRenderActivity so shared (commonMain) UI code
// can trigger Android-specific actions (e.g. runtime permission requests) that need
// an Activity context. Set on each activity resume, cleared on pause/destroy.
object ActivityHolder {
    @Volatile
    var currentActivity: Activity? = null
        private set

    fun set(activity: Activity) {
        currentActivity = activity
    }

    fun clear(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }
}
