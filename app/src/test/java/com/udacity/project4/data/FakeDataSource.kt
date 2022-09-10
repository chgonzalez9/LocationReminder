package com.udacity.project4.data

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(private var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

    private var returnError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (returnError) {
            return Result.Error("Tasks not found")
        }
        reminders?.let {
            return Result.Success(ArrayList(it))
        }
        return Result.Error("Tasks not found")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (returnError) {
            return Result.Error("Tasks not found")
        }
        reminders?.find {
            it.id == id
        }?.let {
            return Result.Success(it)
        }
        return Result.Error("Tasks not found")
    }

    override suspend fun deleteAllReminders() {
        reminders = mutableListOf()
    }

    fun setReturnError(value: Boolean) {
        returnError = value
    }

}