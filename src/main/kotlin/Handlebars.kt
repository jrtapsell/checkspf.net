import org.w3c.dom.HTMLElement
import kotlin.browser.window
import kotlin.dom.clear
import kotlin.js.Promise

external object SPFViewer {
    object templates {
        fun domain(data: Any?): String
        fun ip(data: Any?): String
        fun tree(data: Any?): String
    }
}

object HBS {
    fun render(data: Promise<Any?>, element: HTMLElement, handler: (Any?)->String) {
        element.clear()
        data.then {
            val raw = JSON.parse<Any?>(JSON.stringify(it))
            console.log("Rendering", raw)
            handler(raw)
        }.then {
            element.innerHTML = it
        }
    }
}