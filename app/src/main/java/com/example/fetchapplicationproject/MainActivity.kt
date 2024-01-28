package com.example.fetchapplicationproject

import android.content.ClipData
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fetchapplicationproject.ui.theme.FetchApplicationProjectTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FetchApplicationProjectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FetchData()
                }
            }
        }
    }
}

@Composable
fun FetchData() {
    var data by remember { mutableStateOf<List<Item>>(emptyList()) }

    LaunchedEffect(Unit) {
        data = fetchDataFromUrl("https://fetch-hiring.s3.amazonaws.com/hiring.json")
    }

    val filteredItems = data.filter { it.name?.isNotBlank() == true && it.name != "null" }
    val sortedItems = filteredItems.sortedWith(compareBy({ it.listId }, { it.extractNumericFromName() }))
    val groupedItems = sortedItems.groupBy { it.listId }

    LazyColumn {
        items(groupedItems.keys.sorted()) { listId ->
            val items = groupedItems[listId] ?: emptyList()
            Greeting(listId.toString(), items)
        }
    }
}

fun Item.extractNumericFromName(): Int {
    val numericPart = name?.replace(Regex("[^0-9]"), "") ?: ""
    return if (numericPart.isNotEmpty()) numericPart.toInt() else 0
}

suspend fun fetchDataFromUrl(urlString: String): List<Item> {
    return withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection

        return@withContext try {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val jsonString = reader.readText()
            parseJson(jsonString)
        } finally {
            connection.disconnect()
        }
    }
}

fun parseJson(jsonString: String): List<Item> {
    val jsonArray = JSONArray(jsonString)
    val items = mutableListOf<Item>()

    for (i in 0 until jsonArray.length()) {
        val jsonObject: JSONObject = jsonArray.getJSONObject(i)
        val id = jsonObject.getInt("id")
        val listId = jsonObject.getInt("listId")
        val name = jsonObject.getString("name")
        items.add(Item(id, listId, name))
    }

    return items
}

data class Item(val id: Int, val listId: Int, val name: String)

@Composable
fun Greeting(listId: String, items: List<Item>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            text = "listId: $listId",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        items.forEach { item ->
            Text(
                text = " id: ${item.id}, name: ${item.name}",
                modifier = modifier.padding(start = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FetchApplicationProjectTheme {
        Greeting("1", listOf(Item(1, 1, "Item 1")))
    }
}