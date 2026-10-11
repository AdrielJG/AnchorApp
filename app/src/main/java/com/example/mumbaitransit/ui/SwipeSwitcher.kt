package com.example.mumbaitransit.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.AnimationUtils
import android.widget.ViewAnimator
import com.example.mumbaitransit.R
import kotlin.math.abs

/**
 * A two-page switcher you change by sliding sideways.
 *
 * Used for the Railway / Metro halves of the boarding and alighting selectors.
 * Both sets of stations belong to the same question — where do you get on —
 * so stacking them vertically doubled the height of the screen for something
 * the commuter only ever answers once. Only one page is mounted at a time and
 * the other is a slide away.
 *
 * ViewPager2 would do this too, but it wants an adapter and a RecyclerView per
 * page for what is two static grids, and it is not currently a dependency.
 * ViewAnimator already carries the slide animations this app uses elsewhere.
 */
class SwipeSwitcher @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewAnimator(context, attrs) {

    /** Fired after the page actually changes, never on a rejected swipe. */
    var onPageChanged: ((Int) -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var dragging = false

    /** Off when only one mode is in range — there is nothing to slide to. */
    var swipeEnabled = true

    init {
        // Height is the taller of the two pages rather than whichever is on
        // screen, so the card doesn't jump as the grids slide past each other.
        measureAllChildren = true
    }

    /**
     * Station buttons are clickable, so they swallow the gesture unless the
     * parent claims it first. The claim is only made once the finger has
     * travelled further sideways than down, which leaves vertical scrolling
     * and plain taps alone.
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!swipeEnabled || childCount < 2) return false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x; downY = ev.y; dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - downX
                val dy = ev.y - downY
                if (abs(dx) > touchSlop && abs(dx) > abs(dy) * 1.5f) {
                    dragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!swipeEnabled || childCount < 2) return false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x; downY = ev.y
                return true
            }
            MotionEvent.ACTION_MOVE -> return true
            MotionEvent.ACTION_UP -> {
                val dx = ev.x - downX
                // Dragging left moves forward — the next page comes in from the
                // right, the way the finger is pushing the current one away.
                if (abs(dx) > touchSlop * 2) step(if (dx < 0) 1 else -1)
                release()
                return true
            }
            MotionEvent.ACTION_CANCEL -> release()
        }
        return dragging
    }

    private fun release() {
        dragging = false
        parent?.requestDisallowInterceptTouchEvent(false)
    }

    /** Wraps, so either direction gets to the other page of a two-page set. */
    fun step(delta: Int) {
        if (childCount < 2) return
        val target = ((displayedChild + delta) % childCount + childCount) % childCount
        showPage(target, forward = delta > 0, animate = true)
    }

    fun showPage(index: Int, forward: Boolean = true, animate: Boolean = true) {
        if (index !in 0 until childCount || index == displayedChild) return
        if (animate) {
            inAnimation = AnimationUtils.loadAnimation(
                context, if (forward) R.anim.slide_in_right else R.anim.slide_in_left
            )
            outAnimation = AnimationUtils.loadAnimation(
                context, if (forward) R.anim.slide_out_left else R.anim.slide_out_right
            )
        } else {
            inAnimation = null
            outAnimation = null
        }
        displayedChild = index
        onPageChanged?.invoke(index)
    }
}
