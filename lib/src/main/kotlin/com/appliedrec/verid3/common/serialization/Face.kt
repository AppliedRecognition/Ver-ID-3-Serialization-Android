package com.appliedrec.verid3.common.serialization

import android.graphics.PointF
import android.graphics.RectF
import com.appliedrec.verid3.common.EulerAngle
import com.appliedrec.verid3.common.Face
import com.appliedrec.verid3.serialization.common.FaceOuterClass
import com.appliedrec.verid3.serialization.common.Pointf

fun Face.serialize(): ByteArray {
    val builder = FaceOuterClass.Face.newBuilder()
        .setX(this.bounds.left)
        .setY(this.bounds.top)
        .setWidth(this.bounds.width())
        .setHeight(this.bounds.height())
        .setYaw(this.angle.yaw)
        .setPitch(this.angle.pitch)
        .setRoll(this.angle.roll)
        .setQuality(this.quality)
        .addAllLandmarks(this.landmarks.map { Pointf.PointF.newBuilder().setX(it.x).setY(it.y).build() })
        .setLeftEye(Pointf.PointF.newBuilder().setX(this.leftEye.x).setY(this.leftEye.y))
        .setRightEye(Pointf.PointF.newBuilder().setX(this.rightEye.x).setY(this.rightEye.y))
    this.noseTip?.let {
        builder.setNoseTip(Pointf.PointF.newBuilder().setX(it.x).setY(it.y))
    }
    this.mouthCentre?.let {
        builder.setMouthCentre(Pointf.PointF.newBuilder().setX(it.x).setY(it.y))
    }
    this.mouthLeftCorner?.let {
        builder.setMouthLeftCorner(Pointf.PointF.newBuilder().setX(it.x).setY(it.y))
    }
    this.mouthRightCorner?.let {
        builder.setMouthRightCorner(Pointf.PointF.newBuilder().setX(it.x).setY(it.y))
    }
    return builder.build().toByteArray()
}

fun Face.Companion.deserialize(bytes: ByteArray): Face {
    val face = FaceOuterClass.Face.parseFrom(bytes)
    return Face(
        RectF(face.x, face.y, face.x + face.width, face.y + face.height),
        EulerAngle(face.yaw, face.pitch, face.roll),
        face.quality,
        face.landmarksList.map { PointF(it.x, it.y) }.toTypedArray(),
        PointF(face.leftEye.x, face.leftEye.y),
        PointF(face.rightEye.x, face.rightEye.y),
        if (face.hasNoseTip()) PointF(face.noseTip.x, face.noseTip.y) else null,
        if (face.hasMouthCentre()) PointF(face.mouthCentre.x, face.mouthCentre.y) else null,
        if (face.hasMouthLeftCorner()) PointF(face.mouthLeftCorner.x, face.mouthLeftCorner.y) else null,
        if (face.hasMouthRightCorner()) PointF(face.mouthRightCorner.x, face.mouthRightCorner.y) else null
    )
}