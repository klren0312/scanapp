package com.example.scanapp.util

private enum class DiffOp {
    KEEP, DELETE, INSERT
}

fun <T : Any> MutableList<T>.diffUpdate(
    newList: List<T>,
    areItemsTheSame: ((T, T) -> Boolean)? = null
) {
    if (newList.isEmpty() && this.isEmpty()) return
    if (newList.isEmpty()) {
        clear()
        return
    }
    if (this.isEmpty()) {
        addAll(newList)
        return
    }

    val equals: (T, T) -> Boolean = areItemsTheSame ?: { a, b -> a.equals(b) }

    val n = this.size
    val m = newList.size
    var prefixSize = 0
    while (prefixSize < n && prefixSize < m && equals(this[prefixSize], newList[prefixSize])) {
        prefixSize++
    }

    var suffixSize = 0
    while (
        suffixSize < n - prefixSize &&
        suffixSize < m - prefixSize &&
        equals(this[n - suffixSize - 1], newList[m - suffixSize - 1])
    ) {
        suffixSize++
    }

    val oldMiddleSize = n - prefixSize - suffixSize
    val newMiddleSize = m - prefixSize - suffixSize
    if (oldMiddleSize == 0 && newMiddleSize == 0) return

    if (oldMiddleSize.toLong() * newMiddleSize > MAX_DIFF_MATRIX_CELLS) {
        repeat(oldMiddleSize) { removeAt(prefixSize) }
        repeat(newMiddleSize) { offset -> add(prefixSize + offset, newList[prefixSize + offset]) }
        return
    }

    val dp = Array(oldMiddleSize + 1) { IntArray(newMiddleSize + 1) }
    for (i in oldMiddleSize - 1 downTo 0) {
        for (j in newMiddleSize - 1 downTo 0) {
            dp[i][j] = if (equals(this[prefixSize + i], newList[prefixSize + j])) {
                dp[i + 1][j + 1] + 1
            } else {
                maxOf(dp[i + 1][j], dp[i][j + 1])
            }
        }
    }

    val ops = mutableListOf<Pair<DiffOp, T?>>()
    var i = 0
    var j = 0
    while (i < oldMiddleSize && j < newMiddleSize) {
        if (equals(this[prefixSize + i], newList[prefixSize + j])) {
            ops.add(DiffOp.KEEP to null)
            i++
            j++
        } else if (dp[i + 1][j] >= dp[i][j + 1]) {
            ops.add(DiffOp.DELETE to null)
            i++
        } else {
            ops.add(DiffOp.INSERT to newList[prefixSize + j])
            j++
        }
    }
    while (i < oldMiddleSize) {
        ops.add(DiffOp.DELETE to null)
        i++
    }
    while (j < newMiddleSize) {
        ops.add(DiffOp.INSERT to newList[prefixSize + j])
        j++
    }

    var pos = prefixSize
    for ((kind, value) in ops) {
        when (kind) {
            DiffOp.KEEP -> pos++
            DiffOp.DELETE -> removeAt(pos)
            DiffOp.INSERT -> {
                add(pos, value!!)
                pos++
            }
        }
    }
}

private const val MAX_DIFF_MATRIX_CELLS = 250_000L
