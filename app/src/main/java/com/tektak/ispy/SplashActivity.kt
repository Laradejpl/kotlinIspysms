package com.tektak.ispy

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        progressBar = findViewById(R.id.progress_bar)
        progressBar.max = 10
        val currentProgress = 6

        ObjectAnimator.ofInt(progressBar, "progress", currentProgress)
            .setDuration(2000)
            .start()


        // Utiliser un Handler pour retarder le lancement de l'activité principale
        Handler(Looper.getMainLooper()).postDelayed({
            // Passer à l'activité principale
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Terminer l'activité splash pour qu'elle ne soit pas en arrière-plan
        }, 3000) // 3000 millisecondes = 3 secondes
    }
}