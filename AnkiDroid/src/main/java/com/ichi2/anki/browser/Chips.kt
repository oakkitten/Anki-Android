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

import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.ichi2.anki.CardBrowser
import com.ichi2.anki.R

fun CardBrowser.setupChips(chips: ChipGroup) {
    chips.findViewById<Chip>(R.id.chip_deck)
        .setOnClickListener { chip ->
            (chip as Chip).isChecked = !chip.isChecked
            DecksPieceOfSheetFragment().show(supportFragmentManager, DecksPieceOfSheetFragment.TAG)
        }

    chips.findViewById<Chip>(R.id.chip_tag)
        .setOnClickListener { chip ->
            (chip as Chip).isChecked = !chip.isChecked
            TagsPieceOfSheetFragment().show(supportFragmentManager, TagsPieceOfSheetFragment.TAG)
        }

    chips.findViewById<Chip>(R.id.chip_flag)
        .setOnClickListener { chip ->
            (chip as Chip).isChecked = !chip.isChecked
            FlagsPieceOfSheetFragment().show(supportFragmentManager, FlagsPieceOfSheetFragment.TAG)
        }

    chips.findViewById<Chip>(R.id.chip_today)
        .setOnClickListener { chip ->
            (chip as Chip).isChecked = !chip.isChecked
            TodayPieceOfSheetFragment().show(supportFragmentManager, TodayPieceOfSheetFragment.TAG)
        }

    chips.findViewById<Chip>(R.id.chip_state)
        .setOnClickListener { chip ->
            (chip as Chip).isChecked = !chip.isChecked
            StatePieceOfSheetFragment().show(supportFragmentManager, StatePieceOfSheetFragment.TAG)
        }
}

fun CardBrowser.updateChips(
    chips: ChipGroup,
    oldSearchParameters: SearchParameters,
    newSearchParameters: SearchParameters,
) {
    if (oldSearchParameters.deckIds != newSearchParameters.deckIds) {
        chips.findViewById<Chip>(R.id.chip_deck).let { chip ->
            chip.update(
                activeItems = newSearchParameters.deckIds,
                inactiveText = "Decks",
                activeTextGetter = { deckId -> col.decks.name(deckId).substringAfterLast("::") }
            )
        }
    }

    if (oldSearchParameters.tags != newSearchParameters.tags) {
        chips.findViewById<Chip>(R.id.chip_tag).let { chip ->
            chip.update(
                activeItems = newSearchParameters.tags,
                inactiveText = "Tags",
                activeTextGetter = { tag -> tag.substringAfterLast("::") }
            )
        }
    }

    if (oldSearchParameters.flagNumbers != newSearchParameters.flagNumbers) {
        chips.findViewById<Chip>(R.id.chip_flag).let { chip ->
            chip.update(
                activeItems = newSearchParameters.flagNumbers,
                inactiveText = "Flags",
                activeTextGetter = { number -> getFlagName(col, number.toInt()) }
            )
        }
    }

    if (oldSearchParameters.todays != newSearchParameters.todays) {
        chips.findViewById<Chip>(R.id.chip_today).let { chip ->
            chip.update(
                activeItems = newSearchParameters.todays,
                inactiveText = "Today",
                activeTextGetter = { today -> today.label }
            )
        }
    }

    if (oldSearchParameters.states != newSearchParameters.states) {
        chips.findViewById<Chip>(R.id.chip_state).let { chip ->
            chip.update(
                activeItems = newSearchParameters.states,
                inactiveText = "State",
                activeTextGetter = { state -> state.label }
            )
        }
    }
}

private fun <T> Chip.update(activeItems: Collection<T>, inactiveText: String, activeTextGetter: (T) -> String) {
    if (activeItems.isEmpty()) {
        isChecked = false
        text = inactiveText
    } else {
        isChecked = true

        text = if (activeItems.size == 1) {
            activeTextGetter(activeItems.first())
        } else {
            activeTextGetter(activeItems.first()) + "+" + (activeItems.size - 1).toString()
        }
    }
}
