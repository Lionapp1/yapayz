#!/bin/bash
# GitHub'a yükleme scripti

echo "🚀 Türkçe AI - GitHub Push Scripti"
echo "=================================="

# Gradle wrapper jar kontrol
if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "📦 Gradle wrapper indiriliyor..."
    
    # Wrapper properties'den URL al
    GRADLE_VERSION=$(grep "distributionUrl" gradle/wrapper/gradle-wrapper.properties | sed 's/.*gradle-\(.*\)-bin.zip/\1/')
    echo "Gradle versiyonu: $GRADLE_VERSION"
    
    # Wrapper jar indir
    mkdir -p gradle/wrapper
    curl -L "https://raw.githubusercontent.com/gradle/gradle/v$GRADLE_VERSION.0/gradle/wrapper/gradle-wrapper.jar" \
        -o gradle/wrapper/gradle-wrapper.jar 2>/dev/null || \
    curl -L "https://github.com/gradle/gradle/raw/v$GRADLE_VERSION.0/gradle/wrapper/gradle-wrapper.jar" \
        -o gradle/wrapper/gradle-wrapper.jar
    
    echo "✅ Gradle wrapper indirildi"
fi

# Git repo init
echo "📝 Git başlatılıyor..."
git init

# Tüm dosyaları ekle
echo "📁 Dosyalar ekleniyor..."
git add .

# Commit
echo "💾 Commit yapılıyor..."
git commit -m "feat: Initial commit - Turkish AI Chat with Jetpack Compose

- Kotlin + Jetpack Compose modern UI
- HuggingFace DeepSeek LLM entegrasyonu  
- GitHub Actions CI/CD ile otomatik build
- MVVM mimarisi
- Türkçe yapay zeka sohbet"

# Remote ekle (Senin repoadresin)
echo "🔗 Remote ekleniyor..."
git remote add origin https://github.com/Lionapp1/yapayz.git 2>/dev/null || \
git remote set-url origin https://github.com/Lionapp1/yapayz.git

# Push
echo "☁️ GitHub'a gönderiliyor..."
git branch -M main
git push -u origin main --force

echo ""
echo "✅ TAMAMLANDI!"
echo "🔗 Repo: https://github.com/Lionapp1/yapayz"
echo ""
echo "GitHub Actions otomatik çalışacak ve APK build edecek."
echo "Actions sekmesinden takip edebilirsin."
