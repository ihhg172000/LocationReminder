package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(
    private val reminders: MutableList<ReminderDTO> = mutableListOf(),
    var returnError: Boolean = false
) : ReminderDataSource {

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (returnError) {
            return Result.Error("You have an error")
        }
        return Result.Success(reminders)
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (returnError) {
            Result.Error("You have an error")
        }

        reminders.firstOrNull {
            it.id == id
        }?.let {
            return Result.Success(it)
        }

        return Result.Error("Reminder not found")
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }


}