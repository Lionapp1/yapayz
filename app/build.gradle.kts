plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.apkpro.editor"
    compileSdk = 34

    // Version from CI/CD or defaults
    val versionCodeProp = (project.findProperty("versionCode") as? String)?.toIntOrNull() ?: 1
    val versionNameProp = (project.findProperty("versionName") as? String) ?: "1.0.0"
    
    logger.lifecycle("BUILD: versionCode=$versionCodeProp, versionName=$versionNameProp")
    
    defaultConfig {
        applicationId = "com.apkpro.editor"
        minSdk = 26
        targetSdk = 34
        versionCode = versionCodeProp
        versionName = versionNameProp

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Get signing configuration from environment
    val keystorePath = System.getenv("KEYSTORE_PATH") ?: ""
    val keystorePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
    val keyAlias = System.getenv("KEY_ALIAS") ?: ""
    val keyPassword = System.getenv("KEY_PASSWORD") ?: ""
    
    // Log signing info (passwords masked)
    logger.lifecycle("SIGNING: KEYSTORE_PATH = $keystorePath")
    logger.lifecycle("SIGNING: keystorePath isEmpty = ${keystorePath.isEmpty()}")
    logger.lifecycle("SIGNING: keystorePassword isEmpty = ${keystorePassword.isEmpty()}")
    logger.lifecycle("SIGNING: keyAlias isEmpty = ${keyAlias.isEmpty()}")
    logger.lifecycle("SIGNING: keyPassword isEmpty = ${keyPassword.isEmpty()}")
    
    val hasSigningConfig = keystorePath.isNotEmpty() && 
                          keystorePassword.isNotEmpty() && 
                          keyAlias.isNotEmpty() && 
                          keyPassword.isNotEmpty()
    
    logger.lifecycle("SIGNING: hasSigningConfig = $hasSigningConfig")
    
    signingConfigs {
        create("release") {
            if (hasSigningConfig) {
                val keystoreFile = file(keystorePath)
                logger.lifecycle("SIGNING: Checking keystore file at ${keystoreFile.absolutePath}")
                logger.lifecycle("SIGNING: keystoreFile.exists() = ${keystoreFile.exists()}")
                
                if (keystoreFile.exists()) {
                    storeFile = keystoreFile
                    storePassword = keystorePassword
                    this.keyAlias = keyAlias
                    this.keyPassword = keyPassword
                    logger.lifecycle("SIGNING: Release signing config configured successfully")
                } else {
                    logger.lifecycle("SIGNING: ERROR - Keystore file not found!")
                }
            } else {
                logger.lifecycle("SIGNING: WARNING - Incomplete signing configuration, release will be unsigned")
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
            
            if (hasSigningConfig && file(keystorePath).exists()) {
                signingConfig = signingConfigs.getByName("release")
                logger.lifecycle("SIGNING: Release build type WILL BE SIGNED")
            } else {
                logger.lifecycle("SIGNING: Release build type will be UNSIGNED (no valid keystore)")
            }
        }
        debug {
            // Use debug signing by default
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // DEX Parser (APK'nın kodlarını okumak için)
    implementation("org.smali:dexlib2:2.5.2")
    implementation("org.smali:baksmali:2.5.2")
    
    // AXML Parser (Android Binary XML)
    implementation("net.sf.kxml:kxml2:2.3.0")
    
    // ZIP/Archive işlemleri
    implementation("org.zeroturnaround:zt-zip:1.15")
    
    // File Picker
    implementation("androidx.documentfile:documentfile:1.0.1")
    
    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
