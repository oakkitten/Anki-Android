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

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import java.util.concurrent.CancellationException

inline fun <reified E : Throwable, T, R : Any> Iterable<T>.mapNotThrowing(transform: (T) -> R?) =
    mapNotNull {
        suppressOrNull<E, _> { transform(it) }
    }

inline fun <reified T : Throwable, R> suppressOrNull(f: () -> R) =
    try {
        f()
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        if (t is T) null else throw t
    }

inline fun <reified T : Enum<T>, V> ((T) -> V).findOrNull(value: V): T? {
    return enumValues<T>().firstOrNull { this(it) == value }
}

inline fun <reified T : Parcelable> Bundle.retrieveParcelable(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, T::class.java)
    } else {
        @Suppress("DEPRECATION") getParcelable(key)
    }
}
