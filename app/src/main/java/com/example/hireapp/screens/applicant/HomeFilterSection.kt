package com.example.hireapp.screens.applicant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hireapp.data.Category

@Composable
fun SubCategorySection(
    subs: List<String>,
    selectedSubs: List<String>?,
    onSubToggled: (String) -> Unit
) {
    val chunkedSubs = subs.chunked(3)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chunkedSubs.forEach { rowSubs ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowSubs.forEach { sub ->
                    if (selectedSubs != null) {
                        FilterChip(
                            selected = selectedSubs.contains(sub),
                            onClick = { onSubToggled(sub) },
                            label = { Text(sub) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterSection(
    majorOptions: List<Category>,
    subOptionsMap: Map<String, List<String>>,
    selectedMajor: Category?,
    onMajorSelected: (Category) -> Unit,
    selectedSubs: List<String>?,
    onSubToggled: (String) -> Unit,
    onApplyFilter: () -> Unit,
    onResetFilter: () -> Unit
) {
    val chunkedMajors = majorOptions.chunked(2)

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("대분류 선택", style = MaterialTheme.typography.titleMedium)
        chunkedMajors.forEach {rowMajors ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowMajors.forEach { major ->
                    FilterChip(
                        selected = selectedMajor == major,
                        onClick = { onMajorSelected(major) },
                        label = { Text(major.label) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        selectedMajor?.let { major ->
            val subs = subOptionsMap[major.label].orEmpty()
            Text("소분류 선택", style = MaterialTheme.typography.titleMedium)
            SubCategorySection(
                subs = subs,
                selectedSubs = selectedSubs,
                onSubToggled = onSubToggled
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onApplyFilter) {
                Text("적용")
            }
            Button(onClick = onResetFilter) {
                Text("필터 초기화")
            }
        }
    }
}