package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import com.google.common.truth.Truth.*
import com.udacity.project4.getOrAwaitValue
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {
    private lateinit var application: Application

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    private val validReminder = ReminderDataItem(
        "Test Reminder",
        "Testing",
        "Sydney",
        -33.865143,
        151.209900
    )

    private val nullTitleReminder = ReminderDataItem(
        null,
        null,
        "Sydney",
        -33.865143,
        151.209900
    )

    private val nullLocationReminder = ReminderDataItem(
        "Test Reminder",
        "Testing",
        null,
        null,
        null
    )

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun init() {
        stopKoin()
        application = ApplicationProvider.getApplicationContext()

        fakeDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(
            application,
            fakeDataSource
        )
    }

    @Test
    fun validateAndSaveReminder_showAndHideLoading() = runBlockingTest {
        mainCoroutineRule.pauseDispatcher()

        saveReminderViewModel.validateAndSaveReminder(validReminder)

        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue()).isTrue()

        mainCoroutineRule.resumeDispatcher()

        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue()).isFalse()
    }

    @Test
    fun validateAndSaveReminder_validReminder() = runBlockingTest {
        assertThat(saveReminderViewModel.validateAndSaveReminder(validReminder)).isTrue()

        assertThat(saveReminderViewModel.showToast.getOrAwaitValue()).isEqualTo(
            application.getString(R.string.reminder_saved)
        )

        assertThat(saveReminderViewModel.navigationCommand.getOrAwaitValue()).isEqualTo(
            NavigationCommand.Back
        )
    }

    @Test
    fun validateAndSaveReminder_nullTitleReminder_showEnterTitleMessage() = runBlockingTest {
        assertThat(saveReminderViewModel.validateAndSaveReminder(nullTitleReminder)).isFalse()

        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue()).isEqualTo(
            R.string.err_enter_title
        )
    }

    @Test
    fun validateAndSaveReminder_nullLocationReminder_showSelectLocationMessage() = runBlockingTest {
        assertThat(saveReminderViewModel.validateAndSaveReminder(nullLocationReminder)).isFalse()

        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue()).isEqualTo(
            R.string.err_select_location
        )
    }
}