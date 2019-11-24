package com.wtmevent.automl

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.automl.FirebaseAutoMLLocalModel
import com.google.firebase.ml.vision.automl.FirebaseAutoMLRemoteModel
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    /*Specify the name of remote model so that FirebaseModelManager can download it*/
    private val mRemoteModel by lazy {
        FirebaseAutoMLRemoteModel.Builder(AppConstants.DATA_MODEL).build()
    }

    /*Specify the path to load the local model*/
    private val mLocalModel by lazy {
        FirebaseAutoMLLocalModel.Builder()
            .setAssetFilePath(AppConstants.Path.LOCAL_MODEL)
            .build()
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

                    val optionBuilder = if (modelDownloaded) {
                        FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder(mRemoteModel)
                    } else {
                        FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder(mLocalModel)
                    }

                    //Threshold to range from 0.0 to 1.0
                    optionBuilder.setConfidenceThreshold(0.5f)

                    val labeler = FirebaseVision.getInstance()
                        .getOnDeviceAutoMLImageLabeler(optionBuilder.build())

                    processSelectedImage(data, labeler)
                }
        }
    }

    /**
     * Get the URI of the selected image from gallery and process it using the
     * firebase API for image processing.
     *
     * Append all the result found and set it to the text view, in case no result is
     * found then display an alert message.
     */
    private fun processSelectedImage(
        data: Intent,
        labeler: FirebaseVisionImageLabeler
    ) {
        data.data?.let { uri ->
            ivSelectedImage.setImageURI(uri)

            val image = FirebaseVisionImage.fromFilePath(this@MainActivity, uri)
            labeler.processImage(image)
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

    private fun setOnClickListener() {

        btSelectImage.setOnClickListener {
            getImageFromGallery()
        }
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
            .addOnCompleteListener { }
    }

    /**
     * An extension over float to convert any float point number into two digit
     * integer and append % symbol at the end.
     */
    private val Float.calculatePercentage
        get() = "${this.times(100).toInt()} %"

    private fun String.showSnackBar() {
        Snackbar.make(btSelectImage, this, Snackbar.LENGTH_LONG)
            .show()
    }
}
