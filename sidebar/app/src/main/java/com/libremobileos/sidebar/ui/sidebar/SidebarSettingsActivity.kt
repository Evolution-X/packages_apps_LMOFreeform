package com.libremobileos.sidebar.ui.sidebar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.libremobileos.sidebar.ui.theme.SidebarTheme

/**
 * @author KindBrave
 * @since 2023/10/21
 */
class SidebarSettingsActivity : ComponentActivity() {
    private val viewModel: SidebarSettingsViewModel by viewModels { SidebarSettingsViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            SidebarTheme {
                SettingsTheme {
                    SidebarSettingsPage(viewModel = viewModel)
                }
            }
        }
    }
}
