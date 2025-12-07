package com.example.taskpulse

data class TimesheetEntry(
    val date: String,
    val day: String,
    var intakeId: String = "NA",
    var workDescription: String = "Development",
    var applicationName: String = "Recovery",
    var futureBenefit: String = "Efficiency improvement",
    var inTime: String = "",
    var outTime: String = "",
    var hours: String = "",
    var isLeave: Boolean = false,
    var leaveReason: String = "",
    var isHoliday: Boolean = false,
    var holidayName: String = "",
    var isWeekend: Boolean = false,
    var taskDescription: String = ""
)