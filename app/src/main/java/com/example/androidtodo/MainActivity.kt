package com.example.androidtodo

import android.app.Application
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.androidtodo.ui.theme.AndroidTodoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    sealed interface UiState {
        data object Loading : UiState
        data object Screen : UiState
        data object Blank : UiState
        data object Success : UiState
        data object Error : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading) // 1
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun getById(id: Int): Flow<Todo> {
        var todo: Flow<Todo> = flow { }

        viewModelScope.launch {
            try {
                todo = todoModel?.getById(id) ?: throw Exception("idがデータが無い")
                delay(1000) // ロード中テスト用
                _uiState.value = UiState.Screen // 2
            } catch (e: Exception) {
                Log.e("Exception", "例外: ${e.message}")
                _uiState.value = UiState.Error // 3
            }
        }
        return todo
    }

    fun update(id: Int, content: String, created_at: Long) {
        if (content.isBlank()) {
            _uiState.value = UiState.Blank // 4
            return
        }
        viewModelScope.launch {
            try {
                todoModel.update(id = id, content = content, created_at = created_at)
                _uiState.value = UiState.Success // 5
            } catch (e: Exception) {
                _uiState.value = UiState.Error // 6
                Log.e("Exception", "例外: ${e.message}")
            }
        }
    }

    fun setScreen() { // 7
        _uiState.value = UiState.Screen
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
    sealed interface UiState { // 1
        data object Screen : UiState
        data object Blank : UiState
        data object Success : UiState
        data object Error : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Screen) // 2
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun create(content: String) {
        if (content.isBlank()) {
            _uiState.value = UiState.Blank // 3
            return
        }
        viewModelScope.launch {
            try {
                todoModel.create(content = content)
                _uiState.value = UiState.Success // 4
            } catch (e: Exception) {
                _uiState.value = UiState.Error // 5
                Log.e("Exception", "例外: ${e.message}")
            }
        }
    }

    fun setScreen() {
        _uiState.value = UiState.Screen // 6
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    viewModel: EditScreenViewModel,
    toListScreen: () -> Unit,
    id: Int,
) {
    val todo by viewModel.getById(id).collectAsState(initial = Todo())
    var content by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(true) } // 1
    content = todo.content
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val uiStateTemp = uiState

    Scaffold(
        topBar = {
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
                onClick = {
                    viewModel.update(
                        id = id, content = content, created_at = todo.created_at
                    )
                }
            ) { Text("更新") }
        }

        if (isLoading) { // 2
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
            ) {
                CircularProgressIndicator()
            }
        }

        LaunchedEffect(uiState) {
            when (uiStateTemp) {
                EditScreenViewModel.UiState.Loading -> {} // 3
                EditScreenViewModel.UiState.Screen -> { // 4
                    isLoading = false
                }

                EditScreenViewModel.UiState.Blank -> {
                    Toast.makeText(context, "入力してください", Toast.LENGTH_SHORT).show()
                    viewModel.setScreen()
                }

                EditScreenViewModel.UiState.Success -> { // 5
                    Toast.makeText(context, "更新しました", Toast.LENGTH_SHORT).show()
                    toListScreen()
                }

                EditScreenViewModel.UiState.Error -> {
                    Toast.makeText(context, "エラーが発生しました", Toast.LENGTH_SHORT).show()
                }
            }
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
    toListScreen: () -> Unit,
) {
    var content by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current // 1
    val uiState by viewModel.uiState.collectAsState() // 2
    val uiStateTemp = uiState // 3

    Scaffold(
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
                }
            ) {
                Text("追加")
            }
        }

        LaunchedEffect(uiState) { // 4
            when (uiStateTemp) { // 5
                CreateScreenViewModel.UiState.Screen -> {} // 6
                CreateScreenViewModel.UiState.Blank -> { // 7
                    Toast.makeText(context, "入力してください", Toast.LENGTH_SHORT).show()
                    viewModel.setScreen()
                }
                CreateScreenViewModel.UiState.Success -> { // 8
                    Toast.makeText(context, "追加しました", Toast.LENGTH_SHORT).show()
                    content = ""
                    viewModel.setScreen()
                }
                CreateScreenViewModel.UiState.Error -> { // 9
                    Toast.makeText(context, "エラーが発生しました", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val content: String = "",
    val created_at: Long = 0,
    val test: Int = 0, // マイグレーション検証用
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

// マイグレーション対応 version = 1 → version = 2
@Database(entities = [Todo::class], version = 2, exportSchema = false)

abstract class AppDatabase : RoomDatabase() { // 2
    abstract fun todoDao(): TodoDao
}

class RoomApplication : Application() { // 3
    // マイグレーション対応 SQL
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE todos ADD COLUMN test INTEGER NOT NULL DEFAULT 0")
        }
    }

    companion object { // 4
        lateinit var database: AppDatabase
    }

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "todos" // 5
        )
            .addMigrations(MIGRATION_1_2) // マイグレーション対応 SQLを渡す
            .build()
    }
}