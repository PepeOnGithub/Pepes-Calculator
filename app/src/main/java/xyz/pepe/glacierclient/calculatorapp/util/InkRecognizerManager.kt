package xyz.pepe.glacierclient.calculatorapp.util

import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import kotlinx.coroutines.tasks.await

/**
 * Wraps Google ML Kit's on-device Digital Ink Recognition — this is what actually turns Math
 * Notes' handwritten strokes into text; there is no simulation here. The model ("en-US", which
 * recognizes Latin letters and digits well) downloads once over the network the first time it's
 * used and then runs fully on-device. It doesn't understand 2D math layout the way MyScript's
 * commercial math engine does — it recognizes the strokes as handwritten text/digits, which is
 * then read as a normal left-to-right expression, same as if it had been typed.
 */
class InkRecognizerManager {

    private val modelIdentifier = DigitalInkRecognitionModelIdentifier.EN_US
    private val model: DigitalInkRecognitionModel = DigitalInkRecognitionModel.builder(modelIdentifier).build()
    private val remoteModelManager = RemoteModelManager.getInstance()
    private val recognizer: DigitalInkRecognizer =
        DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())

    private var modelReady = false

    suspend fun ensureModelDownloaded(): Boolean {
        if (modelReady) return true
        return try {
            if (!remoteModelManager.isModelDownloaded(model).await()) {
                remoteModelManager.download(model, DownloadConditions.Builder().build()).await()
            }
            modelReady = true
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Recognizes [strokes] (each a list of (x, y) points from one continuous touch drag) and
     *  returns the single best-guess text, or null if recognition failed or isn't ready yet. */
    suspend fun recognize(strokes: List<List<Pair<Float, Float>>>): String? {
        if (strokes.isEmpty()) return null
        if (!ensureModelDownloaded()) return null

        val inkBuilder = Ink.builder()
        val baseTime = System.currentTimeMillis()
        strokes.forEach { points ->
            val strokeBuilder = Ink.Stroke.builder()
            points.forEachIndexed { index, (x, y) ->
                strokeBuilder.addPoint(Ink.Point.create(x, y, (baseTime + index).toLong()))
            }
            inkBuilder.addStroke(strokeBuilder.build())
        }

        return try {
            val result = recognizer.recognize(inkBuilder.build()).await()
            result.candidates.firstOrNull()?.text
        } catch (e: Exception) {
            null
        }
    }
}
