package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Note
import com.example.ui.theme.NoteColorPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CalendarViewMode(val label: String) {
    MONTHLY("Monthly"),
    WEEKLY("Weekly"),
    DAILY("Daily")
}

@Composable
fun CalendarViewSection(
    notes: List<Note>,
    unlockedNotes: Map<String, String>,
    isDarkTheme: Boolean,
    onSelectNote: (Note) -> Unit,
    onCreateNoteForTimestamp: (Long) -> Unit,
    onBackToNotes: () -> Unit
) {
    BackHandler { onBackToNotes() }

    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTHLY) }
    var selectedDateMillis by remember { mutableLongStateOf(startOfDayMillis(System.currentTimeMillis())) }
    var remindersOnly by remember { mutableStateOf(false) }

    val selectedDayNotes = remember(notes, selectedDateMillis, remindersOnly) {
        notesForDay(notes, selectedDateMillis, remindersOnly)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_view_container")
    ) {
        // Calendar Mode Switcher (Monthly | Weekly | Daily) + Reminder Icon Filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalendarViewMode.entries.forEach { mode ->
                    val selected = viewMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { viewMode = mode },
                        leadingIcon = {
                            Icon(
                                imageVector = when (mode) {
                                    CalendarViewMode.MONTHLY -> Icons.Default.CalendarMonth
                                    CalendarViewMode.WEEKLY -> Icons.Default.CalendarViewWeek
                                    CalendarViewMode.DAILY -> Icons.Default.CalendarViewDay
                                },
                                contentDescription = mode.label,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(mode.label) },
                        modifier = Modifier.testTag("calendar_mode_${mode.name.lowercase()}")
                    )
                }
            }

            Surface(
                color = if (remindersOnly) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 1.dp,
                        color = if (remindersOnly) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { remindersOnly = !remindersOnly }
                    .padding(horizontal = 10.dp)
                    .testTag("calendar_reminders_only_chip")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Reminders filter",
                        tint = if (remindersOnly) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Date Navigation Bar (Prev / Current Period Label / Today / Next)
        CalendarHeaderNavigator(
            viewMode = viewMode,
            selectedDateMillis = selectedDateMillis,
            onNavigate = { newMillis -> selectedDateMillis = startOfDayMillis(newMillis) },
            onJumpToday = { selectedDateMillis = startOfDayMillis(System.currentTimeMillis()) }
        )

        Spacer(modifier = Modifier.height(6.dp))

        when (viewMode) {
            CalendarViewMode.MONTHLY -> {
                MonthlyCalendarContent(
                    notes = notes,
                    unlockedNotes = unlockedNotes,
                    isDarkTheme = isDarkTheme,
                    remindersOnly = remindersOnly,
                    selectedDateMillis = selectedDateMillis,
                    selectedDayNotes = selectedDayNotes,
                    onSelectDay = { dayMillis -> selectedDateMillis = dayMillis },
                    onSelectNote = onSelectNote,
                    onCreateNoteForTimestamp = {
                        onCreateNoteForTimestamp(defaultReminderTimeForDay(selectedDateMillis, 9))
                    }
                )
            }

            CalendarViewMode.WEEKLY -> {
                WeeklyCalendarContent(
                    notes = notes,
                    unlockedNotes = unlockedNotes,
                    isDarkTheme = isDarkTheme,
                    remindersOnly = remindersOnly,
                    selectedDateMillis = selectedDateMillis,
                    onSelectDay = { dayMillis -> selectedDateMillis = dayMillis },
                    onSelectNote = onSelectNote,
                    onCreateNoteForDay = { dayMillis ->
                        onCreateNoteForTimestamp(defaultReminderTimeForDay(dayMillis, 9))
                    }
                )
            }

            CalendarViewMode.DAILY -> {
                DailyCalendarContent(
                    unlockedNotes = unlockedNotes,
                    isDarkTheme = isDarkTheme,
                    selectedDateMillis = selectedDateMillis,
                    dayNotes = selectedDayNotes,
                    onSelectNote = onSelectNote,
                    onCreateNoteAtHour = { hourOfDay ->
                        onCreateNoteForTimestamp(defaultReminderTimeForDay(selectedDateMillis, hourOfDay))
                    }
                )
            }
        }
    }
}

