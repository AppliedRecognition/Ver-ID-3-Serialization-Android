import com.google.protobuf.gradle.proto
import java.util.Properties

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.protobuf)
    `maven-publish`
    signing
}

android {
    namespace = "com.appliedrec.verid3.common.serialization"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testOptions.targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
        externalNativeBuild {
            cmake {
                abiFilters("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                // Get the NDK root path
                val properties = Properties()
                properties.load(project.rootProject.file("local.properties").inputStream())
                val ndkRootPath: String = properties.getProperty("sdk.dir") + "/ndk/" + libs.versions.androidNdk.get()

                // Pass the NDK root path to CMake
                arguments("-DNDK_ROOT_PATH=${ndkRootPath}")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }
    ndkVersion = libs.versions.androidNdk.get()
    sourceSets {
        getByName("main") {
            proto {
                srcDir("../proto")  // Point to the directory where your .proto files are located
                include("../proto")  // Include all .proto files
            }
            java.srcDirs("src/main/kotlin", layout.buildDirectory.file("generated/source/proto/main/kotlin"))
            jniLibs.srcDirs("src/main/cpp")
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.protobuf.javalite)
    implementation(libs.verid.common)
    implementation(libs.jxl.coder)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

protobuf {
    protoc {
        artifact = libs.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

//tasks.register<Exec>("buildProtobufTypes") {
//    commandLine("sh", "../build.sh")
//}
//
//tasks.named("build") {
//    dependsOn("buildProtobufTypes")
//}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.appliedrec"
            artifactId = "verid3-serialization"
            version = "1.0.0"
            afterEvaluate {
                from(components["release"])
            }
            pom {
                name.set("Ver-ID Serialization")
                description.set("Serialization of Ver-ID SDK types using protocol buffers")
                url.set("https://github.com/AppliedRecognition/Ver-ID-Serialization-Android")
                licenses {
                    license {
                        name.set("Commercial")
                        url.set("https://raw.githubusercontent.com/AppliedRecognition/Ver-ID-Serialization-Android/main/LICENCE.txt")
                    }
                }
                developers {
                    developer {
                        id.set("appliedrec")
                        name.set("Applied Recognition")
                        email.set("support@appliedrecognition.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/AppliedRecognition/Ver-ID-Serialization-Android.git")
                    developerConnection.set("scm:git:ssh://github.com/AppliedRecognition/Ver-ID-Serialization-Android.git")
                    url.set("https://github.com/AppliedRecognition/Ver-ID-Serialization-Android")
                }
            }
        }
    }

    repositories {
        maven {
            name = "MavenCentral"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("mavenCentralUsername") as String?
                password = project.findProperty("mavenCentralPassword") as String?
            }
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/AppliedRecognition/Ver-ID-Releases-Android")
            credentials {
                username = project.findProperty("gpr.user") as String?
                password = project.findProperty("gpr.token") as String?
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["release"])
}