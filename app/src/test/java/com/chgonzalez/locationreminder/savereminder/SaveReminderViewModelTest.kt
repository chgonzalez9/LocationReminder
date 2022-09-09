package com.chgonzalez.locationreminder.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chgonzalez.locationreminder.MainCoroutineRule
import com.chgonzalez.locationreminder.R
import com.chgonzalez.locationreminder.base.NavigationCommand
import com.chgonzalez.locationreminder.data.FakeDataSource
import com.chgonzalez.locationreminder.getOrAwaitValue
import com.chgonzalez.locationreminder.locationreminders.data.dto.ReminderDTO
import com.chgonzalez.locationreminder.locationreminders.reminderslist.ReminderDataItem
import com.chgonzalez.locationreminder.locationreminders.savereminder.SaveReminderFragmentDirections
import com.chgonzalez.locationreminder.locationreminders.savereminder.SaveReminderViewModel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.bouncycastle.asn1.x500.style.RFC4519Style.description
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest
import org.koin.test.KoinTest

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