@Composable
private fun CalendarHeaderNavigator(
    viewMode: CalendarViewMode,
    selectedDateMillis: Long,
    onNavigate: (Long) -> Unit,
    onJumpToday: () -> Unit
) {
    val headerText = remember(viewMode, selectedDateMillis) {
        when (viewMode) {
            CalendarViewMode.MONTHLY -> SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                .format(Date(selectedDateMillis))

            CalendarViewMode.WEEKLY -> {
                val weekStart = startOfWeekMillis(selectedDateMillis)
                val weekEnd = addDays(weekStart, 6)
                val fmtStart = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(weekStart))
                val fmtEnd = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(weekEnd))
                "$fmtStart – $fmtEnd"
            }

            CalendarViewMode.DAILY -> SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
                .format(Date(selectedDateMillis))
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    val next = when (viewMode) {
                        CalendarViewMode.MONTHLY -> addMonths(selectedDateMillis, -1)
                        CalendarViewMode.WEEKLY -> addDays(selectedDateMillis, -7)
                        CalendarViewMode.DAILY -> addDays(selectedDateMillis, -1)
                    }
                    onNavigate(next)
                },
                modifier = Modifier.testTag("calendar_prev_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous period"
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = onJumpToday,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("calendar_today_button")
                ) {
                    Text("Today", style = MaterialTheme.typography.labelLarge)
                }
            }

            IconButton(
                onClick = {
                    val next = when (viewMode) {
                        CalendarViewMode.MONTHLY -> addMonths(selectedDateMillis, 1)
                        CalendarViewMode.WEEKLY -> addDays(selectedDateMillis, 7)
                        CalendarViewMode.DAILY -> addDays(selectedDateMillis, 1)
                    }
                    onNavigate(next)
                },
                modifier = Modifier.testTag("calendar_next_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next period"
                )
            }
        }
    }
}

