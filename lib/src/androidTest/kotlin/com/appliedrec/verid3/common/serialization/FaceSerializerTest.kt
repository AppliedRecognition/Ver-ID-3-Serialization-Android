package com.appliedrec.verid3.common.serialization

import android.graphics.PointF
import android.graphics.RectF
import com.appliedrec.verid3.common.EulerAngle
import com.appliedrec.verid3.common.Face
import com.google.protobuf.InvalidProtocolBufferException
import org.junit.Assert
import org.junit.Test

class FaceSerializerTest {

    @Test
    fun testSerializeFace() {
        face.serialize()
    }

    @Test
    fun testDeserializeFace() {
        val serialized = face.serialize()
        val deserialized = Face.deserialize(serialized)
        assert(deserialized == face)
    }

    @Test
    fun testFailToDeserializeInvalidFace() {
        Assert.assertThrows(InvalidProtocolBufferException::class.java, {
            Face.deserialize(
                ByteArray(32)
            )
        })
    }

    private val face: Face by lazy {
        Face(
            RectF(25f, 20f, 345f, 431f),
            EulerAngle(1.3f, 0.5f, 0.1f),
            9.9f,
            arrayOf(PointF(10f, 12f)),
            PointF(51f, 72f),
            PointF(89f, 73f),
            PointF(65f,92f),
            PointF(66f, 123f)
        )
    }
}