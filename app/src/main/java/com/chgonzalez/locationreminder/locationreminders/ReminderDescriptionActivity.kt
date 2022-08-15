package com.chgonzalez.locationreminder.locationreminders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.chgonzalez.locationreminder.R
import com.chgonzalez.locationreminder.databinding.ActivityReminderDescriptionBinding
import com.chgonzalez.locationreminder.locationreminders.reminderslist.ReminderDataItem
/**
 * Activity that displays the reminder details after the user clicks on the notification
 */
class ReminderDescriptionActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ReminderDataItem = "EXTRA_ReminderDataItem"

        //        receive the reminder object after the user clicks on the notification
        fun newIntent(context: Context, reminderDataItem: ReminderDataItem): Intent {
            val intent = Intent(context, ReminderDescriptionActivity::class.java)
            intent.putExtra(EXTRA_ReminderDataItem, reminderDataItem)
            return intent
        }
    }

    private lateinit var _binding: ActivityReminderDescriptionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_reminder_description
        )

        val reminderItem = intent.getSerializableExtra(EXTRA_ReminderDataItem) as ReminderDataItem?

        if(reminderItem == null){
            Toast.makeText(this, "Unable to load reminder data", Toast.LENGTH_SHORT)
                .show()
        }

        reminderItem.let {
            _binding.reminderDataItem = it
        }
    }
}
