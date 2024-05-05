package com.example.androidtodo

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidTodoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CeateScreen()
                }
            }
        }
    }
}

@Composable
fun CeateScreen() { // 1
    val dao = RoomApplication.database.todoDao() // 2

    var content by rememberSaveable { mutableStateOf("") } // 3
    Column {
        OutlinedTextField( // 4
            value = content,
            onValueChange = { content = it },
        )
        Button(
            onClick = { // 5
                val createdAt = System.currentTimeMillis() // 6
                CoroutineScope(Dispatchers.Default).launch { // 7
                    dao.create(Todo(content = content, created_at = createdAt)) // 8
                    content = "" // 9
                }
            }
        ) {
            Text("追加")
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