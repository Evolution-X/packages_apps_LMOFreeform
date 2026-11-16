package com.libremobileos.sidebar.ui.sidebar

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_PROFILE_AVAILABLE
import android.content.Intent.ACTION_PROFILE_UNAVAILABLE
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.libremobileos.sidebar.app.SidebarApplication
import com.libremobileos.sidebar.bean.SidebarAppInfo
import com.libremobileos.sidebar.room.DatabaseRepository
import com.libremobileos.sidebar.service.ServiceViewModel.Companion.KEY_SHOW_PREDICTED_APPS
import com.libremobileos.sidebar.service.SidebarService
import com.libremobileos.sidebar.service.SidebarMonitorService
import com.libremobileos.sidebar.utils.Logger
import com.libremobileos.sidebar.utils.contains
import com.libremobileos.sidebar.utils.getSidebarFilteredUsers
import com.libremobileos.sidebar.utils.isResizeableActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Collections

/**
 * @author KindBrave
 * @since 2023/10/21
 */
class SidebarSettingsViewModel(private val application: Application) : AndroidViewModel(application) {
    private val logger = Logger("SidebarSettingsViewModel")
    private val repository = DatabaseRepository(application)
    private val allAppList = ArrayList<SidebarAppInfo>()
    val appListFlow: StateFlow<List<SidebarAppInfo>>
        get() = _appList.asStateFlow()
    private val _appList = MutableStateFlow<List<SidebarAppInfo>>(emptyList())
    private val appComparator = AppComparator()

    val isEnabled = UserHandle.myUserId() == 0
    private val appContext = application.applicationContext
    private lateinit var launcherApps: LauncherApps
    private lateinit var userManager: UserManager
    private lateinit var sp: SharedPreferences

