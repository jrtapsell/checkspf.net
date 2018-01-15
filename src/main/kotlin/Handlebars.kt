import org.w3c.dom.HTMLElement
import kotlin.browser.window
import kotlin.dom.clear
import kotlin.js.Promise

external class Template {
    fun call(o: Template, data: Any?): String
}

external object Handlebars {
    fun compile(input: String): Template
    fun registerPartial(name: String, partial: String)
}


object HBS {
    fun render(url: String, data: Promise<Any?>, element: HTMLElement) {
        element.clear()
        val rawTemplate = window.fetch(url).then { it.text() }
        val partial = window.fetch("result.hbs").then { it.text() }
        Promise.all(arrayOf(rawTemplate, data, partial)).then { (rawTemplate: Any?, data: Any?, partial: Any?) ->
            partial as String
            rawTemplate as String
            Handlebars.registerPartial("showResult", partial)
            val template = Handlebars.compile(rawTemplate)
            val js = JSON.parse<Any?>(JSON.stringify(data))
            console.log("Rendering", js)
            val out = template.call(template, js)
            element.innerHTML = out
        }.catch {
            console.error("err", it)
        }
    }
}