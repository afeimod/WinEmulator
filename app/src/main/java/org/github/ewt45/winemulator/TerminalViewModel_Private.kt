package org.github.ewt45.winemulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class TerminalViewModelPrivate:ViewModel() {
    private var process: Process? = null
    private var processWriter: OutputStreamWriter? = null

    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()

    private val _errorOutput = MutableStateFlow("")
    val errorOutput: StateFlow<String> = _errorOutput.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun startProcess(command: List<String>) {
        if (_isRunning.value) return // Prevent starting multiple times

        viewModelScope.launch(Dispatchers.IO) { // Use IO dispatcher for blocking operations
            try {
                _isRunning.value = true
                _output.value = "" // Clear previous output
                _errorOutput.value = ""

                val processBuilder = ProcessBuilder(command)
                process = processBuilder.start()
                processWriter = OutputStreamWriter(process?.outputStream)

                // Launch coroutines to read streams
                launch { readStream(process?.inputStream, _output) }
                launch { readStream(process?.errorStream, _errorOutput) }

                // Wait for process to finish (optional, depending on needs)
                val exitCode = process?.waitFor()
                _isRunning.value = false
                _output.value += "\nProcess exited with code: $exitCode"

            } catch (e: Exception) {
                _errorOutput.value += "\nError starting process: ${e.message}"
                _isRunning.value = false
            } finally {
                // Clean up resources in case of errors during startup too
                closeResources()
            }
        }
    }

    private suspend fun readStream(inputStream: java.io.InputStream?, stateFlow: MutableStateFlow<String>) {
        inputStream ?: return
        withContext(Dispatchers.IO) { // Ensure reading is on IO thread
            try {
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stateFlow.value += line + "\n" // Append line to the state flow
                    }
                }
            } catch (e: Exception) {
                if (!e.message.orEmpty().contains("Stream closed")) { // Avoid logging expected close error
                    _errorOutput.value += "\nError reading stream: ${e.message}"
                }
            }
        }
    }

    fun sendCommand(command: String) {
        if (!_isRunning.value || processWriter == null) {
            _errorOutput.value += "\nCannot send command: Process not running or writer not available."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Ensure command ends with a newline, often required by shell commands
                processWriter?.write(command + "\n")
                processWriter?.flush() // Ensure the command is sent immediately
            } catch (e: Exception) {
                _errorOutput.value += "\nError sending command: ${e.message}"
            }
        }
    }

    fun stopProcess() {
        viewModelScope.launch(Dispatchers.IO) {
            closeResources()
            _isRunning.value = false
        }
    }

    // Clean up resources
    private fun closeResources() {
        try { processWriter?.close() } catch (e: Exception) { /* Ignore */ }
        try { process?.outputStream?.close() } catch (e: Exception) { /* Ignore */ }
        try { process?.inputStream?.close() } catch (e: Exception) { /* Ignore */ }
        try { process?.errorStream?.close() } catch (e: Exception) { /* Ignore */ }
        try { process?.destroy() } catch (e: Exception) { /* Ignore */ }
        process = null
        processWriter = null
    }


    // Ensure process is destroyed when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        stopProcess() // Use the stopProcess logic which includes resource cleanup
    }
}