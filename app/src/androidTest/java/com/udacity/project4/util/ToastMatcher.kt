package com.udacity.project4.util

import android.os.IBinder

import android.view.WindowManager.LayoutParams.TYPE_TOAST
import androidx.test.espresso.Espresso
import androidx.test.espresso.Root
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

/**
 * Author: http://www.qaautomated.com/2016/01/how-to-test-toast-message-using-espresso.html
 */
class ToastMatcher : TypeSafeMatcher<Root>() {

    private var failures = 0

    override fun describeTo(description: Description?) {
        description?.appendText("is toast")
    }

    override fun matchesSafely(item: Root): Boolean {
        val type: Int = item.windowLayoutParams.get().type
        if (type == TYPE_TOAST) {
            val windowToken: IBinder = item.decorView.windowToken
            val appToken: IBinder = item.decorView.applicationWindowToken
            if (windowToken === appToken) { // means this window isn't contained by any other windows.
                return true
            }
        }
        return false
    }

    companion object {

        /** Default for maximum number of retries to wait for the toast to pop up */
        private const val DEFAULT_MAX_FAILURES = 5

        fun onToast(text: String, maxRetries: Int = DEFAULT_MAX_FAILURES) = Espresso.onView(
            ViewMatchers.withText(text)
        ).inRoot(isToast(maxRetries))!!

        fun onToast(textId: Int, maxRetries: Int = DEFAULT_MAX_FAILURES) = Espresso.onView(
            ViewMatchers.withText(textId)
        ).inRoot(isToast(maxRetries))!!

        fun isToast(maxRetries: Int = DEFAULT_MAX_FAILURES): Matcher<Root> {
            return ToastMatcher()
        }
    }

}