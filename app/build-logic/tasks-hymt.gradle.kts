import java.security.MessageDigest

// CPU engine from llama.cpp PR22836, pinned commit 1e411d8f5a1e23525fa3265dfb4bd76265465397.
// Reproduce with tools/hymt_translate and the local Windows/NDK build recipe in the development report.
val hymtLibraryHash = "7f8b23fc4a0f25d9ab9fbea70fed3ebff04fd5dfb5f6b956883fe826076f8b8e"
val hymtModelHash = "cc497fe8f033b52b3b8b00a7669e9661435432f9d4cd43f7ed24400c01507a93"
fun hymtDigest(file: File): String {
    val hash = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(1024 * 1024)
        while (true) {val n=input.read(buffer);if(n<0) break;hash.update(buffer,0,n)}
    }
    return hash.digest().joinToString("") {"%02x".format(it)}
}
val hymtModelFile = providers.gradleProperty("cyime.hymtModelFile")
    .map { File(it) }.getOrElse(rootProject.file(".codex-artifacts/hymt/Hy-MT2-1.8B-1.25Bit.gguf"))
val hymtLibFile = rootProject.file(".codex-artifacts/hymt/libs/arm64-v8a/libcyhymt.so")
val prepareHymtAssets by tasks.registering {
    val destination=layout.buildDirectory.dir("generated/hymt-assets/builtin-translation")
    inputs.file(hymtModelFile);inputs.property("hash",hymtModelHash)
    outputs.dir(destination)
    doLast {
        check(hymtModelFile.length()==461860800L && hymtDigest(hymtModelFile)==hymtModelHash) {"Bundled Hy-MT2 model is missing or invalid"}
        val target=File(destination.get().asFile,"Hy-MT2-1.8B-1.25Bit.gguf")
        target.parentFile.mkdirs();hymtModelFile.copyTo(target,overwrite=true)
    }
}
val prepareHymtLibraries by tasks.registering {
    val destination=layout.buildDirectory.dir("generated/hymt-jniLibs")
    inputs.file(hymtLibFile);inputs.property("hash",hymtLibraryHash)
    outputs.dir(destination)
    doLast {
        check(hymtDigest(hymtLibFile)==hymtLibraryHash) {"Hy-MT2 runtime is missing or invalid"}
        val target=File(destination.get().asFile,"arm64-v8a/libcyhymt.so")
        target.parentFile.mkdirs();hymtLibFile.copyTo(target,overwrite=true)
    }
}
tasks.matching {it.name=="preBuild"}.configureEach {dependsOn(prepareHymtAssets,prepareHymtLibraries)}
