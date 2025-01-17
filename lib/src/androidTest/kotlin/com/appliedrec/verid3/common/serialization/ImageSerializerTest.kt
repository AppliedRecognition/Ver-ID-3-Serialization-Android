package com.appliedrec.verid3.common.serialization

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.appliedrec.verid3.common.Image
import com.appliedrec.verid3.common.ImageFormat
import com.awxkee.jxlcoder.InvalidJXLException
import com.awxkee.jxlcoder.JxlChannelsConfiguration
import com.awxkee.jxlcoder.JxlCoder
import com.awxkee.jxlcoder.JxlCompressionOption
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class ImageSerializerTest {

    @Test
    fun testSerializeImage() {
        val bitmap = createTestBitmap()
        val image = Image.fromBitmap(bitmap)
        val bytes = image.serialized()
        Assert.assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun testDeserializeImage() {
        val imageSize = 100
        val bytesPerRow = imageSize * 4
        val bitmap = createTestBitmap(imageSize, imageSize)
        val bytes = serializeImage(bitmap)
        val image = Image.deserialize(bytes)
        Assert.assertEquals(imageSize, image.width)
        Assert.assertEquals(imageSize, image.height)
        Assert.assertEquals(bytesPerRow, image.bytesPerRow)
        Assert.assertEquals(ImageFormat.RGBA, image.format)
        // Red
        Assert.assertEquals((-1).toByte(), image.data[0])
        Assert.assertEquals(0.toByte(), image.data[1])
        Assert.assertEquals(0.toByte(), image.data[2])
        Assert.assertEquals((-1).toByte(), image.data[3])
        // Green
        Assert.assertEquals(0.toByte(), image.data[bytesPerRow / 2])
        Assert.assertEquals((-1).toByte(), image.data[bytesPerRow / 2 + 1])
        Assert.assertEquals(0.toByte(), image.data[bytesPerRow / 2 + 2])
        Assert.assertEquals((-1).toByte(), image.data[bytesPerRow / 2 + 3])
        // Blue
        Assert.assertEquals(0.toByte(), image.data[bytesPerRow * imageSize / 2])
        Assert.assertEquals(0.toByte(), image.data[bytesPerRow * imageSize / 2 + 1])
        Assert.assertEquals((-1).toByte(), image.data[bytesPerRow * imageSize / 2 + 2])
        Assert.assertEquals((-1).toByte(), image.data[bytesPerRow * imageSize / 2 + 3])
        // White
        Assert.assertEquals((-1).toByte(), image.data[bytesPerRow * imageSize / 2 + bytesPerRow / 2])
        Assert.assertEquals((-1).toByte(), image.data[bytesPerRow * imageSize / 2 + bytesPerRow / 2 + 1])
        Assert.assertEquals((-1).toByte(), image.data[bytesPerRow * imageSize / 2 + bytesPerRow / 2 + 2])
        Assert.assertEquals((-1).toByte(), image.data[bytesPerRow * imageSize / 2 + bytesPerRow / 2 + 3])
    }

    @Test
    fun testFailToDeserializeInvalidImage() {
        Assert.assertThrows(InvalidJXLException::class.java, {
            ImageSerializer.deserialize(
                ByteArray(100 * 100 * 4)
            )
        })
    }

    @Test
    @Ignore("Outputs a file in downloads folder")
    fun testSerializeJxl() {
        val bitmap = createTestBitmap()
        val bytes = JxlCoder.encode(bitmap, JxlChannelsConfiguration.RGB, JxlCompressionOption.LOSSLESS)
        Assert.assertTrue(bytes.isNotEmpty())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "test.jxl")
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jxl")
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues) ?: throw Exception("Failed to create image file URI")
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(bytes)
        }
        Log.d("Ver-ID", "Wrote ${bytes.size} bytes to ${uri}")
    }

    private fun createTestBitmap(width: Int = 100, height: Int = 100): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            drawRect(0f, 0f, width.toFloat() / 2f, height.toFloat() / 2f, Paint().apply { color = Color.RED })
            drawRect(width.toFloat() / 2f, 0f, width.toFloat(), height.toFloat() / 2f, Paint().apply { color = Color.GREEN })
            drawRect(0f, height.toFloat() / 2f, width.toFloat() / 2f, height.toFloat(), Paint().apply { color = Color.BLUE })
        }
        return bitmap
    }

    private fun serializeImage(bitmap: Bitmap): ByteArray {
        val image = Image.fromBitmap(bitmap)
        return ImageSerializer.serialize(image)
    }
}