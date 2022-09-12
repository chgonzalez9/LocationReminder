package com.udacity.project4.locationreminders.reminderslist

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
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.GrantPermissionRule
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.get
import org.mockito.Mockito.mock
import com.udacity.project4.R
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragmentDirections
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationFragment
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationFragmentDirections
import com.udacity.project4.util.monitorActivity
import org.koin.test.AutoCloseKoinTest
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest() {

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
            single { RemindersLocalRepository(get())}
            single<ReminderDataSource> {get<RemindersLocalRepository>()}
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

    @Test
    fun clickAddLocationButton_navigateToSelectLocation() = runBlocking {

        val scenario = launchFragmentInContainer<SaveReminderFragment>(Bundle(), R.style.Theme_LocationReminder)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.selectLocation)).perform(click())
        verify(navController).navigate(
            SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
        )
    }

    @Test
    fun clickSaveLocation_navigateToSaveReminder() = runBlocking {

        val scenario = launchFragmentInContainer<SelectLocationFragment>(Bundle(), R.style.Theme_LocationReminder)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.map_button)).perform(click())
        verify(navController).navigate(
            SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment()
        )
    }


    @Test
    fun remindersList_displayReminders(): Unit = runBlocking {

        repository.saveReminder(ReminderDTO("UI Test title", "UI Test Description", "UI Test Location", 1.0, 1.0))

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.Theme_LocationReminder)

        onView(withId(R.id.title)).check(matches(isDisplayed()))
        onView(withId(R.id.title)).check(matches(withText("UI Test title")))

        onView(withId(R.id.description)).check(matches(isDisplayed()))
        onView(withId(R.id.description)).check(matches(withText("UI Test Description")))

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
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.err_select_location)))

        activityScenario.close()

    }

}