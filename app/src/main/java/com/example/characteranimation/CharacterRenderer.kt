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
    private val blinkInterval = 3f // 3秒ごとに瞬き
    private var isBlinking = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()

    // === パーツ ===
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

        // 瞬きは0.2秒間
        if (isBlinking && blinkTimer > 0.2f) {
            isBlinking = false
        }
    }

    fun draw(canvas: Canvas, width: Int, height: Int) {
        val centerX = width / 2f
        val centerY = height / 2f

        // 画面に合わせた全体スケール
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
        val headRotation = headTilt + headShake

        // === 体 ===
        drawBitmapCentered(
            canvas = canvas,
            bitmap = body,
            x = centerX,
            y = centerY + 100f + bodyOffsetY,
            scale = baseScale,
            rotation = 0f
        )

        // === 腕（左右逆位相）===
        drawBitmapCentered(
            canvas = canvas,
            bitmap = armLeft,
            x = centerX - 80f * baseScale,
            y = centerY + 50f + bodyOffsetY,
            scale = baseScale * 0.8f,
            rotation = armAngle
        )
        drawBitmapCentered(
            canvas = canvas,
            bitmap = armRight,
            x = centerX + 80f * baseScale,
            y = centerY + 50f + bodyOffsetY,
            scale = baseScale * 0.8f,
            rotation = -armAngle
        )

        // === 頭グループ（首支点でまとめて回す）===
        // head の首支点（画像内の比率）
        val headPivotXR = 0.5f
        val headPivotYR = 0.90f  // ← ここが「だいたい90%」の指定

        val headX = centerX
        val headY = centerY - 100f + bodyOffsetY

        drawHeadGroup(
            canvas = canvas,
            headX = headX,
            headY = headY,
            baseScale = baseScale,
            rotation = headRotation,
            headPivotXR = headPivotXR,
            headPivotYR = headPivotYR
        )
    }

    /**
     * 頭 + 目 + 口 を「首支点」で1つの剛体として回転させる
     */
    private fun drawHeadGroup(
        canvas: Canvas,
        headX: Float,
        headY: Float,
        baseScale: Float,
        rotation: Float,
        headPivotXR: Float,
        headPivotYR: Float
    ) {
        val eyeLeft = if (isBlinking) eyeLeftClosed else eyeLeftOpen
        val eyeRight = if (isBlinking) eyeRightClosed else eyeRightOpen
        val mouth = if (emotionData.arousal > 0.6f) mouthOpen else mouthClosed

        // 目・口のローカル配置（「頭の基準点＝center」からのオフセットをそのまま使う）
        // 元コードの world座標: (centerX ± 30*scale, centerY -110 + bodyOffsetY)
        // 頭は (centerX, centerY -100 + bodyOffsetY) なので差分は (-30, -10) / (+30, -10)
        val eyeOffsetLeftX = -30f
        val eyeOffsetRightX = +30f
        val eyeOffsetY = -10f
        val mouthOffsetY = +30f

        canvas.save()
        // 首支点をワールド座標に合わせる
        canvas.translate(headX, headY)
        canvas.rotate(rotation)
        canvas.scale(baseScale, baseScale)

        // head を「首支点が(0,0)に来る」ように描画
        val headPivotX = head.width * headPivotXR
        val headPivotY = head.height * headPivotYR
        canvas.drawBitmap(head, -headPivotX, -headPivotY, paint)

        // 左目
        canvas.save()
        canvas.translate(eyeOffsetLeftX, eyeOffsetY)
        canvas.scale(0.5f, 0.5f)
        canvas.drawBitmap(eyeLeft, -eyeLeft.width / 2f, -eyeLeft.height / 2f, paint)
        canvas.restore()

        // 右目
        canvas.save()
        canvas.translate(eyeOffsetRightX, eyeOffsetY)
        canvas.scale(0.5f, 0.5f)
        canvas.drawBitmap(eyeRight, -eyeRight.width / 2f, -eyeRight.height / 2f, paint)
        canvas.restore()

        // 口
        canvas.save()
        canvas.translate(0f, mouthOffsetY)
        canvas.scale(0.6f, 0.6f)
        canvas.drawBitmap(mouth, -mouth.width / 2f, -mouth.height / 2f, paint)
        canvas.restore()

        canvas.restore()
    }

    /**
     * 画像中心を基準に回転・拡縮して描画（体・腕など用）
     */
    private fun drawBitmapCentered(
        canvas: Canvas,
        bitmap: Bitmap,
        x: Float,
        y: Float,
        scale: Float,
        rotation: Float
    ) {
        matrix.reset()
        matrix.postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
        matrix.postScale(scale, scale)
        matrix.postRotate(rotation)
        matrix.postTranslate(x, y)
        canvas.drawBitmap(bitmap, matrix, paint)
    }
}
