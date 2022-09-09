package com.chgonzalez.locationreminder.reminderslist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chgonzalez.locationreminder.MainCoroutineRule
import com.chgonzalez.locationreminder.data.FakeDataSource
import com.chgonzalez.locationreminder.getOrAwaitValue
import com.chgonzalez.locationreminder.locationreminders.reminderslist.RemindersListViewModel
import com.chgonzalez.locationreminder.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private lateinit var remindersListViewModel : RemindersListViewModel

    private lateinit var remindersRepository : FakeDataSource

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupRemindersListViewModel() {
        // We initialise the repository with no tasks
        remindersRepository = FakeDataSource()

        remindersListViewModel = RemindersListViewModel(Application(), remindersRepository)
    }

    @Test
    fun invalidateShowNoData_displayNoReminders() {
        remindersRepository.setReturnError(true)
        remindersListViewModel.loadReminders()

        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(true))
        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`("Tasks not found"))
    }

}