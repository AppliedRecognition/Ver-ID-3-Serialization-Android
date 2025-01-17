//
// Created by Jakub Dolejs on 17/12/2024.
//

#include <jni.h>
#include <android/bitmap.h>
#include <cstdint>
#include <cstring>
#include <algorithm>
#include <string>
#include <memory>

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_appliedrec_verid3_common_serialization_ImageSerializer_swapChannels(
        JNIEnv *env, jobject thiz, jobject bitmap, jintArray layout) {

    AndroidBitmapInfo info;
    void *pixels;

    // Retrieve the channel layout array
    jint *channelLayout = env->GetIntArrayElements(layout, nullptr);
    jsize layoutLength = env->GetArrayLength(layout);

    // Validate input layout
    if (layoutLength < 3 || layoutLength > 4) {
        env->ReleaseIntArrayElements(layout, channelLayout, JNI_ABORT);
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),
                      "Channel layout must have 3 or 4 elements");
        return nullptr;
    }

    // Validate and lock the bitmap
    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) {
        env->ReleaseIntArrayElements(layout, channelLayout, JNI_ABORT);
        env->ThrowNew(env->FindClass("java/lang/Exception"), "Failed to get bitmap info");
        return nullptr;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        env->ReleaseIntArrayElements(layout, channelLayout, JNI_ABORT);
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),
                      "Bitmap format must be RGBA_8888");
        return nullptr;
    }

    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        env->ReleaseIntArrayElements(layout, channelLayout, JNI_ABORT);
        env->ThrowNew(env->FindClass("java/lang/Exception"), "Failed to lock bitmap");
        return nullptr;
    }

    // Calculate output size
    uint32_t pixelCount = info.width * info.height;
    size_t outputSize = pixelCount * layoutLength;
    jbyteArray output = env->NewByteArray(outputSize);

    // Create a local buffer to store swapped channels
    jbyte *outputBuffer = new jbyte[outputSize];
    uint8_t *src = static_cast<uint8_t *>(pixels);

    // Process each pixel
    for (uint32_t i = 0, k = 0; i < pixelCount; i++) {
        uint8_t rgba[4] = {src[4 * i + 0], src[4 * i + 1], src[4 * i + 2], src[4 * i + 3]};
        for (jsize j = 0; j < layoutLength; j++) {
            outputBuffer[k++] = rgba[channelLayout[j]];
        }
    }

    // Copy the local buffer to the output jbyteArray
    env->SetByteArrayRegion(output, 0, outputSize, outputBuffer);

    // Cleanup
    delete[] outputBuffer;
    AndroidBitmap_unlockPixels(env, bitmap);
    env->ReleaseIntArrayElements(layout, channelLayout, JNI_ABORT);

    return output;
}
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_appliedrec_verid3_common_serialization_ImageSerializer_convertToGrayscale(
        JNIEnv *env,
        jobject thiz,
        jobject bitmap,
        jfloatArray weights
) {
    AndroidBitmapInfo info;
    void *pixels;

    // Validate and lock the bitmap
    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) {
        env->ThrowNew(env->FindClass("java/lang/Exception"), "Failed to get bitmap info");
        return nullptr;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),
                      "Bitmap format must be RGBA_8888");
        return nullptr;
    }

    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        env->ThrowNew(env->FindClass("java/lang/Exception"), "Failed to lock bitmap");
        return nullptr;
    }
    jsize weightsSize = env->GetArrayLength(weights);
    if (weightsSize != 3) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Weigths array must have 3 elements representing the weights for the individual RGB channels");
        return nullptr;
    }
    float *rgbWeights = env->GetFloatArrayElements(weights, nullptr);
    if (rgbWeights == nullptr) {
        AndroidBitmap_unlockPixels(env, bitmap);
        env->ThrowNew(env->FindClass("java/lang/Exception"), "Failed to get weights array");
        return nullptr;
    }
    uint32_t pixelCount = info.width * info.height;
    auto *src = static_cast<uint8_t *>(pixels);
    auto *outputBuffer = new jbyte[pixelCount];
    for (uint32_t i = 0, k = 0; i < pixelCount; i++) {
        uint8_t r = src[4 * i];       // Red
        uint8_t g = src[4 * i + 1];   // Green
        uint8_t b = src[4 * i + 2];   // Blue
        float gray = r * rgbWeights[0] + g * rgbWeights[1] + b * rgbWeights[2];
        outputBuffer[i] = static_cast<jbyte>(std::clamp(static_cast<int>(gray), 0, 255));
    }
    env->ReleaseFloatArrayElements(weights, rgbWeights, JNI_ABORT);
    AndroidBitmap_unlockPixels(env, bitmap);
    jbyteArray output = env->NewByteArray(pixelCount);
    env->SetByteArrayRegion(output, 0, pixelCount, outputBuffer);
    delete[] outputBuffer;
    return output;
}

