package com.redex.pro.data.parser

import com.redex.pro.data.model.ActivityInfo
import com.redex.pro.data.model.ManifestInfo
import com.redex.pro.data.model.PermissionInfo
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

class AxmlParser {
    
    fun parse(inputStream: InputStream): ManifestInfo? {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(inputStream, null)
            
            var packageName = ""
            var versionName = ""
            var versionCode = 0L
            var minSdk = 0
            var targetSdk = 0
            var compileSdk: Int? = null
            var applicationName: String? = null
            val activities = mutableListOf<ActivityInfo>()
            val permissions = mutableListOf<PermissionInfo>()
            val usesPermissions = mutableListOf<String>()
            
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "manifest" -> {
                            packageName = parser.getAttributeValue(null, "package") ?: ""
                            versionName = parser.getAttributeValue(null, "android:versionName") ?: ""
                            versionCode = parser.getAttributeValue(null, "android:versionCode")?.toLongOrNull() ?: 0
                        }
                        "uses-sdk" -> {
                            minSdk = parser.getAttributeValue(null, "android:minSdkVersion")?.toIntOrNull() ?: 0
                            targetSdk = parser.getAttributeValue(null, "android:targetSdkVersion")?.toIntOrNull() ?: 0
                        }
                        "application" -> {
                            applicationName = parser.getAttributeValue(null, "android:name")
                            compileSdk = parser.getAttributeValue(null, "android:compileSdkVersion")?.toIntOrNull()
                        }
                        "activity" -> {
                            val name = parser.getAttributeValue(null, "android:name") ?: ""
                            val exported = parser.getAttributeValue(null, "android:exported")?.toBoolean() ?: false
                            val intentFilter = findIntentFilter(parser)
                            val isMain = intentFilter.contains("action.MAIN")
                            val isLauncher = intentFilter.contains("category.LAUNCHER")
                            
                            activities.add(ActivityInfo(name, isMain, isLauncher, exported))
                        }
                        "permission" -> {
                            val name = parser.getAttributeValue(null, "android:name") ?: ""
                            val level = parser.getAttributeValue(null, "android:protectionLevel") ?: "normal"
                            permissions.add(PermissionInfo(name, level))
                        }
                        "uses-permission" -> {
                            val name = parser.getAttributeValue(null, "android:name") ?: ""
                            usesPermissions.add(name)
                        }
                    }
                }
                eventType = parser.next()
            }
            
            ManifestInfo(
                packageName = packageName,
                versionName = versionName,
                versionCode = versionCode,
                minSdk = minSdk,
                targetSdk = targetSdk,
                compileSdk = compileSdk,
                applicationName = applicationName,
                activities = activities,
                permissions = permissions,
                usesPermissions = usesPermissions
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun findIntentFilter(parser: XmlPullParser): String {
        val result = StringBuilder()
        var depth = parser.depth
        var eventType = parser.next()
        
        while (eventType != XmlPullParser.END_DOCUMENT && parser.depth > depth) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "action", "category" -> {
                        val name = parser.getAttributeValue(null, "android:name") ?: ""
                        result.append(name).append(",")
                    }
                }
            }
            eventType = parser.next()
        }
        
        return result.toString()
    }
}
