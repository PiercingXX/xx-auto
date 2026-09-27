package com.piercingxx.xxauto.settings

/**
 * The version line in Settings' static block (the sibling `AboutVersion`
 * helper's job). Pure so it is JVM-testable; the screen feeds it the
 * installed package's versionName / versionCode.
 */
object AboutVersion {
    const val APP_NAME = "xx-auto"

    fun line(versionName: String?, versionCode: Long): String =
        if (versionName.isNullOrBlank()) APP_NAME else "$APP_NAME $versionName ($versionCode)"
}
