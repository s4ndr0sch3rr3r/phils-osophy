package com.example.phils_osophy.ui

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity

class TvTimeImportReportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val report = intent.getStringExtra(EXTRA_REPORT)
        if (report.isNullOrBlank()) {
            finish()
            return
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("TV Time import complete")
            .setMessage(report)
            .setPositiveButton("Copy", null)
            .setNegativeButton("Close") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val clipboard = getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("TV Time import report", report))
                Toast.makeText(this, "Import report copied", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    companion object {
        private const val EXTRA_REPORT = "tv_time_import_report"

        fun show(context: Context, report: String) {
            context.startActivity(
                Intent(context, TvTimeImportReportActivity::class.java)
                    .putExtra(EXTRA_REPORT, report)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
