package com.garyjacobs.weathertest

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View

import android.widget.ImageView

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.util.Log
import android.util.LruCache
import io.reactivex.Single
import io.reactivex.SingleObserver

/**
 * Created by gjacobs on 11/5/15.
 */
class ImageManager(maxSize: Int) {

    private val memoryCache = LruCache<String,Bitmap>(maxSize)

    private val TAG: String = "ImageManager"

    fun setImage(icon: String, image: ImageView) {

        val url = "${image.resources.getString(R.string.openweathermap_base_url)}/img/w/$icon"
        val bitmap: Bitmap? = memoryCache.get(url)
        if (bitmap != null) {
            image.setImageBitmap(bitmap)
        } else {
            getBitmapObservable(url, this::oldBitmapFetcher)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(BitMapObserver(image))
        }
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

    fun log(s: String): Int = Log.d(TAG, "${Thread.currentThread().name} : ${s}")

    inner class BitMapObserver(val image: ImageView) : SingleObserver<Pair<String, Bitmap?>> {
        override fun onError(e: Throwable) {
            log("BitMapObserver::onError")
            e.printStackTrace()

        }

        override fun onSubscribe(d: Disposable) {
            log("BitMapObserver::onSubscribe")
        }

        override fun onSuccess(pair: Pair<String, Bitmap?>) {
            log("BitMapObserver::onSuccess")
            pair.second?.let {
                memoryCache.put(pair.first, pair.second)
                image.setImageBitmap(pair.second)
            }
        }
    }


    @Throws(Exception::class)
    fun oldBitmapFetcher(url: String): Bitmap {
        val httpURLConnection = URL(url).openConnection() as HttpURLConnection
        return BitmapFactory.decodeStream(httpURLConnection.inputStream)
    }

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


//    private fun loadImageViewNew(icon: String, image: ImageView) {
//        val builder = Retrofit.Builder()
//        builder.addConverterFactory(GsonConverterFactory.create())
//        builder.baseUrl(image.resources.getString(R.string.openweathermap_base_url))
//        val retrofit = builder.build()
//
//        val getImageData = retrofit.create<GetImageData>(GetImageData::class.java!!)
//        val call = getImageData.getIcon(icon, image.resources.getString(R.string.openweathermap_appid))
//
//        call.enqueue(object : Callback<ResponseBody> {
//            override fun onResponse(response: Response<ResponseBody>, retrofit: Retrofit) {
//                try {
//                    val bitmap = BitmapFactory.decodeStream(response.body().byteStream())
//                    this@ImageManager.memoryCache.set(icon, bitmap)
//                    image.setImageBitmap(bitmap)
//                } catch (ioe: IOException) {
//
//                }
//
//            }
//
//            override fun onFailure(throwable: Throwable) {
//
//            }
//        })
//
//    }

//}
