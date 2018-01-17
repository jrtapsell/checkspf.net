import kotlin.browser.window
import kotlin.js.Promise

external class TXTQuestion {
    val name: String
    val type: String
    val `class`: String
}

external class TXTAnswer {
    val `class`: String
    val name: String
    val rdata: String
    val rdlength: Int
    val ttl: Int
    val type: String
}

external class TXTResponse {
    val answer: Array<TXTAnswer>
}
external class ErrorResponse {
    val code: Int
}
object DNS {
    val ALL_SERVERS = listOf("google1", "google2", "he", "opendns1", "opendns2")
    val chosenServer by lazy { ALL_SERVERS.choose() }
    fun getTxt(domain: String): Promise<Array<TXTAnswer>> {
        return Promise{resolve, reject ->
            window.fetch("https://www.checkspf.net/dns-lg/$chosenServer/$domain/txt").then { request ->
                request.json().then {
                    if (request.status != 200.toShort()) {
                        val r = it.unsafeCast<ErrorResponse>()
                        when (r.code) {
                            503 -> arrayOf()
                            else -> throw AssertionError(JSON.stringify(it))
                        }
                    } else {
                        it?.unsafeCast<TXTResponse>()?.answer?.filter { it.rdata != null }?.toTypedArray() ?: arrayOf()
                    }
                }.then(resolve).catch(reject)
            }.catch(reject)
        }
    }
}

val TXTAnswer.strings: List<String>
    get() {
        val out = mutableListOf<String>()
        var escaped = false
        var instring = false
        var current = ""
        for (c in rdata) {
            when (c) {
                '"' -> {
                    if (instring) {
                        if (escaped) {
                            escaped = false
                            current += '"'
                        } else {
                            instring = !instring
                            out.add(current)
                            current = ""
                        }
                    } else {
                        instring = !instring
                    }
                }
                '\\' -> {
                    if (escaped) {
                        current += '\\'
                    } else {
                        escaped = !escaped
                    }
                }
                else -> {
                    if (instring) {
                        current += c
                    }
                }
            }
        }
        return out
    }
