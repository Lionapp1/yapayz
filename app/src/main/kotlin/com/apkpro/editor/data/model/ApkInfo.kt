package com.apkpro.editor.data.model

data class ApkInfo(
    val name: String,
    val path: String,
    val packageName: String = "",
    val versionName: String = "",
    val versionCode: Long = 0,
    val minSdk: Int = 0,
    val targetSdk: Int = 0,
    val size: Long = 0,
    val md5: String = "",
    val sha1: String = "",
    val sha256: String = "",
    val isSigned: Boolean = false,
    val permissions: List<String> = emptyList(),
    val activities: List<String> = emptyList(),
    val services: List<String> = emptyList(),
    val receivers: List<String> = emptyList(),
    val dexFiles: List<DexFile> = emptyList(),
    val resources: ResourceInfo = ResourceInfo(),
    val manifest: ManifestInfo? = null
)

data class DexFile(
    val name: String,
    val size: Long,
    val classCount: Int,
    val methodCount: Int,
    val fieldCount: Int,
    val stringCount: Int,
    val classes: List<ClassInfo> = emptyList()
)

data class ClassInfo(
    val name: String,
    val superClass: String?,
    val interfaces: List<String>,
    val methods: List<MethodInfo>,
    val fields: List<FieldInfo>,
    val accessFlags: Int,
    val sourceFile: String?
)

data class MethodInfo(
    val name: String,
    val descriptor: String,
    val accessFlags: Int,
    val codeSize: Int = 0
)

data class FieldInfo(
    val name: String,
    val type: String,
    val accessFlags: Int
)

data class ResourceInfo(
    val stringCount: Int = 0,
    val drawableCount: Int = 0,
    val layoutCount: Int = 0,
    val colorCount: Int = 0,
    val rawCount: Int = 0,
    val assetCount: Int = 0,
    val strings: Map<String, String> = emptyMap(),
    val drawables: List<String> = emptyList(),
    val layouts: List<String> = emptyList()
)

data class ManifestInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val compileSdk: Int?,
    val applicationName: String?,
    val activities: List<ActivityInfo> = emptyList(),
    val permissions: List<PermissionInfo> = emptyList(),
    val usesPermissions: List<String> = emptyList()
)

data class ActivityInfo(
    val name: String,
    val isMain: Boolean = false,
    val isLauncher: Boolean = false,
    val exported: Boolean = false
)

data class PermissionInfo(
    val name: String,
    val protectionLevel: String = "normal"
)