static jobject createBitmap(JNIEnv *env, jint width, jint height) {
    // Step 1: Get the Bitmap class
    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    if (bitmapClass == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/Exception"), "Failed to find Bitmap class");
        return nullptr;
    }

    // Step 2: Get Bitmap.Config class and ARGB_8888 constant
    jclass bitmapConfigClass = env->FindClass("android/graphics/Bitmap$Config");
    if (bitmapConfigClass == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/Exception"), "Failed to find Bitmap$Config class");
        return nullptr;
    }
    jfieldID argb8888FieldID = env->GetStaticFieldID(bitmapConfigClass, "ARGB_8888", "Landroid/graphics/Bitmap$Config;");
    jobject argb8888 = env->GetStaticObjectField(bitmapConfigClass, argb8888FieldID);

    // Step 3: Get the createBitmap method ID
    jmethodID createBitmapMethodID = env->GetStaticMethodID(
            bitmapClass,
            "createBitmap",
            "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    if (createBitmapMethodID == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/Exception"), "Failed to find createBitmap method");
        return nullptr;
    }

    // Step 4: Call createBitmap(width, height, Bitmap.Config.ARGB_8888)
    jobject bitmap = env->CallStaticObjectMethod(bitmapClass, createBitmapMethodID, width, height, argb8888);
    if (bitmap == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/Exception"), "Failed to create Bitmap");
    }
    return bitmap;
}

static std::string getImageFormatAsString(JNIEnv *env, jobject enumObject) {
    // Get the class of the enum object
    jclass enumClass = env->GetObjectClass(enumObject);

    // Get the method ID for the name() method
    jmethodID nameMethod = env->GetMethodID(enumClass, "name", "()Ljava/lang/String;");

    // Call name() and get the jstring result
    jstring enumName = (jstring)env->CallObjectMethod(enumObject, nameMethod);

    // Convert jstring to std::string
    const char *nameChars = env->GetStringUTFChars(enumName, nullptr);
    std::string enumNameStr(nameChars);
    env->ReleaseStringUTFChars(enumName, nameChars);

    return enumNameStr;
}

