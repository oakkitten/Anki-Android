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
import android.view.LayoutInflater
import android.view.View
import android.view.View.LAYOUT_DIRECTION_RTL
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_ID
import com.ichi2.anki.R
import com.ichi2.anki.UIUtils
import java.lang.String.CASE_INSENSITIVE_ORDER
import java.util.*

class TreeAdapter(context: Context) : SillyTreeAdapter(context) {
    data class SourceItem(
        val id: Long,
        val icon: Int,
        val text: CharSequence,
        val indent: Int,
        val checkable: Boolean,
    )

    fun update(
        sourceItems: List<SourceItem>? = null,
        collapsedItemIds: Set<Long>? = null,
        checkedItemIds: Set<Long>? = null
    ) {
        val usedSourceItems = sourceItems ?: this.sourceItems
        val usedCollapsedItemIds = collapsedItemIds ?: this.collapsedItemIds
        val usedCheckedItemIds = checkedItemIds ?: this.checkedItemIds

        val items = mutableListOf<Item>()

        usedSourceItems.forEachWithNext { current, next ->
            items.lastOrNull()?.let { last ->
                if (current.indent > last.indent && last.collapsed == Collapsed.Yes) return@forEachWithNext
            }

            val collapsed = when {
                next == null || current.indent >= next.indent -> Collapsed.NotCollapsible
                current.id in usedCollapsedItemIds -> Collapsed.Yes
                else -> Collapsed.No
            }

            val checked = when {
                !current.checkable -> Checked.NotCheckable
                current.id in usedCheckedItemIds -> Checked.Yes
                else -> Checked.No
            }

            items.add(Item(current.id, current.icon, current.text, current.indent, collapsed, checked))
        }

        this.sourceItems = usedSourceItems

        setItems(items, usedCollapsedItemIds, usedCheckedItemIds)
    }

    override fun updateCollapsedItemIds(ids: Set<Long>) = update(collapsedItemIds = ids)
    override fun updateCheckedItemIds(ids: Set<Long>) = update(checkedItemIds = ids)

    private var sourceItems: List<SourceItem> = listOf()
}

/**************************************************************************************************/

abstract class SillyTreeAdapter(val context: Context) : RecyclerView.Adapter<SillyTreeAdapter.Holder>() {
    init { this@SillyTreeAdapter.setHasStableIds(true) }

    enum class Collapsed { Yes, No, NotCollapsible }

    enum class Checked { Yes, No, NotCheckable }

    data class Item(
        val id: Long,
        val icon: Int,
        val text: CharSequence,
        val indent: Int,
        val collapsed: Collapsed,
        val checked: Checked,
    )

    var onItemClickedListener: ((Long) -> Unit) = { }
    var onItemCheckedListener: ((Long) -> Unit) = { }

    abstract fun updateCollapsedItemIds(ids: Set<Long>)
    abstract fun updateCheckedItemIds(ids: Set<Long>)

    protected fun setItems(items: List<Item>, collapsedItemIds: Set<Long>, checkedItemIds: Set<Long>) {
        val diff = DiffUtil.calculateDiff(DiffCallback(this.items, items))

        this.items = items
        this.collapsedItemIds = collapsedItemIds
        this.checkedItemIds = checkedItemIds

        diff.dispatchUpdatesTo(this)
    }

    object UpdateCollapsedOrCheckedOnly

    private class DiffCallback(old: List<Item>, new: List<Item>) : ListDiffCallback<Item>(old, new) {
        override fun areItemsTheSame(old: Item, new: Item) = old.id == new.id
        override fun areContentsTheSame(old: Item, new: Item) = old == new
        override fun getChangePayload(old: Item, new: Item) =
            if (old.icon != new.icon || old.text != new.text || old.indent != new.indent)
                null else UpdateCollapsedOrCheckedOnly
    }

    var collapsedItemIds = setOf<Long>()
        private set
    var checkedItemIds = setOf<Long>()
        private set

    private var items: List<Item> = listOf()

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val chevron: ImageView = view.findViewById(R.id.chevron)
        val icon: ImageView = view.findViewById(R.id.icon)
        val text: TextView = view.findViewById(R.id.text)
        val checkbox: CheckBox = view.findViewById(R.id.checkbox)

        init {
            chevron.setOnClickListenerWithId { id ->
                updateCollapsedItemIds(collapsedItemIds xor id)
            }

            checkbox.setOnClickListenerWithId { id ->
                updateCheckedItemIds(checkedItemIds xor id)
                onItemCheckedListener(id)
            }

            view.setOnClickListenerWithId { id ->
                onItemClickedListener(id)
            }
        }

