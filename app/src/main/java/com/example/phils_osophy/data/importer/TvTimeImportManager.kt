package com.example.phils_osophy.data.importer

import android.content.Context
import android.net.Uri
import com.example.phils_osophy.ui.TvTimeImportReportActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TvTimeImportState(
    val isRunning: Boolean = false,
    val message: String? = null,
    val failed: Boolean = false,
    val completedAtEpochMillis: Long = 0
)

object TvTimeImportManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(TvTimeImportState())
    private var activeJob: Job? = null

    val state: StateFlow<TvTimeImportState> = mutableState.asStateFlow()

    fun start(context: Context, uri: Uri) {
        if (activeJob?.isActive == true) {
            return
        }

        val applicationContext = context.applicationContext
        mutableState.value = TvTimeImportState(isRunning = true)

        activeJob = scope.launch {
            mutableState.value = try {
                val result = ParallelTvTimeGdprImporter.importBackup(
                    context = applicationContext,
                    uri = uri
                )
                TvTimeImportReportActivity.show(
                    context = applicationContext,
                    report = result.completionReport()
                )
                TvTimeImportState(
                    message = result.summary(),
                    completedAtEpochMillis = System.currentTimeMillis()
                )
            } catch (exception: Exception) {
                TvTimeImportState(
                    message = exception.localizedMessage
                        ?: "The TV Time backup could not be imported.",
                    failed = true,
                    completedAtEpochMillis = System.currentTimeMillis()
                )
            }
        }
    }
}
