package lt.tbu.a9.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Kontaktai su telefonais; įkeliami tik kai suteiktas READ_CONTACTS ir funkcija įjungta. */
class ContactRepository(private val context: Context, private val scope: CoroutineScope) {
    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val contacts: StateFlow<List<ContactItem>> = _contacts

    private var enabled = false
    private var observing = false
    private var job: Job? = null

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = scheduleRefresh()
    }

    fun hasPermission() =
        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun setEnabled(value: Boolean) {
        enabled = value
        if (value && hasPermission()) {
            if (!observing) {
                context.contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
                observing = true
            }
            scheduleRefresh(0)
        } else {
            if (observing) {
                context.contentResolver.unregisterContentObserver(observer)
                observing = false
            }
            _contacts.value = emptyList()
        }
    }

    fun scheduleRefresh(delayMs: Long = 500) {
        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            delay(delayMs)
            if (enabled && hasPermission()) _contacts.value = load()
        }
    }

    private fun load(): List<ContactItem> {
        val names = LinkedHashMap<Long, Triple<String, String, MutableList<String>>>()
        val projection = arrayOf(Phone.CONTACT_ID, Phone.LOOKUP_KEY, Phone.DISPLAY_NAME, Phone.NUMBER)
        context.contentResolver.query(Phone.CONTENT_URI, projection, null, null, "${Phone.DISPLAY_NAME} COLLATE LOCALIZED ASC")?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val entry = names.getOrPut(id) { Triple(c.getString(1).orEmpty(), c.getString(2).orEmpty(), mutableListOf()) }
                c.getString(3)?.let { if (it !in entry.third) entry.third += it }
            }
        }
        return names.map { (id, t) -> ContactItem("c:$id", t.second, id, t.first, t.third) }
            .filter { it.label.isNotBlank() }
    }
}
