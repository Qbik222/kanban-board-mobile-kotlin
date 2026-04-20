package com.kanban.mobile.feature.boards

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun parseDeadlineFieldToLocalDate(value: String): LocalDate? {
    val t = value.trim()
    if (t.isEmpty()) return null
    return runCatching {
        if (t.length >= 10) {
            LocalDate.parse(t.substring(0, 10))
        } else {
            LocalDate.parse(t)
        }
    }.getOrNull()
}

@Composable
fun DeadlineDateField(
    label: String,
    isoValue: String,
    onIsoChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val display = remember(isoValue, locale) {
        parseDeadlineFieldToLocalDate(isoValue)?.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale),
        ).orEmpty()
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text("—") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        OutlinedButton(
            onClick = {
                val initial = parseDeadlineFieldToLocalDate(isoValue) ?: LocalDate.now()
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val picked = LocalDate.of(year, month + 1, dayOfMonth)
                        onIsoChange(picked.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    },
                    initial.year,
                    initial.monthValue - 1,
                    initial.dayOfMonth,
                ).show()
            },
            enabled = enabled,
        ) {
            Text("Календар")
        }
    }
    if (isoValue.isNotEmpty()) {
        TextButton(onClick = { onIsoChange("") }, enabled = enabled) {
            Text("Очистити")
        }
    }
}
