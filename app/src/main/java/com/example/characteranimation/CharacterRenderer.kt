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
    private val blinkInterval = 3f  // 3秒ごとに瞬き
    private var isBlinking = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()

    // パーツ
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

        // 瞬きタイマー
        blinkTimer += deltaTime
        if (blinkTimer >= blinkInterval) {
            blinkTimer = 0f
            isBlinking = true
        }

        // 瞬きは0.2秒間
        if (isBlinking && blinkTimer > 0.2f) {
            isBlinking = false
        }
    }

    fun draw(canvas: Canvas, width: Int, height: Int) {
        val centerX = width / 2f
        val centerY = height / 2f

        // 全体スケール（画面に合わせて）
        val baseScale = (width / 500f).coerceAtMost(height / 700f)

        // 体の上下揺れ（arousalで振幅と速度変化）
        val bounceAmplitude = 10f * emotionData.arousal
        val bounceSpeed = 2f + emotionData.arousal * 2f
        val bodyOffsetY = sin(elapsedTime * bounceSpeed) * bounceAmplitude

        // 腕のスイング（arousalで変化）
        val armSwingAmplitude = 15f * emotionData.arousal
        val armSwingSpeed = 1.5f + emotionData.arousal
        val armAngle = sin(elapsedTime * armSwingSpeed) * armSwingAmplitude

        // 頭の傾き（valenceで傾き方向）
        val headTilt = emotionData.valence * 10f
        val headShake = sin(elapsedTime * 1.5f) * 3f

        // ====== サイズ設計（「枠に合わせる」方式） ======
        // ここを変えるだけで、差し替え画像の見た目サイズが揃う
        val bodyTargetW = 260f * baseScale   // 胴体の表示幅
        val headTargetW = 220f * baseScale   // 頭の表示幅（今は大きい→小さくするなら 180〜200）
        val armTargetW  = 170f * baseScale   // 腕の表示幅
        val eyeTargetW  = 60f  * baseScale   // 目の表示幅
        val mouthTargetW = 90f * baseScale   // 口の表示幅

        // 体
        drawBitmapFitWidth(canvas, body, centerX, centerY + 120f + bodyOffsetY, bodyTargetW, 0f)

        // 腕（左右逆位相）
        drawBitmapFitWidth(canvas, armLeft, centerX - 95f * baseScale, centerY + 70f + bodyOffsetY, armTargetW, armAngle)
        drawBitmapFitWidth(canvas, armRight, centerX + 95f * baseScale, centerY + 70f + bodyOffsetY, armTargetW, -armAngle)

        // 頭
        drawBitmapFitWidth(canvas, head, centerX, centerY - 80f + bodyOffsetY, headTargetW, headTilt + headShake)

        // 目（瞬き）
        val eyeLeft = if (isBlinking) eyeLeftClosed else eyeLeftOpen
        val eyeRight = if (isBlinking) eyeRightClosed else eyeRightOpen
        drawBitmapFitWidth(canvas, eyeLeft, centerX - 38f * baseScale, centerY - 92f + bodyOffsetY, eyeTargetW, headTilt + headShake)
        drawBitmapFitWidth(canvas, eyeRight, centerX + 38f * baseScale, centerY - 92f + bodyOffsetY, eyeTargetW, headTilt + headShake)

        // 口（arousalで開閉）
        val mouth = if (emotionData.arousal > 0.6f) mouthOpen else mouthClosed
        drawBitmapFitWidth(canvas, mouth, centerX, centerY - 55f + bodyOffsetY, mouthTargetW, headTilt + headShake)
    }

    /**
     * bitmap を「指定した表示幅 targetW」にフィットさせて描画する。
     * 画像サイズがバラバラでも、見た目を揃えやすい。
     */
    private fun drawBitmapFitWidth(
        canvas: Canvas,
        bitmap: Bitmap,
        x: Float,
        y: Float,
        targetW: Float,
        rotation: Float
    ) {
        val bw = bitmap.width.toFloat().coerceAtLeast(1f)
        val scale = targetW / bw
        drawBitmap(canvas, bitmap, x, y, scale, rotation)
    }

    private fun drawBitmap(canvas: Canvas, bitmap: Bitmap, x: Float, y: Float, scale: Float, rotation: Float) {
        matrix.reset()
        matrix.postScale(scale, scale)
        matrix.postRotate(rotation)
        matrix.postTranslate(
            x - bitmap.width * scale / 2f,
            y - bitmap.height * scale / 2f
        )
        canvas.drawBitmap(bitmap, matrix, paint)
    }
}
