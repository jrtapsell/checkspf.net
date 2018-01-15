import kotlin.js.Math
import kotlin.math.*
fun <T> List<T>.choose(): T {
    val item = (Math.random() * size).toInt()
    return get(item)
}