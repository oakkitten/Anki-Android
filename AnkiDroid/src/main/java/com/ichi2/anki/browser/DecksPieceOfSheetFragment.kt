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
import com.ichi2.anki.servicelayer.DeckService.shouldShowDefaultDeck
import com.ichi2.libanki.DeckManager

class DecksPieceOfSheetFragment : PieceOfSheetFragment(), CardBrowserFragment {
    override val filterHintResource = R.string.filter_hint_search_decks

    override fun onPrepareAdapter() {
        val collection = cardBrowser.col
        val deckManager = collection.decks
        val shouldShowDefaultDeck = shouldShowDefaultDeck(collection)
        val dynamicDeckIds = deckManager.allDynamicDeckIds()

        val deckRows = deckManager
            .getDeckIdToName()
            .filter { shouldShowDefaultDeck || it.first != DEFAULT_DECK_ID }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.second })
            .map { (id, chain) ->
                val icon = if (id in dynamicDeckIds) R.drawable.ic_deck_filtered else R.drawable.ic_deck
                Row(id, icon, chain)
            }

        adapter.update(
            sourceItems = deckRows.toCheckableSourceItems().withResettingItemPrepended("All decks"),
            checkedItemIds = cardBrowser.lastSearchParameters.deckIds
        )
    }

    override fun onItemsSelected(ids: Set<Long>) {
        cardBrowser.performSearch(cardBrowser.lastSearchParameters.copy(deckIds = ids))
    }

    companion object { const val TAG = "DecksPieceOfSheetFragment" }
}

private fun DeckManager.getDeckIdToName(): List<Pair<Long, String>> =
    all().map {
        it.getLong("id") to it.getString("name")
    }

private const val DEFAULT_DECK_ID = 1L
