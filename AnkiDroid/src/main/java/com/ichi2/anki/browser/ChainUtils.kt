package com.ichi2.anki.browser

import java.util.*

/**
 * A Chain is a string of what we shall call links joined with double colon,
 * E.g. `foo::bar` is a chain with two links, `foo` and `bar`, and `` is chain with a single link ``.
 * This could be a value class but since it's put in a TreeSet it'd be boxed.
 */
typealias Chain = String

val Chain.linkCount get() = countNonOverlapping("::") + 1

val Chain.lastLink get() = substringAfterLast("::")

val Chain.hasMultipleLinks get() = contains("::")

fun Chain.getSubchainWithoutLastLinkOrNull(): Chain? {
    val index = lastIndexOf("::")
    return if (index == -1) null else substring(0, index)
}

fun Iterable<String>.toSortedSetOfChainsWithMissingSubchainsAdded(): SortedSet<Chain> =
    TreeSet(Chain.CASE_INSENSITIVE_ORDER).also { result ->
        forEach { string ->
            var chain: Chain? = string

            while (chain != null) {
                result.add(chain)
                chain = chain.getSubchainWithoutLastLinkOrNull()
            }
        }
    }
