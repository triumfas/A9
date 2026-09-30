package lt.tbu.a9.data

import android.os.UserHandle
import android.content.ComponentName

/** Rezultato elementas: programėlė arba kontaktas. */
sealed interface Item {
    val id: String
    val label: String
}

data class AppItem(
    override val id: String,
    override val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val userSerial: Long,
    val isSystem: Boolean,
    val isWorkProfile: Boolean,
) : Item {
    val packageName: String get() = component.packageName
}

data class ContactItem(
    override val id: String,
    override val label: String,
    val contactId: Long,
    val lookupKey: String,
    val phones: List<String>,
) : Item

fun appId(component: ComponentName, userSerial: Long) = "${component.flattenToShortString()}@$userSerial"
