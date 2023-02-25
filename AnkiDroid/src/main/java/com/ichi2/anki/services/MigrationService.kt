/*
 *  Copyright (c) 2023 David Allison <davidallisongithub@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 3 of the License, or (at your option) any later
 *  version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ichi2.anki.services

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.ichi2.anki.*
import com.ichi2.anki.servicelayer.ScopedStorageService.PREF_MIGRATION_DESTINATION
import com.ichi2.anki.servicelayer.ScopedStorageService.PREF_MIGRATION_SOURCE
import com.ichi2.anki.servicelayer.scopedstorage.MigrateEssentialFiles
import com.ichi2.anki.servicelayer.scopedstorage.MoveConflictedFile
import com.ichi2.anki.servicelayer.scopedstorage.migrateuserdata.MigrateUserData
import com.ichi2.utils.FileUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import kotlin.concurrent.thread
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A service which migrates the AnkiDroid collection from a legacy directory to an app-specific directory.
 */
class MigrationService : Service() {

    interface Progress {
        object CalculatingTransferSize : Progress

        data class Transferring(val transferredBytes: Long, val totalBytes: Long) : Progress

        interface Done : Progress
        object Success : Done
        data class Failure(val e: Exception) : Done
    }

    var flowOfProgress: MutableStateFlow<Progress> = MutableStateFlow(Progress.CalculatingTransferSize)

    private var isStarted = false

    // TODO Test cancellation
    private val lifecycleScope = CoroutineScope(EmptyCoroutineContext)

    private fun getRestartBehavior() = START_STICKY

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If a service is called twice, onStartCommand is called twice
        if (isStarted) {
            Timber.v("rejected onStartCommand")
            return getRestartBehavior()
        }
        isStarted = true
        Timber.d("onStartCommand")

        val migrateUserDataTask = try {
            MigrateUserData.createInstance(AnkiDroidApp.getSharedPrefs(this))
        } catch (e: MigrateUserData.MissingDirectoryException) {
            // TODO: Log and handle - likely SD card removal
            throw e
        } catch (e: Exception) {
            stopSelf()
            return getRestartBehavior()
        }

        // a migration is not taking place
        if (migrateUserDataTask == null) {
            Timber.w("MigrationService started when a migration was not taking place")
            stopSelf()
            return getRestartBehavior()
        }

        // TODO Use a coroutine here?
        thread(name = "Storage Migration") {
            flowOfProgress.tryEmit(Progress.CalculatingTransferSize)

            try {
                var transferredBytes = 0L
                val totalBytesToTransfer = getRemainingTransferSize(migrateUserDataTask)
                migrateUserDataTask.migrateFiles(progressListener = { deltaTransferredBytes ->
                    transferredBytes += deltaTransferredBytes
                    flowOfProgress.tryEmit(Progress.Transferring(transferredBytes, totalBytesToTransfer))
                })
                flowOfProgress.tryEmit(Progress.Success)
            } catch (e: Exception) {
                CrashReportService.sendExceptionReport(e, "Storage Migration Failed")
                flowOfProgress.tryEmit(Progress.Failure(e))
            }
        }

        lifecycleScope.launch {
            flowOfProgress.collect { progress ->
                startForeground(2, makeMigrationProgressNotification(progress))

                if (progress == Progress.Success) {
                    AnkiDroidApp.getSharedPrefs(this@MigrationService).edit {
                        remove(PREF_MIGRATION_DESTINATION)
                        remove(PREF_MIGRATION_SOURCE)
                    }
                    UIUtils.showThemedToast(this@MigrationService, R.string.migration_successful_message, true)
                    stopSelf()
                }
            }
        }

        return getRestartBehavior()
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        lifecycleScope.coroutineContext.cancel()

        // TODO: Why the below line was necessary?
        // if (::migrateUserDataTask.isInitialized) { migrateUserDataTask.executor.terminate() }
    }

    // TODO This is inadequate, instead of calculating the remaining transfer size
    //   every time migration is started,
    //   we should be calculating this size only when starting migration for the first time,
    //   and keeping track of the transferred and the remaining files.
    private fun getRemainingTransferSize(task: MigrateUserData): Long {
        val ignoredFiles = MigrateEssentialFiles.iterateEssentialFiles(task.source) +
            File(task.source.directory, MoveConflictedFile.CONFLICT_DIRECTORY)
        val ignoredSpace = ignoredFiles.sumOf { FileUtil.getSize(it) }
        val folderSize = FileUtil.DirectoryContentInformation.fromDirectory(task.source.directory).totalBytes
        val remainingSpaceToMigrate = folderSize - ignoredSpace
        Timber.d("folder size: %d, safe: %d, remaining: %d", folderSize, ignoredSpace, remainingSpaceToMigrate)
        return remainingSpaceToMigrate
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     *
     * See: https://developer.android.com/guide/components/bound-services#Binder
     */
    inner class LocalBinder : Binder(), SimpleBinder<MigrationService> {
        override fun getService(): MigrationService = this@MigrationService
    }

    override fun onBind(intent: Intent): IBinder = LocalBinder()
}

private fun Context.makeMigrationProgressNotification(progress: MigrationService.Progress): Notification {
    val builder = NotificationCompat.Builder(this, Channel.SCOPED_STORAGE_MIGRATION.id )
        .setSmallIcon(R.drawable.ic_star_notify)
        .setContentTitle(resources.getString(R.string.migrating_data_message))
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setSilent(true)

    when (progress) {
        is MigrationService.Progress.CalculatingTransferSize -> {
            builder.setOngoing(true)
            builder.setProgress(0, 0, true)
            builder.setContentText("Calculating transfer size…")
        }

        is MigrationService.Progress.Transferring -> {
            val progressRatio = progress.transferredBytes.toFloat() / progress.totalBytes

            val transferredSizeText = Formatter.formatShortFileSize(this, progress.transferredBytes)
            val totalSizeText = Formatter.formatShortFileSize(this, progress.totalBytes)

            builder.setOngoing(true)
            builder.setProgress(Int.MAX_VALUE, (progressRatio * Int.MAX_VALUE).toInt(), false)
            builder.setContentText("Migrated $transferredSizeText of $totalSizeText")
        }

        is MigrationService.Progress.Success -> {
            builder.setProgress(100, 100, false)
            builder.setContentText("Migration done")
        }

        is MigrationService.Progress.Failure -> {
            // TODO Add back Copy debug info
            builder.setContentText("Migration failed: ${progress.e}")
        }
    }

    return builder.build()
}
