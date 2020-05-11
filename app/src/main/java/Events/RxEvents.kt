package Events

import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.RecyclerView
import com.garyjacobs.weathertest.R
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import model.CurrentWeather
import network.GetForecastData
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Created by garyjacobs on 12/28/17.
 */
enum class UserMotionEvent() {
    FLINGEVENT, SINGLETAP, LONGPRESS;
}

data class UserMotionData(val userMotionEvent: UserMotionEvent, val event1: MotionEvent? = null, val event2: MotionEvent? = null)

fun getFlingObervable(view: View): Observable<FlingEvent> {

    return Observable.create<FlingEvent> { emitter ->

        val gestureDector = GestureDetectorCompat(view.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                emitter.onNext(FlingEvent(e1, e2, velocityX, velocityY))
                //emitter.onComplete()
                return super.onFling(e1, e2, velocityX, velocityY)
            }
        })

        view.setOnTouchListener { v, event ->
            if (!v.hasOnClickListeners()) {
                v.setOnClickListener { }
            }
            gestureDector.onTouchEvent(event)
        }
    }
}

fun getSingleTapObservable(view: View): Observable<MotionEvent> {
    return Observable.create<MotionEvent> { emitter ->
        val gestureDetecor = GestureDetectorCompat(view.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                emitter.onNext(e!!)
                return super.onSingleTapConfirmed(e)
            }
        })
        if (view is RecyclerView) {
            view.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    gestureDetecor.onTouchEvent(e)
                    return super.onInterceptTouchEvent(rv, e)
                }
            })

        } else {
            view.setOnTouchListener { v, event ->
                if (!v.hasOnClickListeners()) {
                    v.setOnClickListener { }
                }
                gestureDetecor.onTouchEvent(event)
            }
        }
    }
}

fun getLongPressObservable(view: View): Observable<MotionEvent> {
    return Observable.create<MotionEvent> { emitter ->
        val gestureDector = GestureDetectorCompat(view.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent?) {
                emitter.onNext(e!!)
                return super.onLongPress(e)
            }
        })
        if (view is RecyclerView) {
            view.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    gestureDector.onTouchEvent(e)
                    return super.onInterceptTouchEvent(rv, e)
                }
            })

        } else {
            view.setOnTouchListener({ v, event ->
                if (!v.hasOnClickListeners()) {
                    v.setOnClickListener { }
                }
                gestureDector.onTouchEvent(event)

            })
        }
    }
}

fun getUserMotionEventObservable(view: View): Observable<UserMotionData> {
    return Observable.create<UserMotionData> { emitter ->
        val gestureDetector = GestureDetector(view.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                emitter.onNext(UserMotionData(UserMotionEvent.FLINGEVENT, e1, e2))
                return super.onFling(e1, e2, velocityX, velocityY)
            }

            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                emitter.onNext(UserMotionData(UserMotionEvent.SINGLETAP, e))
                return super.onSingleTapConfirmed(e)
            }

            override fun onLongPress(e: MotionEvent?) {
                emitter.onNext(UserMotionData(UserMotionEvent.LONGPRESS, e))
                super.onLongPress(e)
            }
        })

        if (!view.hasOnClickListeners()) {
            view.setOnClickListener { }
        }
        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
    }
}

data class FlingEvent(val event1: MotionEvent?, val event2: MotionEvent?, val velocityX: Float, val velocityY: Float)

fun myLog(func: String, e1: MotionEvent, e2: MotionEvent? = null) {
    val sb = StringBuilder()
    sb.append("$func: ").append(" e1:${e1.toString()}")
    e2?.let { sb.append(" e2:${it.toString()}") }
    Log.d("RxEvents", sb.toString())
}

fun getCurrentWeather(context: Context, latitude: Double, longitude: Double): Single<CurrentWeather> {
    return Retrofit.Builder()
            .baseUrl(context.resources.getString(R.string.openweathermap_base_url))
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(GetForecastData::class.java)
            .getCurrrentWeatherByCoords(latitude, longitude, context.resources.getString(R.string.openweathermap_appid))
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .singleOrError()
}