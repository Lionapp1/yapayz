package com.redex.pro.data.editor

import android.content.Context
import com.redex.pro.data.model.ApkFileEntry
import java.io.*
import java.util.zip.*

class ApkEditor(private val context: Context) {
    
    private val tempDir = File(context.cacheDir, "apk_edit")
    
    init {
        tempDir.mkdirs()
    }
    
    data class ModifiedFile(
        val entryPath: String,
        val newContent: ByteArray,
        val action: FileAction
    ) {
        enum class FileAction {
            REPLACE,
            ADD,
            DELETE
        }
    }
    
    /**
     * APK dosyasını yeniden paketle (değişiklikleri uygula)
     */
    fun repackApk(
        originalApk: File,
        modifications: List<ModifiedFile>,
        outputFile: File
    ): Boolean {
        return try {
            val zipOutput = ZipOutputStream(FileOutputStream(outputFile))
            val processedEntries = mutableSetOf<String>()
            
            // 1. Orijinal APK'dan dosyaları kopyala (değiştirilmeyenleri)
            val originalZip = ZipFile(originalApk)
            
            originalZip.entries().asSequence().forEach { entry ->
                val mod = modifications.find { it.entryPath == entry.name }
                
                when {
                    mod?.action == ModifiedFile.FileAction.DELETE -> {
                        // Sil - kopyalama
                    }
                    mod?.action == ModifiedFile.FileAction.REPLACE -> {
                        // Değiştir - yeni içerik yaz
                        zipOutput.putNextEntry(ZipEntry(entry.name))
                        zipOutput.write(mod.newContent)
                        zipOutput.closeEntry()
                        processedEntries.add(entry.name)
                    }
                    else -> {
                        // Kopyala - orijinali
                        zipOutput.putNextEntry(ZipEntry(entry.name))
                        originalZip.getInputStream(entry).copyTo(zipOutput)
                        zipOutput.closeEntry()
                        processedEntries.add(entry.name)
                    }
                }
            }
            
            originalZip.close()
            
            // 2. Yeni eklenen dosyaları yaz
            modifications.filter { it.action == ModifiedFile.FileAction.ADD }.forEach { mod ->
                if (mod.entryPath !in processedEntries) {
                    zipOutput.putNextEntry(ZipEntry(mod.entryPath))
                    zipOutput.write(mod.newContent)
                    zipOutput.closeEntry()
                }
            }
            
            zipOutput.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * XML dosyasını düzenle ve geri yaz
     */
    fun editXmlFile(
        originalApk: File,
        xmlEntry: ApkFileEntry,
        newXmlContent: String,
        outputFile: File
    ): Boolean {
        return repackApk(
            originalApk,
            listOf(ModifiedFile(
                entryPath = xmlEntry.path,
                newContent = newXmlContent.toByteArray(),
                action = ModifiedFile.FileAction.REPLACE
            )),
            outputFile
        )
    }
    
    /**
     * Birden fazla dosyayı değiştir
     */
    fun editMultipleFiles(
        originalApk: File,
        changes: Map<String, ByteArray>,
        outputFile: File
    ): Boolean {
        val modifications = changes.map { (path, content) ->
            ModifiedFile(
                entryPath = path,
                newContent = content,
                action = ModifiedFile.FileAction.REPLACE
            )
        }
        return repackApk(originalApk, modifications, outputFile)
    }
    
    /**
     * DEX dosyasını değiştir (smali'den derlenmiş)
     */
    fun replaceDexFile(
        originalApk: File,
        dexPath: String,
        newDexContent: ByteArray,
        outputFile: File
    ): Boolean {
        return repackApk(
            originalApk,
            listOf(ModifiedFile(
                entryPath = dexPath,
                newContent = newDexContent,
                action = ModifiedFile.FileAction.REPLACE
            )),
            outputFile
        )
    }
    
    /**
     * APK içindeki dosyayı sil
     */
    fun deleteFile(
        originalApk: File,
        entryPath: String,
        outputFile: File
    ): Boolean {
        return repackApk(
            originalApk,
            listOf(ModifiedFile(
                entryPath = entryPath,
                newContent = byteArrayOf(),
                action = ModifiedFile.FileAction.DELETE
            )),
            outputFile
        )
    }
    
    /**
     * Dosyayı geçici olarak çıkar
     */
    fun extractToTemp(apkFile: File, entryPath: String): File? {
        return try {
            val zipFile = ZipFile(apkFile)
            val entry = zipFile.getEntry(entryPath) ?: return null
            
            val tempFile = File(tempDir, entryPath.replace("/", "_"))
            zipFile.getInputStream(entry).use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            zipFile.close()
            tempFile
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Smali kodunu bytecode'a derle (sadeleştirilmiş)
     */
    fun compileSmali(smaliCode: String): ByteArray? {
        // Gerçek implementasyon için smali/baksmali kütüphaneleri gerekir
        // Şimdilik placeholder
        return null
    }
    
    fun clearTemp() {
        tempDir.listFiles()?.forEach { it.delete() }
    }
}
