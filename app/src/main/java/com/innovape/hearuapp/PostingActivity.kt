package com.innovape.hearuapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.innovape.hearuapp.databinding.ActivityPostingBinding

class PostingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPostingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPostingBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}