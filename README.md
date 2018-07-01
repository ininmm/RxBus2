RxBus2
=======
This is a RxBus using by RxJava2

## Usage

You also need to register and unregister before post something.

It is recommended to register in onCreate and unregister in onDestroy:
```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        // ...
        RxBus.getBus().register(this)

    }
    
    override fun onDestroy() {
        // ...
        RxBus.getBus().unregister(this)
    }
```

You can post an event like this:
```kotlin
    private fun postSomeThing() {
        val news = "simpleSubscribe"
        RxBus.getBus().post(tag = SAMPLE_POST, event = news)
    }
```

And use @[Subscribe](/library/src/main/java/com/github/ininmm/library/annotation/Subscribe.kt) to receive event:
```kotlin
    @Subscribe(thread = EventThread.MainThread, tags = [TagModel.Tag(SAMPLE_POST)])
    fun simpleSubscribe(news: String) {
        Toast.makeText(this, news, Toast.LENGTH_SHORT).show()
    }
```

If you want to post a sticky event, you should use @[Produce](/library/src/main/java/com/github/ininmm/library/annotation/Produce.kt) to post an event:
```kotlin
    private fun postDataStick() {
        sendByProduce.add("123")
        sendByProduce.add("456")
    }
    
    @Produce(thread = EventThread.IO, tags = [TagModel.Tag(SAMPLE_STICK)])
    fun produceSample(): ArrayList<String> {
        return sendByProduce
    }
```
then receive event by @[Subscribe](/library/src/main/java/com/github/ininmm/library/annotation/Subscribe.kt).
