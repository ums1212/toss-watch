package dev.comon.toss_watch.feature.setting.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.model.watch.WatchAlarmImageSlot
import dev.comon.toss_watch.core.model.watch.WatchAlarmImageSync
import dev.comon.toss_watch.feature.setting.domain.model.AlarmImage
import dev.comon.toss_watch.feature.setting.domain.model.AlarmImageCrop
import dev.comon.toss_watch.feature.setting.domain.repository.AlarmImageRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * 슬롯별 이미지를 앱 내부 저장소(`filesDir/alarm_images/{up|flat|down}.jpg`)에 둔다.
 * 파일이 없으면 그 슬롯은 기본 이미지다.
 */
@Singleton
internal class AlarmImageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) : AlarmImageRepository {

    private val directory = File(context.filesDir, DIRECTORY)

    // null = 아직 디스크에서 읽지 않음.
    private val images = MutableStateFlow<List<AlarmImage>?>(null)

    override fun observe(): Flow<List<AlarmImage>> = flow {
        images.compareAndSet(null, read())
        emitAll(images.filterNotNull())
    }.flowOn(dispatcherProvider.io)

    override suspend fun decode(sourceUri: String): Bitmap? = withContext(dispatcherProvider.io) {
        try {
            decodeSampled(Uri.parse(sourceUri))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    override suspend fun set(
        slot: WatchAlarmImageSlot,
        source: Bitmap,
        crop: AlarmImageCrop,
    ): Boolean = withContext(dispatcherProvider.io) {
        try {
            val side = min(
                (crop.right - crop.left) * source.width,
                (crop.bottom - crop.top) * source.height,
            ).roundToInt().coerceIn(1, min(source.width, source.height))
            val left = (crop.left * source.width).roundToInt().coerceIn(0, source.width - side)
            val top = (crop.top * source.height).roundToInt().coerceIn(0, source.height - side)
            val size = WatchAlarmImageSync.IMAGE_SIZE_PX
            val output = Bitmap.createScaledBitmap(
                Bitmap.createBitmap(source, left, top, side, side), size, size, true,
            )

            directory.mkdirs()
            // 워치 전송(PhoneAlarmImageSync)이 반쯤 쓰인 파일을 읽지 않도록 임시 파일에 쓴 뒤 교체한다.
            val temp = File(directory, "${slot.key}.tmp")
            val written = temp.outputStream().use { output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            val saved = written && temp.renameTo(file(slot))
            if (saved) images.value = read() else temp.delete()
            saved
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        } catch (_: OutOfMemoryError) {
            false
        }
    }

    override suspend fun reset(slot: WatchAlarmImageSlot) = withContext(dispatcherProvider.io) {
        file(slot).delete()
        images.value = read()
    }

    private fun file(slot: WatchAlarmImageSlot) = File(directory, "${slot.key}.jpg")

    private fun read(): List<AlarmImage> = WatchAlarmImageSlot.entries.map { slot ->
        val file = file(slot)
        if (file.exists()) AlarmImage(slot, file.absolutePath, file.lastModified()) else AlarmImage(slot)
    }

    /** 큰 원본도 메모리에 무리 없이 편집할 수 있도록 긴 변을 [MAX_SOURCE_PX] 이하로 줄여 읽는다. */
    private fun decodeSampled(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_SOURCE_PX) sampleSize *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null

        // 카메라 사진은 픽셀이 눕혀진 채 EXIF 회전값만 기록돼 있는 경우가 많다.
        val degrees = runCatching {
            resolver.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }
        }.getOrNull() ?: 0f
        if (degrees == 0f) return bitmap
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true,
        )
    }

    private companion object {
        const val DIRECTORY = "alarm_images"
        const val MAX_SOURCE_PX = 2048
        const val JPEG_QUALITY = 90
    }
}
