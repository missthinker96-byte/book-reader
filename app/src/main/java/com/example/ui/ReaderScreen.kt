package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReaderTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: LibraryViewModel,
    onNavigateBack: () -> Unit
) {
    val activeBook by viewModel.activeBook.collectAsState()
    val currentTheme by viewModel.readerTheme.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val pageBitmap by viewModel.pageBitmap.collectAsState()
    val isLoading by viewModel.isReaderLoading.collectAsState()

    var showThemeMenu by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Reader Top App Bar with Book Title and Theme Selector
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeBook?.title ?: "PDF Reader",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = currentTheme.textPrimary
                        )
                        Text(
                            text = activeBook?.author ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = currentTheme.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Library",
                            tint = currentTheme.textPrimary
                        )
                    }
                },
                actions = {
                    // Zoom Out
                    IconButton(
                        onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.75f) },
                        modifier = Modifier.testTag("zoom_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomOut,
                            contentDescription = "Zoom out",
                            tint = currentTheme.textPrimary
                        )
                    }

                    // Zoom In
                    IconButton(
                        onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(3.0f) },
                        modifier = Modifier.testTag("zoom_in_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom in",
                            tint = currentTheme.textPrimary
                        )
                    }

                    // Theme selector button
                    IconButton(
                        onClick = { showThemeMenu = !showThemeMenu },
                        modifier = Modifier.testTag("theme_selector_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Change reading theme",
                            tint = currentTheme.accent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentTheme.surface.copy(alpha = 0.95f)
                )
            )

            // Prompt 3 Requirement: Keep themes (Paper Light, Slate Dark, Midnight Black) working in reader.
            AnimatedVisibility(visible = showThemeMenu) {
                Surface(
                    color = currentTheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reading Theme",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = currentTheme.textPrimary
                            )
                            IconButton(
                                onClick = { showThemeMenu = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = currentTheme.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReaderTheme.values().forEach { theme ->
                                val isSelected = currentTheme == theme
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setReaderTheme(theme)
                                    },
                                    label = {
                                        Text(
                                            text = theme.displayName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = theme.surface,
                                        labelColor = theme.textPrimary,
                                        selectedContainerColor = theme.accent.copy(alpha = 0.2f),
                                        selectedLabelColor = theme.accent
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) theme.accent else theme.textSecondary.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_chip_${theme.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // PDF Content Area with pinch-to-zoom and pan
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(0.75f, 3.5f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = currentTheme.accent,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Rendering Page ${currentPage + 1}...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = currentTheme.textSecondary
                        )
                    }
                } else if (pageBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = if (currentTheme.isDark) 2.dp else 6.dp,
                        color = when (currentTheme) {
                            ReaderTheme.PAPER_LIGHT -> Color(0xFFFCFAF5)
                            ReaderTheme.SLATE_DARK -> Color(0xFF242933)
                            ReaderTheme.MIDNIGHT_BLACK -> Color(0xFF101010)
                        },
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = offsetX
                                translationY = offsetY
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = currentTheme.textSecondary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Image(
                            bitmap = pageBitmap!!.asImageBitmap(),
                            contentDescription = "PDF Page ${currentPage + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pdf_page_image")
                        )
                    }
                } else {
                    Text(
                        text = "Unable to render PDF page. Tap Back to return.",
                        color = currentTheme.textSecondary
                    )
                }
            }

            // Reader Bottom Control Bar (Pagination, Page Slider)
            Surface(
                color = currentTheme.surface.copy(alpha = 0.95f),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Previous Page Button
                        IconButton(
                            onClick = {
                                viewModel.prevPage()
                                offsetX = 0f
                                offsetY = 0f
                            },
                            enabled = currentPage > 0,
                            modifier = Modifier.testTag("reader_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Page",
                                tint = if (currentPage > 0) currentTheme.textPrimary else currentTheme.textSecondary.copy(alpha = 0.3f)
                            )
                        }

                        // Page Counter
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = currentTheme.background,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Page ${currentPage + 1} of $totalPages",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = currentTheme.textPrimary,
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                    .testTag("page_counter_text")
                            )
                        }

                        // Next Page Button
                        IconButton(
                            onClick = {
                                viewModel.nextPage()
                                offsetX = 0f
                                offsetY = 0f
                            },
                            enabled = currentPage < totalPages - 1,
                            modifier = Modifier.testTag("reader_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Page",
                                tint = if (currentPage < totalPages - 1) currentTheme.textPrimary else currentTheme.textSecondary.copy(alpha = 0.3f)
                            )
                        }
                    }

                    // Page Scrub Slider
                    if (totalPages > 1) {
                        Slider(
                            value = currentPage.toFloat(),
                            onValueChange = {
                                viewModel.jumpToPage(it.toInt())
                                offsetX = 0f
                                offsetY = 0f
                            },
                            valueRange = 0f..(totalPages - 1).toFloat(),
                            steps = (totalPages - 2).coerceAtLeast(0),
                            colors = SliderDefaults.colors(
                                thumbColor = currentTheme.accent,
                                activeTrackColor = currentTheme.accent,
                                inactiveTrackColor = currentTheme.textSecondary.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .testTag("page_slider")
                        )
                    }
                }
            }
        }
    }
}
