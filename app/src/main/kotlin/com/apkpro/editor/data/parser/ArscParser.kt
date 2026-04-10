package com.apkpro.editor.data.parser

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ArscParser {
    
    fun parse(inputStream: InputStream): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val buffer = ByteBuffer.wrap(inputStream.readBytes())
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        
        try {
            // ARSC header
            val type = buffer.getShort()
            val headerSize = buffer.getShort()
            val fileSize = buffer.getInt()
            
            if (type.toInt() != 0x0002) return result // Not ARSC
            
            // Skip to string pool
            buffer.position(headerSize.toInt())
            
            // Parse string pool
            val strings = parseStringPool(buffer)
            
            // Resource mapping
            for ((index, string) in strings.withIndex()) {
                result["string_$index"] = string
            }
            
        } catch (e: Exception) {
            // Parse hatası
        }
        
        return result
    }
    
    private fun parseStringPool(buffer: ByteBuffer): List<String> {
        val strings = mutableListOf<String>()
        
        try {
            val startPos = buffer.position()
            val type = buffer.getShort()
            val headerSize = buffer.getShort()
            val size = buffer.getInt()
            val stringCount = buffer.getInt()
            val styleCount = buffer.getInt()
            val flags = buffer.getInt()
            val stringsStart = buffer.getInt()
            val stylesStart = buffer.getInt()
            
            // String offsets
            val stringOffsets = IntArray(stringCount) { buffer.getInt() }
            
            // Read strings
            for (offset in stringOffsets) {
                buffer.position(startPos + stringsStart + offset)
                val string = readString(buffer, flags)
                strings.add(string)
            }
            
        } catch (e: Exception) {
            // String pool parse hatası
        }
        
        return strings
    }
    
    private fun readString(buffer: ByteBuffer, flags: Int): String {
        return try {
            val utf8 = (flags and 0x100) != 0
            
            if (utf8) {
                // UTF-8 format
                val byteLen = buffer.get()
                val charLen = buffer.get()
                val bytes = ByteArray(byteLen.toInt())
                buffer.get(bytes)
                String(bytes, Charsets.UTF_8)
            } else {
                // UTF-16 format
                val length = buffer.getShort().toInt()
                val chars = CharArray(length)
                for (i in 0 until length) {
                    chars[i] = buffer.getShort().toChar()
                }
                String(chars)
            }
        } catch (e: Exception) {
            ""
        }
    }
}
