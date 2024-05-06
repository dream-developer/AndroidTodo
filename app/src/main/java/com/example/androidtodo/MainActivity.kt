package com.example.androidtodo

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.androidtodo.ui.theme.AndroidTodoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

class MainActivity : ComponentActivity() {
    private val todoModel = TodoModel() // 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidTodoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(todoModel = todoModel)
                }
            }
        }
    }
}


@Composable
fun MainApp(todoModel: TodoModel) { // 1
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "/"
    ) {
        composable(route = "/") {
            ListScreen(
                viewModel = ListScreenViewModel(todoModel = todoModel),
                toCreateScreen = { navController.navigate("/create") },
                toEditScreen = { id -> navController.navigate("/edit/$id") },
            )
        }
        composable(route = "/create") { // 3
            CreateScreen(
                viewModel = CreateScreenViewModel(todoModel = todoModel),
                toListScreen = { navController.navigate("/") }
            )
        }
        composable(
            route = "/edit/{id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.IntType
                },
            ),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: throw Exception("idがnull")
            EditScreen(
                viewModel = EditScreenViewModel(todoModel = todoModel),
                toListScreen = { navController.navigate("/") },
                id = id
            )
        }
    }
}

class TodoModel {
    private val dao = RoomApplication.database.todoDao()
    suspend fun create(content: String) {
        val createdAt = System.currentTimeMillis()
        dao.create(Todo(content = content, created_at = createdAt))
    }

    fun getAll(): Flow<List<Todo>> {
        return dao.getAll()
    }

    fun getById(id: Int): Flow<Todo>? {
        return dao.getById(id)
    }

    suspend fun update(id: Int, content: String, created_at: Long) {
        dao.update(Todo(id = id, content = content, created_at = created_at))
    }

    suspend fun delete(id: Int) {
        dao.delete(Todo(id = id))
    }
}

class EditScreenViewModel(private val todoModel: TodoModel) : ViewModel() {
    fun getById(id: Int): Flow<Todo> { // 1
        var todo: Flow<Todo> = flow { }
        viewModelScope.launch {
            try {
                todo = todoModel?.getById(id) ?: throw Exception("idがnull") // 2
            } catch (e: Exception) {
                Log.e("Exception", "例外: ${e.message}")
            }
        }
        return todo
    }

    fun update(id: Int, content: String, created_at: Long) { // 3
        if (content.isBlank()) {
            return
        }
        viewModelScope.launch {
            try {
                todoModel.update(id = id, content = content, created_at = created_at)
            } catch (e: Exception) {
                Log.e("Exception", "例外: ${e.message}")
            }
        }
    }
}

class ListScreenViewModel(private val todoModel: TodoModel) : ViewModel() {
    fun getAll(): Flow<List<Todo>> { // 1
        var todoList: Flow<List<Todo>> = flow {}
        viewModelScope.launch {
            try {
                todoList = todoModel.getAll()
            } catch (e: Exception) {
                Log.e("Exception", "例外: ${e.message}")
            }
        }
        return todoList
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            try {
                todoModel.delete(id = id)
            } catch (e: Exception) {
                Log.e("Exception", "例外: ${e.message}")
            }
        }
    }
}

class CreateScreenViewModel(private val todoModel: TodoModel) : ViewModel() {
    fun create(content: String) {
        if (content.isBlank()) {
            return
        }
        viewModelScope.launch {
            try {
                todoModel.create(content = content)
            } catch (e: Exception) {
                Log.e("Exception", "例外: ${e.message}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen( // 1
    viewModel: EditScreenViewModel,
    toListScreen: () -> Unit,
    id: Int,
) {
    val todo by viewModel.getById(id).collectAsState(initial = Todo()) // 2
    var content by rememberSaveable { mutableStateOf("") }
    content = todo.content

    Scaffold(
        topBar = { // 3
            TopAppBar(
                title = { Text(text = "編集画面") },
                navigationIcon = {
                    IconButton(onClick = { toListScreen() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
            )
            Button(
                onClick = { // 4
                    viewModel.update(
                        id = id, content = content, created_at = todo.created_at
                    )
                    toListScreen()
                }
            ) { Text("更新") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    viewModel: ListScreenViewModel,
    toCreateScreen: () -> Unit,
    toEditScreen: (Int) -> Unit, // 1
) {
    val todoList by viewModel.getAll().collectAsState(initial = emptyList())
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    var showDialog by remember { mutableStateOf(false) } // 1
    var deleteId by remember { mutableStateOf(0) } // 2

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "一覧画面") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { toCreateScreen() }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) {
            items(count = todoList.size,
                key = { index -> todoList[index].id }
            ) { index ->
                var todo = todoList[index]
                Row {
                    Text(
                        text = "ID ${todo.id}: ",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "created at: ${sdf.format(todo.created_at)}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(text = todo.content)
                Row {
                    IconButton(
                        onClick = {
                            toEditScreen(todo.id)
                        }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                        )
                    }
                    IconButton( // 3
                        onClick = {
                            deleteId = todo.id
                            showDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete"
                        )
                    }
                }
                Divider()
            }
        }
        if (showDialog) { // 4
            AlertDialog(
                onDismissRequest = { showDialog = false },
                text = { Text("削除しますか？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.delete(id = deleteId)
                            showDialog = false
                        }) { Text("はい") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                        }
                    ) { Text("いいえ") }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    viewModel: CreateScreenViewModel,
    toListScreen: () -> Unit, // 1
) {
    var content by rememberSaveable { mutableStateOf("") }

    Scaffold( // 2
        topBar = {
            TopAppBar(
                title = { Text(text = "追加画面") },
                navigationIcon = {
                    IconButton(onClick = { toListScreen() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
            )
            Button(
                onClick = {
                    viewModel.create(content = content)
                    content = ""
                }
            ) {
                Text("追加")
            }
        }
    }
}

@Entity(tableName = "todos") // 1
data class Todo(
    @PrimaryKey(autoGenerate = true) // 2
    val id: Int = 0,
    val content: String = "", // 3
    val created_at: Long = 0, // 4
)

@Dao // 1
interface TodoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) // 2
    suspend fun create(todos: Todo)

    @Query("SELECT * FROM todos ORDER BY created_at DESC")
    fun getAll(): Flow<List<Todo>> // 3

    @Query("SELECT * FROM todos WHERE id = :id")
    fun getById(id: Int): Flow<Todo>?

    @Update
    suspend fun update(todo: Todo)

    @Delete
    suspend fun delete(todo: Todo)
}

@Database(entities = [Todo::class], version = 1, exportSchema = false) // 1

abstract class AppDatabase : RoomDatabase() { // 2
    abstract fun todoDao(): TodoDao
}

class RoomApplication : Application() { // 3

    companion object { // 4
        lateinit var database: AppDatabase
    }

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "todos" // 5
        ).build()
    }
}