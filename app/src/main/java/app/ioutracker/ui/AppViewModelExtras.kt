package app.ioutracker.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/**
 * The `CreationExtras` a ViewModel factory needs inside a navigation entry.
 *
 * Nav3 hands every back-stack entry its own `ViewModelStoreOwner` (see `appEntryDecorators` in
 * `Navigation.kt`) — that is what makes a ViewModel die with the screen that owns it. Its default
 * extras carry what `createSavedStateHandle()` reads, but unlike an Activity's they have no
 * `APPLICATION_KEY`, and every factory here reads that key to reach `AppContainer`. Without it
 * `this[APPLICATION_KEY]` is null and the cast in the initializer throws the moment the screen opens.
 *
 * So a screen inside a navigation entry passes this: `viewModel(factory = …, extras = …)`, which
 * replaces the owner's defaults rather than adding to them, hence the copy.
 * Composables outside `NavDisplay` resolve to the Activity and need nothing.
 */
@Composable
fun appViewModelExtras(): CreationExtras {
    val application = LocalContext.current.applicationContext as Application
    val owner = LocalViewModelStoreOwner.current
    return remember(application, owner) {
        val defaults = (owner as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras
        MutableCreationExtras(defaults ?: CreationExtras.Empty).apply { set(APPLICATION_KEY, application) }
    }
}
