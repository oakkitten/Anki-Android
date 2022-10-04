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

class CheckableTreeAdapter(context: Context) :
    WizardAdapter<CheckableTreeAdapter.Item>(context, R.layout.bottom_sheet_item) {

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
    ) : IconAndTextWizard.Item, CollapseWizard.Item, CheckWizard.Item, TreeFilteringWitch.Item

    fun update(
        sourceItems: List<SourceItem> = this.sourceItems,
        collapsedItemIds: Set<Long> = this.collapsedItemIds,
        checkedItemIds: Set<Long> = this.checkedItemIds,
        filterInput: String = this.filterInput,
    ) {
        val items = sourceItems.mapWithNext { current, next ->
            val collapsed = when {
                next == null || current.indent >= next.indent -> CollapseWizard.Collapsed.NotCollapsible
                current.id in collapsedItemIds -> CollapseWizard.Collapsed.Yes
                else -> CollapseWizard.Collapsed.No
            }

            val checked = when {
                !current.checkable -> CheckWizard.Checked.NotCheckable
                current.id in checkedItemIds -> CheckWizard.Checked.Yes
                else -> CheckWizard.Checked.No
            }

            Item(current.id, current.indent, collapsed, current.icon, current.text, checked)
        }

        val filter = { item: Item ->
            item.id == ALL_ITEMS_ID || item.text.contains(filterInput, ignoreCase = true)
        }

        this.sourceItems = sourceItems
        this.collapsedItemIds = collapsedItemIds
        this.checkedItemIds = checkedItemIds
        this.filterInput = filterInput

        setItems(CollapseWizard.filterCollapsedItems(TreeFilteringWitch.filterItems(items, filter)))
    }

    var checkedItemIds = setOf<Long>()
    private var collapsedItemIds = setOf<Long>()
    private var sourceItems: List<SourceItem> = listOf()
    private var filterInput = ""

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

/**************************************************************************************************/

data class Row(val id: Long, val icon: Int, val chain: Chain)

fun Iterable<Row>.toCheckableSourceItems(): List<CheckableTreeAdapter.SourceItem> {
    val flat = this.none { it.chain.hasMultipleLinks }

    return this.map { (id, icon, chain) ->
        CheckableTreeAdapter.SourceItem(
            id = id,
            icon = icon,
            text = chain.lastLink,
            indent = if (flat) -1 else chain.linkCount - 1,
            checkable = true
        )
    }
}

fun List<CheckableTreeAdapter.SourceItem>.withResettingItemPrepended(text: CharSequence):
    List<CheckableTreeAdapter.SourceItem> {
    val resettingItem = CheckableTreeAdapter.SourceItem(
        id = ALL_ITEMS_ID,
        icon = R.drawable.ic_all_items,
        text = text,
        indent = this.firstOrNull()?.indent ?: -1,
        checkable = false,
    )

    return listOf(resettingItem) + this
}
