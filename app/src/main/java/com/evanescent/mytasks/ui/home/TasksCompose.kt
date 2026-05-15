/*
 * Copyright (c) 2025. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

package com.evanescent.mytasks.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh // The modifier
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState // State for the modifier

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.evanescent.mytasks.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TasksCompose {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TasksScreen(
        onFabClick: () -> Unit,
        onMenuClick: () -> Unit
    ) {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val toolbarTitle = stringResource(id = R.string.app_name)

        var isRefreshing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        // Simpler state creation for the modifier approach
        val pullRefreshState = rememberPullToRefreshState()

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                // Only the TopAppBar's nested scroll is needed on the Scaffold here.
                // The pullToRefresh modifier will handle its own nested scroll internally.
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = toolbarTitle,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontFamily = FontFamily(Font(R.font.lexend_semibold))
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_menu_black_24dp),
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onFabClick,
                    containerColor = colorResource(id = R.color.floatingAddButton)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add_black_48dp),
                        contentDescription = stringResource(id = R.string.add_task_text),
                        tint = colorResource(id = R.color.addIconTint)
                    )
                }
            },
            bottomBar = {
                BottomAppBar(
                    actions = {
                        // You can add actions here
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    // Apply the pullToRefresh modifier here
                    .pullToRefresh(
                        state = pullRefreshState,
                        isRefreshing = isRefreshing, // Driven by your state
                        onRefresh = {
                            scope.launch {
                                isRefreshing = true
                                // Simulate actual refresh logic
                                delay(2000L) // e.g., fetch data from ViewModel
                                // After refresh is complete:
                                // Update your data if necessary
                                isRefreshing = false
                            }
                        },
                        // Optional: Customize the indicator
                        // indicator = {
                        //    PullToRefreshDefaults.Indicator(state = pullRefreshState, isRefreshing = isRefreshing)
                        // }
                    )
            ) {
                IncludeRecycler() // Your scrollable content

                // The indicator is drawn by the .pullToRefresh modifier by default.
                // If you provide a custom `indicator` lambda to .pullToRefresh, it will be used.
                // If you want to manually place the default indicator when using the modifier:
                // PullToRefreshDefaults.Indicator(
                //     modifier = Modifier.align(Alignment.TopCenter),
                //     state = pullRefreshState,
                //     isRefreshing = isRefreshing
                // )
                // However, usually, the modifier handles its placement well.
                // For this setup, the modifier's default indicator placement is sufficient.
            }
        }
    }

    @Composable
    fun IncludeRecycler() {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp), // Consider if FAB overlaps last item
            contentPadding = PaddingValues(16.dp),
            // state = rememberLazyListState() // Good practice if you need to control scroll position
        ) {
            items(20) { index -> // Increased items for better scroll testing
                TaskItem(index)
            }
        }
    }

    @Composable
    fun TaskItem(index: Int) { // Added index for differentiation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Text(
                text = "Tâche #$index",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

// Added Preview Function
@Preview(showBackground = true, name = "Tasks Screen Preview")
@Composable
fun TasksScreenPreview() {
    // It's good practice to wrap previews in your app's theme.
    // If you have a custom theme like `YourAppTheme`, use that.
    // Otherwise, a default MaterialTheme can be used.
    // For this example, assuming MaterialTheme is sufficient as the Composable uses it directly.
    MaterialTheme { // Replace with your actual app theme if you have one e.g., ToDoListTheme
        // Create an instance of TasksCompose to call its member function
        val tasksCompose = TasksCompose()
        tasksCompose.TasksScreen(
            onFabClick = {
                // Log or do nothing for preview
                println("FAB Clicked in Preview")
            },
            onMenuClick = {
                // Log or do nothing for preview
                println("Menu Clicked in Preview")
            }
        )
    }
}

