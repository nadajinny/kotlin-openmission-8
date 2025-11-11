package com.example.tagmoa.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tagmoa.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerModal(
    initialStartDateMillis: Long? = null,
    initialEndDateMillis: Long? = null,
    onDateRangeSelected: (Pair<Long?, Long?>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartDateMillis,
        initialSelectedEndDateMillis = initialEndDateMillis
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateRangeSelected(state.selectedStartDateMillis to state.selectedEndDateMillis)
                    onDismiss()
                }
            ) {
                Text(text = stringResource(id = R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val selectionText = remember(state.selectedStartDateMillis, state.selectedEndDateMillis) {
                val formatter = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA)
                val start = state.selectedStartDateMillis?.let { formatter.format(Date(it)) }
                val end = state.selectedEndDateMillis?.let { formatter.format(Date(it)) }
                when {
                    start != null && end != null -> "$start - $end"
                    start != null -> start
                    end != null -> end
                    else -> ""
                }
            }
            DateRangePicker(
                state = state,
                title = {
                    Text(
                        text = stringResource(id = R.string.title_select_date_range),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                headline = {
                    Text(
                        text = if (selectionText.isEmpty()) stringResource(id = R.string.label_select_period_placeholder) else selectionText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                showModeToggle = false,
                modifier = modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .padding(vertical = 16.dp)
            )
        }
    }
}
