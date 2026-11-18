package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.domain.model.CameraScanPolicy
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ThresholdCameraAnalysisController(private val context: Context) {

    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val thresholdCaptureRequested = AtomicBoolean(false)

    @Volatile
    private var scanPolicy: CameraScanPolicy = CameraScanPolicy.THRESHOLD_ONLY

    @Volatile
    private var lastSparseSampleMillis = 0L

    fun updateScanPolicy(policy: CameraScanPolicy) {
        scanPolicy = policy
    }

    fun requestThresholdCapture() {
        thresholdCaptureRequested.set(true)
    }

    fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?,
        onFrameCaptured: (bitmap: Bitmap, isThresholdTriggered: Boolean) -> Unit,
        onBindSuccess: () -> Unit,
        onBindError: () -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val selector = when {
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ->
                        CameraSelector.DEFAULT_BACK_CAMERA
                    else -> {
                        onBindError()
                        return@addListener
                    }
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    try {
                        val now = System.currentTimeMillis()
                        val isThresholdCapture = thresholdCaptureRequested.compareAndSet(true, false)
                        // Battery-first & privacy-first:
                        // - THRESHOLD_ONLY (intervalMillis == null) never captures passively
                        // - SPARSE_10S / SPARSE_30S / SPARSE_60S captures sparsely on configured cycle
                        val interval = scanPolicy.intervalMillis
                        val shouldSampleSparse = interval != null &&
                            (now - lastSparseSampleMillis >= interval)

                        if (!isThresholdCapture && !shouldSampleSparse) {
                            return@setAnalyzer
                        }
                        if (!isThresholdCapture) {
                            lastSparseSampleMillis = now
                        }

                        val rawBitmap = imageProxy.toBitmap()
                        val rotation = imageProxy.imageInfo.rotationDegrees.toFloat()
                        val orientedBitmap = if (rotation != 0f) {
                            val matrix = Matrix().apply { postRotate(rotation) }
                            Bitmap.createBitmap(
                                rawBitmap,
                                0,
                                0,
                                rawBitmap.width,
                                rawBitmap.height,
                                matrix,
                                true
                            ).also {
                                if (it !== rawBitmap) rawBitmap.recycle()
                            }
                        } else {
                            rawBitmap
                        }

                        onFrameCaptured(orientedBitmap, isThresholdCapture)
                    } catch (_: Throwable) {
                    } finally {
                        imageProxy.close()
                    }
                }

                cameraProvider.unbindAll()
                if (previewView != null) {
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageAnalysis
                    )
                } else {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        imageAnalysis
                    )
                }
                onBindSuccess()
            } catch (_: Throwable) {
                onBindError()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun unbindAll() {
        try {
            val future = ProcessCameraProvider.getInstance(context)
            if (future.isDone) {
                future.get().unbindAll()
            }
        } catch (_: Throwable) {
        }
    }

    fun release() {
        unbindAll()
        analysisExecutor.shutdown()
    }
}
