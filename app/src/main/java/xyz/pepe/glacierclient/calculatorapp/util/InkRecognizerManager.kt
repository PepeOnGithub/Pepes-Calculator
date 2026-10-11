package xyz.pepe.glacierclient.calculatorapp.util

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import kotlinx.coroutines.tasks.await
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.InkPoint

/**
 * Google ML Kit's on-device Digital Ink Recognition — what turns Math Notes' handwriting into
 * text. The "en-US" model downloads once (needs a connection) and then runs fully offline. It
 * reads strokes left to right as text, so "3x+2=" works, but it doesn't lay out 2D fractions.
 */
class InkRecognizerManager {

  private val model: DigitalInkRecognitionModel =
    DigitalInkRecognitionModel.builder(DigitalInkRecognitionModelIdentifier.EN_US).build()
  private val remoteModelManager = RemoteModelManager.getInstance()
  private val recognizer: DigitalInkRecognizer =
    DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())

  @Volatile private var modelReady = false

  suspend fun ensureModelDownloaded(): Boolean {
    if (modelReady) return true
    return try {
      if (!remoteModelManager.isModelDownloaded(model).await()) {
        remoteModelManager.download(model, DownloadConditions.Builder().build()).await()
      }
      modelReady = true
      true
    } catch (_: Exception) {
      false
    }
  }

  /** Best candidate for [strokes], cleaned up for math, or null if recognition failed. */
  suspend fun recognize(strokes: List<List<InkPoint>>): String? {
    if (strokes.isEmpty() || !ensureModelDownloaded()) return null
    val ink = Ink.builder()
    strokes.forEach { points ->
      val stroke = Ink.Stroke.builder()
      points.forEach { stroke.addPoint(Ink.Point.create(it.x, it.y, it.t)) }
      ink.addStroke(stroke.build())
    }
    return try {
      val candidates = recognizer.recognize(ink.build()).await().candidates.map { it.text }
      // Prefer a candidate that reads as math over one that reads as a word ("2+3" over "Zt3").
      (candidates.firstOrNull { looksLikeMath(it) } ?: candidates.firstOrNull())?.let(::cleanUp)
    } catch (_: Exception) {
      null
    }
  }

  private fun looksLikeMath(s: String) = s.count { it.isDigit() || it in "+-×x*/÷=()^.%" } >= s.length / 2

  /** Handwriting look-alikes → calculator symbols. */
  private fun cleanUp(s: String): String = s
    .replace('÷', '/').replace('—', '-').replace('–', '-').replace('•', '×')
    .replace(Regex("""(?<=\d)\s*[xX]\s*(?=\d)"""), "×")
    .replace(Regex("""(?<=\d)[oO](?=\d)|(?<=\d)[oO]\b"""), "0")
    .replace(Regex("""(?<=\d)[lI|](?=\d)"""), "1")
    .trim()
}
