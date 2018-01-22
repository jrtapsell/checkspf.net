import kotlin.js.Promise

/**
 *
 * @param verb
 *      Fits into the sentence: `The email should be <<verb>> if...`
 */
enum class SPFRuleType(val char: Char, val verb: String) {
    PASS('+', "treated as not spam"),
    NEUTRAL('?', "accepted"),
    SOFT_FAIL('~', "marked as spam"),
    FAIL('-', "dropped");

    companion object {
        val ALL_CHARS by lazy { values().map { it.char } }
        fun getForChar(char: Char): SPFRuleType {
            return values().filter { it.char == char }.first()
        }
    }
    val prefix = char.toString()

}
enum class SPFTokenType {
    ALL {
        override fun describeCondition(payload: String?, base: String) = "no other rules match"
    }, IP4 {
        override fun describeCondition(payload: String?, base: String) = "the sending IP (v4) matches $payload"
    }, IP6 {
        override fun describeCondition(payload: String?, base: String) = "the sending IP (v6) matches $payload"
    }, A {
        override fun describeCondition(payload: String?, base: String) =  "the address is an A or AAAA record for ${payload ?: base}"
    }, MX {
        override fun describeCondition(payload: String?, base: String) = "the address is an MX record for ${payload ?: base}"
    }, PTR {
        override fun describeCondition(payload: String?, base: String) = "the address has a ptr <-> a link below this domain"
    }, EXISTS {
        override fun describeCondition(payload: String?, base: String) = "the domain $payload exists"
    }, INCLUDE {
        override fun describeCondition(payload: String?, base: String) = "the email would pass $payload as $base"
    }, REDIRECT {
        override fun describeCondition(payload: String?, base: String) = "the email would pass $payload as $payload"
    }, EXP {
        override fun describeCondition(payload: String?, base: String) = "TODO"
    };
    val token = name.toLowerCase()
    abstract fun describeCondition(payload: String?, base: String): String
}

data class SPFToken(val rule: SPFRuleType?, val type:SPFTokenType, val payload: String?, val base: String) {
    val treatedRule = rule ?: SPFRuleType.PASS

    val explained = explain(base)

    fun explain(domain: String): String {
        if (type == SPFTokenType.EXP) {
            "The rejection message should be $payload"
        }
        return "The email should be ${treatedRule.verb} if ${type.describeCondition(payload, domain)}"
    }
}

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

    fun parsePart(input: String, domain: String): SPFToken {
        val parsed = partRegex.matchEntire(input)
        val (qualifier, typeName, data) = parsed!!.groupValues.drop(1)
        val qual = if (qualifier.isBlank()) null else SPFRuleType.getForChar(qualifier[0])
        return SPFToken(qual, SPFTokenType.valueOf(typeName.toUpperCase()), if (data.isBlank()) null else data.substring(1), domain)
    }

    fun parse(input: String, domain: String): SPFRecord? {
        if (SPFRefex.matchEntire(input) == null) {
            return null
        }
        return input.split(spaces).drop(1).map { parsePart(it, domain) }
    }

    data class ParsedRecord(val parsed: SPFRecord?, val raw: List<String>, val errors: List<String>) {
        companion object {
            fun fromObject(input: List<String>, domain: String): ParsedRecord {
                val parsed = SPF.parse(input.joinToString(""), domain)
                return ParsedRecord(parsed, input, parsed?.validate()?: listOf())
            }
        }
    }
    fun getRecords(domain: String): Promise<List<ParsedRecord>> {
        return DNS.getTxt(domain).then {
            val strings = it.map { it.strings }
            val spfs = strings.filter { it.joinToString("").startsWith("v=spf1") }
            spfs.map{ParsedRecord.fromObject(it, domain)}
        }
    }
}