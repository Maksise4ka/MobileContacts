package com.github.maksise4ka.contacts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.maksise4ka.contacts.ui.theme.ContactsTheme
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    private var contacts by mutableStateOf<List<Contact>?>(null)
    private var permissionDenied by mutableStateOf(false)

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) loadContacts() else permissionDenied = true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        if (savedInstanceState != null) {
            contacts = savedInstanceState.restoreContacts()
            permissionDenied = savedInstanceState.getBoolean(KEY_DENIED)
        }

        setContent {
            ContactsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val contacts = contacts
                    when {
                        permissionDenied -> Message(
                            text = stringResource(R.string.permission_denied),
                            modifier = Modifier.padding(innerPadding),
                        )

                        contacts == null -> Unit

                        contacts.isEmpty() -> Message(
                            text = stringResource(R.string.no_contacts),
                            modifier = Modifier.padding(innerPadding),
                        )

                        else -> ContactList(
                            contacts = contacts,
                            onClick = ::dial,
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
            }
        }

        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            contacts = null
            if (!permissionDenied) requestPermission.launch(Manifest.permission.READ_CONTACTS)
        } else if (contacts == null) {
            loadContacts()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_DENIED, permissionDenied)

        contacts?.let { list ->
            outState.putStringArray(KEY_NAMES, list.map { it.name }.toTypedArray())
            outState.putStringArray(KEY_NUMBERS, list.map { it.phoneNumber }.toTypedArray())
            outState.putStringArray(KEY_EMAILS, list.map { it.email }.toTypedArray())
        }
    }

    private fun Bundle.restoreContacts(): List<Contact>? {
        val names = getStringArray(KEY_NAMES) ?: return null
        val numbers = getStringArray(KEY_NUMBERS) ?: return null
        val emails = getStringArray(KEY_EMAILS) ?: return null

        return names.indices.map { Contact(names[it], numbers[it], emails[it]) }
    }

    private fun loadContacts() {
        val list = fetchAllContacts()
        contacts = list
        val message = resources.getQuantityString(R.plurals.contacts_found, list.size, list.size)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dial(contact: Contact) {
        val number = contact.phoneNumber ?: return
        startActivity(Intent(Intent.ACTION_DIAL, "tel:$number".toUri()))
    }

    private companion object {
        const val KEY_DENIED = "permission_denied"
        const val KEY_NAMES = "names"
        const val KEY_NUMBERS = "numbers"
        const val KEY_EMAILS = "emails"
    }
}

@Composable
fun Message(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ContactList(
    contacts: List<Contact>,
    onClick: (Contact) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(contacts) { contact ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(contact) }
                    .padding(16.dp),
            ) {
                Text(
                    contact.name ?: stringResource(R.string.unknown),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    contact.phoneNumber ?: stringResource(R.string.unknown),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
