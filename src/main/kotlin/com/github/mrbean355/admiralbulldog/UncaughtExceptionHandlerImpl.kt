package com.github.mrbean355.admiralbulldog

import com.github.mrbean355.admiralbulldog.service.logAnalyticsEvent
import javafx.application.HostServices
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * [java.lang.Thread.UncaughtExceptionHandler] which creates a log file containing the stack trace with some additional
 * info. Also shows an [Alert] to the user, asking them to report the issue on Discord.
 */
class UncaughtExceptionHandlerImpl(private val hostServices: HostServices)
    : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread?, e: Throwable?) {
        val file = File("crash_log.txt")
        val stringWriter = StringWriter()
        e?.printStackTrace(PrintWriter(stringWriter))
        val stackTrace = stringWriter.toString()
        runCatching { logAnalyticsEvent("unhandled_exception", stackTrace) }
        file.writeText("""
            |os.name      = ${System.getProperty("os.name")}
            |os.version   = ${System.getProperty("os.version")}
            |os.arch      = ${System.getProperty("os.arch")}
            |java.version = ${System.getProperty("java.version")}
            |thread info  = $t
            
            |$stackTrace
        """.trimMargin())

        Platform.runLater {
            val discordButton = ButtonType("Discord", ButtonBar.ButtonData.OK_DONE)
            val result = Alert(Alert.AlertType.ERROR, null, discordButton, ButtonType.CLOSE).run {
                contentText = """
                    Whoops! Something bad has happened, sorry!
                    Please consider reporting this issue so it can be fixed.
                    
                    An error log file was created here:
                    ${file.absolutePath}
                    
                    Please send it to the community on Discord.
                """.trimIndent()
                showAndWait()
            }
            result.ifPresent {
                if (it == discordButton) {
                    hostServices.showDocument("https://discord.gg/pEV4mW5")
                }
            }
        }
    }
}