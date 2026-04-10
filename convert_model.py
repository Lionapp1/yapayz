#!/usr/bin/env python3
"""
Universal Sentence Encoder - TensorFlow Lite Converter
Google'ın multilingual USE modelini TFLite'a dönüştürür
"""

import os
import sys

def convert_use_model():
    """USE modelini TFLite'a dönüştür"""
    
    model_path = "app/src/main/assets/models"
    
    # Kontrol et
    if not os.path.exists(f"{model_path}/saved_model.pb"):
        print("❌ Hata: saved_model.pb bulunamadı!")
        print(f"   Aranan: {model_path}/saved_model.pb")
        return False
    
    print("📦 TensorFlow yükleniyor...")
    try:
        import tensorflow as tf
        print(f"✅ TensorFlow {tf.__version__} yüklendi")
    except ImportError:
        print("❌ TensorFlow kurulu değil! Kurulum yapılıyor...")
        os.system("pip3 install tensorflow --quiet --break-system-packages")
        import tensorflow as tf
        print(f"✅ TensorFlow {tf.__version__} yüklendi")
    
    print("\n🔄 Model dönüştürülüyor...")
    print("   Kaynak: SavedModel")
    print("   Hedef: TFLite")
    
    try:
        # Converter oluştur
        converter = tf.lite.TFLiteConverter.from_saved_model(model_path)
        
        # Optimizasyon ayarları
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_types = [tf.float32]
        
        # Dönüştür
        tflite_model = converter.convert()
        
        # Kaydet
        output_path = f"{model_path}/use_multilingual_v2.tflite"
        with open(output_path, 'wb') as f:
            f.write(tflite_model)
        
        size_mb = len(tflite_model) / (1024 * 1024)
        print(f"\n✅ Başarılı! Model dönüştürüldü")
        print(f"   Çıktı: {output_path}")
        print(f"   Boyut: {size_mb:.1f} MB")
        print(f"   Byte: {len(tflite_model):,}")
        
        return True
        
    except Exception as e:
        print(f"\n❌ Dönüşüm hatası: {e}")
        print("\n💡 Alternatif çözüm:")
        print("   1. Google Colab'de dönüştürün")
        print("   2. TensorFlow Hub'dan hazır TFLite indirin")
        print("   3. Modeli kaldırıp basit template modu kullanın")
        return False

if __name__ == "__main__":
    print("=" * 60)
    print("  Universal Sentence Encoder - TFLite Converter")
    print("=" * 60)
    
    success = convert_use_model()
    
    print("\n" + "=" * 60)
    if success:
        print("✅ Dönüşüm tamamlandı!")
        sys.exit(0)
    else:
        print("❌ Dönüşüm başarısız!")
        sys.exit(1)
