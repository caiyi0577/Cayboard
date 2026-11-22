package com.cayboard.translate

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=android.app.Application::class)
class BundledTranslationModelTest {
 private fun context():Context {
  val base=ApplicationProvider.getApplicationContext<Context>()
  val root=File(base.cacheDir,"hymt-test-${java.util.UUID.randomUUID()}").apply {mkdirs()}
  return object:ContextWrapper(base) {
   override fun getFilesDir()=root
   override fun getSharedPreferences(name:String,mode:Int)=base.getSharedPreferences(root.name+name,mode)
  }
 }
 private val bytes="verified model bytes".toByteArray()
 private val sha get()=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {"%02x".format(it)}
 private fun old(context:Context,id:String)=File(context.filesDir,"models/$id/model.bin").apply {parentFile!!.mkdirs();writeText("keep until verified")}
 @Test fun onlyRetiresTheOldTranslationPairAfterTheNewAssetPassesVerification() {
  val c=context();val a=old(c,"translate-zh-en");val b=old(c,"translate-en-zh");val speech=old(c,"sensevoice-multilingual-int8")
  val f=BundledTranslationModel.prepareVerified(c,bytes.size.toLong(),sha) {bytes.inputStream()}
  assertArrayEquals(bytes,f.readBytes());assertFalse(a.exists());assertFalse(b.exists());assertTrue(speech.exists())
  assertEquals("ready",BundledTranslationModel.phase.value)
  BundledTranslationModel.prepareVerified(c,bytes.size.toLong(),sha) {error("Verified model must not be copied again")}
 }
 @Test fun corruptAssetKeepsOldModelsAndNeverPublishesAPartialFile() {
  val c=context();val old=old(c,"translate-zh-en")
  assertThrows(IllegalStateException::class.java) {BundledTranslationModel.prepareVerified(c,bytes.size.toLong(),sha) {"broken".byteInputStream()}}
  assertTrue(old.exists());assertFalse(BundledTranslationModel.file(c).exists())
  assertFalse(File(BundledTranslationModel.file(c).parentFile,"${BundledTranslationModel.file(c).name}.preparing").exists())
 }
 @Test fun catalogContainsNoDownloadableFirefoxTranslationPairAndLanguagesAreUnique() {
  assertTrue(com.cayboard.model.BuiltinModelCatalog.models.none {it.id in listOf("translate-zh-en","translate-en-zh")})
  assertEquals(TranslationLanguages.all.size,TranslationLanguages.all.map {it.code}.toSet().size)
  assertTrue(TranslationLanguages.supports("auto","ja"));assertTrue(TranslationLanguages.supports("ko","fr"))
  assertFalse(TranslationLanguages.supports("xx","en"));assertFalse(TranslationLanguages.supports("en","xx"))
 }
}
