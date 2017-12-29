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

enum class SlideMotion(val translateAnimation: TranslateAnimation) {
    SLIDEOUTUPLEFT(TranslateAnimation(
            TranslateAnimation.RELATIVE_TO_SELF, 0.toFloat(),
            TranslateAnimation.RELATIVE_TO_PARENT, -1.toFloat(),
            TranslateAnimation.RELATIVE_TO_SELF, 0.toFloat(),
            TranslateAnimation.RELATIVE_TO_SELF, -1.toFloat())),
    SLIDEINDOWNRIGHT(TranslateAnimation(
            TranslateAnimation.RELATIVE_TO_PARENT, -1.toFloat(),
            TranslateAnimation.RELATIVE_TO_SELF, 0.toFloat(),
            TranslateAnimation.RELATIVE_TO_PARENT, -1.toFloat(),
            TranslateAnimation.RELATIVE_TO_SELF, 0.toFloat())),
    SLIDEUPOUT(TranslateAnimation(
            TranslateAnimation.RELATIVE_TO_SELF, 0.toFloat(),
            TranslateAnimation.RELATIVE_TO_PARENT, 0.toFloat(),
            TranslateAnimation.RELATIVE_TO_SELF, 0.toFloat(),
            TranslateAnimation.RELATIVE_TO_PARENT, -1.toFloat())),
    SLIDEDOWNIN(TranslateAnimation(
            TranslateAnimation.RELATIVE_TO_PARENT, 0.toFloat(),
            TranslateAnimation.RELATIVE_TO_SELF, 0.toFloat(),
            TranslateAnimation.RELATIVE_TO_PARENT, -1.toFloat(),
            TranslateAnimation.RELATIVE_TO_SELF, 0.toFloat()));

}

fun doSlideAnimation(view: View, motion: SlideMotion, endFunc: (() -> Unit)? = null) {

    val animationSet = AnimationSet(false);

    val slideAnimation = motion.translateAnimation

    slideAnimation.fillAfter = true

    val mFadeInAnimation = AlphaAnimation(0.0f, 1.0f);
    mFadeInAnimation.fillAfter = true

    animationSet.addAnimation(slideAnimation);
    //animationSet.addAnimation(mFadeInAnimation);

    animationSet.setDuration(1000);

    animationSet.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationEnd(animation: Animation?) {
            Log.d("Animation", "Animation Ended")
            when (motion) {
                SlideMotion.SLIDEOUTUPLEFT -> view.visibility = View.INVISIBLE
                SlideMotion.SLIDEUPOUT -> view.visibility = View.GONE
            }
            endFunc?.invoke()
        }

        override fun onAnimationRepeat(animation: Animation?) {
            Log.d("Animation", "Animation Repeated")
        }

        override fun onAnimationStart(animation: Animation?) {
            Log.d("Animation", "Animation Started")
            when (motion) {
                SlideMotion.SLIDEINDOWNRIGHT -> view.visibility = View.VISIBLE
                SlideMotion.SLIDEDOWNIN -> view.visibility = View.VISIBLE
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