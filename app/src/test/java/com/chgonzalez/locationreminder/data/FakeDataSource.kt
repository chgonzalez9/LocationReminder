package com.chgonzalez.locationreminder.locationreminders.data

import com.chgonzalez.locationreminder.locationreminders.data.ReminderDataSource
import com.chgonzalez.locationreminder.locationreminders.data.dto.ReminderDTO
import com.chgonzalez.locationreminder.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(private var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

//    TODO: Create a fake data source to act as a double to the real data source

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        TODO("Return the reminders")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        TODO("return the reminder with the id")
    }

    override suspend fun deleteAllReminders() {
        reminders = mutableListOf()
    }


}