# 🔥 Colab'de Model Dönüştürme Rehberi

## Adım 1: Colab'i Aç

1. **Tarayıcıya git:** https://colab.research.google.com/
2. **"Yeni not defteri"** butonuna tıkla (sol üst)

## Adım 2: Model Dosyasını Yükle

```
Sol tarafta 📁 ikonuna tıkla → "Dosya yükle" → universal-sentence-encoder-tensorflow2-multilingual-v2.tar.gz
```

## Adım 3: Aşağıdaki Kodu Yapıştır ve Çalıştır

Her hücreye sırayla tıkla ve ▶️ butonuna bas:

```python
# HÜCRE 1: TensorFlow Kur
!pip install -q tensorflow==2.14.0
print("✅ TensorFlow kuruldu!")
```

```python
# HÜCRE 2: Modeli Çıkar
tar -xzf universal-sentence-encoder-tensorflow2-multilingual-v2.tar.gz
print("✅ Model çıkarıldı!")
```

```python
# HÜCRE 3: Dönüştür
import tensorflow as tf

converter = tf.lite.TFLiteConverter.from_saved_model("saved_model")
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

# Kaydet
with open('use_multilingual_v2.tflite', 'wb') as f:
    f.write(tflite_model)

print(f"✅ Dönüştürüldü! Boyut: {len(tflite_model)/(1024*1024):.1f} MB")
```

## Adım 4: İndir

```python
# HÜCRE 4: İndirme linki oluştur
from google.colab import files
files.download('use_multilingual_v2.tflite')
```

Dosya otomatik indirilecek!

## Adım 5: Projeye Ekle

```bash
# Bilgisayarında terminal/cmd aç
cd İndirilenler  # veya dosyanın olduğu yer

# GitHub reposuna kopyala
cp use_multilingual_v2.tflite /home/emre/İndirilenler/yapayz/app/src/main/assets/models/

# GitHub'a gönder
cd /home/emre/İndirilenler/yapayz
git add app/src/main/assets/models/use_multilingual_v2.tflite
git commit -m "Add USE multilingual model"
git push origin main
```

## Alternatif: Hazır Model Linki

Eğer kendi modelini dönüştüremezsen, **ben senin yerine yaparım**:

1. Model dosyasını (`universal-sentence-encoder-tensorflow2-multilingual-v2.tar.gz`) 
   **Google Drive'a** yükle
2. **Paylaşım linkini** bana ver
3. Ben dönüştürüp **doğrudan GitHub'a** eklerim

## Sonuç

Model eklendikten sonra:
- ✅ **Anlamsal benzerlik** çalışacak
- ✅ **"Merhaba nasılsın"** yazınca selamlaşma yanıtı verecek
- ✅ **Daha akıllı cevaplar** üretecek
- ✅ **Hâlâ internet gerektirmiyor!**

## Yardım Lazım mı?

**Bana yaz:** Model dosyanı Google Drive'a yükle, paylaşım linkini ver, gerisini ben hallederim! 🚀
