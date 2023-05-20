package pers.zhc.tools.utils

import android.content.Context
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection

class ProjectionScreenshotReader(context: Context, mediaProjection: MediaProjection) {
    private var imageReader: ImageReader
    private var virtualDisplay: VirtualDisplay?

    init {
        imageReader = MediaUtils.newImageReader(context)
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "VirtualDisplay",
            imageReader.width,
            imageReader.height,
            DisplayUtil.getDensityDpi(context),
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            null
        )
    }

    private var running = true

    fun requestScreenshot(callback: (bitmap: Bitmap) -> Unit) {
        // empty the image buffer, then the new coming one won't capture images when
        // screenColorPickerView has not become transparent
        imageReader.acquireLatestImage()?.close()

        // spinning wait until the image is available
        var image: Image?
        while (true) {
            image = imageReader.acquireLatestImage()
            if (image != null) break
            // if something's wrong (and also the case is not expected) and no image can reach,
            // the loop will be endless
            // we need to set a flag to indicate its working state, and
            // if the user stop the color picker, the loop (if still spinning) should be interrupted
            if (!running) return
        }
        val bitmap = MediaUtils.imageToBitmap(image!!)
        image.close()
        callback(bitmap)
    }

    fun close() {
        imageReader.close()
        virtualDisplay!!.release()
        running = false
    }
}