        fun fullBind(item: Item) {
            chevron.updateLayoutParams<MarginLayoutParams> {
                marginStart = (mandatoryMargin + indentMargin * item.indent).toInt()
            }

            text.text = item.text

            icon.setImageDrawable(AppCompatResources.getDrawable(context, item.icon))

            chevron.animate().cancel()

            when (item.collapsed) {
                Collapsed.No -> {
                    chevron.alpha = 1f
                    chevron.rotation = expandedChevronAngle
                }
                Collapsed.Yes -> {
                    chevron.alpha = 1f
                    chevron.rotation = collapsedChevronAngle
                }
                Collapsed.NotCollapsible -> {
                    chevron.alpha = 0f
                }
            }

            chevron.isClickable = item.collapsed != Collapsed.NotCollapsible

            checkbox.isVisible = item.checked != Checked.NotCheckable
            checkbox.isChecked = item.checked == Checked.Yes
        }

        fun bindCollapsedAndCheckedOnly(item: Item) {
            chevron.animate().apply {
                when (item.collapsed) {
                    Collapsed.No -> rotation(expandedChevronAngle).alpha(1f)
                    Collapsed.Yes -> rotation(collapsedChevronAngle).alpha(1f)
                    Collapsed.NotCollapsible -> alpha(0f)
                }
            }.start()

            chevron.isClickable = item.collapsed != Collapsed.NotCollapsible

            checkbox.isVisible = item.checked != Checked.NotCheckable
            checkbox.isChecked = item.checked == Checked.Yes
        }
    }

    override fun getItemId(position: Int) = items[position].id

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(inflater.inflate(R.layout.bottom_sheet_item, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.fullBind(items[position])
    }

    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.any { it is UpdateCollapsedOrCheckedOnly }) {
            holder.bindCollapsedAndCheckedOnly(items[position])
        } else {
            holder.fullBind(items[position])
        }
    }

    private val inflater = LayoutInflater.from(context)

    private val mandatoryMargin = UIUtils.convertDpToPixel(10f, context)
    private val indentMargin = UIUtils.convertDpToPixel(24f, context)

    private val expandedChevronAngle = if (context.isRtl) -90f else 90f
    private val collapsedChevronAngle = 0f
}

/**************************************************************************************************
 **************************************************************************************************
 **************************************************************************************************/

fun Iterable<String>.toSortedChainSetWithMissingParentsAdded() =
    TreeSet(CASE_INSENSITIVE_ORDER).also { out ->
        forEach { string ->
            val segments = string.split("::")
            (1..segments.size).forEach { end ->
                out.add(segments.subList(0, end).joinToString("::"))
            }
        }
    }

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

fun List<TreeAdapter.SourceItem>.withItemPrepended(sourceItem: TreeAdapter.SourceItem) =
    listOf(sourceItem.copy(indent = this.firstOrNull()?.indent ?: -1)) + this

/**************************************************************************************************/

private val Context.isRtl get() = resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL

private infix fun <E> Set<E>.xor(element: E) =
    if (contains(element)) this - element else this + element

private inline fun <E> List<E>.forEachWithNext(block: (E, E?) -> Unit) {
    (0..lastIndex).forEach { idx ->
        block(this[idx], if (idx == lastIndex) null else this[idx + 1])
    }
}

context(RecyclerView.ViewHolder)
private fun View.setOnClickListenerWithId(block: (Long) -> Unit) {
    setOnClickListener {
        val id = itemId
        if (id != NO_ID) block(id)
    }
}

private abstract class ListDiffCallback<T>(val old: List<T>, val new: List<T>) : DiffUtil.Callback() {
    abstract fun areItemsTheSame(old: T, new: T): Boolean
    abstract fun areContentsTheSame(old: T, new: T): Boolean
    abstract fun getChangePayload(old: T, new: T): Any?

    override fun getOldListSize() = old.size
    override fun getNewListSize() = new.size
    override fun areItemsTheSame(oldPos: Int, newPos: Int) = areItemsTheSame(old[oldPos], new[newPos])
    override fun areContentsTheSame(oldPos: Int, newPos: Int) = areContentsTheSame(old[oldPos], new[newPos])
    override fun getChangePayload(oldPos: Int, newPos: Int) = getChangePayload(old[oldPos], new[newPos])
}
