# Türkçe AI Sohbet 🤖🇹🇷

Kotlin ve Jetpack Compose ile geliştirilmiş, Türkçe konuşan modern yapay zeka sohbet uygulaması.

[![Android CI](https://github.com/{username}/turkce-ai-chat/actions/workflows/android.yml/badge.svg)](https://github.com/{username}/turkce-ai-chat/actions/workflows/android.yml)

## Özellikler ✨

- **Türkçe Sohbet**: Yapay zeka ile Türkçe dilinde doğal sohbet
- **Modern UI**: Jetpack Compose ile tasarlanmış modern arayüz
- **Hugging Face Entegrasyonu**: DeepSeek LLM modeli desteği
- **Offline Destek**: Mesaj geçmişi yerel olarak saklanır
- **GitHub Actions CI/CD**: Otomatik build, test ve deploy

## Teknolojiler 🛠️

| Bileşen | Teknoloji |
|---------|-----------|
| Dil | Kotlin |
| UI Framework | Jetpack Compose |
| Architecture | MVVM |
| Network | Ktor Client |
| JSON | kotlinx.serialization |
| CI/CD | GitHub Actions |

## Ekran Görüntüleri 📱

*(Ekran görüntüleri buraya eklenecek)*

## Kurulum 🚀

### Gereksinimler
- Android Studio Hedgehog veya sonrası
- JDK 17+
- Android SDK 34

### Yerel Kurulum

```bash
# Repoyu klonlayın
git clone https://github.com/{username}/turkce-ai-chat.git
cd turkce-ai-chat

# APK'yı derleyin
./gradlew assembleDebug

# Testleri çalıştırın
./gradlew testDebugUnitTest
```

### API Anahtarı (Opsiyonel)

Uygulama varsayılan olarak Hugging Face Inference API'yi kullanır. Rate limit aşımını önlemek için kendi API anahtarınızı ekleyebilirsiniz:

1. [Hugging Face](https://huggingface.co/settings/tokens) hesabınızdan token alın
2. Uygulama içinde API anahtarı alanına girin

## Proje Yapısı 📁

```
app/src/main/kotlin/com/turkceai/chat/
├── data/
│   ├── model/
│   │   └── Message.kt          # Veri modelleri
│   └── repository/
│       └── ChatRepository.kt   # API iletişimi
├── ui/
│   ├── screens/
│   │   └── ChatScreen.kt       # Ana sohbet ekranı
│   ├── theme/
│   │   ├── Color.kt            # Renk tanımları
│   │   ├── Theme.kt            # Tema yapılandırması
│   │   └── Type.kt             # Tipografi
│   └── viewmodel/
│       └── ChatViewModel.kt    # İş mantığı
├── MainActivity.kt             # Giriş noktası
└── TurkceAIApp.kt              # Application sınıfı
```

## GitHub Actions 🔄

Proje, her push ve PR'da otomatik olarak:

1. **Lint Kontrolü**: Kod kalitesi kontrolü
2. **Unit Testler**: Testlerin çalıştırılması
3. **Debug APK**: Debug sürüm oluşturma
4. **Release APK**: Release sürüm oluşturma (main branch)
5. **GitHub Release**: Otomatik release yayınlama

## Katkıda Bulunma 🤝

1. Fork yapın
2. Feature branch oluşturun (`git checkout -b feature/amazing-feature`)
3. Değişikliklerinizi commit edin (`git commit -m 'feat: Add amazing feature'`)
4. Push edin (`git push origin feature/amazing-feature`)
5. Pull Request açın

## Lisans 📄

```
Copyright 2024 Türkçe AI Chat

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Teşekkürler 🙏

- [DeepSeek AI](https://github.com/deepseek-ai) - LLM modeli
- [Hugging Face](https://huggingface.co) - API altyapısı
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - UI framework

---

**Not**: Bu uygulama eğitim amaçlıdır. Üretim kullanımı için kendi API anahtarınızı kullanın.
