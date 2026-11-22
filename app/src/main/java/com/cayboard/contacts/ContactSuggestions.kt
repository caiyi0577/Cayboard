package com.cayboard.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.cayboard.handwriting.HanziPinyin
import com.cayboard.settings.SettingsPreferences
import com.cayboard.util.FileLogger
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** Native, in-memory contact suggestions; contact data is never written to disk. */
object ContactSuggestions {
    private const val TAG = "ContactSuggestions"
    const val KEY_NAMES = "contacts_names"
    const val KEY_PHONES = "contacts_phones"
    private const val MAX_CONTACTS = 5000
    private const val RELOAD_DELAY_MS = 2_000L

    @Volatile private var index: ContactIndex? = null
    @Volatile private var showNames = true
    @Volatile private var showPhones = true
    /** 每次清空或重读加一：在途的后台读取发现代际变了就丢掉结果。 */
    private val generation = AtomicInteger()
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "contacts-index").apply { isDaemon = true } }
    private val main = Handler(Looper.getMainLooper())
    private val RELOAD_TOKEN = Any()
    private var observer: ContentObserver? = null
    private var observedApp: Context? = null

    val isActive: Boolean get() = index != null

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun sync(context: Context) {
        val app = context.applicationContext
        val prefs = SettingsPreferences.getPrefsPublic(app)
        showNames = prefs.getBoolean(KEY_NAMES, false)
        showPhones = prefs.getBoolean(KEY_PHONES, false)
        if ((!showNames && !showPhones) || !hasPermission(app)) { clear(); return }
        ensureObserver(app)
        val gen = generation.get()
        if (index == null) worker.execute { if (generation.get() == gen) load(app, gen) }
    }

    /** 丢掉内存里的通讯录，不再监听变化。 */
    fun clear() {
        generation.incrementAndGet()
        index = null
        val app = observedApp
        val current = observer
        if (app != null && current != null) runCatching { app.contentResolver.unregisterContentObserver(current) }
        observer = null
        observedApp = null
    }

    /** 当前编码对得上的人名、号码（密码框由调用方挡）。按键线程调用，只查内存。 */
    internal fun itemsFor(letters: String?, digits: String?): List<ContactItem> {
        val table = index ?: return emptyList()
        if (letters == null && digits == null) return emptyList()
        val names = showNames
        val phones = showPhones
        if (!names && !phones) return emptyList()
        val hits = table.lookup(letters, digits)
        if (hits.isEmpty()) return emptyList()
        return ContactCandidates.items(hits, names, phones)
    }

    /** 剪贴板历史用：这段字是联系人的名字或号码时不记录。 */
    fun isContactText(text: CharSequence?): Boolean {
        val table = index ?: return false
        return text != null && table.containsText(text.toString())
    }

    private fun ensureObserver(app: Context) {
        if (observer != null) return
        val created = object : ContentObserver(main) {
            override fun onChange(selfChange: Boolean) {
                // 通讯录改了：过一会儿重读一次（批量导入时只读最后一次）。
                main.removeCallbacksAndMessages(RELOAD_TOKEN)
                main.postAtTime({
                    if (observer === this && hasPermission(app)) {
                        val gen = generation.incrementAndGet()
                        worker.execute { if (generation.get() == gen) load(app, gen) }
                    }
                }, RELOAD_TOKEN, android.os.SystemClock.uptimeMillis() + RELOAD_DELAY_MS)
            }
        }
        runCatching {
            app.contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, created)
            observer = created
            observedApp = app
        }.onFailure { FileLogger.w(TAG, "通讯录变化监听失败：${it.message}") }
    }

    private fun load(app: Context, gen: Int) {
        try {
            val contacts = read(app)
            val built = ContactIndex.build(contacts, HanziPinyin.readTable(app)::lookup)
            if (generation.get() == gen) index = built
            // 只记条数，不记名字。
            FileLogger.i(TAG, "通讯录已读入：${built.size} 人")
        } catch (error: SecurityException) {
            if (generation.get() == gen) index = null
        } catch (error: Exception) {
            FileLogger.w(TAG, "通讯录读取失败：${error.javaClass.simpleName}")
        }
    }

    private fun read(app: Context): List<ContactIndex.RawContact> {
        val resolver = app.contentResolver
        val names = LinkedHashMap<Long, String>()
        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext() && names.size < MAX_CONTACTS) {
                val name = c.getString(1)?.takeIf { it.isNotBlank() } ?: continue
                names[c.getLong(0)] = name
            }
        }
        // 号码：手机排前面，其次是设为默认的号码。
        val phones = HashMap<Long, MutableList<Pair<Int, String>>>()
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY,
            ),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                if (id !in names) continue
                val number = c.getString(1)?.takeIf { it.isNotBlank() } ?: continue
                val rank = when {
                    c.getInt(2) == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> 0
                    c.getInt(3) != 0 -> 1
                    else -> 2
                }
                phones.getOrPut(id) { ArrayList(1) } += rank to number
            }
        }
        return names.map { (id, name) ->
            ContactIndex.RawContact(name, phones[id].orEmpty().sortedBy { it.first }.map { it.second })
        }
    }
}
