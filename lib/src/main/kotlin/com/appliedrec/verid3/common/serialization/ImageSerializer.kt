package com.appliedrec.verid3.common.serialization

import android.graphics.Bitmap
import com.appliedrec.verid3.common.IImage
import com.appliedrec.verid3.common.Image
import com.appliedrec.verid3.common.Image3D as CommonImage3D
import com.appliedrec.verid3.common.ImageFormat
import com.appliedrec.verid3.serialization.capture3d.DepthMapOuterClass
import com.appliedrec.verid3.serialization.capture3d.Image3DOuterClass
import com.appliedrec.verid3.serialization.capture3d.Image3DOuterClass.Image3D
import com.appliedrec.verid3.serialization.common.Pointf
import com.awxkee.jxlcoder.JxlChannelsConfiguration
import com.awxkee.jxlcoder.JxlCoder
import com.awxkee.jxlcoder.JxlCompressionOption
import com.awxkee.jxlcoder.PreferredColorConfig
import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException

internal object ImageSerializer {

    init {
        System.loadLibrary("ImageUtil")
    }

    @JvmStatic
    @Throws(InvalidProtocolBufferException::class)
    fun deserialize(bytes: ByteArray): Image {
        val bitmap = JxlCoder.decode(bytes, PreferredColorConfig.RGBA_8888)
        return Image.fromBitmap(bitmap)
    }
    @JvmStatic
    fun serialize(image: IImage): ByteArray {
        val bitmap = convertToBitmap(image.data, image.width, image.height, image.bytesPerRow, image.format)
        val data = JxlCoder.encode(bitmap, JxlChannelsConfiguration.RGB, JxlCompressionOption.LOSSLESS)
        if (image is CommonImage3D) {
            val depth = image.depthMap?.let { depthMap ->
                DepthMapOuterClass.DepthMap.newBuilder()
                    .setData(ByteString.copyFrom(depthMap.data))
                    .setWidth(depthMap.width)
                    .setHeight(depthMap.height)
                    .setBytesPerRow(depthMap.bytesPerRow)
                    .setBitsPerElement(depthMap.bitsPerPixel)
                    .setFocalLength(
                        Pointf.PointF.newBuilder()
                        .setX(depthMap.focalLength.x)
                        .setY(depthMap.focalLength.y))
                    .setPrincipalPoint(
                        Pointf.PointF.newBuilder()
                        .setX(depthMap.principalPoint.x)
                        .setY(depthMap.principalPoint.y))
                    .setLensDistortionCenter(
                        Pointf.PointF.newBuilder()
                        .setX(depthMap.lensDistortionCenter.x)
                        .setY(depthMap.lensDistortionCenter.y))
                    .addAllLensDistortionLookupTable(depthMap.lensDistortionLookupTable.asIterable())
                    .build()
            }
            val builder = Image3D
                .newBuilder()
                .setJxl(ByteString.copyFrom(data))
            if (depth != null) {
                builder.setDepthMap(depth)
            }
            return builder.build().toByteArray()
        }
        return data
    }

    private external fun swapChannels(image: Bitmap, matrix: IntArray): ByteArray
    private external fun convertToGrayscale(image: Bitmap, weights: FloatArray = floatArrayOf(0.2989f, 0.587f, 0.114f)): ByteArray
    external fun convertToBitmap(data: ByteArray, width: Int, height: Int, bytesPerRow: Int, format: ImageFormat): Bitmap
}