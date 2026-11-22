package com.cayboard.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Contacts
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.cayboard.contacts.ContactSuggestions
import com.cayboard.settings.SettingsPreferences

@Composable
internal fun ContactSettingsSection() {
    val context = LocalContext.current
    val prefs = remember(context) { SettingsPreferences.getPrefsPublic(context) }
    var names by remember { mutableStateOf(prefs.getBoolean(ContactSuggestions.KEY_NAMES, false)) }
    var phones by remember { mutableStateOf(prefs.getBoolean(ContactSuggestions.KEY_PHONES, false)) }
    var pending by remember { mutableStateOf<String?>(null) }
    fun save(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        if (key == ContactSuggestions.KEY_NAMES) names = value else phones = value
        ContactSuggestions.sync(context)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pending?.let { save(it, true) }
        pending = null
    }
    fun change(key: String, value: Boolean) {
        if (value && !ContactSuggestions.hasPermission(context)) {
            pending = key
            permission.launch(Manifest.permission.READ_CONTACTS)
        } else save(key, value)
    }
    SettingsSection("通讯录候选") {
        SettingsToggleItem(Icons.TwoTone.Contacts, "联系人姓名", "输入姓名拼音时显示匹配的联系人，仅在本机内存中查询。",
            checked = names, onCheckedChange = { change(ContactSuggestions.KEY_NAMES, it) })
        HorizontalDivider()
        SettingsToggleItem(Icons.TwoTone.Contacts, "联系人号码", "输入姓名拼音时显示匹配的电话号码。",
            checked = phones, onCheckedChange = { change(ContactSuggestions.KEY_PHONES, it) })
    }
}
