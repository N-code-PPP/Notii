package com.example

import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.PopupMenu
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.local.LocalWorkspaceStore
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.notification.ReminderNotificationHelper
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ProvideSafeTextToolbar
import com.example.ui.viewmodel.NoteViewModel
import com.example.widget.NoteAppWidgetProvider
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private var pendingWidgetAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderNotificationHelper.ensureNotificationChannel(this)
        handleIncomingIntent(intent)

        setContent {
            val context = LocalContext.current
            val prefsRepo = remember(context) { UserPreferencesRepository(context) }
            val prefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(
                initialValue = UserPreferences()
            )

            MyApplicationTheme(darkTheme = prefs.isDarkMode) {
                NotedAppRoot(
                    initialQuickAction = pendingWidgetAction,
                    onConsumeQuickAction = { pendingWidgetAction = null }
                )
            }
        }
    }

    override fun onWindowStartingActionMode(
        callback: ActionMode.Callback?,
        type: Int
    ): ActionMode? {
        if (type == ActionMode.TYPE_FLOATING) {
            return SafeNoOpFloatingActionMode(this)
        }
        return super.onWindowStartingActionMode(callback, type)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val reminderNoteId = intent?.getStringExtra(ReminderNotificationHelper.EXTRA_NOTE_ID)
        when {
            intent?.action == NoteAppWidgetProvider.ACTION_QUICK_NOTE -> pendingWidgetAction = "QUICK_NOTE"
            intent?.action == NoteAppWidgetProvider.ACTION_QUICK_CAMERA -> pendingWidgetAction = "QUICK_CAMERA"
            intent?.action == NoteAppWidgetProvider.ACTION_VOICE_NOTE -> pendingWidgetAction = "VOICE_NOTE"
            !reminderNoteId.isNullOrBlank() -> pendingWidgetAction = "OPEN_NOTE:$reminderNoteId"
        }
    }
}

private class SafeNoOpFloatingActionMode(
    private val activity: ComponentActivity
) : ActionMode() {
    private val backingMenu: Menu = PopupMenu(activity, activity.window.decorView).menu

    init {
        type = TYPE_FLOATING
    }

    override fun setTitle(title: CharSequence?) {}
    override fun setTitle(resId: Int) {}
    override fun setSubtitle(subtitle: CharSequence?) {}
    override fun setSubtitle(resId: Int) {}
    override fun setCustomView(view: View?) {}
    override fun invalidate() {}
    override fun finish() {}
    override fun getMenu(): Menu = backingMenu
    override fun getTitle(): CharSequence = ""
    override fun getSubtitle(): CharSequence = ""
    override fun getCustomView(): View? = null
    override fun getMenuInflater(): MenuInflater = MenuInflater(activity)
}

@Composable
fun NotedAppRoot(
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    initialQuickAction: String? = null,
    onConsumeQuickAction: () -> Unit = {}
) {
    ProvideSafeTextToolbar {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val credentialManager = remember(context) { CredentialManager.create(context) }
        val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
        var showSignInScreen by remember { mutableStateOf(false) }

        val user = currentUser
        if (showSignInScreen && user == null) {
            AuthScreen(
                onAuthSuccess = { showSignInScreen = false },
                onContinueWithoutSignIn = { showSignInScreen = false }
            )
        } else {
            val effectiveUserId = user?.uid ?: LocalWorkspaceStore.LOCAL_USER_ID
            val noteViewModel: NoteViewModel = viewModel(
                key = effectiveUserId,
                factory = NoteViewModel.factory(effectiveUserId)
            )
            WorkspaceScreen(
                viewModel = noteViewModel,
                initialQuickAction = initialQuickAction,
                onConsumeQuickAction = onConsumeQuickAction,
                onRequestSignIn = { showSignInScreen = true },
                onSignOut = {
                    if (user != null) {
                        signOut(
                            credentialManager = credentialManager,
                            onSignOutComplete = { showSignInScreen = false },
                            scope = coroutineScope
                        )
                    } else {
                        showSignInScreen = true
                    }
                }
            )
        }
    }
}
