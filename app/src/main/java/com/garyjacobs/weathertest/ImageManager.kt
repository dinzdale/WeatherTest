package com.garyjacobs.weathertest

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View

import android.widget.ImageView

import com.squareup.okhttp.ResponseBody
import io.reactivex.*

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.internal.operators.single.SingleToObservable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.Buffer
import java.nio.ByteBuffer
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

import javax.net.ssl.HttpsURLConnection

import network.GetImageData
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import retrofit.Call
import retrofit.Callback
import retrofit.GsonConverterFactory
import retrofit.Response
import retrofit.Retrofit


/**
 * Created by gjacobs on 11/5/15.
 */
class ImageManager(maxSize: Int) {
    private val fixedThreadPool: ExecutorService
    private final val memoryCache: MemoryCache

    init {
        memoryCache = MemoryCache.getInstance(maxSize)
        fixedThreadPool = Executors.newFixedThreadPool(10)
    }

    fun setImage(icon: String, image: ImageView) {
        val bitmap: Bitmap? = (memoryCache.get(icon))
        if (bitmap == null) {
            val url = "${image.resources.getString(R.string.openweathermap_base_url)}/img/w/$icon"
            getBitmapObservable(url, this::oldBitmapFetcher)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(BitMapObserver(image))

        } else {
            image.setImageBitmap(bitmap)
        }
    }

    fun kill() {
        fixedThreadPool.shutdownNow()
    }

    fun getBitmapObservable(url: String, bitmapFetcher: (String) -> Bitmap): Single<Pair<String, Bitmap>> {
        return Single.create { subscriber ->
            try {
                log("getBitmapObservable")
                subscriber.onSuccess(Pair(url, bitmapFetcher(url)))
            } catch (ex: Exception) {
                log("getBitmapObservable::error ${ex.message}")
                subscriber.onError(ex)
            }
        }
    }

    fun log(s:String) : Unit = println("${Thread.currentThread().name} : ${s}")

//fun getBitmapObservable(url: String, bitmapFetcher: (String) -> Bitmap): Observable<Pair<String, Bitmap?>> {
//
//
//    val observable: Observable<Pair<String, Bitmap?>> = Observable.create() { s ->
//
//        s.onNext(Pair(url, bitmapFetcher(url)))
//
//        s.onComplete()
//
//    }
////        observable.observeOn(Schedulers.newThread())
////                .subscribeOn(Schedulers.newThread())
//
//    return observable
//}


    inner class BitMapObserver(image: ImageView) : SingleObserver<Pair<String, Bitmap?>> {

        val image: ImageView = image

        override fun onSubscribe(d: Disposable?) {
            log("BitMapObserver::onSubscribe")
        }

        override fun onSuccess(pair: Pair<String, Bitmap?>?) {
            log("BitMapObserver::onSuccess")
            pair?.second?.let {
                memoryCache.set(pair.first, pair.second)
                image.setImageBitmap(pair.second)
            }
        }

        override fun onError(e: Throwable?) {
            log("BitMapObserver::onError")
            e!!.printStackTrace()
        }

    }

    @Throws(Exception::class)
    fun oldBitmapFetcher(url: String): Bitmap {
        log("oldBitmapFetcher")
        val httpURLConnection = URL(url).openConnection() as HttpURLConnection
        val inputStream = httpURLConnection.inputStream
        val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)
        return bitmap
    }


//    private fun loadImageViewOld(icon: String, image: ImageView) {
//
//
//        fixedThreadPool.execute {
//            try {
//                val bitmap: Bitmap
//                val baseURL = image.resources.getString(R.string.openweathermap_base_url)
//                val bmURL = URL(baseURL + "/img/w/" + icon)
//                val httpURLConnection = bmURL.openConnection() as HttpURLConnection
//                val inputStream = httpURLConnection.inputStream
//                bitmap = BitmapFactory.decodeStream(inputStream)
//                inputStream.close();
//                this@ImageManager.memoryCache.set(icon, bitmap)
//                image.post { image.setImageBitmap(bitmap) }
//
//            } catch (ioe: IOException) {
//                ioe.printStackTrace()
//            }
//        }
//    }


    /**
     * Using Retrofit to load images.
     * Probably best to use Picaso

     * @param icon
     * *
     * @param image
     */

    private fun loadImageViewNew(icon: String, image: ImageView) {
        val builder = Retrofit.Builder()
        builder.addConverterFactory(GsonConverterFactory.create())
        builder.baseUrl(image.resources.getString(R.string.openweathermap_base_url))
        val retrofit = builder.build()

        val getImageData = retrofit.create<GetImageData>(GetImageData::class.java!!)
        val call = getImageData.getIcon(icon, image.resources.getString(R.string.openweathermap_appid))

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(response: Response<ResponseBody>, retrofit: Retrofit) {
                try {
                    val bitmap = BitmapFactory.decodeStream(response.body().byteStream())
                    this@ImageManager.memoryCache.set(icon, bitmap)
                    image.setImageBitmap(bitmap)
                } catch (ioe: IOException) {

                }

            }

            override fun onFailure(throwable: Throwable) {

            }
        })

    }

}
