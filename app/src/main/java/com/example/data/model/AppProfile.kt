package com.example.data.model

import androidx.annotation.DrawableRes
import com.example.R

enum class AppProfile(
    val id: String,
    val displayNameEnglish: String,
    val displayNameHindi: String,
    val subtitleEnglish: String,
    val subtitleHindi: String,
    @DrawableRes val splashLogoRes: Int,
    @DrawableRes val headerLogoRes: Int,
    @DrawableRes val drawerLogoRes: Int = headerLogoRes,
    @DrawableRes val appIconRes: Int
) {
    CHURCH(
        id = "CHURCH",
        displayNameEnglish = "New Creation Church",
        displayNameHindi = "नई सृष्टि कलीसिया खैरवार",
        subtitleEnglish = "Fellowship • Worship • The Word of God",
        subtitleHindi = "संगति • आराधना • परमेश्वर का वचन",
        splashLogoRes = R.drawable.church_brand_logo,
        headerLogoRes = R.drawable.church_header_logo,
        drawerLogoRes = R.drawable.church_drawer_logo,
        appIconRes = R.drawable.church_app_icon
    ),
    VINAY(
        id = "VINAY",
        displayNameEnglish = "Vinay Kumar Avj",
        displayNameHindi = "विनय कुमार एवीजे",
        subtitleEnglish = "Fellowship Events • Videos • Photos • Holy Bible",
        subtitleHindi = "संगति • संदेश • आराधना • पवित्र बाइबल",
        splashLogoRes = R.drawable.brand_logo,
        headerLogoRes = R.drawable.header_logo,
        drawerLogoRes = R.drawable.header_logo,
        appIconRes = R.drawable.vinay_app_icon
    );

    companion object {
        const val PROFILE_B_PASSWORD = "Vin@122333"
        val DEFAULT = CHURCH

        fun fromId(id: String?): AppProfile {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}

val LocalAppProfile = androidx.compose.runtime.staticCompositionLocalOf { AppProfile.DEFAULT }

