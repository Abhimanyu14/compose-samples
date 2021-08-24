/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetnews.ui.home

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.jetnews.R
import com.example.jetnews.data.Result
import com.example.jetnews.data.posts.impl.BlockingFakePostsRepository
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostsFeed
import com.example.jetnews.ui.JetnewsDestinations
import com.example.jetnews.ui.article.PostContent
import com.example.jetnews.ui.components.AppNavRail
import com.example.jetnews.ui.components.InsetAwareTopAppBar
import com.example.jetnews.ui.components.JetnewsSnackbarHost
import com.example.jetnews.ui.theme.JetnewsTheme
import com.example.jetnews.utils.isScrolled
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.systemBarsPadding
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking

/**
 * The home screen displaying a list and a detail.
 */
@Composable
fun HomeListDetailScreen(
    uiState: HomeUiState,
    showNavRail: Boolean,
    onToggleFavorite: (String) -> Unit,
    onSelectPost: (String) -> Unit,
    onRefreshPosts: () -> Unit,
    onErrorDismiss: (Long) -> Unit,
    onInteractWithList: () -> Unit,
    onInteractWithDetail: (String) -> Unit,
    openDrawer: () -> Unit,
    navigateToInterests: () -> Unit,
    homeListLazyListState: LazyListState,
    articleDetailLazyListStates: Map<String, LazyListState>,
    scaffoldState: ScaffoldState
) {
    HomeScreenWithList(
        uiState = uiState,
        showNavRail = showNavRail,
        onRefreshPosts = onRefreshPosts,
        onErrorDismiss = onErrorDismiss,
        openDrawer = openDrawer,
        navigateToInterests = navigateToInterests,
        homeListLazyListState = homeListLazyListState,
        scaffoldState = scaffoldState
    ) { hasPostsUiState, modifier ->
        Row {
            PostList(
                postsFeed = hasPostsUiState.postsFeed,
                onArticleTapped = onSelectPost,
                favorites = hasPostsUiState.favorites,
                onToggleFavorite = onToggleFavorite,
                contentPadding = rememberInsetsPaddingValues(
                    insets = LocalWindowInsets.current.systemBars,
                    applyTop = false,
                    applyEnd = false,
                ),
                modifier = modifier
                    .width(334.dp)
                    .pointerInput(Unit) {
                        while (currentCoroutineContext().isActive) {
                            awaitPointerEventScope {
                                awaitPointerEvent(PointerEventPass.Initial)
                                onInteractWithList()
                            }
                        }
                    },
                state = homeListLazyListState
            )
            // Crossfade between different detail posts
            Crossfade(targetState = hasPostsUiState.selectedPost) { detailPost ->
                // Get the lazy list state for this detail view
                val detailLazyListState by derivedStateOf {
                    articleDetailLazyListStates.getValue(detailPost.id)
                }

                // Key against the post id to avoid sharing any state between different posts
                key(detailPost.id) {
                    PostContent(
                        post = detailPost,
                        modifier = modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                while (currentCoroutineContext().isActive) {
                                    awaitPointerEventScope {
                                        awaitPointerEvent(PointerEventPass.Initial)
                                        onInteractWithDetail(detailPost.id)
                                    }
                                }
                            },
                        contentPadding = rememberInsetsPaddingValues(
                            insets = LocalWindowInsets.current.systemBars,
                            applyTop = false,
                            applyStart = false,
                        ),
                        state = detailLazyListState
                    )
                }
            }
        }
    }
}

/**
 * The home screen displaying a list.
 */
@Composable
fun HomeListScreen(
    uiState: HomeUiState,
    showNavRail: Boolean,
    onToggleFavorite: (String) -> Unit,
    onSelectPost: (String) -> Unit,
    onRefreshPosts: () -> Unit,
    onErrorDismiss: (Long) -> Unit,
    onInteractWithList: () -> Unit,
    openDrawer: () -> Unit,
    navigateToInterests: () -> Unit,
    homeListLazyListState: LazyListState,
    scaffoldState: ScaffoldState
) {
    HomeScreenWithList(
        uiState = uiState,
        showNavRail = showNavRail,
        onRefreshPosts = onRefreshPosts,
        onErrorDismiss = onErrorDismiss,
        openDrawer = openDrawer,
        navigateToInterests = navigateToInterests,
        homeListLazyListState = homeListLazyListState,
        scaffoldState = scaffoldState
    ) { hasPostsUiState, modifier ->
        PostList(
            postsFeed = hasPostsUiState.postsFeed,
            onArticleTapped = onSelectPost,
            favorites = hasPostsUiState.favorites,
            onToggleFavorite = onToggleFavorite,
            contentPadding = rememberInsetsPaddingValues(
                insets = LocalWindowInsets.current.systemBars,
                applyTop = false,
            ),
            modifier = modifier
                .pointerInput(Unit) {
                    while (currentCoroutineContext().isActive) {
                        awaitPointerEventScope {
                            awaitPointerEvent(PointerEventPass.Initial)
                            onInteractWithList()
                        }
                    }
                },
            state = homeListLazyListState
        )
    }
}

