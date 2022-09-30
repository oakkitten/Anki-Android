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
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat.LAYOUT_DIRECTION_RTL
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_ID
import com.ichi2.anki.R
import com.ichi2.anki.UIUtils
import kotlin.reflect.KProperty1

class ItemClickWizard<T : WizardAdapter.Item> : WizardAdapter.Wizard<T>() {
    var onItemClickedListener: ((Long) -> Unit) = { }

    context(RecyclerView.ViewHolder) override fun makeBuddy(view: View): Nothing? {
        view.setOnClickListenerWithId { id -> onItemClickedListener(id) }
        return null
    }
}

/**************************************************************************************************/

class IconAndTextWizard<T : IconAndTextWizard.Item>(val context: Context) : WizardAdapter.Wizard<T>() {
    interface Item : WizardAdapter.Item {
        val icon: Int
        val text: CharSequence
    }

    inner class Buddy(view: View) : WizardAdapter.ViewHolderBuddy<T> {
        private val textView = view.findViewById<TextView>(R.id.text)
        private val iconView = view.findViewById<ImageView>(R.id.icon)

        override fun bind(item: T) {
            textView.text = item.text
            iconView.setImageDrawable(AppCompatResources.getDrawable(context, item.icon))
        }
    }

    context(RecyclerView.ViewHolder) override fun makeBuddy(view: View) = Buddy(view)
    override val animatableProperties = emptyList<KProperty1<T, *>>()
}

/**************************************************************************************************/

class CollapseWizard<T : CollapseWizard.Item>(val context: Context) : WizardAdapter.Wizard<T>() {
    enum class Collapsed { Yes, No, NotCollapsible }

    interface Item : WizardAdapter.Item {
        val collapsed: Collapsed
        val indent: Int
    }

    var onChevronClickedListener: ((Long) -> Unit) = { }

    context(RecyclerView.ViewHolder)
    inner class Buddy(view: View) : WizardAdapter.ViewHolderBuddy<T> {
        private val chevron = view.findViewById<ImageView>(R.id.chevron)

        init {
            chevron.setOnClickListenerWithId { id ->
                onChevronClickedListener(id)
            }
        }

        override fun bind(item: T) {
            chevron.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginStart = (mandatoryMargin + indentMargin * item.indent).toInt()
            }

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
        }

        override fun animate(item: T) {
            chevron.animate().apply {
                when (item.collapsed) {
                    Collapsed.No -> rotation(expandedChevronAngle).alpha(1f)
                    Collapsed.Yes -> rotation(collapsedChevronAngle).alpha(1f)
                    Collapsed.NotCollapsible -> alpha(0f)
                }
            }.start()

            chevron.isClickable = item.collapsed != Collapsed.NotCollapsible
        }
    }

    private val mandatoryMargin = UIUtils.convertDpToPixel(10f, context)
    private val indentMargin = UIUtils.convertDpToPixel(24f, context)

    private val expandedChevronAngle = if (context.isRtl) -90f else 90f
    private val collapsedChevronAngle = 0f

    context(RecyclerView.ViewHolder) override fun makeBuddy(view: View) = Buddy(view)
    override val animatableProperties = listOf(Item::collapsed)

    companion object {
        fun <T : Item> filterCollapsedItems(items: Iterable<T>): List<T> {
            var keepUpToIndent = Int.MAX_VALUE

            return items.filter { item ->
                val show = item.indent <= keepUpToIndent
                if (show) {
                    keepUpToIndent = if (item.collapsed == Collapsed.Yes) item.indent else Int.MAX_VALUE
                }
                show
            }
        }
    }
}

/**************************************************************************************************/

class CheckWizard<T : CheckWizard.Item> : WizardAdapter.Wizard<T>() {
    enum class Checked { Yes, No, NotCheckable }

    interface Item : WizardAdapter.Item {
        val checked: Checked
    }

    var onCheckboxClickedListener: ((Long) -> Unit) = { }

    context(RecyclerView.ViewHolder)
    inner class Buddy(view: View) : WizardAdapter.ViewHolderBuddy<T> {
        private val checkbox = view.findViewById<CheckBox>(R.id.checkbox)

        init {
            checkbox.setOnClickListenerWithId { id ->
                onCheckboxClickedListener(id)
            }
        }

        override fun bind(item: T) {
            checkbox.isVisible = item.checked != Checked.NotCheckable
            checkbox.isChecked = item.checked == Checked.Yes
        }
    }

    context(RecyclerView.ViewHolder) override fun makeBuddy(view: View) = Buddy(view)
    override val animatableProperties = listOf(Item::checked)
}

/**************************************************************************************************
 **************************************************************************************************
 **************************************************************************************************/

abstract class WizardAdapter<T : WizardAdapter.Item>(
    context: Context,
    private val itemLayoutResource: Int,
) : RecyclerView.Adapter<WizardAdapter<T>.Holder>() {

    init { this@WizardAdapter.setHasStableIds(true) }

    interface Item { val id: Long }

    interface ViewHolderBuddy<I : Item> {
        fun bind(item: I)
        fun animate(item: I) { bind(item) }
    }

    // https://youtrack.jetbrains.com/issue/KT-51474
    abstract class Wizard<I : Item> {
        context(RecyclerView.ViewHolder) abstract fun makeBuddy(view: View): ViewHolderBuddy<I>?
        open val animatableProperties: Iterable<KProperty1<in I, *>> = emptyList()
    }

    fun setItems(items: List<T>) {
        val oldItems = this.items
        this.items = items
        DiffUtil.calculateDiff(Callback(oldItems, items)).dispatchUpdatesTo(this)
    }

    abstract val wizards: List<Wizard<T>>

    // Can be computed via reflection but this is a bit too slow
    abstract val allNonAnimatableProperties: Collection<KProperty1<T, Any>>

    private inner class Callback(old: List<T>, new: List<T>) : ListDiffCallback<T>(old, new) {
        override fun areItemsTheSame(old: T, new: T) = old.id == new.id
        override fun areContentsTheSame(old: T, new: T) = old == new

        override fun getChangePayload(old: T, new: T): Any? {
            if (allNonAnimatableProperties.any { it.get(old) != it.get(new) }) return null

            return wizards.mapNotNull { wizard ->
                val canAnimate = wizard.animatableProperties.any { it.get(old) != it.get(new) }
                if (canAnimate) wizard else null
            }
        }
    }

    private var items: List<T> = listOf()

    override fun getItemCount() = items.size

    override fun getItemId(position: Int) = items[position].id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(inflater.inflate(itemLayoutResource, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            holder.bind(items[position])
        } else {
            @Suppress("UNCHECKED_CAST") // This is what Callback.getChangePayload returns
            val animatingWizards = payloads.single() as Collection<Wizard<T>>
            holder.animate(items[position], animatingWizards)
        }
    }

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val buddies = wizards.map { it.makeBuddy(view) }

        fun bind(item: T) {
            buddies.forEach { it?.bind(item) }
        }

        fun animate(item: T, animatingWizards: Collection<Wizard<T>>) {
            wizards.forEachIndexed { index, wizard: Wizard<T> ->
                if (wizard in animatingWizards) buddies[index]?.animate(item)
            }
        }
    }

    private val inflater: LayoutInflater = LayoutInflater.from(context)
}

/**************************************************************************************************
 **************************************************************************************************
 **************************************************************************************************/

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

context(RecyclerView.ViewHolder) private fun View.setOnClickListenerWithId(block: (Long) -> Unit) {
    setOnClickListener {
        val id = itemId
        if (id != NO_ID) block(id)
    }
}

private val Context.isRtl get() = resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL
