package com.appliedrec.verid3.common.serialization

import android.graphics.Bitmap
import com.appliedrec.verid3.common.IImage
import com.appliedrec.verid3.common.Image
import com.appliedrec.verid3.common.ImageFormat
import com.appliedrec.verid3.serialization.capture3d.Image3DOuterClass.Image3D
import java.nio.ByteBuffer

fun Image.Companion.deserialize(bytes: ByteArray): Image {
    return ImageSerializer.deserialize(bytes)
}

fun IImage.serialized(): ByteArray {
    return ImageSerializer.serialize(this)
}

fun Image.Companion.fromBitmap(bitmap: Bitmap): Image {
    if (bitmap.config == Bitmap.Config.ARGB_8888) {
        val data = ByteArray(bitmap.byteCount)
        val buffer: ByteBuffer = ByteBuffer.wrap(data)
        bitmap.copyPixelsToBuffer(buffer)
        return Image(data, bitmap.width, bitmap.height, bitmap.rowBytes, ImageFormat.RGBA)
    } else if (bitmap.config == Bitmap.Config.ALPHA_8) {
        val data = ByteArray(bitmap.byteCount)
        val buffer: ByteBuffer = ByteBuffer.wrap(data)
        bitmap.copyPixelsToBuffer(buffer)
        return Image(data, bitmap.width, bitmap.height, bitmap.rowBytes, ImageFormat.GRAYSCALE)
    } else {
        throw IllegalArgumentException("Only ARGB_8888 or ALPHA_8 bitmaps are supported")
    }
}

fun IImage.toBitmap(): Bitmap {
    return ImageSerializer.convertToBitmap(this.data, this.width, this.height, this.bytesPerRow, this.format)
}