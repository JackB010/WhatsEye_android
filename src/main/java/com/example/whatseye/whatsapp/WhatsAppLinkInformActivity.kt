package com.example.whatseye.whatsapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.example.whatseye.R

class WhatsAppLinkInformActivity : AppCompatActivity() {
    private lateinit var colledBy:String
    private lateinit var act:Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whatsapp_link_inform)
        colledBy = intent.getStringExtra("intent").toString()

        val tryLinkAgainButton = findViewById<Button>(R.id.tryLinkAgainButton)

        tryLinkAgainButton.setOnClickListener {
            if(colledBy=="WhatsAppLinkActivity")
                act = Intent(this, WhatsAppLinkActivity::class.java)
            else
                act = Intent(this, WhatsAppLink2Activity::class.java)

            act.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the stack
            startActivity(act)
            finish() // Close this activity
        }
    }


}