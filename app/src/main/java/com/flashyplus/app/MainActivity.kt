package com.flashyplus.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var toggleButton: Button
    private lateinit var cameraManager: CameraManager
    private var flashOn = false
    private val prefs by lazy { getSharedPreferences("flashy", Context.MODE_PRIVATE) }
    private val cameraPermissionCode = 24

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        flashOn = prefs.getBoolean("flash_on", false)
        buildScreen()
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), cameraPermissionCode)
        }
        refreshStatus()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 34, 28, 28)
            setBackgroundColor(0xFF070B12.toInt())
        }

        val title = TextView(this).apply {
            text = "🔦 Flashy+"
            textSize = 32f
            setTextColor(0xFF28A5FF.toInt())
            gravity = Gravity.CENTER
        }
        root.addView(title, matchWrap())

        val subtitle = TextView(this).apply {
            text = "الفلاش في لحظة"
            textSize = 18f
            setTextColor(0xFFEAF3FF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 24)
        }
        root.addView(subtitle, matchWrap())

        status = TextView(this).apply {
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 18)
        }
        root.addView(status, matchWrap())

        toggleButton = Button(this).apply {
            text = "تشغيل / إطفاء الفلاش"
            setOnClickListener { toggleFlash() }
        }
        root.addView(toggleButton, matchWrap())

        val accessButton = Button(this).apply {
            text = "تفعيل زر رفع الصوت"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(this@MainActivity,
                    "ابحث عن Flashy+ ضمن التطبيقات المثبتة ثم فعّله بنفسك.", Toast.LENGTH_LONG).show()
            }
        }
        val accessParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 14 }
        root.addView(accessButton, accessParams)

        val help = TextView(this).apply {
            text = "طريقة الاستخدام:\\n1) امنح التطبيق إذن الكاميرا لتشغيل الفلاش.\\n2) اضغط «تفعيل زر رفع الصوت»، ثم فعّل خدمة Flashy+ من إعدادات إمكانية الوصول.\\n3) اضغط مطولاً على زر رفع الصوت لمدة نصف ثانية تقريباً لتبديل حالة الفلاش.\\n\\nمهم: أندرويد يتطلب تفعيل خدمة إمكانية الوصول يدوياً. قد تختلف استجابة أزرار الصوت حسب الهاتف، وقد لا تعمل أثناء المكالمات أو في بعض التطبيقات. يمكنك إيقاف الخدمة من إعدادات إمكانية الوصول."
            textSize = 15f
            setTextColor(0xFFB9C7D9.toInt())
            setPadding(0, 24, 0, 0)
        }
        root.addView(help, matchWrap())

        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun findFlashCameraId(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) { null }
    }

    private fun toggleFlash() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), cameraPermissionCode)
            return
        }
        val id = findFlashCameraId()
        if (id == null) {
            Toast.makeText(this, "لا يوجد فلاش متاح في هذا الجهاز.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            flashOn = !flashOn
            cameraManager.setTorchMode(id, flashOn)
            prefs.edit().putBoolean("flash_on", flashOn).apply()
            refreshStatus()
        } catch (e: Exception) {
            flashOn = false
            prefs.edit().putBoolean("flash_on", false).apply()
            Toast.makeText(this, "تعذّر تشغيل الفلاش. أغلق تطبيق الكاميرا وحاول مجدداً.", Toast.LENGTH_LONG).show()
            refreshStatus()
        }
    }

    private fun refreshStatus() {
        status.text = if (flashOn) "الفلاش: يعمل 🔆" else "الفلاش: متوقف"
        toggleButton.text = if (flashOn) "إطفاء الفلاش" else "تشغيل الفلاش"
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionCode && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "تم منح إذن الكاميرا.", Toast.LENGTH_SHORT).show()
        } else if (requestCode == cameraPermissionCode) {
            Toast.makeText(this, "يلزم إذن الكاميرا للتحكم في الفلاش.", Toast.LENGTH_LONG).show()
        }
    }
}