    private val userProfileReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            logger.d("userProfileReceiver received ${intent.action}")
            initAllAppList()
        }
    }

    init {
        if (isEnabled) {
            logger.d("init")
            launcherApps = application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            userManager = application.getSystemService(Context.USER_SERVICE) as UserManager
            sp = appContext.getSharedPreferences(SidebarApplication.CONFIG, Context.MODE_PRIVATE)

            initAllAppList()
            observeSidebarApps()
            appContext.registerReceiverAsUser(
                userProfileReceiver,
                UserHandle.CURRENT,
                IntentFilter().apply {
                    addAction(ACTION_PROFILE_AVAILABLE)
                    addAction(ACTION_PROFILE_UNAVAILABLE)
                },
                null,
                null
            )
        }
    }

    override fun onCleared() {
        logger.d("onCleared")
        if (!isEnabled) return
        appContext.unregisterReceiver(userProfileReceiver)
    }

    fun getSidebarEnabled(): Boolean =
        isEnabled && sp.getBoolean(SidebarService.SIDELINE, false)

    fun setSidebarEnabled(enabled: Boolean) =
        sp.edit()
            .putBoolean(SidebarService.SIDELINE, enabled)
            .apply()

    fun addSidebarApp(appInfo: SidebarAppInfo) {
        updateAppPinnedState(appInfo.packageName, appInfo.activityName, appInfo.userId, true)
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSidebarApp(appInfo.packageName, appInfo.activityName, appInfo.userId)
        }
    }

    fun deleteSidebarApp(appInfo: SidebarAppInfo) {
        updateAppPinnedState(appInfo.packageName, appInfo.activityName, appInfo.userId, false)
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSidebarApp(appInfo.packageName, appInfo.activityName, appInfo.userId)
        }
    }

    private fun updateAppPinnedState(packageName: String, activityName: String, userId: Int, isPinned: Boolean) {
        synchronized(allAppList) {
            val index = allAppList.indexOfFirst {
                it.packageName == packageName && it.activityName == activityName && it.userId == userId
            }
            if (index != -1) {
                val current = allAppList[index]
                if (current.isSidebarApp != isPinned) {
                    allAppList[index] = current.copy(isSidebarApp = isPinned)
                    _appList.value = ArrayList(allAppList)
                }
            }
        }
    }

    private fun observeSidebarApps() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getAllSidebarAppsByFlow().collect { sidebarAppEntities ->
                val pinnedSet = sidebarAppEntities?.map {
                    Triple(it.packageName, it.activityName, it.userId)
                }?.toSet() ?: emptySet()

                synchronized(allAppList) {
                    if (allAppList.isEmpty()) return@collect
                    var changed = false
                    for (i in allAppList.indices) {
                        val app = allAppList[i]
                        val isPinned = pinnedSet.contains(Triple(app.packageName, app.activityName, app.userId))
                        if (app.isSidebarApp != isPinned) {
                            allAppList[i] = app.copy(isSidebarApp = isPinned)
                            changed = true
                        }
                    }
                    if (changed) {
                        _appList.value = ArrayList(allAppList)
                    }
                }
            }
        }
    }

    fun getPredictedAppsEnabled(): Boolean =
        sp.getBoolean(KEY_SHOW_PREDICTED_APPS, true)

    fun setPredictedAppsEnabled(enabled: Boolean) =
        sp.edit()
            .putBoolean(KEY_SHOW_PREDICTED_APPS, enabled)
            .apply()

    fun getAutoEnableSelectedAppsEnabled(): Boolean =
        sp.getBoolean(SidebarMonitorService.KEY_AUTO_ENABLE_SELECTED_APPS, false)

    fun setAutoEnableSelectedAppsEnabled(enabled: Boolean) =
        sp.edit()
            .putBoolean(SidebarMonitorService.KEY_AUTO_ENABLE_SELECTED_APPS, enabled)
            .apply()

    fun getFreeformControlMode(): String =
        Settings.System.getString(
            appContext.contentResolver,
            FREEFORM_CONTROL_MODE
        )?.let {
            when {
                it.equals(FREEFORM_BUTTON_MODE, ignoreCase = true) ||
                        it == LEGACY_BUTTON_MODE -> FREEFORM_BUTTON_MODE
                it.equals(FREEFORM_GESTURE_MODE, ignoreCase = true) ||
                        it == LEGACY_GESTURE_MODE -> FREEFORM_GESTURE_MODE
                else -> FREEFORM_GESTURE_MODE
            }
        } ?: FREEFORM_GESTURE_MODE

    fun setFreeformControlMode(mode: String) {
        Settings.System.putString(
            appContext.contentResolver,
            FREEFORM_CONTROL_MODE,
            mode
        )
    }

    private fun initAllAppList() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<SidebarAppInfo>()
            val sidebarAppList = repository.getAllSidebarWithoutLiveData()
            userManager.getSidebarFilteredUsers().forEach { userInfo ->
                logger.d("initAllAppList for user $userInfo")
                val activityList = launcherApps.getActivityList(null, userInfo.userHandle)

                activityList.forEach { info ->
                    val component = info.componentName
                    if (!application.isResizeableActivity(component)) {
                        logger.d("activity not resizeable, skipped $component")
                    } else {
                        list.add(
                            SidebarAppInfo(
                                "${info.label}${userInfo.suffix}",
                                info.getBadgedIcon(0),
                                component.packageName,
                                component.className,
                                userInfo.userId,
                                sidebarAppList?.contains(
                                    info.componentName.packageName,
                                    info.componentName.className,
                                    userInfo.userId
                                ) ?: false
                            )
                        )
                    }
                }
            }

            Collections.sort(list, appComparator)
            synchronized(allAppList) {
                allAppList.clear()
                allAppList.addAll(list)
                _appList.value = ArrayList(allAppList)
            }
            logger.d("emitted allAppList: ${allAppList.size} apps")
        }
    }

    companion object {
        private const val FREEFORM_CONTROL_MODE = "freeform_control_mode"
        const val FREEFORM_BUTTON_MODE = "Button"
        const val FREEFORM_GESTURE_MODE = "Gesture"
        private const val LEGACY_BUTTON_MODE = "0"
        private const val LEGACY_GESTURE_MODE = "1"

        val Factory = viewModelFactory {
            initializer {
                SidebarSettingsViewModel(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                )
            }
        }
    }

    private inner class AppComparator : Comparator<SidebarAppInfo> {
        override fun compare(p0: SidebarAppInfo, p1: SidebarAppInfo): Int {
            return when {
                // put checked items first
                p0.isSidebarApp && !p1.isSidebarApp -> -1
                p1.isSidebarApp && !p0.isSidebarApp -> 1
                else -> Collator.getInstance().compare(p0.label, p1.label)
            }
        }
    }
}
