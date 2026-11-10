package com.nov.memocard.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nov.memocard.ui.MainViewModel
import com.nov.memocard.liveupdate.ReviewIntent
import com.nov.memocard.ui.screens.CardEditScreen
import com.nov.memocard.ui.screens.DeckDetailScreen
import com.nov.memocard.ui.screens.LibraryScreen
import com.nov.memocard.ui.screens.MineScreen
import com.nov.memocard.ui.screens.ReviewScreen

object Routes {
    const val REVIEW = "review"
    const val LIBRARY = "library"
    const val MINE = "mine"
    const val DECK_DETAIL = "deck/{deckId}"
    const val CARD_EDIT = "edit/{deckId}/{cardId}"

    fun deckDetail(deckId: Long) = "deck/$deckId"
    fun cardEdit(deckId: Long, cardId: Long = 0L) = "edit/$deckId/$cardId"
}

@Composable
fun AppRoot(mainViewModel: MainViewModel = viewModel()) {
    val nav = rememberNavController()
    val go: (String) -> Unit = { r ->
        nav.navigate(r) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(Unit) {
        ReviewIntent.openDeck.collect { id ->
            mainViewModel.settings.setReviewDeck(id)
            nav.navigate(Routes.REVIEW) {
                popUpTo(nav.graph.findStartDestination().id)
                launchSingleTop = true
            }
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.REVIEW,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.REVIEW) { ReviewScreen(onNavigate = go) }
            composable(Routes.LIBRARY) {
                LibraryScreen(onOpenDeck = { nav.navigate(Routes.deckDetail(it)) }, onNavigate = go)
            }
            composable(Routes.MINE) { MineScreen(onNavigate = go) }
            composable(
                Routes.DECK_DETAIL,
                arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
            ) {
                DeckDetailScreen(
                    onBack = { nav.popBackStack() },
                    onReview = { id ->
                        mainViewModel.settings.setReviewDeck(id)
                        nav.navigate(Routes.REVIEW) {
                            popUpTo(nav.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    },
                    onAddCard = { nav.navigate(Routes.cardEdit(it)) },
                    onEditCard = { d, c -> nav.navigate(Routes.cardEdit(d, c)) },
                )
            }
            composable(
                Routes.CARD_EDIT,
                arguments = listOf(
                    navArgument("deckId") { type = NavType.LongType },
                    navArgument("cardId") { type = NavType.LongType },
                ),
            ) {
                CardEditScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}