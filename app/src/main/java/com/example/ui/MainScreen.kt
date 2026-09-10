package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.DayType
import com.example.ui.components.CarModeDashboard
import com.example.ui.components.HourlyPriceList
import com.example.ui.components.PriceChartView
import com.example.ui.components.PriceSummaryCards
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ElectricityViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isCarMode by viewModel.isCarMode.collectAsStateWithLifecycle()
    val inspectedPoint by viewModel.inspectedPricePoint.collectAsStateWithLifecycle()

    if (isCarMode) {
        CarModeDashboard(
            uiState = uiState,
            inspectedPoint = inspectedPoint,
            onSelectDay = { viewModel.selectDay(it) },
            onInspectPoint = { viewModel.inspectPoint(it) },
            onRefresh = { viewModel.refresh() },
            onExitCarMode = { viewModel.setCarMode(false) },
            modifier = modifier
        )
        return
    }

    val currentSummary = uiState.activeSummary

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isLoading) AlertAmber else EmeraldGreen)
                            )
                        }
                        Text(
                            text = "Nord Pool spot sähkönhinta Suomi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Car Mode switch button
                    FilledTonalButton(
                        onClick = { viewModel.toggleCarMode() },
                        modifier = Modifier
                            .height(38.dp)
                            .padding(end = 4.dp)
                            .testTag("toggle_car_mode_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Autonäyttö",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Autotila",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.testTag("refresh_btn")
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.refresh)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Three Tabs: Eilinen, Tänään, Huominen
            val tabs = listOf(
                DayType.YESTERDAY to uiState.yesterday,
                DayType.TODAY to uiState.today,
                DayType.TOMORROW to uiState.tomorrow
            )

            SecondaryTabRow(
                selectedTabIndex = tabs.indexOfFirst { it.first == uiState.selectedDay }.coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabs.indexOfFirst { it.first == uiState.selectedDay }.coerceAtLeast(0)),
                        color = ElectricCyan,
                        height = 3.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("day_tabs")
            ) {
                tabs.forEachIndexed { index, (dayType, summary) ->
                    val isSelected = uiState.selectedDay == dayType
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.selectDay(dayType) },
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("tab_${dayType.name.lowercase()}"),
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayType.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = summary.formattedDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    )
                }
            }

            // Error banner if any
            if (uiState.errorMessage != null && !currentSummary.isAvailable) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = uiState.errorMessage ?: "Verkkovirhe",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Scrollable Content: Chart, Stats, Hourly Breakdown
            AnimatedContent(
                targetState = uiState.selectedDay,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "day_content"
            ) { targetDay ->
                val daySummary = when (targetDay) {
                    DayType.YESTERDAY -> uiState.yesterday
                    DayType.TODAY -> uiState.today
                    DayType.TOMORROW -> uiState.tomorrow
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Spot Price Real-Time Chart
                    PriceChartView(
                        summary = daySummary,
                        inspectedPoint = inspectedPoint,
                        onInspectPoint = { viewModel.inspectPoint(it) }
                    )

                    // Daily Statistics Cards
                    PriceSummaryCards(summary = daySummary)

                    // 24h Hourly Breakdown
                    HourlyPriceList(
                        summary = daySummary,
                        inspectedPoint = inspectedPoint,
                        onSelectPoint = { viewModel.inspectPoint(it) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
