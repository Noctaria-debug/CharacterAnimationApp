package com.example.characteranimation

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var characterView: CharacterView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        characterView = CharacterView(this)
        val container = findViewById<FrameLayout>(android.R.id.content)
        container.addView(characterView)
    }
    
    override fun onResume() {
        super.onResume()
        characterView.resume()
    }
    
    override fun onPause() {
        super.onPause()
        characterView.pause()
    }
}
