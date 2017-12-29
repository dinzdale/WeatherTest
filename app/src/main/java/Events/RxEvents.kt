package Events

import android.support.v4.view.GestureDetectorCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import io.reactivex.*

/**
 * Created by garyjacobs on 12/28/17.
 */

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
            (view as RecyclerView)
                    .addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                        override fun onInterceptTouchEvent(rv: RecyclerView?, e: MotionEvent?): Boolean {
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
            (view as RecyclerView)
                    .addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                        override fun onInterceptTouchEvent(rv: RecyclerView?, e: MotionEvent?): Boolean {
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

data class FlingEvent(val event1: MotionEvent?, val event2: MotionEvent?, val velocityX: Float, val velocityY: Float)

fun myLog(func: String, e1: MotionEvent, e2: MotionEvent? = null) {
    val sb = StringBuilder()
    sb.append("$func: ").append(" e1:${e1.toString()}")
    e2?.let { sb.append(" e2:${it.toString()}") }
    Log.d("RxEvents", sb.toString())
}