package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.*
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var database: RemindersDatabase
    private lateinit var dao: RemindersDao

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
    fun init() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()

        dao = database.reminderDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun saveReminderToDatabase() = runBlockingTest {
        dao.saveReminder(reminder)

        assertThat(dao.getReminders()).contains(reminder)
    }

    @Test
    fun getReminderByIdFromDatabase() = runBlockingTest {
        dao.saveReminder(reminder)

        val reminderFromDatabase = dao.getReminderById(reminder.id)

        assertThat(reminderFromDatabase).isNotNull()
        assertThat(reminderFromDatabase?.id).isEqualTo(reminder.id)
        assertThat(reminderFromDatabase?.title).isEqualTo(reminder.title)
        assertThat(reminderFromDatabase?.description).isEqualTo(reminder.description)
        assertThat(reminderFromDatabase?.location).isEqualTo(reminder.location)
        assertThat(reminderFromDatabase?.latitude).isEqualTo(reminder.latitude)
        assertThat(reminderFromDatabase?.longitude).isEqualTo(reminder.longitude)
    }

    @Test
    fun deleteAllRemindersFromDatabase() = runBlockingTest {
        dao.saveReminder(reminder)
        dao.deleteAllReminders()

        assertThat(dao.getReminders()).isEmpty()
    }
}