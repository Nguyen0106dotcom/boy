package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.AlertLogEntry
import com.example.domain.model.AlertReason
import com.example.domain.model.AppUsageEntry
import com.example.domain.model.DailyUsageBar
import com.example.domain.model.DateFilter
import com.example.domain.model.LogEventType
import com.example.ui.KidGuardUiState
import com.example.ui.components.UsageAccessPermissionDialog
import com.example.usage.UsageStatsHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    uiState: KidGuardUiState,
    onClearHistory: () -> Unit,
    onRetryLoadUsage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showConfirmClear by remember { mutableStateOf(false) }
    var showUsageDisclosure by remember { mutableStateOf(false) }
    var selectedDateFilter by remember { mutableStateOf(DateFilter.ALL) }
    var selectedEventType by remember { mutableStateOf(LogEventType.ALL) }
    var selectedLogEntry by remember { mutableStateOf<AlertLogEntry?>(null) }

    val filteredLogs = remember(uiState.alertLogs, selectedDateFilter, selectedEventType) {
        val now = System.currentTimeMillis()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val sevenDaysAgo = now - 7L * 24 * 3600_000L

        uiState.alertLogs.filter { entry ->
            val dateMatches = when (selectedDateFilter) {
                DateFilter.ALL -> true
                DateFilter.TODAY -> entry.timestampMillis >= startOfToday
                DateFilter.LAST_7_DAYS -> entry.timestampMillis >= sevenDaysAgo
            }
            val typeMatches = when (selectedEventType) {
                LogEventType.ALL -> true
                else -> entry.eventType == selectedEventType
            }
            dateMatches && typeMatches
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 840.dp

        if (isWideScreen) {
            // Canonical List-Detail split pane for Tablets / Landscape
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Pane: Charts + Top Apps + Filterable Log List
                LazyColumn(
                    modifier = Modifier.weight(1.1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item { ReportsHeader() }
                    item {
                        AnalyticsStatusBanners(
                            uiState = uiState,
                            onOpenUsage = { showUsageDisclosure = true },
                            onRetryLoadUsage = onRetryLoadUsage
                        )
                    }
                    item {
                        WeeklyBarChartCard(
                            bars = uiState.weeklyBars,
                            isSampleData = uiState.isSampleAnalyticsData
                        )
                    }
                    item {
                        TrackedAppsUsageCard(
                            trackedApps = uiState.todayAppUsages,
                            topDeviceApps = uiState.topDeviceApps,
                            isSampleData = uiState.isSampleAnalyticsData
                        )
                    }
                    item {
                        LogFilterSection(
                            totalCount = filteredLogs.size,
                            selectedDateFilter = selectedDateFilter,
                            selectedEventType = selectedEventType,
                            onDateSelected = { selectedDateFilter = it },
                            onEventTypeSelected = { selectedEventType = it },
                            hasLogs = uiState.alertLogs.isNotEmpty(),
                            onRequestClear = { showConfirmClear = true }
                        )
                    }
                    if (filteredLogs.isEmpty()) {
                        item { EmptyLogsCard() }
                    } else {
                        items(filteredLogs, key = { it.id }) { entry ->
                            HistoryLogCard(
                                entry = entry,
                                isSelected = selectedLogEntry?.id == entry.id,
                                onClick = { selectedLogEntry = entry }
                            )
                        }
                    }
                }

                // Right Detail Pane
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxSize()
                ) {
                    val detailItem = selectedLogEntry ?: filteredLogs.firstOrNull()
                    if (detailItem != null) {
                        LogDetailPane(entry = detailItem)
                    } else {
                        EmptyLogsCard()
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 620.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { ReportsHeader() }

                    item {
                        AnalyticsStatusBanners(
                            uiState = uiState,
                            onOpenUsage = { showUsageDisclosure = true },
                            onRetryLoadUsage = onRetryLoadUsage
                        )
                    }

                    item {
                        WeeklyBarChartCard(
                            bars = uiState.weeklyBars,
                            isSampleData = uiState.isSampleAnalyticsData
                        )
                    }

                    item {
                        TrackedAppsUsageCard(
                            trackedApps = uiState.todayAppUsages,
                            topDeviceApps = uiState.topDeviceApps,
                            isSampleData = uiState.isSampleAnalyticsData
                        )
                    }

                    item {
                        LogFilterSection(
                            totalCount = filteredLogs.size,
                            selectedDateFilter = selectedDateFilter,
                            selectedEventType = selectedEventType,
                            onDateSelected = { selectedDateFilter = it },
                            onEventTypeSelected = { selectedEventType = it },
                            hasLogs = uiState.alertLogs.isNotEmpty(),
                            onRequestClear = { showConfirmClear = true }
                        )
                    }

                    if (filteredLogs.isEmpty()) {
                        item { EmptyLogsCard() }
                    } else {
                        items(filteredLogs, key = { it.id }) { entry ->
                            HistoryLogCard(
                                entry = entry,
                                isSelected = false,
                                onClick = { selectedLogEntry = entry }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = { Text("Xóa toàn bộ nhật ký?") },
            text = { Text("Lịch sử sự kiện và cảnh báo trên thiết bị sẽ bị xóa.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        selectedLogEntry = null
                        showConfirmClear = false
                    }
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showUsageDisclosure) {
        UsageAccessPermissionDialog(
            hasPermission = uiState.permissionsStatus.hasUsageAccess,
            onDismiss = { showUsageDisclosure = false },
            onOpenUsageAccessSettings = {
                showUsageDisclosure = false
                UsageStatsHelper.openUsageAccessSettings(context)
            }
        )
    }
}

@Composable
private fun ReportsHeader() {
    Column {
        Text(
            text = "Nhật ký & Thống kê",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Biểu đồ thời lượng 7 ngày, top ứng dụng dùng nhiều nhất và nhật ký sự kiện lọc theo ngày & loại",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AnalyticsStatusBanners(
    uiState: KidGuardUiState,
    onOpenUsage: () -> Unit,
    onRetryLoadUsage: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (uiState.isLoadingUsage) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Đang đồng bộ thống kê sử dụng từ hệ thống...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (!uiState.usageErrorMessage.isNullOrBlank()) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.usageErrorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    TextButton(onClick = onRetryLoadUsage) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thử lại")
                    }
                }
            }
        }

        if (!uiState.permissionsStatus.hasUsageAccess) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.PrivacyTip,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đang hiển thị dữ liệu xem trước mẫu",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "Cấp quyền Usage Access để hiển thị số phút thực tế từ thiết bị cho biểu đồ 7 ngày và top app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.92f)
                    )
                    Button(
                        onClick = onOpenUsage,
                        modifier = Modifier.testTag("reports_grant_usage_access_button")
                    ) {
                        Text("Cấp quyền Usage Access")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogFilterSection(
    totalCount: Int,
    selectedDateFilter: DateFilter,
    selectedEventType: LogEventType,
    onDateSelected: (DateFilter) -> Unit,
    onEventTypeSelected: (LogEventType) -> Unit,
    hasLogs: Boolean,
    onRequestClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Nhật ký sự kiện",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$totalCount sự kiện phù hợp bộ lọc",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (hasLogs) {
                IconButton(
                    onClick = onRequestClear,
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "Xóa nhật ký",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Date Filter Chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DateFilter.entries.forEach { df ->
                FilterChip(
                    selected = selectedDateFilter == df,
                    onClick = { onDateSelected(df) },
                    modifier = Modifier.testTag("date_filter_${df.name.lowercase()}"),
                    label = { Text(df.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        // Event Type Filter Chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LogEventType.entries.forEach { type ->
                FilterChip(
                    selected = selectedEventType == type,
                    onClick = { onEventTypeSelected(type) },
                    modifier = Modifier.testTag("type_filter_${type.name.lowercase()}"),
                    label = { Text(type.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyLogsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Không có sự kiện nào phù hợp",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Thử đổi bộ lọc ngày/loại sự kiện hoặc các lần chạm giới hạn, phát hiện trẻ em, khóa màn hình và xin thêm giờ sẽ xuất hiện tại đây.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LogDetailPane(entry: AlertLogEntry) {
    val formatter = remember {
        SimpleDateFormat("HH:mm:ss • dd/MM/yyyy", Locale.getDefault())
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Chi tiết sự kiện",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Image(
                painter = painterResource(id = entry.scareTheme.drawableRes),
                contentDescription = entry.scareTheme.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${entry.eventType.label} • ${entry.reason.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Text(
                text = entry.note,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Thời gian: ${formatter.format(Date(entry.timestampMillis))}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tổng thời lượng lúc ghi nhận: ${entry.elapsedMinutes} phút",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyBarChartCard(
    bars: List<DailyUsageBar>,
    isSampleData: Boolean
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.BarChart,
                        contentDescription = null,
                        tint = primaryColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Biểu đồ thời gian dùng 7 ngày",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (isSampleData) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "MẪU",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (bars.isEmpty()) {
                Text(
                    text = "Đang tải dữ liệu thống kê tuần...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxVal = maxOf(
                    bars.maxOf { maxOf(it.usedMinutes, it.limitMinutes) },
                    60
                ).toFloat()

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(top = 8.dp)
                ) {
                    val count = bars.size.coerceAtLeast(1)
                    val slotWidth = size.width / count
                    val barWidth = (slotWidth * 0.48f).coerceAtMost(36.dp.toPx())

                    val guideLimit = bars.lastOrNull()?.limitMinutes ?: 45
                    val guideY = size.height * (1f - (guideLimit / maxVal).coerceIn(0f, 1f))
                    drawLine(
                        color = primaryColor.copy(alpha = 0.5f),
                        start = Offset(0f, guideY),
                        end = Offset(size.width, guideY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    )

                    bars.forEachIndexed { idx, bar ->
                        val ratio = (bar.usedMinutes / maxVal).coerceIn(0f, 1f)
                        val barHeight = (size.height * ratio).coerceAtLeast(6.dp.toPx())
                        val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                        val top = size.height - barHeight
                        val barColor = when {
                            bar.usedMinutes > bar.limitMinutes -> errorColor
                            bar.usedMinutes >= (bar.limitMinutes * 0.8f) -> primaryColor
                            else -> tertiaryColor
                        }

                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(left, 0f),
                            size = Size(barWidth, size.height),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    bars.forEach { bar ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${bar.usedMinutes}p",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (bar.usedMinutes > bar.limitMinutes) {
                                    errorColor
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = bar.dayLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackedAppsUsageCard(
    trackedApps: List<AppUsageEntry>,
    topDeviceApps: List<AppUsageEntry>,
    isSampleData: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSampleData) {
                        "Ứng dụng giới hạn hôm nay (Mẫu)"
                    } else {
                        "Ứng dụng giới hạn hôm nay"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            trackedApps.forEach { entry ->
                val limit = entry.limitMinutes
                val progress = if (limit != null && limit > 0) {
                    (entry.usedMinutes.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val barColor = when {
                    limit == null -> MaterialTheme.colorScheme.outline
                    entry.usedMinutes >= limit -> MaterialTheme.colorScheme.error
                    entry.usedMinutes >= limit * 0.8f -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.tertiary
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = entry.appName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (limit != null) {
                                "${entry.usedMinutes} / $limit phút"
                            } else {
                                "${entry.usedMinutes} phút (Không giới hạn)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = barColor
                        )
                    }
                    if (limit != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    }
                }
            }

            if (topDeviceApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Top ứng dụng dùng nhiều nhất",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                topDeviceApps.forEach { top ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = top.appName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${top.usedMinutes} phút",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryLogCard(
    entry: AlertLogEntry,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val formatter = remember {
        SimpleDateFormat("HH:mm:ss • dd/MM/yyyy", Locale.getDefault())
    }
    val formattedTime = remember(entry.timestampMillis) {
        formatter.format(Date(entry.timestampMillis))
    }

    val badgeColor = when (entry.reason) {
        AlertReason.TIME_LIMIT_EXCEEDED, AlertReason.QUICK_LOCK -> MaterialTheme.colorScheme.error
        AlertReason.APP_LIMIT_EXCEEDED, AlertReason.FOCUS_MODE_LOCK -> MaterialTheme.colorScheme.primary
        AlertReason.BEDTIME_CURFEW, AlertReason.CHILD_DETECTED -> MaterialTheme.colorScheme.secondary
        AlertReason.MANUAL_TEST -> MaterialTheme.colorScheme.tertiary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = entry.scareTheme.drawableRes),
                contentDescription = entry.scareTheme.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "${entry.eventType.label} • ${entry.reason.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = entry.note,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$formattedTime • Đã dùng ${entry.elapsedMinutes} phút",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
