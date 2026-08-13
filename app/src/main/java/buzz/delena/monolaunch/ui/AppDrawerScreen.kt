package buzz.delena.monolaunch.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import buzz.delena.monolaunch.R
import buzz.delena.monolaunch.model.AppInfo
import buzz.delena.monolaunch.viewmodel.LauncherUiState
import kotlinx.coroutines.launch

private sealed class DrawerRow {
    data class SectionHeader(val letter: Char) : DrawerRow()
    data class Entry(val app: AppInfo) : DrawerRow()
}

/**
 * App Drawer: FREQUENT row, then an alphabetical ALL APPS list with an
 * A-Z quick-scroll index on the trailing edge. Reached by swiping up from
 * Home or tapping its search pill (gesture owned by the caller).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerScreen(
    uiState: LauncherUiState,
    onQueryChange: (String) -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onLongClickApp: (AppInfo) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filteredApps = remember(uiState.allApps, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            uiState.allApps
        } else {
            uiState.allApps.filter { it.label.contains(uiState.searchQuery, ignoreCase = true) }
        }
    }

    val rows = remember(filteredApps) {
        filteredApps
            .groupBy { app -> app.label.firstOrNull()?.uppercaseChar()?.takeIf(Char::isLetter) ?: '#' }
            .toSortedMap()
            .flatMap { (letter, apps) -> listOf(DrawerRow.SectionHeader(letter)) + apps.map(DrawerRow::Entry) }
    }

    val sectionStartIndex = remember(rows) {
        buildMap {
            rows.forEachIndexed { index, row ->
                if (row is DrawerRow.SectionHeader) put(row.letter, index)
            }
        }
    }
    val availableLetters = remember(sectionStartIndex) { sectionStartIndex.keys.sorted() }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClose),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }

        DrawerSearchField(
            query = uiState.searchQuery,
            onQueryChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
        )

        if (uiState.frequentApps.isNotEmpty() && uiState.searchQuery.isBlank()) {
            SectionLabel(text = stringResource(R.string.frequent_apps))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                items(uiState.frequentApps, key = { it.componentKey }) { app ->
                    AppIconTile(app = app, onClick = { onLaunchApp(app) }, onLongClick = { onLongClickApp(app) })
                }
            }
        }

        if (uiState.searchQuery.isBlank()) {
            SectionLabel(text = stringResource(R.string.all_apps))
        }

        Row(modifier = Modifier.fillMaxSize()) {
            if (rows.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    if (!uiState.isLoadingApps) {
                        Text(
                            text = stringResource(R.string.no_apps_found),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    items(rows, key = {
                        when (it) {
                            is DrawerRow.SectionHeader -> "header-${it.letter}"
                            is DrawerRow.Entry -> it.app.componentKey
                        }
                    }) { row ->
                        when (row) {
                            is DrawerRow.SectionHeader -> Text(
                                text = row.letter.toString(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 4.dp),
                            )
                            is DrawerRow.Entry -> AppListItem(app = row.app, onClick = { onLaunchApp(row.app) }, onLongClick = { onLongClickApp(row.app) })
                        }
                    }
                }

                if (availableLetters.isNotEmpty()) {
                    AzIndex(
                        letters = availableLetters,
                        onLetterSelected = { letter ->
                            sectionStartIndex[letter]?.let { index ->
                                coroutineScope.launch { listState.scrollToItem(index) }
                            }
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(28.dp)
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
private fun DrawerSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.fillMaxWidth(),
        )
        if (query.isEmpty()) {
            Text(
                text = stringResource(R.string.search_apps_placeholder),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 20.sp,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppIconTile(app: AppInfo, onClick: () -> Unit, onLongClick: () -> Unit) {
    val bitmap = remember(app.componentKey) { app.icon.toBitmap(width = 96, height = 96).asImageBitmap() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = app.label,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
            modifier = Modifier.size(48.dp).clip(CircleShape),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListItem(app: AppInfo, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
    ) {
        val bitmap = remember(app.componentKey) { app.icon.toBitmap(width = 96, height = 96).asImageBitmap() }
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
            modifier = Modifier.size(40.dp).clip(CircleShape),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = app.label, color = Color.White, fontSize = 16.sp, maxLines = 1)
    }
}

/** Drag or tap anywhere in this rail to jump the app list to that letter. */
@Composable
private fun AzIndex(
    letters: List<Char>,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .pointerInput(letters) {
                detectTapGestures { offset ->
                    onLetterSelected(letterAt(offset.y, size.height, letters))
                }
            }
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        onLetterSelected(letterAt(change.position.y, size.height, letters))
                    },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        letters.forEach { letter ->
            Text(
                text = letter.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun letterAt(y: Float, height: Int, letters: List<Char>): Char {
    val slot = (height.toFloat() / letters.size).coerceAtLeast(1f)
    val index = (y / slot).toInt().coerceIn(0, letters.lastIndex)
    return letters[index]
}
