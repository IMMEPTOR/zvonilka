package com.zvoilka.blocker

import android.content.Context

class BlockerPrefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean(KEY_ENABLED, false)
        set(value) { sp.edit().putBoolean(KEY_ENABLED, value).apply() }

    companion object {
        private const val FILE = "zvoilka_prefs"
        private const val KEY_ENABLED = "enabled"
    }
}
