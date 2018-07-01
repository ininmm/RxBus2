package com.github.ininmm.rxbus2

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.github.ininmm.library.RxBus
import com.github.ininmm.library.annotation.Produce
import com.github.ininmm.library.annotation.TagModel
import com.github.ininmm.library.thread.EventThread
import kotlinx.android.synthetic.main.activity_main.*
import android.widget.Toast
import com.github.ininmm.library.annotation.Subscribe


class MainActivity : AppCompatActivity() {
    private lateinit var news: String
    private val sendByProduce = ArrayList<String>()

    companion object {
        private const val SAMPLE_STICK = "sampleStick"
        private const val SAMPLE_POST = "samplePost"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        RxBus.getBus().register(this)
        postSomeThing()

        postBtn.setOnClickListener {
            Log.e(this::class.java.simpleName, "postClick!")
            postDataStick()

            val intent = Intent(this, SecondActivity::class.java)
            startActivity(intent)
        }
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

    /**
     * The sample to a simply post and subscribe, you should register and unregister
     * when you use the [Produce] and [Subscribe] annotation.
     *
     * <p>
     *     When you want to use RxBus in the same activity/fragment, you should register and unregister
     *     in the lifecycle first, post something like below.
     *
     *     Then write function like [simpleSubscribe], add the [Subscribe] annotation, and receive the value.
     *     </p>
     */
    private fun postSomeThing() {
        news = "simpleSubscribe"
        RxBus.getBus().post(tag = SAMPLE_POST, event = news)
    }

    private fun postDataStick() {
        sendByProduce.add("123")
        sendByProduce.add("456")
    }

    @Produce(thread = EventThread.IO, tags = [TagModel.Tag(SAMPLE_STICK)])
    fun produceSample(): ArrayList<String> {
        return sendByProduce
    }

    @Subscribe(thread = EventThread.MainThread, tags = [TagModel.Tag(SAMPLE_POST)])
    fun simpleSubscribe(news: String) {
        Log.e(this::class.java.simpleName, news)
        Toast.makeText(this, news, Toast.LENGTH_SHORT).show()
    }
}
