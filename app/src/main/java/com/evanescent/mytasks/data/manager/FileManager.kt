/*
 * Copyright (c) 2025. myTasks © 2021 by Evan Cocain is licensed under Attribution-NonCommercial-NoDerivatives 4.0 International. (A Creative Commons License)
 * Created and published by Evan Cocain (as Electro Inc.)
 */

package com.evanescent.mytasks.data.manager

import android.content.Context
import android.net.Uri
import com.evanescent.mytasks.data.model.Task
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class FileManager @Inject constructor(@ApplicationContext private val context: Context) {

    // Using a Json instance with prettyPrint for readable output files
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }



    /**
     * Writes a list of tasks to the given URI.
     * This method handles the actual file output stream.
     *
     * @param tasks The list of Task objects to write.
     * @param uri The URI of the file to write to (obtained from ACTION_CREATE_DOCUMENT).
     */
    fun writeTaskListTofile(tasks: List<Task>, uri: Uri) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    val jsonString = json.encodeToString(tasks)
                    writer.write(jsonString)
                    Timber.d("Task list successfully written to URI: $uri")
                }
            } ?: run {
                Timber.e("Failed to open output stream for URI: $uri")
                // No Toast here. The ViewModel will handle informing the user of the error.
            }
        } catch (e: Exception) {
            Timber.e(e, "Error writing task list to URI: $uri")
            // No Toast here. The ViewModel will handle informing the user of the error.
        }
    }

    // Removed createFile: Creating files (and launching intents) is a UI concern.
    // The Activity/ViewModel will handle launching ACTION_CREATE_DOCUMENT and providing the URI.

    // Removed exportToFile: This method was initiating createFile, which is a UI concern.
    // The ViewModel will now call writeTaskListTofile directly with the URI.

    // Removed openFile: Opening files (and launching intents) is a UI concern.
    // The Activity/ViewModel will handle launching ACTION_OPEN_DOCUMENT and providing the URI.

    /**
     * Reads text content from the given URI.
     * This method handles the actual file input stream.
     *
     * @param uri The URI of the file to read from (obtained from ACTION_OPEN_DOCUMENT).
     * @return The content of the file as a String. Returns an empty string on error.
     */
    fun readTextContent(uri: Uri): String {
        val stringBuilder = StringBuilder()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                }
            } ?: run {
                Timber.e("Failed to open input stream for URI: $uri")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading text content from URI: $uri")
        }
        return stringBuilder.toString()
    }

    // Renamed modifyDoc to overwriteFileContent for clarity on its purpose.
    // The persistable URI permission logic should ideally be handled where the URI is initially acquired (Activity/ViewModel).
    // However, for robust re-use, keeping it here when writing is acceptable, but ensure it's logged.
    /**
     * Overwrites the content of a file at the given URI with new text content.
     *
     * @param uri The URI of the file to modify.
     * @param textContent The new content to write to the file.
     */
    fun overwriteFileContent(uri: Uri, textContent: String) {
        try {
            // It's generally better to take persistable URI permissions where the URI is selected
            // (e.g., in TasksActivity.onActivityResult). However, for robustness here,
            // we'll ensure it's attempted, but log if issues arise.
            /* Removed direct call to takePersistableUriPermission here.
               This permission should be handled by the initiating Activity
               when the URI is first obtained (e.g., in onActivityResult after ACTION_CREATE_DOCUMENT).
               If you absolutely must do it here for specific cases, ensure proper error handling
               and user notification if permissions are denied.
            */

            context.contentResolver.openOutputStream(uri, "w")?.use { outputStream -> // Use "w" for overwrite mode
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(textContent)
                    Timber.d("File content successfully overwritten at URI: $uri")
                }
            } ?: run {
                Timber.e("Failed to open output stream for overwriting URI: $uri")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error overwriting file content at URI: $uri")
            // No Toast here. The ViewModel will handle informing the user of the error.
        }
    }

    // Helper for formatting date (can remain here or be a top-level utility function)
    private fun getDate(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return sdf.format(Date())
    }

    // Extension function for String (can be moved to a separate utility file if used elsewhere)
    private fun String.toSafeCase(): String {
        return this.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase(Locale.getDefault())
    }
}



