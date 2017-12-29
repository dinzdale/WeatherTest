package widgets

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation

/**
 * Created by garyjacobs on 12/28/17.
 */

fun doSlideAnimation(view: View, slideOut : Boolean = true) {

    val animationSet = AnimationSet(false);

    val slideAnimation = if (slideOut) TranslateAnimation(
            TranslateAnimation.RELATIVE_TO_SELF, 0.toFloat(),
            TranslateAnimation.RELATIVE_TO_PARENT, -1.toFloat(),
            TranslateAnimation.RELATIVE_TO_SELF, 0.toFloat(),
            TranslateAnimation.RELATIVE_TO_SELF, -1.toFloat())
    else TranslateAnimation(
            TranslateAnimation.RELATIVE_TO_PARENT, -1.toFloat(),
            TranslateAnimation.RELATIVE_TO_SELF, 0.toFloat(),
            TranslateAnimation.RELATIVE_TO_PARENT, -1.toFloat(),
            TranslateAnimation.RELATIVE_TO_SELF, 0.toFloat())

    slideAnimation.fillAfter = true

    val mFadeInAnimation = AlphaAnimation(0.0f, 1.0f);
    mFadeInAnimation.fillAfter = true

    animationSet.addAnimation(slideAnimation);
    //animationSet.addAnimation(mFadeInAnimation);

    animationSet.setDuration(1000);

    animationSet.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationEnd(animation: Animation?) {
            Log.d("Animation", "Animation Ended")
            if (slideOut) {
                view.visibility = View.INVISIBLE
            }
        }

        override fun onAnimationRepeat(animation: Animation?) {
            Log.d("Animation", "Animation Repeated")
        }

        override fun onAnimationStart(animation: Animation?) {
            Log.d("Animation", "Animation Started")
            if (!slideOut) {
                view.visibility = View.VISIBLE
            }
        }
    })
    view.startAnimation(animationSet)
}

fun getAlphaAnimator(view: View): ObjectAnimator {
    val animator = ObjectAnimator.ofFloat(view, "alpha", 0.toFloat(), 1.toFloat())
    animator.repeatCount = ValueAnimator.INFINITE
    animator.repeatMode = ValueAnimator.REVERSE
    animator.duration = 3000
    return animator

}