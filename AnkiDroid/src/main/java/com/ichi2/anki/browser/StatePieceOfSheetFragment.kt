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

class StatePieceOfSheetFragment : PieceOfSheetFragment(), CardBrowserFragment {
    override fun onPrepareAdapter() {
        val stateItems = State.values().map { state ->
            TreeAdapter.SourceItem(
                id = state.id,
                icon = state.icon,
                text = state.label,
                indent = -1,
                checkable = true
            )
        }

        adapter.update(
            sourceItems = stateItems.withItemPrepended(anyCardsSourceItem),
            checkedItemIds = cardBrowser.lastSearchParameters.states.map { it.id }.toSet()
        )
    }

    override fun onItemsSelected(ids: Set<Long>) {
        val state = ids.mapNotNull { id -> State::id.findOrNull(id) }.toSet()
        cardBrowser.performSearch(cardBrowser.lastSearchParameters.copy(states = state))
    }

    companion object { const val TAG = "StatePieceOfSheetFragment" }
}

private val anyCardsSourceItem = TreeAdapter.SourceItem(
    id = ALL_ITEMS_ID,
    icon = R.drawable.ic_all_items,
    text = "Any cards",
    indent = 0,
    checkable = false,
)

enum class State(
    val id: Long,
    val icon: Int,
    val label: String,
    val search: String,
) {
    New(0, R.drawable.ic_card_state_new, "New", "is:new"),
    Learning(1, R.drawable.ic_card_state_learning, "Learning", "is:learn"),
    Review(2, R.drawable.ic_card_state_review, "Review", "is:review"),
    Suspended(3, R.drawable.ic_card_state_suspended, "Suspended", "is:suspended"),
    Buried(4, R.drawable.ic_card_state_buried, "Buried", "is:buried"),
}