/**
 * A display of the home screen that has the list.
 *
 * This sets up the scaffold with the top app bar, and surrounds the [hasPostsContent] with refresh,
 * loading and error handling.
 *
 * This helper functions exists because [HomeListDetailScreen] and [HomeListScreen] are extremely
 * similar, except for the rendered content when there are posts to display.
 */
@Composable
private fun HomeScreenWithList(
    uiState: HomeUiState,
    showNavRail: Boolean,
    onRefreshPosts: () -> Unit,
    onErrorDismiss: (Long) -> Unit,
    openDrawer: () -> Unit,
    navigateToInterests: () -> Unit,
    homeListLazyListState: LazyListState,
    scaffoldState: ScaffoldState,
    hasPostsContent: @Composable (
        uiState: HomeUiState.HasPosts,
        modifier: Modifier
    ) -> Unit
) {
    Row(Modifier.fillMaxSize()) {
        if (showNavRail) {
            AppNavRail(
                currentRoute = JetnewsDestinations.HOME_ROUTE,
                navigateToHome = { /* Do nothing */ },
                navigateToInterests = navigateToInterests
            )
        }
        Scaffold(
            scaffoldState = scaffoldState,
            snackbarHost = {
                JetnewsSnackbarHost(hostState = it, modifier = Modifier.systemBarsPadding())
            },
            topBar = {
                val title = stringResource(id = R.string.app_name)
                InsetAwareTopAppBar(
                    title = {
                        Icon(
                            painter = painterResource(R.drawable.ic_jetnews_wordmark),
                            contentDescription = title,
                            tint = MaterialTheme.colors.onBackground,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 4.dp, top = 10.dp)
                        )
                    },
                    navigationIcon = if (!showNavRail) {
                        {
                            IconButton(onClick = openDrawer) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_jetnews_logo),
                                    contentDescription = stringResource(R.string.cd_open_navigation_drawer),
                                    tint = MaterialTheme.colors.primary
                                )
                            }
                        }
                    } else {
                        null
                    },
                    actions = {
                        IconButton(onClick = { /* TODO: Open search */ }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = stringResource(R.string.cd_search)
                            )
                        }
                    },
                    backgroundColor = MaterialTheme.colors.surface,
                    elevation = if (!homeListLazyListState.isScrolled) 0.dp else 4.dp
                )
            },
        ) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)

            LoadingContent(
                empty = when (uiState) {
                    is HomeUiState.HasPosts -> false
                    is HomeUiState.NoPosts -> uiState.isLoading
                },
                emptyContent = { FullScreenLoading() },
                loading = uiState.isLoading,
                onRefresh = onRefreshPosts,
                content = {
                    when (uiState) {
                        is HomeUiState.HasPosts -> hasPostsContent(uiState, modifier)
                        is HomeUiState.NoPosts -> {
                            if (uiState.errorMessages.isEmpty()) {
                                // if there are no posts, and no error, let the user refresh manually
                                TextButton(
                                    onClick = onRefreshPosts,
                                    modifier.fillMaxSize()
                                ) {
                                    Text(
                                        stringResource(id = R.string.home_tap_to_load_content),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                // there's currently an error showing, don't show any content
                                Box(modifier.fillMaxSize()) { /* empty screen */ }
                            }
                        }
                    }
                }
            )
        }
    }

    // Process one error message at a time and show them as Snackbars in the UI
    if (uiState.errorMessages.isNotEmpty()) {
        // Remember the errorMessage to display on the screen
        val errorMessage = remember(uiState) { uiState.errorMessages[0] }

        // Get the text to show on the message from resources
        val errorMessageText: String = stringResource(errorMessage.messageId)
        val retryMessageText = stringResource(id = R.string.retry)

        // If onRefreshPosts or onErrorDismiss change while the LaunchedEffect is running,
        // don't restart the effect and use the latest lambda values.
        val onRefreshPostsState by rememberUpdatedState(onRefreshPosts)
        val onErrorDismissState by rememberUpdatedState(onErrorDismiss)

        // Effect running in a coroutine that displays the Snackbar on the screen
        // If there's a change to errorMessageText, retryMessageText or scaffoldState,
        // the previous effect will be cancelled and a new one will start with the new values
        LaunchedEffect(errorMessageText, retryMessageText, scaffoldState) {
            val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                message = errorMessageText,
                actionLabel = retryMessageText
            )
            if (snackbarResult == SnackbarResult.ActionPerformed) {
                onRefreshPostsState()
            }
            // Once the message is displayed and dismissed, notify the ViewModel
            onErrorDismissState(errorMessage.id)
        }
    }
}

