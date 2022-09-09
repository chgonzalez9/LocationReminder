package com.chgonzalez.locationreminder

import android.Manifest
import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.chgonzalez.locationreminder.locationreminders.RemindersActivity
import com.chgonzalez.locationreminder.locationreminders.data.ReminderDataSource
import com.chgonzalez.locationreminder.locationreminders.data.local.LocalDB
import com.chgonzalez.locationreminder.locationreminders.data.local.RemindersLocalRepository
import com.chgonzalez.locationreminder.locationreminders.reminderslist.RemindersListViewModel
import com.chgonzalez.locationreminder.locationreminders.savereminder.SaveReminderViewModel
import com.chgonzalez.locationreminder.util.DataBindingIdlingResource
import com.chgonzalez.locationreminder.util.monitorActivity
import com.chgonzalez.locationreminder.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    KoinTest {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    // An Idling Resource that waits for Data Binding to have no pending bindings
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()


    @get:Rule
    var runtimePermissionRule: GrantPermissionRule? = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION)

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() : Unit = IdlingRegistry.getInstance().run {
        register(EspressoIdlingResource.countingIdlingResource)
        register(dataBindingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() : Unit = IdlingRegistry.getInstance().run {
        unregister(EspressoIdlingResource.countingIdlingResource)
        unregister(dataBindingIdlingResource)
    }

    @After
    fun deleteAllReminders() = runBlocking {
        repository.deleteAllReminders()
    }

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Test
    fun createNewReminder() = runBlocking {

        val title = "Test title"
        val description = "Test description"

        // Start up Reminders screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        //When
        Thread.sleep(500)
        onView(withId(R.id.addReminderFAB)).perform(click())

        Thread.sleep(500)
        onView(withId(R.id.reminderDescription)).perform(typeText(description))
        Espresso.closeSoftKeyboard()

        Thread.sleep(500)
        onView(withId(R.id.selectLocation)).perform(click())
        //Wait for the map to load
        Thread.sleep(1500)
        onView(withId(R.id.map)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.map_button)).perform(click())

        Thread.sleep(500)
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.err_enter_title)))

        Thread.sleep(1000)
        onView(withId(R.id.reminderTitle)).perform(typeText(title))
        Espresso.closeSoftKeyboard()

        Thread.sleep(500)
        onView(withId(R.id.saveReminder)).perform(click())

        Thread.sleep(500)
        activityScenario.close()
    }
}

