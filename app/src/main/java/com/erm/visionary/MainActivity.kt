package com.erm.visionary

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_capture.setOnClickListener {
            camera?.captureImage { cameraKitView, capturedImageBytes ->
                val photoFile = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                    ), "${System.currentTimeMillis()}_visionary"
                )

                val outputStream = FileOutputStream(photoFile.path)
                try {
                    Glide.with(this@MainActivity).load(photoFile).into(picture)
                    camera?.visibility = View.GONE
                    picture.visibility = View.VISIBLE
                    outputStream.write(capturedImageBytes)
                } catch (e: Throwable) {
                    e.printStackTrace()
                } finally {
                    outputStream.close()
                }
            }
        }
        btn_switch_camera.setOnClickListener { camera?.toggleFacing() }
        btn_retake.setOnClickListener {
            picture.visibility = View.GONE
            camera.visibility = View.VISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        camera.onStart()
        ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE), 111)
        picture.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        camera.onResume()
        picture.visibility = View.GONE
    }

    override fun onPause() {
        camera.onPause()
        super.onPause()
    }

    override fun onStop() {
        camera.onStop()
        super.onStop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        camera.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
