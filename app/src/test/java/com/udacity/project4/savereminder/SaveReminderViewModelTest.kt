package com.udacity.project4.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.data.FakeDataSource
import com.udacity.project4.getOrAwaitValue
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragmentDirections
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest : AutoCloseKoinTest() {

    private lateinit var saveViewModel : SaveReminderViewModel

    private lateinit var remindersRepository : FakeDataSource

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupViewModel() {
        val reminder1 = ReminderDTO("Test Title 1", "Test Description 1", "Test Location 1", 1.0, 1.0, "1")
        val reminder2 = ReminderDTO("Test Title 2", "Test Description 2", "Test Location 2", 2.0, 2.0, "2")
        val reminder3 = ReminderDTO("Test Title 3", "Test Description 3", "Test Location 3", 3.0, 3.0, "3")
        val reminder4 = ReminderDTO("Test Title 4", "Test Description 4", "Test Location 3", 4.0, 4.0, "4")
        val reminders = listOf(reminder1, reminder2, reminder3, reminder4)
        remindersRepository = FakeDataSource(reminders.toMutableList())

        saveViewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), remindersRepository)
    }

    @Test
    fun reminders_onClear() {

        saveViewModel.onClear()

        assertThat(saveViewModel.reminderTitle.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveViewModel.reminderDescription.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveViewModel.reminderSelectedLocationStr.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveViewModel.selectedPOI.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveViewModel.latitude.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveViewModel.longitude.getOrAwaitValue(), `is`(nullValue()))
    }

    @Test
    fun reminders_saveReminder() {

        saveViewModel.saveReminder(ReminderDataItem(
            title = "save reminder test",
            description = "save reminder test",
            location = "save reminder test",
            latitude = 0.0,
            longitude = 0.0
        ))

        assertThat(saveViewModel.showToast.getOrAwaitValue(), `is`("Reminder Saved !"))
        assertThat(saveViewModel.navigationCommand.getOrAwaitValue(), `is`(NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToReminderListFragment())))
    }

    @Test
    fun validateEnteredData_returnError() {

        val reminder = ReminderDataItem("", "", "", 0.0, 0.0, "1")
        remindersRepository.setReturnError(true)

        assertThat(saveViewModel.validateEnteredData(reminder), `is`(false))
    }

    @Test
    fun validateEnteredData_noTitle_showToast() {

        saveViewModel.validateEnteredData(ReminderDataItem(
            title = null,
            description = "no title test",
            location = "no title test",
            latitude = 0.0,
            longitude = 0.0
        ))

        assertThat(saveViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))

    }

    @Test
    fun validateEnteredData_noDescription_showToast() {

        saveViewModel.validateEnteredData(ReminderDataItem(
            title = "no description test",
            description = null,
            location = "no description test",
            latitude = 0.0,
            longitude = 0.0
        ))

        assertThat(saveViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_description))

    }

    @Test
    fun validateEnteredData_noLocation_showToast() {

        saveViewModel.validateEnteredData(ReminderDataItem(
            title = "no location test",
            description = "no location test",
            location = null,
            latitude = 0.0,
            longitude = 0.0
        ))

        assertThat(saveViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_location))

    }

}