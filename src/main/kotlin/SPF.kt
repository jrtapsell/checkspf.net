import kotlin.js.Promise

enum class SPFRuleType(val char: Char) {
    PASS('+'), NEUTRAL('?'), SOFT_FAIL('~'), FAIL('-');
    companion object {
        val ALL_CHARS by lazy { values().map { it.char } }
        fun getForChar(char: Char): SPFRuleType {
            return values().filter { it.char == char }.first()
        }
    }
    val prefix = char.toString()

}
enum class SPFTokenType {
    ALL, IP4, IP6, A, MX, PTR, EXISTS, INCLUDE, REDIRECT, EXP;
    val token = name.toLowerCase()
}

data class SPFToken(val rule: SPFRuleType?, val type:SPFTokenType, val payload: String?)

typealias SPFRecord = List<SPFToken>

fun SPFRecord.validate(): List<String> {
    val ret = mutableListOf<String>()
    val eachCount = groupingBy { it.type }.eachCount()
    fun countOf(tokenType: SPFTokenType) = eachCount[tokenType]?:0
    if (countOf(SPFTokenType.ALL) > 0 && countOf(SPFTokenType.REDIRECT) > 0) {
        // rfc7208 (5.1)
        ret.add("Has both all and redirect")
    }
    return ret
}

object SPF {
    val partRegex = Regex("""([+?\-~]?)(ip4|ip6|a|mx|ptr|exists|include|all|exp)((?::[^ ]+)?)""")
    val SPFRefex = Regex("""v=spf1( +${partRegex.pattern})*?( [+?\-~]?all)?""")
    val spaces = Regex(" +")

    fun parsePart(input: String): SPFToken {
        val parsed = partRegex.matchEntire(input)
        val (qualifier, typeName, data) = parsed!!.groupValues.drop(1)
        val qual = if (qualifier.isBlank()) null else SPFRuleType.getForChar(qualifier[0])
        return SPFToken(qual, SPFTokenType.valueOf(typeName.toUpperCase()), if (data.isBlank()) null else data.substring(1))
    }

    fun parse(input: String): SPFRecord? {
        if (SPFRefex.matchEntire(input) == null) {
            return null
        }
        return input.split(spaces).drop(1).map { parsePart(it) }
    }

    data class ParsedRecord(val parsed: SPFRecord?, val raw: List<String>, val errors: List<String>) {
        companion object {
            fun fromObject(input: List<String>): ParsedRecord {
                val parsed = SPF.parse(input.joinToString(""))
                return ParsedRecord(parsed, input, parsed?.validate()?: listOf())
            }
        }
    }
    fun getRecords(domain: String): Promise<List<ParsedRecord>> {
        return DNS.getTxt(domain).then {
            val strings = it.map { it.strings }
            val spfs = strings.filter { it.joinToString("").startsWith("v=spf1") }
            spfs.map{ParsedRecord.fromObject(it)}
        }
    }
}