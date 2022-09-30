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
import com.ichi2.anki.R
import java.lang.String.CASE_INSENSITIVE_ORDER
import java.util.*

class TreeAdapter(context: Context) : WizardAdapter<TreeAdapter.Item>(context, R.layout.bottom_sheet_item) {

    data class SourceItem(
        val id: Long,
        val icon: Int,
        val text: CharSequence,
        val indent: Int,
        val checkable: Boolean,
    )

    data class Item(
        override val id: Long,
        override val indent: Int,
        override val collapsed: CollapseWizard.Collapsed,
        override val icon: Int,
        override val text: CharSequence,
        override val checked: CheckWizard.Checked,
    ) : IconAndTextWizard.Item, CollapseWizard.Item, CheckWizard.Item

    fun update(
        sourceItems: List<SourceItem>? = null,
        collapsedItemIds: Set<Long>? = null,
        checkedItemIds: Set<Long>? = null
    ) {
        this.sourceItems = sourceItems ?: this.sourceItems
        this.collapsedItemIds = collapsedItemIds ?: this.collapsedItemIds
        this.checkedItemIds = checkedItemIds ?: this.checkedItemIds

        val items = this.sourceItems.mapWithNext { current, next ->
            val collapsed = when {
                next == null || current.indent >= next.indent -> CollapseWizard.Collapsed.NotCollapsible
                current.id in this.collapsedItemIds -> CollapseWizard.Collapsed.Yes
                else -> CollapseWizard.Collapsed.No
            }

            val checked = when {
                !current.checkable -> CheckWizard.Checked.NotCheckable
                current.id in this.checkedItemIds -> CheckWizard.Checked.Yes
                else -> CheckWizard.Checked.No
            }

            Item(current.id, current.indent, collapsed, current.icon, current.text, checked)
        }

        setItems(CollapseWizard.filterCollapsedItems(items))
    }

    var checkedItemIds = setOf<Long>()
    private var collapsedItemIds = setOf<Long>()
    private var sourceItems: List<SourceItem> = listOf()

    private val itemClickWizard = ItemClickWizard<Item>()
    private val iconAndTextWizard = IconAndTextWizard<Item>(context)
    private val collapseWizard = CollapseWizard<Item>(context)
    private val checkWizard = CheckWizard<Item>()

    init {
        collapseWizard.onChevronClickedListener = { id ->
            update(collapsedItemIds = collapsedItemIds xor id)
        }

        checkWizard.onCheckboxClickedListener = { id ->
            update(checkedItemIds = checkedItemIds xor id)
            onItemCheckedListener(id)
        }
    }

    override val wizards = listOf(itemClickWizard, iconAndTextWizard, checkWizard, collapseWizard)
    override val allNonAnimatableProperties = listOf(Item::indent, Item::icon, Item::text)

    var onItemCheckedListener: (Long) -> Unit = { }
    var onItemClickedListener by itemClickWizard::onItemClickedListener
}

/**************************************************************************************************
 **************************************************************************************************
 **************************************************************************************************/

data class Row(val id: Long, val icon: Int, val chain: String)

fun Iterable<Row>.toSourceItems(filter: String):
    List<TreeAdapter.SourceItem> {
    val flat = this.all { !it.chain.contains("::") }

    val matchingChainsAndTheirParents = this
        .map { it.chain }
        .filter { it.contains(filter, ignoreCase = true) }
        .toSortedChainSetWithMissingParentsAdded()

    return this.mapNotNull { (id, icon, chain) ->
        if (chain !in matchingChainsAndTheirParents) return@mapNotNull null

        TreeAdapter.SourceItem(
            id = id,
            icon = icon,
            text = chain.substringAfterLast("::"),
            indent = if (flat) -1 else chain.split("::").size - 1, // TODO better method?
            checkable = true
        )
    }
}

fun Iterable<String>.toSortedChainSetWithMissingParentsAdded() =
    TreeSet(CASE_INSENSITIVE_ORDER).also { out ->
        forEach { string ->
            val segments = string.split("::")
            (1..segments.size).forEach { end ->
                out.add(segments.subList(0, end).joinToString("::"))
            }
        }
    }

fun List<TreeAdapter.SourceItem>.withItemPrepended(sourceItem: TreeAdapter.SourceItem) =
    listOf(sourceItem.copy(indent = this.firstOrNull()?.indent ?: -1)) + this
