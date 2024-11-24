package im.angry.openeuicc.util

fun String.decodeHex(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }

    val decodedLength = length / 2
    val out = ByteArray(decodedLength)
    for (i in 0 until decodedLength) {
        val i2 = i * 2
        out[i] = substring(i2, i2 + 2).toInt(16).toByte()
    }
    return out
}

fun ByteArray.encodeHex(): String {
    val sb = StringBuilder()
    val length = size
    for (i in 0 until length) {
        sb.append(String.format("%02X", this[i]))
    }
    return sb.toString()
}

fun formatFreeSpace(size: Int): String =
    // SIM cards probably won't have much more space anytime soon.
    if (size >= 1024) {
        "%.2f KiB".format(size.toDouble() / 1024)
    } else {
        "$size B"
    }

fun String.prettyPrintJson(): String {
    val ret = StringBuilder()
    var inQuotes = false
    var escaped = false
    val indentSymbolStack = ArrayDeque<Char>()

    val addNewLine = {
        ret.append('\n')
        repeat(indentSymbolStack.size) {
            ret.append('\t')
        }
    }

    var lastChar = ' '

    for (c in this) {
        when {
            !inQuotes && (c == '{' || c == '[') -> {
                ret.append(c)
                indentSymbolStack.addLast(c)
                addNewLine()
            }

            !inQuotes && (c == '}' || c == ']') -> {
                indentSymbolStack.removeLast()
                if (lastChar != ',') {
                    addNewLine()
                }
                ret.append(c)
            }

            !inQuotes && c == ',' -> {
                ret.append(c)
                addNewLine()
            }

            !inQuotes && c == ':' -> {
                ret.append(c)
                ret.append(' ')
            }

            inQuotes && c == '\\' -> {
                ret.append(c)
                escaped = true
                continue
            }

            !escaped && c == '"' -> {
                ret.append(c)
                inQuotes = !inQuotes
            }

            !inQuotes && c == ' ' -> {
                // Do nothing -- we ignore spaces outside of quotes by default
                // This is to ensure predictable formatting
            }

            else -> ret.append(c)
        }

        if (escaped) {
            escaped = false
        }

        lastChar = c
    }

    return ret.toString()
}