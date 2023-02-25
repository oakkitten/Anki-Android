/*
 Copyright (c) 2023 Ashish Yadav <mailtoashish693@gmail.com>

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free Software
 Foundation; either version 3 of the License, or (at your option) any later
 version.

 This program is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE. See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with
 this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ichi2.anki.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.R
import com.ichi2.anki.services.MigrationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// TODO We probably don't need this and can use the service directly
class MigrationProgressViewModel : ViewModel() {
    val flowOfProgress: MutableStateFlow<MigrationService.Progress> =
        MutableStateFlow(MigrationService.Progress.CalculatingTransferSize)

    fun observeService(context: Context) {
        context.bindToMigrationService { service ->
            viewModelScope.launch {
                service.flowOfProgress.collect { progress -> flowOfProgress.emit(progress) }
            }
        }
    }
}

class MigrationProgressDialogFragment : DialogFragment() {

    private val progressViewModel by viewModels<MigrationProgressViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        progressViewModel.observeService(requireContext())

        val layout = requireActivity().layoutInflater.inflate(R.layout.indeterminate_progress_bar, null)
        val progressBar = layout.findViewById<ProgressBar>(R.id.indeterminate_progressBar)
        val textView = layout.findViewById<TextView>(R.id.migration_text)

        progressBar.max = Int.MAX_VALUE

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                progressViewModel.flowOfProgress.collect { progress ->
                    // TODO Properly display progress
                    textView.text = progress.toString()
                }
            }
        }

        return AlertDialog.Builder(activity)
            .setView(layout)
            .setPositiveButton(R.string.dialog_ok) { _, _ -> dismiss() }
            .setNegativeButton(R.string.scoped_storage_learn_more) { _, _ ->
                (requireActivity() as AnkiActivity).openUrl(R.string.link_scoped_storage_faq)
            }
            .create()
    }
}

// TODO This is temporary and/or misplaced, move somewhere
fun Context.bindToMigrationService(block: (MigrationService) -> Unit) {
    val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            block((service as MigrationService.LocalBinder).getService())
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    val intent = Intent(this, MigrationService::class.java)
    bindService(intent, connection, Context.BIND_AUTO_CREATE)
}