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

    val dp = Array(n + 1) { IntArray(m + 1) }
    for (i in n - 1 downTo 0) {
        for (j in m - 1 downTo 0) {
            dp[i][j] = if (equals(this[i], newList[j])) {
                dp[i + 1][j + 1] + 1
            } else {
                maxOf(dp[i + 1][j], dp[i][j + 1])
            }
        }
    }

    val ops = mutableListOf<Pair<DiffOp, T?>>()
    var i = 0
    var j = 0
    while (i < n && j < m) {
        if (equals(this[i], newList[j])) {
            ops.add(DiffOp.KEEP to null)
            i++
            j++
        } else if (dp[i + 1][j] >= dp[i][j + 1]) {
            ops.add(DiffOp.DELETE to null)
            i++
        } else {
            ops.add(DiffOp.INSERT to newList[j])
            j++
        }
    }
    while (i < n) {
        ops.add(DiffOp.DELETE to null)
        i++
    }
    while (j < m) {
        ops.add(DiffOp.INSERT to newList[j])
        j++
    }

    var pos = 0
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
