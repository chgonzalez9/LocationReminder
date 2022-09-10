package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var localDataSource: ReminderDataSource
    private lateinit var database: RemindersDatabase

    @Before
    fun setup() {
        // Using an in-memory database for testing, because it doesn't survive killing the process.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        localDataSource =
            RemindersLocalRepository(
                database.reminderDao(),
                Dispatchers.Main
            )
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun saveReminder_retrievesReminder() = runBlocking {
        // GIVEN - A new reminder saved in the database.
        val reminder = ReminderDTO("Test title", "Test description","Test Location1",0.0,0.0)
        localDataSource.saveReminder(reminder)

        // WHEN  - Task retrieved by ID.
        val result = localDataSource.getReminder(reminder.id)

        // THEN - Same task is returned.
        result as Result.Success
        assertThat(result.data.title, `is`("Test title"))
        assertThat(result.data.description, `is`("Test description"))
        assertThat(result.data.location, `is`("Test Location1"))
        assertThat(result.data.latitude, `is`(0.0))
        assertThat(result.data.longitude, `is`(0.0))
    }

    @Test
    fun failToSaveReminder_retrievesReminder() = runBlocking {
        //When - Task retrieved by ID
        val result = localDataSource.getReminder("test")

        //Then
        result as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
    }

}