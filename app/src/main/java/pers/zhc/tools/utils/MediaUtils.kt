package pers.zhc.tools.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Handler

/**
 * @author bczhc
 */
class MediaUtils {
    companion object {
        fun imageToBitmap(image: Image): Bitmap {
            val width = image.width
            val height = image.height
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width
            val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)
            return bitmap
        }

        fun asyncTakeScreenshot(
            imageReader: ImageReader,
            handler: Handler? = null,
            onAvailable: (image: Image) -> Unit
        ) {
            imageReader.setOnImageAvailableListener({
                val image = imageReader.acquireLatestImage()!!
                onAvailable(image)
                imageReader.setOnImageAvailableListener(null, handler)
            }, handler)
        }

        private fun getImageReader(context: Context): ImageReader {
            val metrics = DisplayUtil.getMetrics(context)
            return ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 1)
        }

        fun asyncTakeScreenshot(
            context: Context,
            mediaProjectionData: Intent,
            handler: Handler? = null,
            callback: (image: Image) -> Unit
        ) {
            val mpm =
                context.applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mp = mpm.getMediaProjection(Activity.RESULT_OK, mediaProjectionData)
            val ir = getImageReader(context)
            val vd = mp.createVirtualDisplay(
                "VirtualDisplay",
                ir.width,
                ir.height,
                DisplayUtil.getMetrics(context).densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                ir.surface,
                null,
                handler
            )
            asyncTakeScreenshot(ir, handler) { image ->
                callback(image)
                vd.release()
                ir.close()
                mp.stop()
            }
        }
    }
}