package com.chgonzalez.locationreminder.locationreminders.reminderslist

import android.Manifest
import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.GrantPermissionRule
import com.chgonzalez.locationreminder.locationreminders.data.ReminderDataSource
import com.chgonzalez.locationreminder.locationreminders.data.dto.ReminderDTO
import com.chgonzalez.locationreminder.locationreminders.data.local.LocalDB
import com.chgonzalez.locationreminder.locationreminders.data.local.RemindersLocalRepository
import com.chgonzalez.locationreminder.locationreminders.savereminder.SaveReminderViewModel
import com.chgonzalez.locationreminder.util.DataBindingIdlingResource
import com.chgonzalez.locationreminder.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
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
import org.mockito.Mockito.mock
import com.chgonzalez.locationreminder.R
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.chgonzalez.locationreminder.locationreminders.RemindersActivity
import com.chgonzalez.locationreminder.locationreminders.savereminder.SaveReminderFragmentDirections
import com.chgonzalez.locationreminder.util.monitorActivity
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : KoinTest {

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    // An Idling Resource that waits for Data Binding to have no pending bindings
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var runtimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION)

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = ApplicationProvider.getApplicationContext()
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
    fun clickAddReminderButton_navigateToSaveReminder() = runBlocking {

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.Theme_LocationReminder)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.addReminderFAB)).perform(click())
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }

    /** Not working **/
    @Test
    fun clickAddLocationButton_navigateToSelectLocation() = runBlocking {

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.Theme_LocationReminder)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.selectLocation)).perform(click())
        verify(navController).navigate(
            SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
        )
    }

    /** Not working **/
    @Test
    fun remindersList_displayReminders(): Unit = runBlocking {

        repository.saveReminder(ReminderDTO("UI Test title", "UI Test Description", "UI Test Location", 1.0, 1.0))
        repository.saveReminder(ReminderDTO("UI Test title 2", "UI Test Description 2", "UI Test Location 2", 2.0, 2.0))

        launchFragmentInContainer<ReminderListFragment>()

        onView(withId(R.id.title)).check(matches(isDisplayed()))
        onView(withId(R.id.title)).check(matches(withText("UI Test title")))
        onView(withId(R.id.title)).check(matches(withText("UI Test title 2")))

        onView(withId(R.id.description)).check(matches(isDisplayed()))
        onView(withId(R.id.description)).check(matches(withText("UI Test Description")))
        onView(withId(R.id.description)).check(matches(withText("UI Test Description 2")))

    }

    @Test
    fun createNewReminder_errorMessage() = runBlocking {

        val title = "Error Test title"
        val description = "Error Test description"

        // Start up Tasks screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        //When
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(ViewActions.replaceText(title))
        onView(withId(R.id.reminderDescription)).perform(ViewActions.replaceText(description))
        onView(withId(R.id.saveReminder)).perform(click())

        //Check for showToast
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.err_enter_title)))

        activityScenario.close()

    }

//    TODO: test the navigation of the fragments.
//    TODO: test the displayed data on the UI.
}