package com.example.characteranimation

import android.util.Log
import org.json.JSONObject

class EmotionData {
    
    var valence: Float = 0.3f
        set(value) {
            field = value.coerceIn(-1f, 1f)
        }
    
    var arousal: Float = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }
    
    fun parseJson(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            valence = json.optDouble("valence", 0.3).toFloat()
            arousal = json.optDouble("arousal", 0.5).toFloat()
        } catch (e: Exception) {
            Log.e("EmotionData", "Failed to parse JSON", e)
        }
    }
    
    fun toJson(): String {
        return """{"valence":$valence,"arousal":$arousal}"""
    }
}
