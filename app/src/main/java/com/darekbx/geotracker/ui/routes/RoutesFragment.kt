@file:OptIn(
    ExperimentalLifecycleComposeApi::class,
    ExperimentalFoundationApi::class
)

package com.darekbx.geotracker.ui.routes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darekbx.geotracker.model.Route
import com.darekbx.geotracker.viewmodels.RoutesUiState
import com.darekbx.geotracker.viewmodels.RoutesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RoutesFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                Routes()
            }
        }
    }
}

@Composable
private fun Routes(
    routesViewModel: RoutesViewModel = hiltViewModel()
) {
    val uiState by routesViewModel.uiState.collectAsStateWithLifecycle()
    val displayAddDialog = remember { mutableStateOf(false) }

    when (uiState) {
        is RoutesUiState.Success -> {
            Box(modifier = Modifier.fillMaxSize()) {
                RouteList(
                    modifier = Modifier.fillMaxSize(),
                    routes = (uiState as RoutesUiState.Success).routes,
                    routesViewModel = routesViewModel
                )
                FloatingActionButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(32.dp),
                    onClick = { displayAddDialog.value = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
        else -> {
            Text(text = "Error!", color = Color.White)
        }
    }

    if (displayAddDialog.value) {
        PaymentConfirmDialog(displayAddDialog, routesViewModel)
    }
}

@Composable
private fun RouteList(
    modifier: Modifier = Modifier,
    routes: List<Route>,
    routesViewModel: RoutesViewModel
) {
    val context = LocalContext.current
    LazyColumn(modifier.padding(all = 8.dp)) {
        items(routes) { route ->
            Route(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(route.url))
                            context.startActivity(intent)
                        },
                        onLongClick = {
                            routesViewModel.delete(route.id)
                        }
                    ),
                route = route
            )
        }
    }
}

@Preview
@Composable
private fun Route(
    modifier: Modifier = Modifier,
    route: Route = Route(0, "Las kabacki", "http://url.com", System.currentTimeMillis())
    ) {
    Card(modifier = modifier
        .padding(all = 8.dp)
        .fillMaxWidth()) {
        Column(modifier = Modifier.padding(all = 10.dp)) {
            Text(route.label, fontSize = 22.sp, fontWeight = FontWeight.W700, modifier = Modifier.padding(4.dp))
            Text(route.url, color = Color.Gray, modifier = Modifier.padding(4.dp))
        }
    }
}


@Composable
fun PaymentConfirmDialog(
    displayAddDialog: MutableState<Boolean>,
    routesViewModel: RoutesViewModel
) {
    val label = remember { mutableStateOf("") }
    val url = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { displayAddDialog.value = false },
        title = {
            Text(text = "New route")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = label.value,
                    onValueChange = { label.value = it },
                    label = { Text("Label") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    )
                )
                OutlinedTextField(
                    value = url.value,
                    onValueChange = { url.value = it },
                    label = { Text("Url") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    displayAddDialog.value = false
                    routesViewModel.add(label.value, url.value)
                }) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(
                onClick = { displayAddDialog.value = false }) {
                Text("Cancel")
            }
        }
    )
}