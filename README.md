# Ver-ID serialization

This library introduces extensions that serialize Ver-ID's [IImage](https://github.com/AppliedRecognition/Ver-ID-Common-Types-Android/blob/main/lib/src/main/java/com/appliedrec/verid3/common/IImage.kt) and [Face](https://github.com/AppliedRecognition/Ver-ID-Common-Types-Android/blob/main/lib/src/main/java/com/appliedrec/verid3/common/Face.kt) types using protocol buffers.

## Usage

```kotlin
import com.appliedrec.verid3.common.IImage
import com.appliedrec.verid3.common.Image
import com.appliedrec.verid3.common.Face
import com.appliedrec.verid3.common.serialization.serialized
import android.graphics.Bitmap

// Image

val image: IImage // Image obtained from capture library

// Serialize image to protocol buffer
val imageBytes: ByteArray = image.serialized()

// Convert image to bitmap
val bitmap: Bitmap = image.toBitmap()

// Create image from bitmap
val imageCopy: Image = Image.fromBitmap(bitmap)


// Face

val face: Face // Face obtained from capture library

// Serialize face to protocol buffer
val faceBytes: ByteArray = face.serialized()
```