package com.onthecourt.app

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/**
 * Forces the Hebrew locale (and therefore RTL layout direction) for every screen,
 * regardless of the device's system language. [AppCompatDelegate.setApplicationLocales]
 * alone was unreliable on some Android versions, so we wrap the base context directly.
 */
open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val locale = Locale("he")
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
}
