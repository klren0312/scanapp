package com.example.scanapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tencent.kuikly.render.core.KuiklyView
import com.tencent.kuikly.render.core.KuiklyViewDelegator

class KuiklyRenderActivity : AppCompatActivity() {
    
    private lateinit var kuiklyViewDelegator: KuiklyViewDelegator
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val pageName = intent.getStringExtra("pageName") ?: ""
        
        kuiklyViewDelegator = KuiklyViewDelegator(
            this,
            KuiklyView.CodeHandler(pageName)
        )
        
        setContentView(kuiklyViewDelegator.kuiklyView)
        
        kuiklyViewDelegator.loadPage(pageName)
    }
    
    companion object {
        fun start(activity: android.app.Activity, pageName: String) {
            val intent = android.content.Intent(activity, KuiklyRenderActivity::class.java)
            intent.putExtra("pageName", pageName)
            activity.startActivity(intent)
        }
    }
}
