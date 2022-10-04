/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.ichi2.anki.browser

import com.ichi2.anki.R

class TagsPieceOfSheetFragment : PieceOfSheetFragment(), CardBrowserFragment {
    override val filterHintResource = R.string.filter_hint_search_tags

    private lateinit var tagIdToName: Map<Long, String>

    override fun onPrepareAdapter() {
        val tagRows = cardBrowser
            .col.tags.all()
            .toSortedSetOfChainsWithMissingSubchainsAdded()
            .mapIndexed { index, chain ->
                val marked = chain.equals("marked", ignoreCase = true)
                val icon = if (marked) R.drawable.ic_marked else R.drawable.ic_tag
                Row(id = index.toLong(), icon, chain) // TODO More stable ids
            }

        tagIdToName = tagRows.associate { (id, _, chain) -> id to chain }

        adapter.update(
            sourceItems = tagRows.toCheckableSourceItems().withResettingItemPrepended("Any tags"),
            checkedItemIds = tagIdToName.keysBy(values = cardBrowser.lastSearchParameters.tags)
        )
    }

    override fun onItemsSelected(ids: Set<Long>) {
        cardBrowser.performSearch(cardBrowser.lastSearchParameters.copy(tags = tagIdToName.valuesBy(keys = ids)))
    }

    companion object { const val TAG = "TagsPieceOfSheetFragment" }
}

private fun <K, V : Any> Map<K, V>.valuesBy(keys: Set<K>): Set<V> =
    keys.mapNotNullTo(mutableSetOf()) { this[it] }

private fun <K : Any, V> Map<K, V>.keysBy(values: Set<V>): Set<K> =
    entries.mapNotNullTo(mutableSetOf()) { (key, value) -> if (value in values) key else null }
