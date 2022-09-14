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

class TodayPieceOfSheetFragment : PieceOfSheetFragment(), CardBrowserFragment {
    override fun onPrepareAdapter() {
        val todayItems = Today.values().map { today ->
            TreeAdapter.SourceItem(
                id = today.id,
                icon = R.drawable.ic_clock,
                text = today.label,
                indent = -1,
                checkable = true
            )
        }

        adapter.update(
            sourceItems = todayItems.withItemPrepended(anyCardsSourceItem),
            checkedItemIds = cardBrowser.lastSearchParameters.todays.map { it.id }.toSet()
        )
    }

    override fun onItemsSelected(ids: Set<Long>) {
        val today = ids.mapNotNull { id -> Today::id.findOrNull(id) }.toSet()
        cardBrowser.performSearch(cardBrowser.lastSearchParameters.copy(todays = today))
    }

    companion object { const val TAG = "TodayPieceOfSheetFragment" }
}

private val anyCardsSourceItem = TreeAdapter.SourceItem(
    id = ALL_ITEMS_ID,
    icon = R.drawable.ic_all_items,
    text = "Any cards",
    indent = 0,
    checkable = false,
)

enum class Today(
    val id: Long,
    val label: String,
    val search: String,
) {
    Due(0, "Due today", "prop:due=0"),
    Added(1, "Added today", "added:1"),
    Edited(2, "Edited today", "edited:1"),
    Studied(3, "Studied today", "rated:1"),
    FirstReviewed(4, "First reviewed today", "introduced:1"),
    Rescheduled(5, "Rescheduled today", "resched:1"),
    Again(6, "Rated again today", "rated:1:1"),
    Overdue(7, "Overdue", "is:due -prop:due=0"),
}
