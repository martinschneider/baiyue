package io.github.martinschneider.baiyue.data

import android.content.Context
import android.content.SharedPreferences

class OnboardingPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) { prefs.edit().putBoolean("onboarding_completed", value).apply() }

    var disclaimerAccepted: Boolean
        get() = prefs.getBoolean("disclaimer_accepted", false)
        set(value) { prefs.edit().putBoolean("disclaimer_accepted", value).apply() }
}
