package com.wtmevent.automl

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.automl.FirebaseAutoMLRemoteModel
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private var mIsModelAvailable = false

    private val mRemoteModel by lazy {
        FirebaseAutoMLRemoteModel.Builder(AppConstants.DATA_MODEL).build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        downloadModel()
        setOnClickListener()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AppConstants.Request.GALLERY_CODE && resultCode == RESULT_OK && data != null) {

            FirebaseModelManager.getInstance().isModelDownloaded(mRemoteModel)
                .addOnSuccessListener { modelDownloaded ->

                    if (modelDownloaded == true) {
                        data.data?.let { uri ->
                            ivSelectedImage.setImageURI(uri)

                            val image = FirebaseVisionImage.fromFilePath(this@MainActivity, uri)
                            getFirebaseImageLabeler().processImage(image)
                                .addOnSuccessListener {
                                    val builder = StringBuilder()

                                    for (label in it) {
                                        builder.append(label.text).append(":")
                                            .append(label.confidence.calculatePercentage)
                                        builder.append("\n")
                                    }

                                    if (builder.isBlank()) {
                                        getString(R.string.snack_bar_alert_unable_to_detect).showSnackBar()
                                    } else {
                                        tvLabel.text = builder.toString()
                                    }
                                }
                        }
                    }
                }
        }
    }

    private fun setOnClickListener() {

        btSelectImage.setOnClickListener {
            if (mIsModelAvailable) {
                getImageFromGallery()
            } else {
                getString(R.string.snack_bar_alert_wait_until_model_downloads).showSnackBar()
            }
        }
    }

    private fun getFirebaseImageLabeler(): FirebaseVisionImageLabeler {
        val option = FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder(mRemoteModel)
            .setConfidenceThreshold(0.5f)
            .build()

        return FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(option)
    }

    private fun getImageFromGallery() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, AppConstants.Request.GALLERY_CODE)
    }


    /**
     * Download the model on the first launch so that from next time onwards it will be available
     * without any delay.
     *
     * No condition is added however condition like device charging, wifi etc can be added.
     */
    private fun downloadModel() {
        val conditions = FirebaseModelDownloadConditions.Builder()
            .build()

        FirebaseModelManager.getInstance().download(mRemoteModel, conditions)
            .addOnCompleteListener {
                mIsModelAvailable = it.isSuccessful
            }
    }

    private val Float.calculatePercentage
        get() = "${this.times(100).toInt()} %"

    private fun String.showSnackBar() {
        Snackbar.make(btSelectImage, this, Snackbar.LENGTH_LONG)
            .show()
    }
}
