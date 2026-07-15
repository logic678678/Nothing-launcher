package com.example.data

import kotlinx.coroutines.flow.Flow

class LauncherRepository(private val launcherDao: LauncherDao) {

    val pinnedApps: Flow<List<PinnedAppEntity>> = launcherDao.getPinnedAppsFlow()
    val hiddenApps: Flow<List<HiddenAppEntity>> = launcherDao.getHiddenAppsFlow()
    val quickNote: Flow<QuickNoteEntity?> = launcherDao.getQuickNoteFlow()
    val allSettings: Flow<List<SettingEntity>> = launcherDao.getAllSettingsFlow()

    suspend fun pinApp(packageName: String, activityName: String, label: String, orderIndex: Int) {
        launcherDao.insertPinnedApp(
            PinnedAppEntity(packageName, activityName, label, orderIndex)
        )
    }

    suspend fun unpinApp(packageName: String) {
        launcherDao.deletePinnedApp(packageName)
    }

    suspend fun clearAllPinnedApps() {
        launcherDao.clearPinnedApps()
    }

    suspend fun hideApp(packageName: String) {
        launcherDao.insertHiddenApp(HiddenAppEntity(packageName))
    }

    suspend fun unhideApp(packageName: String) {
        launcherDao.deleteHiddenApp(packageName)
    }

    suspend fun saveQuickNote(content: String) {
        launcherDao.insertQuickNote(QuickNoteEntity(content = content))
    }

    suspend fun saveSetting(key: String, value: String) {
        launcherDao.insertSetting(SettingEntity(key, value))
    }

    suspend fun getSettingValue(key: String, defaultValue: String): String {
        return launcherDao.getSettingByKey(key)?.value ?: defaultValue
    }
}
