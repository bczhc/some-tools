package pers.zhc.tools.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper

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

        private fun newImageReader(context: Context): ImageReader {
            val screenSize = DisplayUtil.getScreenSize(context)
            return ImageReader.newInstance(screenSize.x, screenSize.y, PixelFormat.RGBA_8888, 1)
        }

        fun asyncTakeScreenshot(
            context: Context,
            mediaProjectionData: Intent,
            callback: (bitmap: Bitmap) -> Unit,
        ) {
            val mpm =
                context.applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mp = mpm.getMediaProjection(Activity.RESULT_OK, mediaProjectionData)
            var vd: VirtualDisplay? = null

            val ir = newImageReader(context)
            Thread {
                var image: Image? = null
                while (image == null) image = ir.acquireLatestImage()
                val bitmap = imageToBitmap(image)
                vd!!.release()
                ir.close()
                mp.stop()
                image.close()
                callback(bitmap)
            }.start()

            vd = mp.createVirtualDisplay(
                "VirtualDisplay",
                ir.width,
                ir.height,
                DisplayUtil.getMetrics(context).densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                ir.surface,
                null,
                null
            )
        }
    }
}