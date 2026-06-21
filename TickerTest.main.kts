import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking {
    val startTime = System.currentTimeMillis()
    val scope = CoroutineScope(Dispatchers.Default)
    val flow = flow {
        var iterations = 0
        while (iterations < 5) {
            emit(System.currentTimeMillis())
            delay(1000L)
            iterations++
        }
    }
    flow.collect { time ->
        println("Tick at:  ms")
    }
}
