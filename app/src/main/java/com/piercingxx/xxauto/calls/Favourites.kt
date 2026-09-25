package com.piercingxx.xxauto.calls

import android.content.Context
import android.provider.ContactsContract

/**
 * The calls surface's data source (Phase 5, AU8): starred contacts, one primary
 * number each. Queries `ContactsContract` for `STARRED` contacts, maps each
 * phone row through the pure [FavouritePick.primaryNumber] seam (prefer mobile),
 * and returns one [Contact] per starred contact that has a number. The drive
 * screen feeds the result to [FavouritesSheet]; the sheet dials the number.
 */
object Favourites {

    /** One starred contact, resolved to its primary number. */
    data class Contact(
        val name: String,
        val number: String,
    )

    /**
     * The starred contacts with a resolvable primary number, in contact order.
     * Requires `READ_CONTACTS` (declared in the manifest, AU8); without it the
     * content-resolver query throws — the surface toggle is off until the
     * permission is granted (design AU3), so the sheet is not reachable then.
     */
    fun starred(context: Context): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
        )
        val selection = "${ContactsContract.Contacts.STARRED} != 0"
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            selection,
            null,
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val primary = FavouritePick.primaryNumber(numbersFor(context, id))
                if (primary != null) {
                    contacts.add(Contact(name = name, number = primary.value))
                }
            }
        }
        return contacts
    }

    private fun numbersFor(context: Context, contactId: Long): List<FavouritePick.Number> {
        val numbers = mutableListOf<FavouritePick.Number>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            arrayOf(contactId.toString()),
            null,
        )?.use { cursor ->
            val numCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
            while (cursor.moveToNext()) {
                val value = cursor.getString(numCol) ?: continue
                if (value.isBlank()) continue
                numbers.add(
                    FavouritePick.Number(value = value, type = mapType(cursor.getInt(typeCol))),
                )
            }
        }
        return numbers
    }

    private fun mapType(type: Int): FavouritePick.NumberType = when (type) {
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> FavouritePick.NumberType.MOBILE
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> FavouritePick.NumberType.HOME
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> FavouritePick.NumberType.WORK
        else -> FavouritePick.NumberType.OTHER
    }
}