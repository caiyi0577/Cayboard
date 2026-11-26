import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.security.MessageDigest
import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

apply(from = "build-logic/tasks-native.gradle.kts")
apply(from = "build-logic/tasks-source-patches.gradle.kts")
apply(from = "build-logic/tasks-speech.gradle.kts")
apply(from = "build-logic/tasks-bundled-models.gradle.kts")
apply(from = "build-logic/tasks-hymt.gradle.kts")
apply(from = "build-logic/tasks-chinese.gradle.kts")
apply(from = "build-logic/tasks-stroke.gradle.kts")
apply(from = "build-logic/tasks-cangjie.gradle.kts")
apply(from = "build-logic/tasks-t9-grammar.gradle.kts")
apply(from = "build-logic/tasks-input-manifest.gradle.kts")

// 加载签名配置
val keystorePropertiesFile = rootProject.file("app/keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.cayboard"
    if (providers.gradleProperty("bundleModels").orNull == "true") {
        sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/bundled-model-assets").get().asFile)
    }
    // Bundled multilingual CPU translation engine and model.
    sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/hymt-assets").get().asFile)
    sourceSets.getByName("main").jniLibs.srcDir(layout.buildDirectory.dir("generated/hymt-jniLibs").get().asFile)
    sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/chinese-assets").get().asFile)
    sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/stroke-assets").get().asFile)
    sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/cangjie-assets").get().asFile)
    sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/input-manifest").get().asFile)
    sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/speech-assets").get().asFile)
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    // JVM 单测中未 mock 的 Android 框架方法（如 android.util.Log）返回默认值而非抛异常，
    // 使服务层状态机（如语音收尾流程）可直接实例化测试
    testOptions {
        unitTests.isReturnDefaultValues = true
        // Robolectric 渲染测试（如候选栏两端按钮对比图）需要真实资源与 assets。
        unitTests.isIncludeAndroidResources = true
    }

    defaultConfig {
        applicationId = "com.cayboard"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        // 按发布批次定号：兼容修复升 PATCH，兼容功能升 MINOR 并归零 PATCH。
        // 不兼容变更升 MAJOR；日常编辑不逐次升号。完整规则见 docs/packaging.md。
        // versionCode 是独立递增的交付序号；同批普通版与内置版共用。
        versionCode = 20261517
        versionName = "2.1.17"


        // Isolated Compose rendering on a local x86 emulator; ordinary device tests keep the full app.
        testInstrumentationRunner = if (providers.gradleProperty("uiRenderingOnly").orNull == "true")
            "com.cayboard.ui.UiRenderingTestRunner" else "androidx.test.runner.AndroidJUnitRunner"

        // NDK 配置
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        // 构建信息
        buildConfigField("boolean", "BUNDLED_MODELS", (providers.gradleProperty("bundleModels").orNull == "true").toString())
        // The elder package (elder-pack-spec.md); other bundled-model builds such as the friend package keep normal behaviour.
        buildConfigField("boolean", "ELDER", (providers.gradleProperty("elderEdition").orNull == "true").toString())
        // Developer-only features (handwriting training / capture): only the packager's 装到我的设备 preset (-PdevTools=true)
        // and debug builds show them; public, elder and friend packages never do.
        buildConfigField("boolean", "DEV_TOOLS", (providers.gradleProperty("devTools").orNull == "true").toString())
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                // 优先使用 storeFile（已存在的 keystore 文件）；否则解码 keyBase64。
                // keyBase64 方式用于 CI/F-Droid 从 secrets 注入签名。
                val storeFileProp = keystoreProperties.getProperty("storeFile")?.let { file(it) }
                storeFile = if (storeFileProp != null && storeFileProp.exists()) {
                    storeFileProp
                } else {
                    keystoreProperties.getProperty("keyBase64")?.let { keyBase64 ->
                        val ks = File(layout.buildDirectory.get().asFile, "release-keystore.jks")
                        ks.writeBytes(Base64.getDecoder().decode(keyBase64.replace("\n", "").trim()))
                        ks
                    }
                }
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
            // minSdk 28 时 AGP 默认只签 v2；vivo OriginOS 安装器只认 META-INF 里的 v1（JAR）证书，
            // 缺了就报「安装包缺乏开发者证书」并拒绝安装。同一把密钥补签 v1，不影响已装用户覆盖升级。
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        debug {
            // 本地有正式密钥时沿用它，debug/release 都能覆盖原应用，避免卸载丢数据。
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 只在本地有 keystore.properties 时才使用签名配置
            // GitHub Actions 使用自己的签名方式
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        // 离线构建类型：initWith(release)，由 src/offline 的 manifest 叠加层移除 INTERNET 权限。
        // 产物（assembleOffline）供高敏感环境使用：系统层面禁止一切联网，模型/词库下载、
        // 扩展商店、检查更新、在线语音、样本贡献、剪贴板同步在该构建中均不可用，
        // 需配合完整版离线模型内置包。现有 assembleDebug/assembleRelease 任务不受影响。
        create("offline") {
            initWith(getByName("release"))
            // Library modules use release for the offline variant.
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xunused")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }

    // NDK 构建配置
    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // 打包配置
    packaging {
        jniLibs {
            useLegacyPackaging = true
            // tasks-native.gradle.kts extracts this same 1.28.0 runtime for C++ consumers.
            pickFirsts.add("**/libonnxruntime.so")
            pickFirsts.add("**/libc++_shared.so")
        }
    }
    // 九键长句重排已停用（t9_pinyin.schema.yaml），两种包都不再带这个模型；源文件只留给电脑回放对照。
    // Keep vocabulary and licenses; both editions offer the large grammar separately.
    androidResources.ignoreAssetsPatterns.add("t9_sentence.onnx")
    // 语音（SenseVoice）和九键词组模型不这样做：不压缩会让完整版下载各多约 76 MB（2026-11-02 决定保持压缩、解压一份）。
    // 声纹和手写模型保持解压：模型管理（是否装好、删除、设置向导跳过）和手写模板、训练页都按 models/ 下的文件路径读，
    // 为省 7–27 MB 改这些地方不值得。
    androidResources.noCompress.add("cseg")
    // This edition carries Chinese and English scheme resources only, including bundled-model builds.
    listOf("<dir>cayboard_japanese", "<dir>azookey", "<dir>cayboard_letters").forEach { androidResources.ignoreAssetsPatterns.add(it) }
    // Incremental builds can still have these pre-migration directories beside the generated schemes.
    androidResources.ignoreAssetsPatterns.addAll(
        listOf("<dir>rime_chinese", "<dir>rime_ice", "<dir>rime_japanese", "<dir>rime_letters")
    )
    packaging.jniLibs.excludes.add("**/libCyAzooKey.so")
    ndkVersion = "29.0.14206865"

    // F-Droid 一致性验证：不在 APK 中写入依赖元数据（Dependency metadata 签名块）。
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    // 测试 classpath 包含 main assets，使 T9Decoder() 无参构造可加载 pinyin_lm.bin
    sourceSets {
        if (providers.gradleProperty("offlineTranslateSmoke").orNull == "true") {
            getByName("androidTest").java.setSrcDirs(listOf("src/offlineTranslateTest/java"))
            getByName("androidTest").assets.srcDir("src/offlineTranslateTest/assets")
        }
        getByName("test") {
            resources.srcDirs("src/main/assets")
        }
    }
    lint {
        checkReleaseBuilds = false
        checkGeneratedSources = false
        abortOnError = false
        checkDependencies = true
    }

    // 分架构打包
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
}

android.applicationVariants.all {
    val appName = "Cayboard"
    outputs.all {
        val abi = filters.find { it.filterType.toString() == "ABI" }?.identifier ?: "universal"
        val buildLabel = if (buildType.name == "debug") "-debug" else ""
        val edition = if (providers.gradleProperty("bundleModels").orNull == "true") "-full" else ""
        (this as BaseVariantOutputImpl).outputFileName = "$appName-$versionName$edition$buildLabel-$abi.apk"
    }
}

// Nightly 构建通过 androidComponents API 覆盖 versionCode/versionName
androidComponents {
    onVariants { variant ->
        val vc = project.findProperty("versionCode")?.toString()?.toIntOrNull()
        val vn = project.findProperty("versionName")?.toString()
        if (vc != null && vn != null) {
            variant.outputs.forEach { output ->
                output.versionCode.set(vc)
                output.versionName.set(vn)
            }
        }
    }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Kotlin stdlib - CRITICAL for plugin compatibility
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.4.20")
    implementation(libs.kotlinx.coroutines.core)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.activity.compose)

    // Material Icons
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // SavedState
    implementation(libs.androidx.savedstate)

    // Coil (Image Loading)

    // OkHttp for WebSocket and model download
    // Apache Commons Compress for tar.bz2 extraction
    implementation("org.apache.commons:commons-compress:1.28.0")

    // Kaml for YAML parsing
    implementation(libs.kaml)

    // Autofill inline suggestions (API 30+)
    implementation(libs.androidx.autofill)

    // exp4j for calculator expression evaluation
    implementation(libs.exp4j)

    // ZXing for QR code generation
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.28.0")

    implementation(libs.kotlinx.serialization.json)

    // Room 3.0 (SQLite)
    implementation(libs.androidx.room3.runtime)
    ksp(libs.androidx.room3.compiler)

    // Sora Code Editor for YAML viewing/editing
    implementation(platform("io.github.rosemoe:editor-bom:0.24.6"))
    implementation("io.github.rosemoe:editor")
    implementation("io.github.rosemoe:language-textmate")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    debugImplementation(libs.androidx.compose.ui.tooling)
    // Compose 仪器测试需要一个独立宿主 Activity，仅加入 debug 变体。
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
    // JVM 单测使用真实 org.json 实现（android.jar 内为抛异常的 stub）
    testImplementation("org.json:json:20240303")
    // 在电脑上用真实 Compose 代码渲染界面截图（Robolectric 原生绘制），不用模拟器。
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.concurrent:concurrent-futures:1.2.0")
}

// Align concurrent-futures version: espresso 3.7.0 requires 1.2.0
dependencies {
    constraints {
        implementation("androidx.concurrent:concurrent-futures:1.2.0") {
            because("test dependencies (espresso 3.7.0) require 1.2.0")
        }
    }
}

// Fixed offline speech assets supplied by the local packaging entry.
val fixedSpeechAssetsDir = file(
    providers.gradleProperty("cayboard.xasrAssetsDir").orNull
        ?: "src/main/fixed-speech-assets"
)
android.sourceSets.getByName("main").assets.srcDir(fixedSpeechAssetsDir)
val verifyFixedSpeechAssets = tasks.register("verifyFixedSpeechAssets") {
    inputs.dir(fixedSpeechAssetsDir)
    doLast {
        val speechRoot = fixedSpeechAssetsDir.resolve("builtin-speech")
        val requiredFiles = listOf(
            speechRoot.resolve("manifest.json"),
            speechRoot.resolve("xasr/encoder-epoch-99-avg-1.int8.onnx"),
            speechRoot.resolve("xasr/decoder-epoch-99-avg-1.onnx"),
            speechRoot.resolve("xasr/joiner-epoch-99-avg-1.int8.onnx"),
            speechRoot.resolve("xasr/tokens.txt"),
        )
        check(requiredFiles.all { it.isFile && it.length() > 0L }) {
            "Fixed offline X-ASR assets are incomplete under ${speechRoot.absolutePath}"
        }
        val manifestText = requiredFiles.first().readText()
        val expectedAssets = mapOf(
            "encoder-epoch-99-avg-1.int8.onnx" to (161015713L to "7f6aa62056efd8af9da13e0faa81cd3f284d2fb2e3b63de56fd2dfd3450910dc"),
            "decoder-epoch-99-avg-1.onnx" to (11309084L to "72f47405d3c1033bebccbef82f90071e7b4ba3e71b9c986f2b74244b25723aed"),
            "joiner-epoch-99-avg-1.int8.onnx" to (2581422L to "aedb7fa697b2ab43f20499826fff7c997eea7d67db77be97769aeeeb726e63b3"),
            "tokens.txt" to (58806L to "b818a60878b9aae978cbb8ad594acbd403d76d1af2e31ef4197c84e2dbdba27c"),
        )
        check(expectedAssets.keys.all { manifestText.contains("\"name\": \"$it\"") }) {
            "Fixed offline X-ASR manifest must list exactly the bundled model files"
        }
        for ((name, expected) in expectedAssets) {
            check(name.matches(Regex("[A-Za-z0-9_.-]+"))) { "Invalid X-ASR asset name: $name" }
            val asset = speechRoot.resolve("xasr/$name")
            check(asset.isFile && asset.length() == expected.first) {
                "X-ASR asset size mismatch: ${asset.absolutePath}"
            }
            val digest = MessageDigest.getInstance("SHA-256").digest(asset.readBytes())
                .joinToString("") { "%02x".format(it) }
            check(digest == expected.second && manifestText.contains(expected.second)) {
                "X-ASR asset checksum mismatch: ${asset.name}"
            }
        }
    }
}
tasks.named("preBuild").configure { dependsOn(verifyFixedSpeechAssets) }



