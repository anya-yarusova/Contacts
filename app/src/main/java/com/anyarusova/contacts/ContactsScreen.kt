package com.anyarusova.contacts

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

@Composable
fun ContactsScreen() {
    val context = LocalContext.current
    var contacts by rememberSaveable { mutableStateOf<List<Contact>?>(null) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var requested by rememberSaveable { mutableStateOf(false) }
    val foundFormat = stringResource(R.string.found_contacts)
    val dialError = stringResource(R.string.dial_error)

    val load: () -> Unit = {
        val list = context.fetchAllContacts()
        contacts = list
        Toast.makeText(context, String.format(foundFormat, list.size), Toast.LENGTH_LONG).show()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDenied = false
            load()
        } else {
            permissionDenied = true
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        when {
            granted && contacts == null -> load()
            !granted && !requested -> {
                requested = true
                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    val loadedContacts = contacts
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        when {
            permissionDenied -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.no_permission_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.no_permission_text),
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 14.sp
                )
            }

            loadedContacts == null -> Text(
                text = stringResource(R.string.loading),
                modifier = Modifier.align(Alignment.Center)
            )

            else -> Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(R.string.found_contacts, loadedContacts.size),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (loadedContacts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_contacts),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(loadedContacts) { contact ->
                            ContactRow(contact = contact, onClick = {
                                openDialer(context, contact, dialError)
                            })
                        }
                    }
                }
            }
        }
    }
}

private fun openDialer(context: Context, contact: Contact, errorText: String) {
    val number = contact.phoneNumber?.filterNot { it.isWhitespace() }.orEmpty()
    if (number.isEmpty()) {
        Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
        return
    }
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$number".toUri()))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ContactRow(contact: Contact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name ?: "",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.contact_number, contact.phoneNumber ?: ""),
                fontSize = 16.sp,
                color = Color(0xFF444444)
            )
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF2E7D80),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}
