package com.innovape.hearuapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.innovape.hearuapp.databinding.ActivityDaftarBinding

class DaftarActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDaftarBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDaftarBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}