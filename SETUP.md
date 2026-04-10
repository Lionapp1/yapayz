# Kurulum Rehberi

## Gradle Wrapper Kurulumu

GitHub'a yükleyeceğinizde gradle wrapper dosyalarını eklemelisiniz:

```bash
# Terminalde proje dizinine gidin
cd /home/emre/İndirilenler/yapayz

# Gradle wrapper'ı indirin
gradle wrapper --gradle-version 8.4

# Veya wrapper'ı oluşturun
./gradlew wrapper --gradle-version 8.4
```

Eğer gradle yüklü değilse:

```bash
# Linux/Mac
curl -L https://services.gradle.org/distributions/gradle-8.4-bin.zip -o gradle.zip
unzip gradle.zip
export PATH=$PATH:$(pwd)/gradle-8.4/bin

# Sonra wrapper oluştur
gradle wrapper --gradle-version 8.4
```

## GitHub'a Yükleme

```bash
# Git init
git init

# Tüm dosyaları ekle
git add .

# İlk commit
git commit -m "feat: Initial commit - Turkish AI Chat app"

# GitHub repo oluştur (tarayıcıdan veya gh CLI ile)
# Sonra remote ekle
git remote add origin https://github.com/KULLANICI_ADI/turkce-ai-chat.git

# Push
git branch -M main
git push -u origin main
```

## GitHub Secrets Ayarları

Release imzalamak için GitHub'da şu secrets'ları ekleyin:

- `KEYSTORE_PASSWORD`: Keystore şifresi
- `KEY_PASSWORD`: Key şifresi
- `KEY_ALIAS`: Key alias

## API Anahtarı

Hugging Face API anahtarı için:
1. https://huggingface.co/settings/tokens adresine gidin
2. Yeni token oluşturun
3. Uygulamaya ekleyin (opsiyonel, limitsiz kullanım için)