@Composable
private fun MonthlyCalendarContent(
    notes: List<Note>,
    unlockedNotes: Map<String, String>,
    isDarkTheme: Boolean,
    remindersOnly: Boolean,
    selectedDateMillis: Long,
    selectedDayNotes: List<Note>,
    onSelectDay: (Long) -> Unit,
    onSelectNote: (Note) -> Unit,
    onCreateNoteForTimestamp: () -> Unit
) {
    val monthCells = remember(selectedDateMillis) { buildMonthGridCells(selectedDateMillis) }
    val notesByDay = remember(notes, remindersOnly) { indexNotesByDay(notes, remindersOnly) }
    val todayMillis = remember { startOfDayMillis(System.currentTimeMillis()) }
    val selectedDateFormatted = remember(selectedDateMillis) {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(selectedDateMillis))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Weekday Header Row
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { dayName ->
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 6-row x 7-column Month Grid
                    monthCells.chunked(7).forEach { weekRow ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            weekRow.forEach { cell ->
                                val dayNotes = notesByDay[cell.dayStartMillis].orEmpty()
                                val isSelected = cell.dayStartMillis == selectedDateMillis
                                val isToday = cell.dayStartMillis == todayMillis
                                val hasReminder = dayNotes.any { it.reminderEpochMillis > 0L }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { onSelectDay(cell.dayStartMillis) }
                                        .testTag("calendar_day_cell_${cell.dayOfMonth}_${cell.isCurrentMonth}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = cell.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                !cell.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )

                                        if (dayNotes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                dayNotes.take(3).forEach { n ->
                                                    val dotColor = when {
                                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                                        n.reminderEpochMillis > 0L -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.secondary
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(if (hasReminder) 6.dp else 5.dp)
                                                            .clip(CircleShape)
                                                            .background(dotColor)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Action Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Notes for $selectedDateFormatted",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${selectedDayNotes.size} note(s) scheduled or recorded",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onCreateNoteForTimestamp,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("calendar_create_note_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New note on selected date",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Note")
                }
            }
        }

        if (selectedDayNotes.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "No notes on $selectedDateFormatted",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Tap 'Add Note' to create a note or reminder directly for this date.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(selectedDayNotes, key = { it.id }) { note ->
                CalendarNoteRowCard(
                    note = note,
                    unlockedNotes = unlockedNotes,
                    isDarkTheme = isDarkTheme,
                    onClick = { onSelectNote(note) }
                )
            }
        }
    }
}

@Composable
private fun WeeklyCalendarContent(
    notes: List<Note>,
    unlockedNotes: Map<String, String>,
    isDarkTheme: Boolean,
    remindersOnly: Boolean,
    selectedDateMillis: Long,
    onSelectDay: (Long) -> Unit,
    onSelectNote: (Note) -> Unit,
    onCreateNoteForDay: (Long) -> Unit
) {
    val weekStart = remember(selectedDateMillis) { startOfWeekMillis(selectedDateMillis) }
    val weekDays = remember(weekStart) { (0..6).map { offset -> addDays(weekStart, offset) } }
    val notesByDay = remember(notes, remindersOnly) { indexNotesByDay(notes, remindersOnly) }
    val todayMillis = remember { startOfDayMillis(System.currentTimeMillis()) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 7-Day Horizontal Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            weekDays.forEach { dayMillis ->
                val dayNotes = notesByDay[dayMillis].orEmpty()
                val isSelected = dayMillis == selectedDateMillis
                val isToday = dayMillis == todayMillis
                val dayName = remember(dayMillis) { SimpleDateFormat("EEE", Locale.getDefault()).format(Date(dayMillis)) }
                val dayNum = remember(dayMillis) { SimpleDateFormat("d", Locale.getDefault()).format(Date(dayMillis)) }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            }
                        )
                        .clickable { onSelectDay(dayMillis) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dayNum,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        },
                        shape = CircleShape
                    ) {
                        Text(
                            text = dayNotes.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Weekly Agenda Breakdown by Day
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(weekDays, key = { it }) { dayMillis ->
                val dayNotes = notesForDay(notes, dayMillis, remindersOnly)
                val dayTitle = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(dayMillis))
                val isToday = dayMillis == todayMillis

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (dayMillis == selectedDateMillis) 1.5.dp else 1.dp,
                            color = if (dayMillis == selectedDateMillis) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            },
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dayTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isToday) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "TODAY",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { onCreateNoteForDay(dayMillis) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add note for $dayTitle",
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Note", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        if (dayNotes.isEmpty()) {
                            Text(
                                text = "No notes or reminders — tap 'New Note' to schedule.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            dayNotes.forEach { note ->
                                CalendarNoteRowCard(
                                    note = note,
                                    unlockedNotes = unlockedNotes,
                                    isDarkTheme = isDarkTheme,
                                    onClick = { onSelectNote(note) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyCalendarContent(
    unlockedNotes: Map<String, String>,
    isDarkTheme: Boolean,
    selectedDateMillis: Long,
    dayNotes: List<Note>,
    onSelectNote: (Note) -> Unit,
    onCreateNoteAtHour: (Int) -> Unit
) {
    val hours = remember { (7..21).toList() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Schedule (${dayNotes.size} notes)",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = { onCreateNoteAtHour(9) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("daily_quick_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create note for today",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Note")
                }
            }
        }

        // Hourly Time Slots (7:00 AM .. 9:00 PM) + Off-Hours
        items(hours, key = { it }) { hour ->
            val matchingNotes = dayNotes.filter { note ->
                val tsMillis = effectiveNoteTimestampMillis(note, selectedDateMillis)
                val cal = Calendar.getInstance().apply { timeInMillis = tsMillis }
                val h = cal.get(Calendar.HOUR_OF_DAY)
                if (hour == 7) h <= 7 else if (hour == 21) h >= 21 else h == hour
            }
            val hourLabel = remember(hour) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, 0)
                }
                SimpleDateFormat("h:00 a", Locale.getDefault()).format(cal.time)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .clickable { onCreateNoteAtHour(hour) }
                    .padding(12.dp)
                    .testTag("daily_slot_$hour"),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.width(72.dp)) {
                    Text(
                        text = hourLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "+ Add",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (matchingNotes.isEmpty()) {
                        Text(
                            text = "Tap slot to add note at $hourLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                    } else {
                        matchingNotes.forEach { note ->
                            CalendarNoteRowCard(
                                note = note,
                                unlockedNotes = unlockedNotes,
                                isDarkTheme = isDarkTheme,
                                onClick = { onSelectNote(note) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarNoteRowCard(
    note: Note,
    unlockedNotes: Map<String, String>,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val swatch = NoteColorPalette.find { it.hex.equals(note.colorHex, ignoreCase = true) }
        ?: NoteColorPalette.first()
    val cardBg = if (isDarkTheme) swatch.darkSurface else swatch.lightSurface
    val isUnlocked = !note.isLocked || unlockedNotes.containsKey(note.id)
    val preview = if (note.isLocked && !isUnlocked) {
        "Password protected • Tap to unlock"
    } else if (note.isLocked) {
        unlockedNotes[note.id].orEmpty()
    } else {
        note.content
    }

    val timeLabel = remember(note.reminderEpochMillis, note.createdAt, note.updatedAt) {
        val millis = if (note.reminderEpochMillis > 0L) {
            note.reminderEpochMillis
        } else {
            ((note.updatedAt ?: note.createdAt)?.seconds ?: 0L) * 1000L
        }
        if (millis > 0L) {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
        } else {
            "Anytime"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                color = swatch.accent.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = swatch.accent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = note.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = swatch.accent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (note.reminderEpochMillis > 0L) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Reminder",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = timeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (note.isLocked) {
                        Icon(
                            imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (preview.isNotBlank()) {
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (note.isLocked && !isUnlocked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private data class MonthGridCell(
    val dayOfMonth: Int,
    val dayStartMillis: Long,
    val isCurrentMonth: Boolean
)

internal fun startOfDayMillis(timeMillis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

internal fun startOfWeekMillis(timeMillis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = startOfDayMillis(timeMillis)
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    }
    return cal.timeInMillis
}

internal fun addDays(timeMillis: Long, days: Int): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        add(Calendar.DAY_OF_YEAR, days)
    }
    return startOfDayMillis(cal.timeInMillis)
}

internal fun addMonths(timeMillis: Long, months: Int): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        add(Calendar.MONTH, months)
    }
    return startOfDayMillis(cal.timeInMillis)
}

internal fun defaultReminderTimeForDay(dayStartMillis: Long, hourOfDay: Int): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = startOfDayMillis(dayStartMillis)
        set(Calendar.HOUR_OF_DAY, hourOfDay.coerceIn(0, 23))
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun buildMonthGridCells(selectedDateMillis: Long): List<MonthGridCell> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = selectedDateMillis
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val targetMonth = cal.get(Calendar.MONTH)
    val firstDayOfWeekIndex = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    cal.add(Calendar.DAY_OF_YEAR, -firstDayOfWeekIndex)

    return (0 until 42).map {
        val dayStart = startOfDayMillis(cal.timeInMillis)
        val cell = MonthGridCell(
            dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
            dayStartMillis = dayStart,
            isCurrentMonth = cal.get(Calendar.MONTH) == targetMonth
        )
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cell
    }
}

private fun effectiveNoteTimestampMillis(note: Note, targetDayStartMillis: Long): Long {
    if (note.reminderEpochMillis > 0L && startOfDayMillis(note.reminderEpochMillis) == targetDayStartMillis) {
        return note.reminderEpochMillis
    }
    val createdMillis = (note.createdAt?.seconds ?: note.updatedAt?.seconds ?: 0L) * 1000L
    if (createdMillis > 0L && startOfDayMillis(createdMillis) == targetDayStartMillis) {
        return createdMillis
    }
    return if (note.reminderEpochMillis > 0L) note.reminderEpochMillis else createdMillis
}

internal fun indexNotesByDay(
    notes: List<Note>,
    remindersOnly: Boolean
): Map<Long, List<Note>> {
    if (notes.isEmpty()) return emptyMap()
    val map = HashMap<Long, MutableList<Note>>()
    val cal = Calendar.getInstance()
    fun dayStartUsingSharedCal(millis: Long): Long {
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    for (note in notes) {
        val remDay = if (note.reminderEpochMillis > 0L) dayStartUsingSharedCal(note.reminderEpochMillis) else -1L
        if (remDay > 0L) {
            map.getOrPut(remDay) { mutableListOf() }.add(note)
        }
        if (!remindersOnly) {
            val createdMillis = (note.createdAt?.seconds ?: note.updatedAt?.seconds ?: 0L) * 1000L
            if (createdMillis > 0L) {
                val createdDay = dayStartUsingSharedCal(createdMillis)
                if (createdDay != remDay) {
                    map.getOrPut(createdDay) { mutableListOf() }.add(note)
                }
            }
        }
    }
    return map
}

internal fun notesForDay(
    notes: List<Note>,
    dayStartMillis: Long,
    remindersOnly: Boolean
): List<Note> {
    return notes.filter { note ->
        val matchesReminderDate = note.reminderEpochMillis > 0L &&
            startOfDayMillis(note.reminderEpochMillis) == dayStartMillis
        if (remindersOnly) {
            matchesReminderDate
        } else {
            val createdMillis = (note.createdAt?.seconds ?: note.updatedAt?.seconds ?: 0L) * 1000L
            val matchesCreatedDate = createdMillis > 0L && startOfDayMillis(createdMillis) == dayStartMillis
            matchesReminderDate || matchesCreatedDate
        }
    }
}
