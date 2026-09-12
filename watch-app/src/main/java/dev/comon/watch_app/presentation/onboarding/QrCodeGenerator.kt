package dev.comon.watch_app.presentation.onboarding

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import javax.inject.Inject
import dev.comon.watch_app.diagnostics.StartupTiming

class QrCodeGenerator @Inject constructor() {

    fun generate(content: String, sizePx: Int): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 0,
        )
        val bitMatrix = StartupTiming.measure("qr.encode") {
            QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        }
        val pixels = StartupTiming.measure("qr.pixels") {
            IntArray(sizePx * sizePx) { index ->
                if (bitMatrix[index % sizePx, index / sizePx]) Color.BLACK else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        StartupTiming.measure("qr.bitmap") {
            bitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        }
        return bitmap
    }
}
