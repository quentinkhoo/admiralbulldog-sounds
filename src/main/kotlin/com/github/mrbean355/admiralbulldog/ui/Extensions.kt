package com.github.mrbean355.admiralbulldog.ui

import com.github.mrbean355.admiralbulldog.TITLE_MAIN_WINDOW
import com.github.mrbean355.admiralbulldog.bulldogIcon
import javafx.event.EventHandler
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Modality.WINDOW_MODAL
import javafx.stage.Stage
import javafx.stage.Window
import javafx.stage.WindowEvent
import kotlinx.coroutines.yield
import java.io.File
import java.io.InputStream
import java.util.Optional
import java.util.ResourceBundle

val strings: ResourceBundle = ResourceBundle.getBundle("strings")

fun getString(key: String): String {
    return if (strings.containsKey(key)) strings.getString(key) else key
}

fun getString(key: String, vararg formatArgs: Any?): String {
    return getString(key).format(*formatArgs)
}

fun Stage.showModal(owner: Window, wait: Boolean = false) {
    initModality(WINDOW_MODAL)
    initOwner(owner)
    if (wait) {
        showAndWait()
    } else {
        show()
    }
}

fun Stage.finalise(title: String, root: Parent, closeOnEscape: Boolean = true, onCloseRequest: EventHandler<WindowEvent>? = null) {
    this.title = title
    scene = Scene(root)
    icons.add(bulldogIcon())
    if (closeOnEscape) {
        addEventFilter(KeyEvent.KEY_PRESSED) {
            if (it.code == KeyCode.ESCAPE) {
                close()
            }
        }
    }
    if (onCloseRequest != null) {
        setOnCloseRequest(onCloseRequest)
    }
}

@Suppress("FunctionName")
fun Alert(
        type: Alert.AlertType,
        header: String,
        content: String,
        buttons: Array<ButtonType> = emptyArray(),
        owner: Window? = null
): Alert {
    return Alert(type, header, *buttons).apply {
        title = TITLE_MAIN_WINDOW
        headerText = header
        contentText = content
        if (owner != null) {
            initOwner(owner)
        }
    }
}

fun <T> Optional<T>.toNullable(): T? {
    return orElse(null)
}

fun String.replaceFileSeparators(): String {
    return replace("/", File.separator)
}

fun String.removeVersionPrefix(): String {
    return replace(Regex("^v"), "")
}

fun Double.format(decimalPlaces: Int): String {
    return "%.${decimalPlaces}f".format(this)
}

/**
 * Stream this [InputStream] into the given [file], calling [onProgress] with the current number of bytes written.
 */
suspend fun InputStream.streamToFile(file: File, onProgress: suspend (Long) -> Unit) {
    file.outputStream().use { output ->
        var bytesCopied = 0L
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytes = read(buffer)
        while (bytes >= 0) {
            yield()
            output.write(buffer, 0, bytes)
            bytesCopied += bytes
            onProgress(bytesCopied)
            bytes = read(buffer)
        }
    }
}