/**
 * Display an initial empty state or swipe to refresh content.
 *
 * @param empty (state) when true, display [emptyContent]
 * @param emptyContent (slot) the content to display for the empty state
 * @param loading (state) when true, display a loading spinner over [content]
 * @param onRefresh (event) event to request refresh
 * @param content (slot) the main content to show
 */
@Composable
private fun LoadingContent(
    empty: Boolean,
    emptyContent: @Composable () -> Unit,
    loading: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit
) {
    if (empty) {
        emptyContent()
    } else {
        SwipeRefresh(
            state = rememberSwipeRefreshState(loading),
            onRefresh = onRefresh,
            content = content,
        )
    }
}

/**
 * Display a feed of posts.
 *
 * When a post is clicked on, [onArticleTapped] will be called.
 *
 * @param postsFeed (state) the feed to display
 * @param onArticleTapped (event) request navigation to Article screen
 * @param modifier modifier for the root element
 */
@Composable
private fun PostList(
    postsFeed: PostsFeed,
    onArticleTapped: (postId: String) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding
    ) {
        item { PostListTopSection(postsFeed.highlightedPost, onArticleTapped) }
        if (postsFeed.recommendedPosts.isNotEmpty()) {
            item {
                PostListSimpleSection(
                    postsFeed.recommendedPosts,
                    onArticleTapped,
                    favorites,
                    onToggleFavorite
                )
            }
        }
        if (postsFeed.popularPosts.isNotEmpty()) {
            item { PostListPopularSection(postsFeed.popularPosts, onArticleTapped) }
        }
        if (postsFeed.recentPosts.isNotEmpty()) {
            item { PostListHistorySection(postsFeed.recentPosts, onArticleTapped) }
        }
    }
}

/**
 * Full screen circular progress indicator
 */
@Composable
private fun FullScreenLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Top section of [PostList]
 *
 * @param post (state) highlighted post to display
 * @param navigateToArticle (event) request navigation to Article screen
 */
@Composable
private fun PostListTopSection(post: Post, navigateToArticle: (String) -> Unit) {
    Text(
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        text = stringResource(id = R.string.home_top_section_title),
        style = MaterialTheme.typography.subtitle1
    )
    PostCardTop(
        post = post,
        modifier = Modifier.clickable(onClick = { navigateToArticle(post.id) })
    )
    PostListDivider()
}

/**
 * Full-width list items for [PostList]
 *
 * @param posts (state) to display
 * @param navigateToArticle (event) request navigation to Article screen
 */
@Composable
private fun PostListSimpleSection(
    posts: List<Post>,
    navigateToArticle: (String) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit
) {
    Column {
        posts.forEach { post ->
            PostCardSimple(
                post = post,
                navigateToArticle = navigateToArticle,
                isFavorite = favorites.contains(post.id),
                onToggleFavorite = { onToggleFavorite(post.id) }
            )
            PostListDivider()
        }
    }
}

/**
 * Horizontal scrolling cards for [PostList]
 *
 * @param posts (state) to display
 * @param navigateToArticle (event) request navigation to Article screen
 */
@Composable
private fun PostListPopularSection(
    posts: List<Post>,
    navigateToArticle: (String) -> Unit
) {
    Column {
        Text(
            modifier = Modifier.padding(16.dp),
            text = stringResource(id = R.string.home_popular_section_title),
            style = MaterialTheme.typography.subtitle1
        )

        LazyRow(modifier = Modifier.padding(end = 16.dp)) {
            items(posts) { post ->
                PostCardPopular(
                    post,
                    navigateToArticle,
                    Modifier.padding(start = 16.dp, bottom = 16.dp)
                )
            }
        }
        PostListDivider()
    }
}

