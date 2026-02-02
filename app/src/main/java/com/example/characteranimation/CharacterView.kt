package com.example.characteranimation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class CharacterView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    
    private val TAG = "CharacterView"
    
    private var renderThread: RenderThread? = null
    private val emotionData = EmotionData()
    private val assetLoader = AssetLoader(context)
    private val renderer = CharacterRenderer(emotionData, assetLoader)
    
    private val debugPaint = Paint().apply {
        color = context.getColor(R.color.debug_text)
        textSize = 32f
        isAntiAlias = true
    }
    
    init {
        holder.addCallback(this)
        
        try {
            val json = context.assets.open("model/emotion.json").bufferedReader().use { it.readText() }
            emotionData.parseJson(json)
            Log.i(TAG, "Loaded emotion: valence=${emotionData.valence}, arousal=${emotionData.arousal}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load emotion.json, using defaults", e)
        }
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.i(TAG, "Surface created")
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.i(TAG, "Surface changed: ${width}x${height}")
        resume()
    }
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.i(TAG, "Surface destroyed")
        pause()
    }
    
    fun resume() {
        if (renderThread == null || !renderThread!!.isRunning) {
            renderThread = RenderThread()
            renderThread?.start()
        }
    }
    
    fun pause() {
        renderThread?.stopRendering()
        renderThread = null
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y
            val centerX = width / 2f
            val centerY = height / 2f
            
            if (x < centerX) {
                emotionData.valence = (emotionData.valence - 0.1f).coerceIn(-1f, 1f)
            } else {
                emotionData.valence = (emotionData.valence + 0.1f).coerceIn(-1f, 1f)
            }
            
            if (y < centerY) {
                emotionData.arousal = (emotionData.arousal + 0.1f).coerceIn(0f, 1f)
            } else {
                emotionData.arousal = (emotionData.arousal - 0.1f).coerceIn(0f, 1f)
            }
            
            Log.d(TAG, "Touch: valence=${emotionData.valence}, arousal=${emotionData.arousal}")
            return true
        }
        return super.onTouchEvent(event)
    }
    
    private inner class RenderThread : Thread("RenderThread") {
        
        var isRunning = false
            private set
        
        override fun run() {
            isRunning = true
            var lastTime = System.nanoTime()
            val targetFps = 60
            val targetFrameTime = 1000000000L / targetFps
            
            while (isRunning) {
                val currentTime = System.nanoTime()
                val deltaTime = (currentTime - lastTime) / 1000000000f
                lastTime = currentTime
                
                val safeDeltaTime = deltaTime.coerceAtMost(0.1f)
                
                drawFrame(safeDeltaTime)
                
                val frameTime = System.nanoTime() - currentTime
                val sleepTime = (targetFrameTime - frameTime) / 1000000L
                if (sleepTime > 0) {
                    try {
                        sleep(sleepTime)
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            }
        }
        
        fun stopRendering() {
            isRunning = false
            interrupt()
            try {
                join(1000)
            } catch (e: InterruptedException) {
                Log.w(TAG, "RenderThread interrupted during join")
            }
        }
        
        private fun drawFrame(deltaTime: Float) {
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    synchronized(holder) {
                        canvas.drawColor(Color.BLACK)
                        
                        renderer.update(deltaTime)
                        renderer.draw(canvas, width, height)
                        
                        val infoText = context.getString(
                            R.string.emotion_label,
                            emotionData.valence,
                            emotionData.arousal
                        )
                        canvas.drawText(infoText, 20f, 50f, debugPaint)
                        canvas.drawText(
                            context.getString(R.string.tap_instruction),
                            20f,
                            90f,
                            debugPaint
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error drawing frame", e)
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error unlocking canvas", e)
                    }
                }
            }
        }
    }
}
