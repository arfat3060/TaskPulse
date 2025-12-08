package com.example.taskpulse

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class TimesheetAdapter(
    private val entries: MutableList<TimesheetEntry>,
    private val onTimeClicked: (position: Int, is_in_time: Boolean) -> Unit
) : RecyclerView.Adapter<TimesheetAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.timesheet_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.bind(entry)
    }

    override fun getItemCount() = entries.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.date_text_view)
        private val statusBadgeTextView: TextView = itemView.findViewById(R.id.status_badge_text_view)
        private val totalHoursTextView: TextView = itemView.findViewById(R.id.total_hours_text_view)
        private val inTimeEditText: TextInputEditText = itemView.findViewById(R.id.in_time_edit_text)
        private val outTimeEditText: TextInputEditText = itemView.findViewById(R.id.out_time_edit_text)
        private val taskDescriptionEditText: TextInputEditText = itemView.findViewById(R.id.task_description_edit_text)
        private val leaveCheckbox: CheckBox = itemView.findViewById(R.id.leave_checkbox)
        private val leaveReasonInputLayout: TextInputLayout = itemView.findViewById(R.id.leave_reason_input_layout)
        private val leaveReasonEditText: TextInputEditText = itemView.findViewById(R.id.leave_reason_edit_text)

        fun bind(entry: TimesheetEntry) {
            dateTextView.text = entry.date

            val isWorkableDay = !entry.isWeekend && !entry.isHoliday
            inTimeEditText.isEnabled = isWorkableDay && !entry.isLeave
            outTimeEditText.isEnabled = isWorkableDay && !entry.isLeave
            taskDescriptionEditText.isEnabled = isWorkableDay && !entry.isLeave
            leaveCheckbox.isEnabled = isWorkableDay

            statusBadgeTextView.visibility = View.GONE
            totalHoursTextView.visibility = View.VISIBLE

            if (entry.isHoliday) {
                statusBadgeTextView.text = entry.holidayName
                statusBadgeTextView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.md_theme_light_primaryContainer))
                statusBadgeTextView.visibility = View.VISIBLE
            } else if (entry.isWeekend) {
                statusBadgeTextView.text = "Weekend"
                statusBadgeTextView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.md_theme_light_errorContainer))
                statusBadgeTextView.visibility = View.VISIBLE
            } else if (entry.isLeave) {
                statusBadgeTextView.text = "Leave"
                statusBadgeTextView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.md_theme_light_tertiaryContainer))
                statusBadgeTextView.visibility = View.VISIBLE
            }

            if (entry.isWeekend || entry.isHoliday) {
                itemView.alpha = 0.7f
                totalHoursTextView.visibility = View.INVISIBLE
            } else {
                itemView.alpha = 1.0f
            }

            inTimeEditText.setText(entry.inTime)
            outTimeEditText.setText(entry.outTime)
            taskDescriptionEditText.setText(entry.taskDescription)

            updateTotalHours(entry)

            inTimeEditText.setOnClickListener {
                onTimeClicked(adapterPosition, true)
            }

            outTimeEditText.setOnClickListener {
                onTimeClicked(adapterPosition, false)
            }

            taskDescriptionEditText.addTextChangedListener {
                entries[adapterPosition].taskDescription = it.toString()
            }

            leaveCheckbox.isChecked = entry.isLeave
            leaveReasonInputLayout.visibility = if (entry.isLeave) View.VISIBLE else View.GONE

            leaveCheckbox.setOnCheckedChangeListener { _, isChecked ->
                entries[adapterPosition].isLeave = isChecked
                inTimeEditText.isEnabled = !isChecked
                outTimeEditText.isEnabled = !isChecked
                taskDescriptionEditText.isEnabled = !isChecked
                leaveReasonInputLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            }

            leaveReasonEditText.setText(entry.leaveReason)
            leaveReasonEditText.addTextChangedListener {
                entries[adapterPosition].leaveReason = it.toString()
            }
        }

        private fun updateTotalHours(entry: TimesheetEntry) {
            val hours = calculateHours(entry.inTime, entry.outTime)
            totalHoursTextView.text = hours.first
            val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(hours.second)

            if (totalMinutes > 0 && totalMinutes < 9 * 60) {
                totalHoursTextView.setTextColor(Color.RED)
            } else {
                totalHoursTextView.setTextColor(Color.GREEN)
            }
        }

        private fun calculateHours(inTime: String, outTime: String): Pair<String, Long> {
            if (inTime.isBlank() || outTime.isBlank()) {
                return "" to 0L
            }
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            try {
                val inDate = timeFormat.parse(inTime)
                val outDate = timeFormat.parse(outTime)

                if (inDate != null && outDate != null) {
                    val diff = outDate.time - inDate.time
                    if (diff >= 0) {
                        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                        val hours = totalMinutes / 60
                        val minutes = totalMinutes % 60
                        return String.format(Locale.getDefault(), "%d:%02d", hours, minutes) to diff
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return "" to 0L
        }
    }
}