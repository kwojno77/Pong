package com.example.pong

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.room.Room
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception


class GameView(context: Context, attributeSet: AttributeSet) :
    SurfaceView(context, attributeSet), SurfaceHolder.Callback
{

    private var thread : GameThread
    private var ballX = 0f
    private var ballY = 0f
    private var SIZE = 50f
    private var dx = 10f
    private var dy = 10f
    private var player1bar : DefenceBar = DefenceBar()
    private var player2bar : DefenceBar = DefenceBar()
    private var player1score : Int = 0
    private var player2score : Int = 0
    private val scoreSize : Float = 300f

    private lateinit var red: Paint
    private lateinit var blue: Paint
    private lateinit var green: Paint
    private lateinit var yellow: Paint
    private lateinit var gray: Paint

    private lateinit var database: AppDatabase

    init {
        holder.addCallback(this)
        colorsInit()
        thread = GameThread(holder, this)
    }

    private fun prepareGame() {
        player1bar.posX = width/2f - player1bar.barLength/2f
        player1bar.posY = height - 50f

        player2bar.posX = width/2f - player2bar.barLength/2f
        player2bar.posY = 60f

        ballX = width/2f - (SIZE/2)
        ballY = height/2f - (SIZE/2)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        GlobalScope.launch {
            try {
                database = Room.databaseBuilder(
                        context,
                        AppDatabase::class.java,
                        "score.db"
                ).build()
                val save = database.scoreDAO().getAll()
                if (save.size == 2) {
                    if (save.first().playerId == 1) {
                        player1score = save.first().score
                        player2score = save.last().score
                    } else {
                        player2score = save.first().score
                        player1score = save.last().score
                    }
                }
            } catch (e: Exception) {
                Log.d("Pong2021", e.message.toString())
            }
        }
        if (ballY == 0f)
            prepareGame()
        thread = GameThread(holder, this)
        thread.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        thread.running = false
        thread.join()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        if(canvas == null) return

        val textPosX = width / 2f - scoreSize / 4f
        val textPosXshift = width / 2f - scoreSize / 2f
        if (player1score < 10)
            canvas.drawText(player1score.toString(), textPosX, height * 3f / 4f, gray)
        else
            canvas.drawText(player1score.toString(), textPosXshift, height * 3f / 4f, gray)
        canvas.save()
        canvas.rotate(180f, width / 2f, height / 2f)
        if (player2score < 10)
            canvas.drawText(player2score.toString(), textPosX, height * 3f / 4f, gray)
        else
            canvas.drawText(player2score.toString(), textPosXshift, height * 3f / 4f, gray)
        canvas.restore()

        canvas.drawRect(player1bar.posX, player1bar.posY + 10f, player1bar.posX + player1bar.barLength, player1bar.posY, green)
        canvas.drawRect(player1bar.posX + 2f, player1bar.posY + 8f, player1bar.posX + player1bar.barLength - 2f, player1bar.posY + 2f, blue)

        canvas.drawRect(player2bar.posX, player2bar.posY + 10f, player2bar.posX + player2bar.barLength, player2bar.posY, green)
        canvas.drawRect(player2bar.posX + 2f, player2bar.posY + 8f, player2bar.posX + player2bar.barLength - 2f, player2bar.posY + 2f, blue)

        canvas.drawLine(0.0f, height / 2.0f, width * 1.0f, height / 2.0f, blue)

        canvas.drawOval(ballX, ballY, ballX + SIZE, ballY + SIZE, yellow)
        canvas.drawOval(ballX + 4f, ballY + 4f, ballX + SIZE - 4f, ballY + SIZE - 4f, red)

    }

    fun update() {
        ballX += dx
        ballY += dy

        if (ballX <= 0 || ballX+SIZE >= width) {
            dx = -dx
        } else if (dy > 0) {
            if (player1bar.posY - (ballY+SIZE+dy/2f) <= 0f &&
                    ballX+SIZE/2 >= (player1bar.posX + dx) &&
                    ballX+SIZE/2 <= (player1bar.posX + dx + player1bar.barLength)) {
                dy = -dy
            }
            else if (ballY+SIZE >= player1bar.posY+10f) {
                goal(2)
            }
        } else {
            if ((ballY+dy/2f) - player2bar.posY-10f <= 0f &&
                    ballX+SIZE/2 >= (player2bar.posX + dx) &&
                    ballX+SIZE/2 <= (player2bar.posX + dx + player2bar.barLength)) {
                dy = -dy
            }
            else if (ballY <= player2bar.posY) {
                goal(1)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerCount = event.pointerCount

        for (i in 0 until pointerCount) {

            val touchedX = event.getX(i).toInt()
            val touchedY = event.getY(i).toInt()

            if (touchedY > height / 2f) {
                player1bar.posX = touchedX - (player1bar.barLength/2)
            } else {
                player2bar.posX = touchedX - (player2bar.barLength/2)
            }
        }
        return true
    }

    private fun goal(playerInd: Int) {
        ballX = width/2f - (SIZE/2)
        ballY = height/2f - (SIZE/2)
        dy = -dy
        lateinit var saveGoal: Score
        GlobalScope.launch {
            if (playerInd == 1) {
                player1score++
                saveGoal = Score(1, player1score, 1)
            } else if (playerInd == 2) {
                player2score++
                saveGoal = Score(2, player2score, 2)
            }
            Log.i("Pong2021", "-------------------------------------------------------------------------------------------")
            //Log.i("Pong2021", "BEFORE123: ${database.scoreDAO().getAll()[123].playerId}: ${database.scoreDAO().getAll()[123].score}")
            for (i in database.scoreDAO().getAll()) {
                Log.i("Pong2021", "BEFORE: ID(${i.id}), PlayerID(${i.playerId}), score(${i.score})")
            }
            if (database.scoreDAO().getAll().isEmpty())
                database.scoreDAO().insert(saveGoal)
            else if (database.scoreDAO().getAll().size < 2 && saveGoal.playerId != database.scoreDAO().getAll().first().playerId)
                database.scoreDAO().insert(saveGoal)
            else
                database.scoreDAO().updateScore(saveGoal)
            for (i in database.scoreDAO().getAll()) {
                Log.i("Pong2021", "AFTER: ID(${i.id}), PlayerID(${i.playerId}), score(${i.score})")
            }
            //Log.i("Pong2021", "AFTER124: ${database.scoreDAO().getAll()[124].playerId}: ${database.scoreDAO().getAll()[124].score}")
        }
    }

    private fun colorsInit() {
        red = Paint().apply {
            color = Color.RED
        }
        blue = Paint().apply {
            color = Color.BLUE
        }
        green = Paint().apply {
            color = Color.GREEN
        }
        yellow = Paint().apply {
            color = Color.YELLOW
        }
        gray = Paint().apply {
            color = Color.GRAY
        }
        gray.textSize = scoreSize
    }

}