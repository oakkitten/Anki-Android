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

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ichi2.anki.R
import com.ichi2.libanki.Collection
import com.ichi2.utils.JSONException

class FlagsPieceOfSheetFragment : PieceOfSheetFragment(), CardBrowserFragment {
    override fun onPrepareAdapter() {
        val flagNumberToName = requireContext().getFlagNumberToName(cardBrowser.col)

        val anyFlagsSourceItem = TreeAdapter.SourceItem(
            id = ALL_ITEMS_ID,
            icon = R.drawable.ic_all_items,
            text = "All flags",
            indent = 0,
            checkable = false,
        )

        val flagsSourceItems = Flags.values().map { flag ->
            TreeAdapter.SourceItem(
                id = flag.number.toLong(),
                icon = flag.icon,
                text = flagNumberToName.getOrDefault(flag.number, "What?"),
                indent = -1,
                checkable = true
            )
        }

        adapter.update(
            sourceItems = flagsSourceItems.withItemPrepended(anyFlagsSourceItem),
            checkedItemIds = cardBrowser.lastSearchParameters.flagNumbers
        )
    }

    override fun onItemsSelected(ids: Set<Long>) {
        cardBrowser.performSearch(cardBrowser.lastSearchParameters.copy(flagNumbers = ids))
    }

    companion object { const val TAG = "FlagsPieceOfSheetFragment" }
}

/**************************************************************************************************/

private enum class Flags(
    val number: Int,
    @StringRes val nameResource: Int,
    @DrawableRes val icon: Int
) {
    NoFlag(0, R.string.menu_flag_card_zero, R.drawable.label_icon_flags), // TODO Change icon
    Red(1, R.string.menu_flag_card_one, R.drawable.ic_flag_red),
    Orange(2, R.string.menu_flag_card_two, R.drawable.ic_flag_orange),
    Green(3, R.string.menu_flag_card_three, R.drawable.ic_flag_green),
    Blue(4, R.string.menu_flag_card_four, R.drawable.ic_flag_blue),
    Pink(5, R.string.menu_flag_card_five, R.drawable.ic_flag_pink),
    Turquoise(6, R.string.menu_flag_card_six, R.drawable.ic_flag_turquoise),
    Purple(7, R.string.menu_flag_card_seven, R.drawable.ic_flag_purple);
}

private fun Context.getFlagNumberToName(collection: Collection): Map<Int, String> {
    val flagLabelsJsonObject = collection.conf.getJSONObject("flagLabels")

    return Flags.values().associate { flag ->
        flag.number to try {
            flagLabelsJsonObject.getString(flag.number.toString())
        } catch (e: JSONException) {
            getString(flag.nameResource)
        }
    }
}

fun Context.getFlagName(collection: Collection, number: Int): String {
    val flagLabelsJsonObject = collection.conf.getJSONObject("flagLabels")

    return try {
        flagLabelsJsonObject.getString(number.toString())
    } catch (e: JSONException) {
        Flags::number.findOrNull(number)?.let { getString(it.nameResource) } ?: "What?"
    }
}
