package com.example.characteranimation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.sin

class CharacterRenderer(
    private val emotionData: EmotionData,
    private val assetLoader: AssetLoader
) {
    
    private var elapsedTime = 0f
    private var blinkTimer = 0f
    private val blinkInterval = 3f
    private var isBlinking = false
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()
    
    private lateinit var body: Bitmap
    private lateinit var head: Bitmap
    private lateinit var armLeft: Bitmap
    private lateinit var armRight: Bitmap
    private lateinit var eyeLeftOpen: Bitmap
    private lateinit var eyeLeftClosed: Bitmap
    private lateinit var eyeRightOpen: Bitmap
    private lateinit var eyeRightClosed: Bitmap
    private lateinit var mouthOpen: Bitmap
    private lateinit var mouthClosed: Bitmap
    
    init {
        loadAssets()
    }
    
    private fun loadAssets() {
        body = assetLoader.loadBitmap("model/body.png", 0xFF8B7355.toInt())
        head = assetLoader.loadBitmap("model/head.png", 0xFFFFDBAC.toInt())
        armLeft = assetLoader.loadBitmap("model/arm.left.png", 0xFFCD853F.toInt())
        armRight = assetLoader.loadBitmap("model/arm.right.png", 0xFFCD853F.toInt())
        eyeLeftOpen = assetLoader.loadBitmap("model/eye.left.open.png", 0xFF000000.toInt())
        eyeLeftClosed = assetLoader.loadBitmap("model/eye.left.closed.png", 0xFF000000.toInt())
        eyeRightOpen = assetLoader.loadBitmap("model/eye.right.open.png", 0xFF000000.toInt())
        eyeRightClosed = assetLoader.loadBitmap("model/eye.right.closed.png", 0xFF000000.toInt())
        mouthOpen = assetLoader.loadBitmap("model/mouth.open.png", 0xFFFF6B6B.toInt())
        mouthClosed = assetLoader.loadBitmap("model/mouth.closed.png", 0xFFFF6B6B.toInt())
    }
    
    fun update(deltaTime: Float) {
        elapsedTime += deltaTime
        
        blinkTimer += deltaTime
        if (blinkTimer >= blinkInterval) {
            blinkTimer = 0f
            isBlinking = true
        }
        
        if (isBlinking && blinkTimer > 0.2f) {
            isBlinking = false
        }
    }
    
    fun draw(canvas: Canvas, width: Int, height: Int) {
        val centerX = width / 2f
        val centerY = height / 2f
        
        val scale = (width / 500f).coerceAtMost(height / 700f)
        
        val bounceAmplitude = 10f * emotionData.arousal
        val bounceSpeed = 2f + emotionData.arousal * 2f
        val bodyOffsetY = sin(elapsedTime * bounceSpeed) * bounceAmplitude
        
        val armSwingAmplitude = 15f * emotionData.arousal
        val armSwingSpeed = 1.5f + emotionData.arousal
        val armAngle = sin(elapsedTime * armSwingSpeed) * armSwingAmplitude
        
        val headTilt = emotionData.valence * 10f
        val headShake = sin(elapsedTime * 1.5f) * 3f
        
        drawBitmap(canvas, body, centerX, centerY + 100f + bodyOffsetY, scale, 0f)
        
        drawBitmap(canvas, armLeft, centerX - 80f * scale, centerY + 50f + bodyOffsetY, scale * 0.8f, armAngle)
        drawBitmap(canvas, armRight, centerX + 80f * scale, centerY + 50f + bodyOffsetY, scale * 0.8f, -armAngle)
        
        drawBitmap(canvas, head, centerX, centerY - 100f + bodyOffsetY, scale, headTilt + headShake)
        
        val eyeLeft = if (isBlinking) eyeLeftClosed else eyeLeftOpen
        val eyeRight = if (isBlinking) eyeRightClosed else eyeRightOpen
        drawBitmap(canvas, eyeLeft, centerX - 30f * scale, centerY - 110f + bodyOffsetY, scale * 0.5f, headTilt + headShake)
        drawBitmap(canvas, eyeRight, centerX + 30f * scale, centerY - 110f + bodyOffsetY, scale * 0.5f, headTilt + headShake)
        
        val mouth = if (emotionData.arousal > 0.6f) mouthOpen else mouthClosed
        drawBitmap(canvas, mouth, centerX, centerY - 70f + bodyOffsetY, scale * 0.6f, headTilt + headShake)
    }
    
    private fun drawBitmap(canvas: Canvas, bitmap: Bitmap, x: Float, y: Float, scale: Float, rotation: Float) {
        matrix.reset()
        matrix.postScale(scale, scale)
        matrix.postRotate(rotation)
        matrix.postTranslate(x - bitmap.width * scale / 2, y - bitmap.height * scale / 2)
        canvas.drawBitmap(bitmap, matrix, paint)
    }
}