/**
 * Full-width list items that display "based on your history" for [PostList]
 *
 * @param posts (state) to display
 * @param navigateToArticle (event) request navigation to Article screen
 */
@Composable
private fun PostListHistorySection(
    posts: List<Post>,
    navigateToArticle: (String) -> Unit
) {
    Column {
        posts.forEach { post ->
            PostCardHistory(post, navigateToArticle)
            PostListDivider()
        }
    }
}

/**
 * Full-width divider with padding for [PostList]
 */
@Composable
private fun PostListDivider() {
    Divider(
        modifier = Modifier.padding(horizontal = 14.dp),
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f)
    )
}

@Preview("Home list drawer screen")
@Preview("Home list drawer screen (dark)", uiMode = UI_MODE_NIGHT_YES)
@Preview("Home list drawer screen (big font)", fontScale = 1.5f)
@Composable
fun PreviewHomeListDrawerScreen() {
    val postsFeed = runBlocking {
        (BlockingFakePostsRepository().getPostsFeed() as Result.Success).data
    }
    JetnewsTheme {
        HomeListScreen(
            uiState = HomeUiState.HasPosts(
                postsFeed = postsFeed,
                selectedPost = postsFeed.highlightedPost,
                isArticleOpen = false,
                favorites = emptySet(),
                isLoading = false,
                errorMessages = emptyList()
            ),
            showNavRail = false,
            onToggleFavorite = {},
            onSelectPost = {},
            onRefreshPosts = {},
            onErrorDismiss = {},
            onInteractWithList = {},
            openDrawer = {},
            navigateToInterests = {},
            homeListLazyListState = rememberLazyListState(),
            scaffoldState = rememberScaffoldState()
        )
    }
}

@Preview("Home list navrail screen", device = Devices.NEXUS_7_2013)
@Preview("Home list navrail screen (dark)", uiMode = UI_MODE_NIGHT_YES, device = Devices.NEXUS_7_2013)
@Preview("Home list navrail screen (big font)", fontScale = 1.5f, device = Devices.NEXUS_7_2013)
@Composable
fun PreviewHomeListNavRailScreen() {
    val postsFeed = runBlocking {
        (BlockingFakePostsRepository().getPostsFeed() as Result.Success).data
    }
    JetnewsTheme {
        HomeListScreen(
            uiState = HomeUiState.HasPosts(
                postsFeed = postsFeed,
                selectedPost = postsFeed.highlightedPost,
                isArticleOpen = false,
                favorites = emptySet(),
                isLoading = false,
                errorMessages = emptyList()
            ),
            showNavRail = true,
            onToggleFavorite = {},
            onSelectPost = {},
            onRefreshPosts = {},
            onErrorDismiss = {},
            onInteractWithList = {},
            openDrawer = {},
            navigateToInterests = {},
            homeListLazyListState = rememberLazyListState(),
            scaffoldState = rememberScaffoldState()
        )
    }
}

@Preview("Home list detail screen", device = Devices.PIXEL_C)
@Preview("Home list detail screen (dark)", uiMode = UI_MODE_NIGHT_YES, device = Devices.PIXEL_C)
@Preview("Home list detail screen (big font)", fontScale = 1.5f, device = Devices.PIXEL_C)
@Composable
fun PreviewHomeListDetailScreen() {
    val postsFeed = runBlocking {
        (BlockingFakePostsRepository().getPostsFeed() as Result.Success).data
    }
    JetnewsTheme {
        HomeListDetailScreen(
            uiState = HomeUiState.HasPosts(
                postsFeed = postsFeed,
                selectedPost = postsFeed.highlightedPost,
                isArticleOpen = false,
                favorites = emptySet(),
                isLoading = false,
                errorMessages = emptyList()
            ),
            showNavRail = true,
            onToggleFavorite = {},
            onSelectPost = {},
            onRefreshPosts = {},
            onErrorDismiss = {},
            onInteractWithList = {},
            onInteractWithDetail = {},
            openDrawer = {},
            navigateToInterests = {},
            homeListLazyListState = rememberLazyListState(),
            articleDetailLazyListStates = postsFeed.allPosts.associate { post ->
                key(post.id) {
                    post.id to rememberLazyListState()
                }
            },
            scaffoldState = rememberScaffoldState()
        )
    }
}
