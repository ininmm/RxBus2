package com.github.ininmm.rxbus2

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.github.ininmm.library.RxBus
import com.github.ininmm.library.annotation.Subscribe
import com.github.ininmm.library.annotation.TagModel
import com.github.ininmm.library.thread.EventThread
import kotlinx.android.synthetic.main.activity_second.*


class SecondActivity : AppCompatActivity() {

    companion object {
        private const val SAMPLE_STICK = "sampleStick"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
        RxBus.getBus().register(this)

    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        RxBus.getBus().unregister(this)
    }


    @Subscribe(thread = EventThread.MainThread, tags = [TagModel.Tag(SAMPLE_STICK)])
    fun subscribeProduce(words: ArrayList<String>) {
        Log.e(this::class.java.simpleName, words[0])
        postTextView.text = words[1]
        Toast.makeText(this, words[0], Toast.LENGTH_SHORT).show()
    }
}
