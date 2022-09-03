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

package com.ichi2.anki.preferences

import android.R
import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

/**
 * Non-clickable preference that shows help text. In XML, you only want to specify the summary, e.g.
 *
 *     <com.ichi2.anki.preferences.HtmlHelpPreference android:summary="@string/foo" />
 *
 * The summary is parsed as HTML, which can contain all tags that TextViews can display,
 * including new lines and links. Raw HTML can be entered using the CDATA tag, e.g.
 *
 *     <string name="foo"><![CDATA[<b>Bold text</b>]]></string>
 */
class HtmlHelpPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    init {
        isSelectable = false
        isPersistent = false
    }

    override fun getSummary() = super.getSummary()?.let {
        HtmlCompat.fromHtml(it.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val summary = holder.findViewById(R.id.summary) as TextView
        summary.movementMethod = LinkMovementMethod.getInstance()
        summary.maxHeight = Int.MAX_VALUE
    }
}
