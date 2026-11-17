package com.ashishxhub.xhubrea

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class PythonAnalysisBridgeInstrumentedTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun bundledPythonEngineAnalyzesApkAndReturnsExpectedContract() {
        val apk = createFixtureApk()
        try {
            val report = JSONObject(PythonAnalysisBridge.analyze(context, apk))

            assertEquals("XhubREA Python APK Engine", report.getString("engine"))
            assertEquals("read_only", report.getString("mode"))
            assertTrue(report.getBoolean("zip_valid"))
            assertTrue(report.getJSONObject("manifest").getBoolean("present"))
            assertEquals(1, report.getJSONObject("dex").getInt("count"))
            assertTrue(report.has("resource_intelligence"))
        } finally {
            apk.delete()
        }
    }

    @Test
    fun bridgeRejectsMissingApkBeforeCallingPython() {
        val missing = File(context.cacheDir, "does-not-exist-xhubrea-test.apk")
        assertTrue(!missing.exists())

        var rejected = false
        try {
            PythonAnalysisBridge.analyze(context, missing)
        } catch (expected: IllegalArgumentException) {
            rejected = expected.message?.contains("does not exist") == true
        }
        assertTrue("Missing APK must be rejected by the bridge.", rejected)
    }

    @Test
    fun bundledPythonEngineRejectsInvalidZip() {
        val invalid = File.createTempFile("xhubrea-invalid-", ".apk", context.cacheDir)
        try {
            invalid.writeBytes("not-a-zip".toByteArray())
            var rejected = false
            try {
                PythonAnalysisBridge.analyze(context, invalid)
            } catch (expected: Exception) {
                rejected = true
            }
            assertTrue("Invalid ZIP input must not produce a successful report.", rejected)
        } finally {
            invalid.delete()
        }
    }

    private fun createFixtureApk(): File {
        val file = File.createTempFile("xhubrea-bridge-fixture-", ".apk", context.cacheDir)
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zip.write("fixture-manifest".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("classes.dex"))
            zip.write("dex-fixture".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("res/layout/main.xml"))
            zip.write("<layout />".toByteArray())
            zip.closeEntry()
        }
        return file
    }
}
