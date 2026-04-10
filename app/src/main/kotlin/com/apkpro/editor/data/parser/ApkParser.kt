package com.apkpro.editor.data.parser

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.apkpro.editor.data.model.*
import org.jf.dexlib2.DexFileFactory as LibDexFileFactory
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class ApkParser(private val context: Context) {
    
    private val cacheDir = File(context.cacheDir, "apk_extract")
    
    init {
        cacheDir.mkdirs()
    }
    
    suspend fun parseApk(uri: Uri): ApkInfo {
        val tempFile = copyUriToTemp(uri)
        return parseApkFile(tempFile)
    }
    
    suspend fun parseApkFile(file: File): ApkInfo {
        val zipFile = ZipFile(file)
        
        // APK bilgilerini topla
        val size = file.length()
        val md5 = calculateHash(file, "MD5")
        val sha1 = calculateHash(file, "SHA-1")
        val sha256 = calculateHash(file, "SHA-256")
        
        // AndroidManifest.xml'den bilgileri al
        val manifestEntry = zipFile.getEntry("AndroidManifest.xml")
        val manifestInfo = manifestEntry?.let { parseManifest(zipFile.getInputStream(it)) }
        
        // DEX dosyalarını analiz et
        val dexFiles = parseDexFiles(zipFile)
        
        // Kaynakları analiz et
        val resources = parseResources(zipFile)
        
        zipFile.close()
        
        return ApkInfo(
            name = file.name,
            path = file.absolutePath,
            packageName = manifestInfo?.packageName ?: "",
            versionName = manifestInfo?.versionName ?: "",
            versionCode = manifestInfo?.versionCode ?: 0,
            minSdk = manifestInfo?.minSdk ?: 0,
            targetSdk = manifestInfo?.targetSdk ?: 0,
            size = size,
            md5 = md5,
            sha1 = sha1,
            sha256 = sha256,
            isSigned = checkSigned(zipFile),
            permissions = manifestInfo?.usesPermissions ?: emptyList(),
            activities = manifestInfo?.activities?.map { it.name } ?: emptyList(),
            services = emptyList(),
            receivers = emptyList(),
            dexFiles = dexFiles,
            resources = resources,
            manifest = manifestInfo
        )
    }
    
    private fun parseDexFiles(zipFile: ZipFile): List<com.apkpro.editor.data.model.DexFile> {
        val dexFiles = mutableListOf<com.apkpro.editor.data.model.DexFile>()
        var index = 0
        
        while (true) {
            val entryName = if (index == 0) "classes.dex" else "classes$index.dex"
            val entry = zipFile.getEntry(entryName) ?: break
            
            val tempDex = File(cacheDir, entryName)
            zipFile.getInputStream(entry).use { input ->
                tempDex.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            try {
                val dexFile = LibDexFileFactory.loadDexFile(tempDex, null)
                val classCount = dexFile.classes.count()
                var methodCount = 0
                var fieldCount = 0
                var stringCount = 0
                
                for (classDef in dexFile.classes) {
                    val methods = classDef.methods
                    val fields = classDef.fields
                    for (method in methods) {
                        methodCount++
                    }
                    for (field in fields) {
                        fieldCount++
                    }
                }
                
                val classes = dexFile.classes.map { classDef ->
                    ClassInfo(
                        name = classDef.type,
                        superClass = classDef.superclass,
                        interfaces = classDef.interfaces.toList(),
                        methods = classDef.methods.map { method ->
                            MethodInfo(
                                name = method.name,
                                descriptor = "()V", // Placeholder - dexlib2 farklı API
                                accessFlags = method.accessFlags,
                                codeSize = 0 // Simplified
                            )
                        },
                        fields = classDef.fields.map { field ->
                            FieldInfo(
                                name = field.name,
                                type = field.type,
                                accessFlags = field.accessFlags
                            )
                        },
                        accessFlags = classDef.accessFlags,
                        sourceFile = classDef.sourceFile
                    )
                }
                
                dexFiles.add(
                    com.apkpro.editor.data.model.DexFile(
                        name = entryName,
                        size = entry.size,
                        classCount = classCount,
                        methodCount = methodCount,
                        fieldCount = fieldCount,
                        stringCount = stringCount,
                        classes = classes
                    )
                )
            } catch (e: Exception) {
                // DEX ayrıştırma hatası
            }
            
            tempDex.delete()
            index++
        }
        
        return dexFiles
    }
    
    private fun parseResources(zipFile: ZipFile): ResourceInfo {
        var stringCount = 0
        var drawableCount = 0
        var layoutCount = 0
        var colorCount = 0
        var rawCount = 0
        var assetCount = 0
        val drawables = mutableListOf<String>()
        val layouts = mutableListOf<String>()
        
        zipFile.entries().asSequence().forEach { entry ->
            when {
                entry.name.startsWith("res/drawable") -> {
                    drawableCount++
                    drawables.add(entry.name)
                }
                entry.name.startsWith("res/layout") -> {
                    layoutCount++
                    layouts.add(entry.name)
                }
                entry.name.startsWith("res/values") -> stringCount++
                entry.name.startsWith("res/color") -> colorCount++
                entry.name.startsWith("res/raw") -> rawCount++
                entry.name.startsWith("assets/") -> assetCount++
            }
        }
        
        // resources.arsc varsa parse et
        val arscEntry = zipFile.getEntry("resources.arsc")
        val strings = mutableMapOf<String, String>()
        
        arscEntry?.let {
            try {
                val arscParser = ArscParser()
                val arscStrings = arscParser.parse(zipFile.getInputStream(it))
                strings.putAll(arscStrings)
            } catch (e: Exception) {
                // ARSC parse hatası
            }
        }
        
        return ResourceInfo(
            stringCount = stringCount,
            drawableCount = drawableCount,
            layoutCount = layoutCount,
            colorCount = colorCount,
            rawCount = rawCount,
            assetCount = assetCount,
            strings = strings,
            drawables = drawables,
            layouts = layouts
        )
    }
    
    private fun parseManifest(inputStream: InputStream): ManifestInfo? {
        return try {
            val axmlParser = AxmlParser()
            axmlParser.parse(inputStream)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun checkSigned(zipFile: ZipFile): Boolean {
        return zipFile.getEntry("META-INF/MANIFEST.MF") != null ||
               zipFile.getEntry("META-INF/CERT.RSA") != null ||
               zipFile.getEntry("META-INF/CERT.DSA") != null ||
               zipFile.getEntry("META-INF/CERT.EC") != null
    }
    
    private fun calculateHash(file: File, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
        }
        val hash = BigInteger(1, digest.digest())
        return hash.toString(16).padStart(32, '0')
    }
    
    private fun copyUriToTemp(uri: Uri): File {
        val fileName = getFileNameFromUri(uri) ?: "temp.apk"
        val tempFile = File(cacheDir, fileName)
        
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        return tempFile
    }
    
    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.substringAfterLast('/')
        }
        return result
    }
}
