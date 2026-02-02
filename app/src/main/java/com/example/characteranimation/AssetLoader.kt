package com.example.characteranimation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log

class AssetLoader(private val context: Context) {
    
    private val TAG = "AssetLoader"
    private val cache = mutableMapOf<String, Bitmap?>()
    
    fun loadBitmap(assetPath: String, fallbackColor: Int): Bitmap {
        if (cache.containsKey(assetPath)) {
            val cached = cache[assetPath]
            if (cached != null) return cached
        }
        
        try {
            val inputStream = context.assets.open(assetPath)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (bitmap != null) {
                cache[assetPath] = bitmap
                Log.d(TAG, "Loaded: $assetPath (${bitmap.width}x${bitmap.height})")
                return bitmap
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load $assetPath: ${e.message}")
        }
        
        val fallback = createFallbackBitmap(fallbackColor)
        cache[assetPath] = fallback
        Log.i(TAG, "Using fallback for $assetPath")
        return fallback
    }
    
    private fun createFallbackBitmap(color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return bitmap
    }
    
    fun getBitmap(assetPath: String): Bitmap? {
        return cache[assetPath]
    }
}
