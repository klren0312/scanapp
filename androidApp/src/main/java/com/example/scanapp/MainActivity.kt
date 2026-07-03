package com.example.scanapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.scanapp.database.AndroidDatabaseDriver

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化数据库驱动
        AndroidDatabaseDriver.initialize(applicationContext)
        // Kuikly will initialize here
    }
}