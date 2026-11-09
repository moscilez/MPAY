package app.rork.qu1cg8ux8i9pbr72mmsss.scan

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Live camera preview that runs on-device text recognition and reports a parsed card. */
@Composable
fun CameraCardScanner(
    torch: Boolean,
    onResult: (ScannedCard) -> Unit,
    onCameraError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val callback = rememberUpdatedState(onResult)
    val torchState = rememberUpdatedState(torch)
    val previewView = androidx.compose.runtime.remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val cameraRef = androidx.compose.runtime.remember { arrayOfNulls<Camera>(1) }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val busy = AtomicBoolean(false)
        var lastNumber: String? = null
        var hits = 0
        var accumulated = ScannedCard()
        val main = Handler(Looper.getMainLooper())
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null

        providerFuture.addListener({
            try {
                val p = providerFuture.get()
                provider = p
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor, object : ImageAnalysis.Analyzer {
                    @OptIn(ExperimentalGetImage::class)
                    override fun analyze(proxy: androidx.camera.core.ImageProxy) {
                        val media = proxy.image
                        if (media == null || !busy.compareAndSet(false, true)) { proxy.close(); return }
                        val img = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                        recognizer.process(img)
                            .addOnSuccessListener { text ->
                                val lines = text.textBlocks.flatMap { b -> b.lines.map { it.text } }
                                val parsed = CardTextParser.parse(lines)
                                accumulated = accumulated.merge(parsed)
                                if (parsed.number != null) {
                                    if (parsed.number == lastNumber) hits++ else { lastNumber = parsed.number; hits = 1 }
                                    if (hits >= 2) {
                                        val result = accumulated.copy(number = parsed.number)
                                        main.post { callback.value(result) }
                                        hits = 0
                                    }
                                }
                            }
                            .addOnCompleteListener { busy.set(false); proxy.close() }
                    }
                })
                val selector = when {
                    p.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    p.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> { onCameraError(); return@addListener }
                }
                p.unbindAll()
                cameraRef[0] = p.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                if (cameraRef[0]?.cameraInfo?.hasFlashUnit() == true) cameraRef[0]?.cameraControl?.enableTorch(torchState.value)
            } catch (e: Exception) {
                onCameraError()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            provider?.unbindAll()
            cameraRef[0] = null
            recognizer.close()
            executor.shutdown()
        }
    }

    DisposableEffect(torch) {
        val cam = cameraRef[0]
        if (cam?.cameraInfo?.hasFlashUnit() == true) cam.cameraControl.enableTorch(torch)
        onDispose { }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/** Enables NFC reader mode while composed, so a contactless card held to the phone fills the form. */
@Composable
fun NfcCardListener(enabled: Boolean, onResult: (ScannedCard) -> Unit, onError: () -> Unit) {
    val context = LocalContext.current
    val cb = rememberUpdatedState(onResult)
    val err = rememberUpdatedState(onError)
    DisposableEffect(enabled) {
        val activity = context as? Activity ?: run {
            var c = context
            while (c is android.content.ContextWrapper && c !is Activity) c = c.baseContext
            c as? Activity
        }
        val adapter = activity?.let { NfcAdapter.getDefaultAdapter(it) }
        val main = Handler(Looper.getMainLooper())
        if (enabled && activity != null && adapter != null && adapter.isEnabled) {
            adapter.enableReaderMode(
                activity,
                { tag ->
                    val iso = IsoDep.get(tag) ?: return@enableReaderMode
                    val r = runCatching { NfcCardReader.read(iso) }.getOrNull()
                    main.post { if (r?.number != null) cb.value(r) else err.value() }
                },
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                null
            )
        }
        onDispose { if (activity != null && adapter != null) runCatching { adapter.disableReaderMode(activity) } }
    }
}

enum class NfcStatus { Unsupported, Disabled, Ready }

fun nfcStatus(activity: android.content.Context): NfcStatus {
    val a = NfcAdapter.getDefaultAdapter(activity) ?: return NfcStatus.Unsupported
    return if (a.isEnabled) NfcStatus.Ready else NfcStatus.Disabled
}
