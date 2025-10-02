package com.innovape.hearuapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.innovape.hearuapp.databinding.ActivityMasukBinding

class MasukActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMasukBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMasukBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}