package com.zvoilka.blocker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: BlockerPrefs
    private lateinit var toggleButton: ImageButton
    private lateinit var statusText: TextView
    private lateinit var hintText: TextView
    private lateinit var langRu: TextView
    private lateinit var langEn: TextView

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (!isCallScreeningRoleHeld()) {
            prefs.enabled = false
            Toast.makeText(this, R.string.toast_role_required, Toast.LENGTH_LONG).show()
        }
        updateUi()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        requestCallScreeningRole()
        updateUi()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(DEFAULT_LANG))
        }

        setContentView(R.layout.activity_main)

        prefs = BlockerPrefs(this)
        toggleButton = findViewById(R.id.toggle_button)
        statusText = findViewById(R.id.status_text)
        hintText = findViewById(R.id.hint_text)
        langRu = findViewById(R.id.lang_ru)
        langEn = findViewById(R.id.lang_en)

        ensureNotificationChannel(this)

        toggleButton.setOnClickListener {
            if (prefs.enabled) {
                prefs.enabled = false
            } else {
                prefs.enabled = true
                requestRequiredPermissions()
            }
            updateUi()
        }

        langRu.setOnClickListener { setLanguage(LANG_RU) }
        langEn.setOnClickListener { setLanguage(LANG_EN) }

        updateLangUi()
        updateUi()
    }

    override fun onResume() {
        super.onResume()
        if (prefs.enabled && !isCallScreeningRoleHeld()) {
            prefs.enabled = false
        }
        updateLangUi()
        updateUi()
    }

    private fun setLanguage(tag: String) {
        if (currentLanguage() == tag) return
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    private fun currentLanguage(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        val tag = if (locales.isEmpty) DEFAULT_LANG else locales[0]?.language ?: DEFAULT_LANG
        return if (tag.startsWith(LANG_EN)) LANG_EN else LANG_RU
    }

    private fun updateLangUi() {
        val lang = currentLanguage()
        stylePill(langRu, lang == LANG_RU)
        stylePill(langEn, lang == LANG_EN)
    }

    private fun stylePill(view: TextView, active: Boolean) {
        if (active) {
            view.setBackgroundResource(R.drawable.bg_segment_active)
            view.setTextColor(ContextCompat.getColor(this, R.color.bg))
        } else {
            view.setBackgroundResource(0)
            view.setTextColor(ContextCompat.getColor(this, R.color.fg_dim))
        }
    }

    private fun requestRequiredPermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            needed += android.Manifest.permission.READ_CONTACTS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            needed += android.Manifest.permission.POST_NOTIFICATIONS
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            requestCallScreeningRole()
        }
    }

    private fun isCallScreeningRoleHeld(): Boolean {
        val rm = getSystemService(Context.ROLE_SERVICE) as? RoleManager ?: return false
        return rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun requestCallScreeningRole() {
        val rm = getSystemService(Context.ROLE_SERVICE) as? RoleManager ?: return
        if (rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) return
        if (!rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this, R.string.toast_unsupported, Toast.LENGTH_LONG).show()
            return
        }
        val intent = rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        roleLauncher.launch(intent)
    }

    private fun updateUi() {
        val on = prefs.enabled
        toggleButton.setBackgroundResource(if (on) R.drawable.bg_toggle_on else R.drawable.bg_toggle_off)
        statusText.setText(if (on) R.string.status_on else R.string.status_off)
        hintText.setText(if (on) R.string.hint_active else R.string.hint_tap)
    }

    companion object {
        const val CHANNEL_ID = "blocked_calls"
        private const val LANG_RU = "ru"
        private const val LANG_EN = "en"
        private const val DEFAULT_LANG = LANG_RU

        fun ensureNotificationChannel(ctx: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
                if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                    val ch = NotificationChannel(
                        CHANNEL_ID,
                        ctx.getString(R.string.channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = ctx.getString(R.string.channel_desc)
                    }
                    nm.createNotificationChannel(ch)
                }
            }
        }
    }
}
