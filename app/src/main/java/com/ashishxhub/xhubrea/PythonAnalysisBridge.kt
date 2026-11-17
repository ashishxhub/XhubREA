package com.ashishxhub.xhubrea

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONException
import org.json.JSONObject
import java.io.File

/**
 * Single entry point for the embedded Python APK engine.
 *
 * This bridge only accepts an existing readable file and validates the
 * Python engine's JSON contract before returning a report to the UI.
 */
internal object PythonAnalysisBridge {
    private val startupLock = Any()

    fun analyze(context: Context, apkFile: File): String {
        require(apkFile.isFile) { "APK file does not exist." }
        require(apkFile.canRead()) { "APK file cannot be read." }
        require(apkFile.length() > 0L) { "APK file is empty." }

        val applicationContext = context.applicationContext
        synchronized(startupLock) {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(applicationContext))
            }
        }

        val rawReport = Python.getInstance()
            .getModule("xhubrea_engine")
            .callAttr("analyze_apk_json", apkFile.absolutePath)
            .toString()

        val report = try {
            JSONObject(rawReport)
        } catch (error: JSONException) {
            throw IllegalStateException("Python engine returned invalid JSON.", error)
        }

        require(report.optString("engine") == "XhubREA Python APK Engine") {
            "Python engine returned an unexpected report."
        }
        require(report.optString("mode") == "read_only") {
            "Python engine report is not marked read-only."
        }
        require(report.optBoolean("zip_valid", false)) {
            "Python engine did not confirm a valid APK/ZIP container."
        }
        require(report.has("manifest") && report.has("dex") && report.has("resource_intelligence")) {
            "Python engine report is missing required sections."
        }

        return rawReport
    }
}
