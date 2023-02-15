package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.*
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
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

    private lateinit var database: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    private val reminder = ReminderDTO(
        "Test Reminder",
        "Testing",
        "Sydney",
        -33.865143,
        151.209900
    )

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun  init() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        repository = RemindersLocalRepository(
            database.reminderDao(),
            Dispatchers.Main
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun saveReminder() = runBlocking {
        repository.saveReminder(reminder)

        val result = repository.getReminders()

        assertThat(result).isInstanceOf(Result.Success::class.java)

        result as Result.Success

        assertThat(result.data).contains(reminder)
    }

    @Test
    fun getReminder_ExistingReminder_successResult() = runBlocking {
        repository.saveReminder(reminder)

        val result = repository.getReminder(reminder.id)

        assertThat(result).isInstanceOf(Result.Success::class.java)

        result as Result.Success

        assertThat(result.data.id).isEqualTo(reminder.id)
        assertThat(result.data.title).isEqualTo(reminder.title)
        assertThat(result.data.description).isEqualTo(reminder.description)
        assertThat(result.data.location).isEqualTo(reminder.location)
        assertThat(result.data.latitude).isEqualTo(reminder.latitude)
        assertThat(result.data.longitude).isEqualTo(reminder.longitude)
    }

    @Test
    fun getReminder_NonExistingReminder_errorResult() = runBlocking {
        val result = repository.getReminder(reminder.id)

        assertThat(result).isInstanceOf(Result.Error::class.java)

        result as Result.Error

        assertThat(result.message).isEqualTo("Reminder not found!")
    }


    @Test
    fun deleteAllReminders() = runBlocking {
        repository.saveReminder(reminder)
        repository.deleteAllReminders()

        val result = repository.getReminders()

        assertThat(result).isInstanceOf(Result.Success::class.java)

        result as Result.Success

        assertThat(result.data).isEmpty()
    }
}