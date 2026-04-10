package com.redex.pro.data.parser

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.redex.pro.data.model.*
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
        
        // Önce imzayı kontrol et (zip kapanmadan önce)
        val isSigned = checkSigned(zipFile)
        
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
            isSigned = isSigned,
            permissions = manifestInfo?.usesPermissions ?: emptyList(),
            activities = manifestInfo?.activities?.map { it.name } ?: emptyList(),
            services = emptyList(),
            receivers = emptyList(),
            dexFiles = dexFiles,
            resources = resources,
            manifest = manifestInfo
        )
    }
    
    private fun parseDexFiles(zipFile: ZipFile): List<com.redex.pro.data.model.DexFile> {
        val dexFiles = mutableListOf<com.redex.pro.data.model.DexFile>()
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
                    com.redex.pro.data.model.DexFile(
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
    
    // ========== APK DOSYA GEZGİNİ ==========
    
    fun getApkStructure(file: File): ApkStructure {
        val zipFile = ZipFile(file)
        val allFiles = mutableListOf<ApkFileEntry>()
        val dexFiles = mutableListOf<ApkFileEntry>()
        val resources = mutableListOf<ApkFileEntry>()
        val assets = mutableListOf<ApkFileEntry>()
        val libraries = mutableListOf<ApkFileEntry>()
        val certificates = mutableListOf<ApkFileEntry>()
        var manifest: ApkFileEntry? = null
        
        zipFile.entries().asSequence().forEach { entry ->
            val type = determineFileType(entry.name)
            val fileEntry = ApkFileEntry(
                path = entry.name,
                name = entry.name.substringAfterLast('/'),
                size = entry.size,
                compressedSize = entry.compressedSize,
                crc = entry.crc,
                type = type,
                isDirectory = entry.isDirectory,
                canEdit = canEditFile(entry.name, type),
                canView = canViewFile(entry.name, type)
            )
            
            allFiles.add(fileEntry)
            
            when {
                entry.name == "AndroidManifest.xml" -> manifest = fileEntry
                entry.name.endsWith(".dex") -> dexFiles.add(fileEntry)
                entry.name.startsWith("res/") -> resources.add(fileEntry)
                entry.name.startsWith("assets/") -> assets.add(fileEntry)
                entry.name.startsWith("lib/") -> libraries.add(fileEntry)
                entry.name.startsWith("META-INF/") && 
                    (entry.name.endsWith(".RSA") || entry.name.endsWith(".DSA") || entry.name.endsWith(".EC")) -> 
                    certificates.add(fileEntry)
            }
        }
        
        zipFile.close()
        
        return ApkStructure(
            allFiles = allFiles.sortedBy { it.path },
            rootFiles = allFiles.filter { !it.path.contains('/') },
            dexFiles = dexFiles.sortedBy { it.name },
            manifest = manifest,
            resources = resources.sortedBy { it.path },
            assets = assets.sortedBy { it.path },
            libraries = libraries.sortedBy { it.path },
            certificates = certificates.sortedBy { it.name }
        )
    }
    
    private fun determineFileType(path: String): ApkFileType {
        return when {
            path == "AndroidManifest.xml" -> ApkFileType.MANIFEST
            path.endsWith(".dex") -> ApkFileType.DEX
            path == "resources.arsc" -> ApkFileType.RESOURCE_ARSC
            path.startsWith("res/layout") && path.endsWith(".xml") -> ApkFileType.LAYOUT
            path.startsWith("res/drawable") -> ApkFileType.DRAWABLE
            path.startsWith("res/values") && path.endsWith(".xml") -> ApkFileType.XML_RESOURCE
            path.startsWith("res/") && path.endsWith(".xml") -> ApkFileType.XML_RESOURCE
            path.startsWith("assets/") -> ApkFileType.ASSET
            path.startsWith("lib/") -> ApkFileType.LIBRARY
            path.startsWith("META-INF/") && 
                (path.endsWith(".RSA") || path.endsWith(".DSA") || path.endsWith(".EC")) -> ApkFileType.CERTIFICATE
            path.startsWith("META-INF/") -> ApkFileType.META_INF
            else -> ApkFileType.RAW
        }
    }
    
    private fun canEditFile(path: String, type: ApkFileType): Boolean {
        return when (type) {
            ApkFileType.XML_RESOURCE,
            ApkFileType.LAYOUT,
            ApkFileType.ASSET -> true
            else -> false
        }
    }
    
    private fun canViewFile(path: String, type: ApkFileType): Boolean {
        return when (type) {
            ApkFileType.DEX,
            ApkFileType.MANIFEST,
            ApkFileType.XML_RESOURCE,
            ApkFileType.LAYOUT,
            ApkFileType.ASSET,
            ApkFileType.RAW -> true
            else -> false
        }
    }
    
    fun extractFileFromApk(apkFile: File, entryPath: String): ByteArray? {
        return try {
            val zipFile = ZipFile(apkFile)
            val entry = zipFile.getEntry(entryPath) ?: return null
            val bytes = zipFile.getInputStream(entry).readBytes()
            zipFile.close()
            bytes
        } catch (e: Exception) {
            null
        }
    }
    
    // ========== SMALI DÖNÜŞTÜRME ==========
    
    fun getSmaliForClass(dexFile: File, className: String): SmaliClass? {
        return try {
            val dex = LibDexFileFactory.loadDexFile(dexFile, null)
            val classDef = dex.classes.find { it.type == className } ?: return null
            
            val smaliCode = generateSmaliCode(classDef)
            val methods = classDef.methods.map { method ->
                SmaliMethod(
                    name = method.name,
                    signature = "(${method.parameters.joinToString("", transform = { it.type })}${method.returnType}",
                    access = getAccessString(method.accessFlags),
                    code = generateMethodSmali(method),
                    lineCount = method.implementation?.instructions?.count() ?: 0
                )
            }
            
            SmaliClass(
                className = classDef.type,
                superClass = classDef.superclass ?: "Ljava/lang/Object;",
                sourceFile = classDef.sourceFile,
                smaliCode = smaliCode,
                methods = methods
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun generateSmaliCode(classDef: org.jf.dexlib2.iface.ClassDef): String {
        val sb = StringBuilder()
        sb.append(".class ${getAccessString(classDef.accessFlags)} ${classDef.type}\n")
        sb.append(".super ${classDef.superclass ?: "Ljava/lang/Object;"}\n")
        classDef.sourceFile?.let { sb.append(".source \"$it\"\n") }
        sb.append("\n")
        
        // Fields
        classDef.fields.forEach { field ->
            sb.append(".field ${getAccessString(field.accessFlags)} ${field.name}:${field.type}\n")
        }
        if (classDef.fields.any()) sb.append("\n")
        
        // Methods
        classDef.methods.forEach { method ->
            sb.append(".method ${getAccessString(method.accessFlags)} ${method.name}(${method.parameters.joinToString("", transform = { it.type })})${method.returnType}\n")
            method.implementation?.let { impl ->
                impl.instructions.forEach { inst ->
                    sb.append("    ${inst}\n")
                }
            }
            sb.append(".end method\n\n")
        }
        
        return sb.toString()
    }
    
    private fun generateMethodSmali(method: org.jf.dexlib2.iface.Method): String {
        val sb = StringBuilder()
        sb.append(".method ${getAccessString(method.accessFlags)} ${method.name}(${method.parameters.joinToString("", transform = { it.type })})${method.returnType}\n")
        method.implementation?.let { impl ->
            impl.instructions.forEach { inst ->
                sb.append("    ${inst}\n")
            }
        }
        sb.append(".end method")
        return sb.toString()
    }
    
    private fun getAccessString(accessFlags: Int): String {
        val flags = mutableListOf<String>()
        if (accessFlags and 0x1 != 0) flags.add("public")
        if (accessFlags and 0x2 != 0) flags.add("private")
        if (accessFlags and 0x4 != 0) flags.add("protected")
        if (accessFlags and 0x8 != 0) flags.add("static")
        if (accessFlags and 0x10 != 0) flags.add("final")
        if (accessFlags and 0x400 != 0) flags.add("abstract")
        if (accessFlags and 0x200 != 0) flags.add("interface")
        if (accessFlags and 0x4000 != 0) flags.add("enum")
        return flags.joinToString(" ")
    }
    
    // ========== DEX EDİTÖR PLUS ==========
    
    fun getAllDexFilesWithClasses(apkFile: File): List<Pair<ApkFileEntry, List<ClassInfo>>> {
        val result = mutableListOf<Pair<ApkFileEntry, List<ClassInfo>>>()
        val structure = getApkStructure(apkFile)
        
        structure.dexFiles.forEach { dexEntry ->
            val dexBytes = extractFileFromApk(apkFile, dexEntry.path) ?: return@forEach
            val tempDex = File(cacheDir, dexEntry.name)
            tempDex.writeBytes(dexBytes)
            
            try {
                val dex = LibDexFileFactory.loadDexFile(tempDex, null)
                val classes = dex.classes.map { classDef ->
                    ClassInfo(
                        name = classDef.type,
                        superClass = classDef.superclass,
                        interfaces = classDef.interfaces.toList(),
                        methods = classDef.methods.map { method ->
                            MethodInfo(
                                name = method.name,
                                descriptor = "(${method.parameters.joinToString("", transform = { it.type })})${method.returnType}",
                                accessFlags = method.accessFlags,
                                codeSize = method.implementation?.instructions?.count() ?: 0
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
                result.add(Pair(dexEntry, classes))
            } catch (e: Exception) {
                // Skip this dex
            }
            tempDex.delete()
        }
        
        return result
    }
}
