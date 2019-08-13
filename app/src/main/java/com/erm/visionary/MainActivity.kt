package com.erm.visionary

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.DialogInterface
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    private var photoFile: File? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_capture.setOnClickListener {
            captureAndSetImage {}
        }
        btn_switch_camera.setOnClickListener { camera?.toggleFacing() }
        btn_retake.setOnClickListener {
            photoFile = null
            picture.visibility = View.GONE
            camera.visibility = View.VISIBLE
        }
        btn_recognize_text.isLongClickable = true
        btn_recognize_text.setOnClickListener {
            val detector = FirebaseVision.getInstance()
                .onDeviceTextRecognizer
            recognizeText(detector)
        }
        btn_recognize_text.setOnLongClickListener {
            val detector = FirebaseVision.getInstance()
                .cloudTextRecognizer
            recognizeText(detector)
            Toast.makeText(this, "Using the cloud!", Toast.LENGTH_LONG).show()
            true
        }

        btn_face.setOnClickListener {

            if (photoFile != null && photoFile?.exists() == true) {
                val bitmap = BitmapFactory.decodeFile(photoFile!!.path)
                val firebaseImage = FirebaseVisionImage.fromBitmap(bitmap)
                val firebaseOptions = FirebaseVisionFaceDetectorOptions.Builder()
                    .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                    .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                    .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                    .build()
                val detector = FirebaseVision.getInstance()
                    .getVisionFaceDetector(firebaseOptions)

                val result = detector.detectInImage(firebaseImage)
                    .addOnSuccessListener {
                        if (it.isNotEmpty()) {
                            val smiling = it.first().smilingProbability
                            val leftEye = it.first().leftEyeOpenProbability
                            val rightEye = it.first().rightEyeOpenProbability
                            showAlert("Smiling probability $smiling\nLeft Eye prob: $leftEye\nRight Eye prob: $rightEye")
                        } else {
                            showAlert("No smile detected!")
                        }
                    }
                    .addOnFailureListener {
                        showAlert("Failed to detect face, ex $it")
                    }
            } else {
                captureAndSetImage { btn_face.performClick() }
            }
        }

        btn_label.isLongClickable = true
        btn_label.setOnClickListener {

            val labeler = FirebaseVision.getInstance()
                .onDeviceImageLabeler
            labelImage(labeler)
        }

        btn_label.setOnLongClickListener {
            val labeler = FirebaseVision.getInstance()
                .cloudImageLabeler
            labelImage(labeler)
            Toast.makeText(this, "Using the cloud!", Toast.LENGTH_LONG).show()
            true
        }
    }

    private fun captureAndSetImage(listener: () -> Unit) {
        camera?.captureImage { _, capturedImageBytes ->
            photoFile = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                ), "${System.currentTimeMillis()}_visionary"
            )

            val outputStream = FileOutputStream(photoFile?.path)
            try {
                outputStream.write(capturedImageBytes)
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                outputStream.close()
            }
            listener.invoke()
            Glide.with(this@MainActivity).load(capturedImageBytes).into(picture)
            camera?.visibility = View.GONE
            picture.visibility = View.VISIBLE
        }
    }

    private fun labelImage(labeler: FirebaseVisionImageLabeler) {
        if (photoFile != null && photoFile?.exists() == true) {

            val bitmap = BitmapFactory.decodeFile(photoFile!!.path)
            val firebaseImage = FirebaseVisionImage.fromBitmap(bitmap)
            labeler.processImage(firebaseImage)
                .addOnSuccessListener {
                    if (it.isNotEmpty()) {
                        showAlert("Detected: ${it.map { "${it.text}\n" }}")
                    } else {
                        showAlert("Nothing detected!")
                    }
                }
                .addOnFailureListener {
                    showAlert("Nothing detected! Exception: $it")
                }
        } else {
            captureAndSetImage { labelImage(labeler) }
        }
    }

    private fun recognizeText(detector: FirebaseVisionTextRecognizer) {
        if (photoFile != null && photoFile?.exists() == true) {
            val bitmap = BitmapFactory.decodeFile(photoFile!!.path)
            val firebaseImage = FirebaseVisionImage.fromBitmap(bitmap)
            val result = detector.processImage(firebaseImage)
                .addOnSuccessListener {
                    val resultText = it.getText()
                    showAlert(resultText)
                    for (block in it.getTextBlocks()) {
                        val blockText = block.getText()
                        val blockConfidence = block.getConfidence()
                        val blockLanguages = block.getRecognizedLanguages()
                        val blockCornerPoints = block.getCornerPoints()
                        val blockFrame = block.getBoundingBox()
                        for (line in block.getLines()) {
                            val lineText = line.getText()
                            val lineConfidence = line.getConfidence()
                            val lineLanguages = line.getRecognizedLanguages()
                            val lineCornerPoints = line.getCornerPoints()
                            val lineFrame = line.getBoundingBox()
                            for (element in line.getElements()) {
                                val elementText = element.getText()
                                val elementConfidence = element.getConfidence()
                                val elementLanguages = element.getRecognizedLanguages()
                                val elementCornerPoints = element.getCornerPoints()
                                val elementFrame = element.getBoundingBox()
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    showAlert("Failed to read text, exception")
                }
        } else {
            captureAndSetImage { recognizeText(detector) }
        }
    }

    private fun showAlert(text: String) {
        AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage(text)
            .setPositiveButton("Ok", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    dialog?.dismiss()
                }
            }).show()
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
