import org.w3c.dom.HTMLElement
import kotlin.browser.document
import kotlin.js.Promise

fun <T> T.single(): List<T> = listOf(this)
data class DomainResult(val domain: String, val data: List<SPF.ParsedRecord>)
fun DomainResult?.validate(): List<String> {
    if (this == null) return "No record, no SPF protection".single()
    return when (this.data.size) {
        0 -> "No record, no SPF protection".single()
        1 -> {
            val record = data[0]
            val parsed = record.parsed
            if (parsed == null) "Invalid record, no SPF protection".single() else {
                return parsed.validate()
            }
        }
        else -> "Multiple records".single()
    }
}

data class OverallResult(val domains: Array<out DomainResult>, val result: DomainResult?, val status: List<String>)
object Controller {
    fun loadURL(url: String) {
        val parts = url.split(".")
        val urls = (0 until parts.size).map { parts.subList(it, parts.size).joinToString(".") }
        val promises = urls.map { domain ->
            SPF.getRecords(domain)
                .then {
                    DomainResult(domain, it)
                }
        }
        val data = Promise.all(promises.toTypedArray())
        val out = data.then {
            val first = it[0]
            OverallResult(it, first, first.validate())
        }
        HBS.render(out, document.getElementById("domain-tab") as HTMLElement, SPFViewer.templates::domain)
        HBS.render(out, document.getElementById("tree-tab") as HTMLElement, SPFViewer.templates::tree)
        HBS.render(out, document.getElementById("ip-tab") as HTMLElement, SPFViewer.templates::ip)
    }
}