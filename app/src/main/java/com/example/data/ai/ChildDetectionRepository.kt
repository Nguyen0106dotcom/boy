package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.FaceDetector
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.example.domain.model.ChildDetectionResult
import com.example.domain.model.DemoTargetProfile
import com.example.domain.model.DetectedSubject
import com.example.domain.model.DetectionSensitivity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "ChildDetectionRepo"

/**
 * Single AI repository interface for soft age-group estimation.
 * Uses Gemini Vision via Firebase AI Logic when configured & enabled,
 * and seamlessly falls back to on-device simulation when Firebase/camera is unavailable.
 */
interface ChildDetectionRepository {
    suspend fun analyzeFrame(
        bitmap: Bitmap?,
        demoProfile: DemoTargetProfile,
        assumePresenceWithoutCamera: Boolean,
        isThresholdTriggered: Boolean = false,
        aiEnabled: Boolean = true,
        sensitivity: DetectionSensitivity = DetectionSensitivity.MEDIUM
    ): ChildDetectionResult
}

/**
 * Hybrid repository: tries Gemini Vision (`gemini-3.8-flash`) via Firebase AI Logic when Firebase is configured,
 * and falls back cleanly to PlaceholderChildDetectionRepository when unconfigured, offline, or missing camera.
 */
class HybridChildDetectionRepository(
    context: Context,
    private val placeholder: PlaceholderChildDetectionRepository = PlaceholderChildDetectionRepository()
) : ChildDetectionRepository {

    private val appContext = context.applicationContext

    private val ageEstimationSchema = Schema.obj(
        properties = mapOf(
            "subject" to Schema.enumeration(
                values = listOf("CHILD", "ADULT", "NONE"),
                description = "Ước lượng mềm đối tượng chính trước camera: CHILD (trẻ em), ADULT (người lớn), hoặc NONE"
            ),
            "confidencePercent" to Schema.integer(description = "Độ tự tin ước lượng từ 0 đến 100"),
            "faceCount" to Schema.integer(description = "Số khuôn mặt quan sát thấy"),
            "reason" to Schema.string(description = "Giải thích ngắn gọn bằng tiếng Việt")
        )
    )

    private val geminiVisionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = ageEstimationSchema
            }
        )
    }

    override suspend fun analyzeFrame(
        bitmap: Bitmap?,
        demoProfile: DemoTargetProfile,
        assumePresenceWithoutCamera: Boolean,
        isThresholdTriggered: Boolean,
        aiEnabled: Boolean,
        sensitivity: DetectionSensitivity
    ): ChildDetectionResult {
        if (!aiEnabled) {
            return placeholder.analyzeFrame(
                bitmap = bitmap,
                demoProfile = demoProfile,
                assumePresenceWithoutCamera = assumePresenceWithoutCamera,
                isThresholdTriggered = isThresholdTriggered,
                aiEnabled = false,
                sensitivity = sensitivity
            )
        }

        val firebaseReady = isFirebaseConfigured(appContext)
        if (!firebaseReady || bitmap == null) {
            return placeholder.analyzeFrame(
                bitmap = bitmap,
                demoProfile = demoProfile,
                assumePresenceWithoutCamera = assumePresenceWithoutCamera,
                isThresholdTriggered = isThresholdTriggered,
                aiEnabled = true,
                sensitivity = sensitivity
            )
        }

        return try {
            val scaled = scaleForVisionPayload(bitmap)
            val promptText = """
                Bạn là trợ lý bảo vệ thị lực trẻ em. Hãy xem khung hình camera trước này và ước lượng mềm:
                người đang nhìn màn hình có khả năng là TRẺ EM (CHILD), NGƯỜI LỚN (ADULT), hay KHÔNG CÓ KHUÔN MẶT (NONE).
                Độ nhạy yêu cầu: ${sensitivity.label} (ngưỡng tin cậy >= ${sensitivity.confidenceThreshold}%).
                Trả về JSON theo schema, giải thích ngắn gọn bằng tiếng Việt và không lưu ảnh.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                geminiVisionModel.generateContent(
                    content {
                        image(scaled)
                        text(promptText)
                    }
                )
            }
            if (scaled !== bitmap && !scaled.isRecycled) {
                scaled.recycle()
            }

            val rawJson = response.text ?: throw IllegalStateException("Gemini Vision trả về phản hồi trống.")
            val json = JSONObject(rawJson)
            val subjectStr = json.optString("subject", "NONE")
            val confidence = json.optInt("confidencePercent", 80).coerceIn(0, 100)
            val faces = json.optInt("faceCount", if (subjectStr == "NONE") 0 else 1).coerceAtLeast(0)
            val reason = json.optString("reason", "Ước lượng mềm qua Gemini Vision (không lưu ảnh).")

            val subject = when (subjectStr) {
                "CHILD" -> if (confidence >= sensitivity.confidenceThreshold) DetectedSubject.CHILD else DetectedSubject.NONE
                "ADULT" -> DetectedSubject.ADULT
                else -> DetectedSubject.NONE
            }

            ChildDetectionResult(
                subject = subject,
                faceCount = faces,
                confidencePercent = confidence,
                summary = "Gemini Vision: $reason (Ước lượng mềm, không lưu ảnh).",
                sourceModeLabel = "GEMINI VISION",
                isPlaceholderAi = false,
                errorMessage = null,
                timestampMillis = System.currentTimeMillis()
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini Vision request timed out", e)
            placeholder.analyzeFrame(
                bitmap = bitmap,
                demoProfile = demoProfile,
                assumePresenceWithoutCamera = assumePresenceWithoutCamera,
                isThresholdTriggered = isThresholdTriggered,
                aiEnabled = true,
                sensitivity = sensitivity
            ).copy(
                errorMessage = "Gemini Vision hết thời gian chờ (60s) — đã chuyển sang chế độ mô phỏng trên máy."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Vision failed; falling back to local simulation", e)
            placeholder.analyzeFrame(
                bitmap = bitmap,
                demoProfile = demoProfile,
                assumePresenceWithoutCamera = assumePresenceWithoutCamera,
                isThresholdTriggered = isThresholdTriggered,
                aiEnabled = true,
                sensitivity = sensitivity
            ).copy(
                errorMessage = "Gemini Vision tạm gián đoạn (${e.localizedMessage ?: "lỗi kết nối"}) — đang dùng mô phỏng."
            )
        }
    }

    private fun scaleForVisionPayload(source: Bitmap): Bitmap {
        val maxDim = 512f
        val scale = minOf(maxDim / source.width, maxDim / source.height, 1f)
        if (scale >= 0.99f) return source
        val w = (source.width * scale).toInt().coerceAtLeast(1)
        val h = (source.height * scale).toInt().coerceAtLeast(1)
        return source.scale(w, h, true)
    }
}

/**
 * Privacy-first local face-detection + clearly labeled simulation estimator.
 * Runs completely on-device and never saves child photos to disk.
 */
class PlaceholderChildDetectionRepository : ChildDetectionRepository {

    override suspend fun analyzeFrame(
        bitmap: Bitmap?,
        demoProfile: DemoTargetProfile,
        assumePresenceWithoutCamera: Boolean,
        isThresholdTriggered: Boolean,
        aiEnabled: Boolean,
        sensitivity: DetectionSensitivity
    ): ChildDetectionResult = withContext(Dispatchers.Default) {
        if (!aiEnabled) {
            return@withContext ChildDetectionResult(
                subject = when (demoProfile) {
                    DemoTargetProfile.SIMULATE_CHILD -> DetectedSubject.CHILD
                    DemoTargetProfile.SIMULATE_ADULT -> DetectedSubject.ADULT
                },
                faceCount = 0,
                confidencePercent = 100,
                summary = "Phân tích AI đang tắt — áp dụng bộ đếm theo hồ sơ đã chọn.",
                sourceModeLabel = "THỦ CÔNG",
                isPlaceholderAi = true,
                timestampMillis = System.currentTimeMillis()
            )
        }

        val detectedFaces = if (bitmap != null) countFacesOnDevice(bitmap) else 0
        val hasSubject = detectedFaces > 0 || assumePresenceWithoutCamera || (isThresholdTriggered && bitmap != null)

        if (!hasSubject) {
            return@withContext ChildDetectionResult(
                subject = DetectedSubject.NONE,
                faceCount = 0,
                confidencePercent = 0,
                summary = "Chưa phát hiện khuôn mặt trước camera (chế độ mô phỏng).",
                sourceModeLabel = "MÔ PHỎNG",
                isPlaceholderAi = true,
                timestampMillis = System.currentTimeMillis()
            )
        }

        val targetSubject = when (demoProfile) {
            DemoTargetProfile.SIMULATE_CHILD -> DetectedSubject.CHILD
            DemoTargetProfile.SIMULATE_ADULT -> DetectedSubject.ADULT
        }

        val confidence = when {
            detectedFaces > 0 -> (88 + sensitivity.ordinal * 4).coerceAtMost(98)
            else -> 82
        }

        val sourceLabel = when {
            isThresholdTriggered && bitmap != null ->
                "Đã chụp 1 khung hình kiểm tra khi vượt ngưỡng ($detectedFaces khuôn mặt)"
            detectedFaces > 0 ->
                "Phát hiện $detectedFaces khuôn mặt trên máy (Chưa bật Firebase AI)"
            else ->
                "Chế độ mô phỏng (chưa bật Firebase / thiếu camera)"
        }

        val ageNotice = when (targetSubject) {
            DetectedSubject.CHILD -> "Ước lượng mềm: Trẻ em (độ nhạy ${sensitivity.label}, không lưu ảnh)."
            DetectedSubject.ADULT -> "Ước lượng mềm: Người lớn — tạm hoãn nhắc nhở."
            DetectedSubject.NONE -> "Không có khuôn mặt"
        }

        ChildDetectionResult(
            subject = targetSubject,
            faceCount = detectedFaces,
            confidencePercent = confidence,
            summary = "$sourceLabel • $ageNotice",
            sourceModeLabel = "MÔ PHỎNG",
            isPlaceholderAi = true,
            timestampMillis = System.currentTimeMillis()
        )
    }

    private fun countFacesOnDevice(source: Bitmap): Int {
        return try {
            val maxDim = 240
            val scaleFactor = minOf(maxDim.toFloat() / source.width, maxDim.toFloat() / source.height, 1f)
            val rawW = (source.width * scaleFactor).toInt().coerceAtLeast(2)
            val targetW = if (rawW % 2 == 0) rawW else rawW - 1
            val targetH = (source.height * scaleFactor).toInt().coerceAtLeast(2)

            val rgb565 = createBitmap(targetW, targetH, Bitmap.Config.RGB_565)
            val canvas = Canvas(rgb565)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(
                source,
                null,
                android.graphics.Rect(0, 0, targetW, targetH),
                paint
            )

            val faces = arrayOfNulls<FaceDetector.Face>(3)
            val detector = FaceDetector(targetW, targetH, 3)
            val count = detector.findFaces(rgb565, faces)
            rgb565.recycle()
            count
        } catch (_: Throwable) {
            0
        }
    }
}
