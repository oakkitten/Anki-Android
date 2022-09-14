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

import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.R

// TODO More stable ids
class TagsPieceOfSheetFragment : PieceOfSheetFragment(), CardBrowserFragment {
    override val filterHintResource = R.string.filter_hint_search_tags

    override val layoutResource get() = if (tagRows.size > 10)
        R.layout.bottom_sheet_list_with_filter else R.layout.bottom_sheet_list_without_filter

    private lateinit var tagRows: List<Row>
    private lateinit var tagIdToName: Map<Long, String>

    override fun onPrepareAdapter() {
        tagRows = (requireActivity() as AnkiActivity)
            .col.tags.all()
            .toSortedChainSetWithMissingParentsAdded()
            .mapIndexed { index, chain ->
                val marked = chain.equals("marked", ignoreCase = true)
                val icon = if (marked) R.drawable.ic_marked else R.drawable.ic_tag
                Row(id = index.toLong(), icon, chain)
            }

        tagIdToName = tagRows.associate { (id, _, chain) -> id to chain }

        val tagNameToId = tagRows.associate { (id, _, chain) -> chain to id }

        adapter.update(
            sourceItems = tagRows.toSourceItems(filter = "").withItemPrepended(allTagsSourceItem),
            checkedItemIds = tagNameToId.values(cardBrowser.lastSearchParameters.tags)
        )
    }

    override fun onFilterInputChanged(filterInput: String) {
        val tagsSourceItems = tagRows.toSourceItems(filterInput)

        adapter.update(
            sourceItems = tagsSourceItems.withItemPrepended(allTagsSourceItem),
            collapsedItemIds = if (filterInput.isEmpty()) null else emptySet()
        )
    }

    override fun onItemsSelected(ids: Set<Long>) {
        cardBrowser.performSearch(cardBrowser.lastSearchParameters.copy(tags = tagIdToName.values(ids)))
    }

    companion object { const val TAG = "TagsPieceOfSheetFragment" }
}

private val allTagsSourceItem = TreeAdapter.SourceItem(
    id = ALL_ITEMS_ID,
    icon = R.drawable.ic_all_items,
    text = "Any tags",
    indent = 0,
    checkable = false,
)

private fun <K, V> Map<K, V>.values(keys: Set<K>) = keys.mapNotNull { this[it] }.toSet()