static std::unique_ptr<uint8_t[]> convertToRGBA(
        JNIEnv *env,
        jbyteArray imageData,
        jint width,
        jint height,
        jint bytesPerRow,
        const std::string &imageFormat
) {
    if (imageData == nullptr || width <= 0 || height <= 0 || bytesPerRow <= 0) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Invalid input: image data must not be null and width, height and bytes per row must be grater than 0");
        return nullptr;
    }
    jsize length = env->GetArrayLength(imageData);
    if (length < bytesPerRow * height) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Image data length must be equal to height * bytesPerRow");
        return nullptr; // Data size mismatch
    }

    jbyte *inputData = env->GetByteArrayElements(imageData, nullptr);
    if (!inputData) {
        env->ThrowNew(env->FindClass("java/lang/Exception"), "Failed to read image input");
        return nullptr;
    }

    // Allocate output buffer as unique_ptr
    size_t outputSize = width * height * 4; // 4 bytes per pixel for RGBA
    std::unique_ptr<uint8_t[]> outputBuffer = std::make_unique<uint8_t[]>(outputSize);
    uint8_t *src = reinterpret_cast<uint8_t *>(inputData);

    if (imageFormat == "RGB") {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int inputIndex = y * bytesPerRow + x * 3;
                int outputIndex = (y * width + x) * 4;

                outputBuffer[outputIndex + 0] = src[inputIndex + 0]; // R
                outputBuffer[outputIndex + 1] = src[inputIndex + 1]; // G
                outputBuffer[outputIndex + 2] = src[inputIndex + 2]; // B
                outputBuffer[outputIndex + 3] = 255;                 // A
            }
        }
    } else if (imageFormat == "BGR") {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int inputIndex = y * bytesPerRow + x * 3;
                int outputIndex = (y * width + x) * 4;

                outputBuffer[outputIndex + 0] = src[inputIndex + 2]; // R
                outputBuffer[outputIndex + 1] = src[inputIndex + 1]; // G
                outputBuffer[outputIndex + 2] = src[inputIndex + 0]; // B
                outputBuffer[outputIndex + 3] = 255;                 // A
            }
        }
    } else if (imageFormat == "ARGB") {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int inputIndex = y * bytesPerRow + x * 4;
                int outputIndex = (y * width + x) * 4;

                outputBuffer[outputIndex + 0] = src[inputIndex + 1]; // R
                outputBuffer[outputIndex + 1] = src[inputIndex + 2]; // G
                outputBuffer[outputIndex + 2] = src[inputIndex + 3]; // B
                outputBuffer[outputIndex + 3] = src[inputIndex + 0]; // A
            }
        }
    } else if (imageFormat == "ABGR") {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int inputIndex = y * bytesPerRow + x * 4;
                int outputIndex = (y * width + x) * 4;

                outputBuffer[outputIndex + 0] = src[inputIndex + 3]; // R
                outputBuffer[outputIndex + 1] = src[inputIndex + 2]; // G
                outputBuffer[outputIndex + 2] = src[inputIndex + 1]; // B
                outputBuffer[outputIndex + 3] = src[inputIndex + 0]; // A
            }
        }
    } else if (imageFormat == "BGRA") {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int inputIndex = y * bytesPerRow + x * 4;
                int outputIndex = (y * width + x) * 4;

                outputBuffer[outputIndex + 0] = src[inputIndex + 2]; // R
                outputBuffer[outputIndex + 1] = src[inputIndex + 1]; // G
                outputBuffer[outputIndex + 2] = src[inputIndex + 0]; // B
                outputBuffer[outputIndex + 3] = src[inputIndex + 3]; // A
            }
        }
    } else if (imageFormat == "RGBA") {
        for (int y = 0; y < height; y++) {
            memcpy(outputBuffer.get() + y * width * 4, src + y * bytesPerRow, width * 4);
        }
    } else if (imageFormat == "GRAYSCALE") {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int inputIndex = y * bytesPerRow + x;
                int outputIndex = (y * width + x) * 4;

                uint8_t gray = src[inputIndex];
                outputBuffer[outputIndex + 0] = gray; // R
                outputBuffer[outputIndex + 1] = gray; // G
                outputBuffer[outputIndex + 2] = gray; // B
                outputBuffer[outputIndex + 3] = 255;  // A
            }
        }
    } else {
        // Unsupported format
        env->ReleaseByteArrayElements(imageData, inputData, JNI_ABORT);
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Unsupported image format");
        return nullptr;
    }

    env->ReleaseByteArrayElements(imageData, inputData, JNI_ABORT);

    return outputBuffer; // Unique pointer will manage the memory
}


extern "C"
JNIEXPORT jobject JNICALL
Java_com_appliedrec_verid3_common_serialization_ImageSerializer_convertToBitmap(
        JNIEnv *env,
        jobject thiz,
        jbyteArray data,
        jint width,
        jint height,
        jint bytes_per_row,
        jobject format
) {
    std::string imageFormat = getImageFormatAsString(env, format);
    std::unique_ptr<uint8_t[]> rgbaData = convertToRGBA(env, data, width, height, bytes_per_row, imageFormat);
    jobject bitmap = createBitmap(env, width, height);
    void *pixels;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        env->ThrowNew(env->FindClass("java/lang/Exception"), "Failed to lock Bitmap pixels");
        return nullptr;
    }
    // Copy the RGBA data into the Bitmap's buffer
    uint8_t *dst = reinterpret_cast<uint8_t *>(pixels);
    memcpy(dst, rgbaData.get(), width * height * 4);
    AndroidBitmap_unlockPixels(env, bitmap);
    return bitmap;
}