package com.lexmasterteam.cameratestapp

import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lexmasterteam.cameratestapp.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // request camera permissions
        if(allPremissionsGranted()){
            startCamera()
        }else{
            ActivityCompat.requestPermissions(this,REQUIRED_PERMISSIONS,REQUEST_CODE_PERMISSIONS)
        }

        //Setup the listener take for photo button

        binding.btTakePhoto.setOnClickListener {
            takePhoto()
        }
        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it,resources.getString(R.string.app_name)).apply {
                mkdir()
            }
        }
        return  if(mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun allPremissionsGranted() = REQUIRED_PERMISSIONS.all{
        ContextCompat.checkSelfPermission(baseContext,it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            //podpięcie livecycle
            val cameraProvider:ProcessCameraProvider = cameraProviderFuture.get()

            // podgląd
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.ViewPreview.surfaceProvider) // podpięcie widoku ekranu
            }

            imageCapture = ImageCapture.Builder().build()

            // przypnij tylną kamerę

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // odepnij use case przy przepiąć kamerę
                cameraProvider.unbindAll()

                // podepnij use case
                cameraProvider.bindToLifecycle(this,cameraSelector,preview,imageCapture)
            } catch (exc:Exception){
                Log.e(TAG,"Use binding failed",exc)
            }
        },ContextCompat.getMainExecutor(this))

    }

    private fun takePhoto() {
        // złap widok z kamery

        val imageCapture = imageCapture?:return

        // przygotuj plik

        val photoFile = File(outputDirectory,SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())+".jpg")

        // stwórz plik z metadanymi

        val outputOPTIONS = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        //ustawienie przejęcia obrazu

        imageCapture.takePicture(outputOPTIONS, ContextCompat.getMainExecutor(this),
        object  : ImageCapture.OnImageSavedCallback{
            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG,"Photo capture failed: ${exception.message}",exception)
            }

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                val msg = "Photo captured succeded: $savedUri"
                Toast.makeText(baseContext,msg,Toast.LENGTH_SHORT).show()

                Log.d(TAG,msg)
            }
        }

        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPremissionsGranted()){
                startCamera()
            }else{
                Toast.makeText(this,"Permissions not granted!",Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object{
        private const val TAG = "CameraXTest"
        private const val FILENAME_FORMAT = "yyy-MM-dd-HH-mm-ss-SSS"
        private const val  REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    }
}