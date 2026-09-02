package tv.mango.app.pairing

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.UUID

/**
 * A minimal, short-lived local web server, so a manifest URL can be typed on
 * a phone instead of on a television remote.
 *
 * Deliberately as small as a working HTTP server can be. It understands one
 * request shape - a GET, on one path scoped by a random token, optionally
 * carrying a `url` query parameter - and nothing else: no file serving, no
 * other route, no code of the phone's returned to run on this device. What
 * is submitted is treated exactly like anything typed by hand on the field
 * beside it - it still goes through [tv.mango.app.addon.AddonInstaller]'s own
 * validation before anything is installed, and nothing here writes to
 * storage itself.
 *
 * The token matters because this server sits on the home network for as long
 * as the screen showing it is open: without one, any other device on that
 * network could find the port and submit an add-on url of its own before the
 * intended phone does.
 *
 * Runs only while a pairing screen is open, on an OS-assigned port, and is
 * stopped the moment that screen closes - nothing is listening the rest of
 * the time.
 */
class AddonPairingServer(
    private val onUrlReceived: (String) -> Unit,
) {

    private val token: String = UUID.randomUUID().toString().replace("-", "").take(TOKEN_LENGTH)
    private var serverSocket: ServerSocket? = null

    /** The path a phone must request; unguessable from the port alone. */
    val path: String get() = "/$token"

    /** Starts listening. Returns the bound port, or null if the socket could not be opened. */
    fun start(): Int? = runCatching {
        val socket = ServerSocket(0)
        serverSocket = socket
        Thread(::acceptLoop, "addon-pairing-server").apply {
            isDaemon = true
            start()
        }
        socket.localPort
    }.getOrNull()

    fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    private fun acceptLoop() {
        while (true) {
            val server = serverSocket ?: return
            val client = runCatching { server.accept() }.getOrNull() ?: return
            runCatching { handle(client) }
            runCatching { client.close() }
        }
    }

    private fun handle(client: Socket) {
        client.soTimeout = SOCKET_TIMEOUT_MILLIS
        val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))

        val requestLine = reader.readLine() ?: return
        // The rest of the request is headers this server has no use for -
        // read and discarded so the socket can be closed cleanly rather than
        // reset out from under a browser still sending them.
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
        }

        val target = requestLine.split(" ").getOrNull(1) ?: return
        if (!target.startsWith(path)) {
            respond(client, 404, "text/plain", "Not found.")
            return
        }

        val url = target.substringAfter('?', missingDelimiterValue = "")
            .split('&')
            .map { it.split('=', limit = 2) }
            .firstOrNull { it.size == 2 && it[0] == "url" }
            ?.get(1)
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }
            ?.takeIf { it.isNotBlank() }

        if (url == null) {
            respond(client, 200, "text/html; charset=utf-8", FORM_HTML)
        } else {
            respond(client, 200, "text/html; charset=utf-8", RECEIVED_HTML)
            onUrlReceived(url)
        }
    }

    private fun respond(client: Socket, status: Int, contentType: String, body: String) {
        val statusText = if (status == 404) "Not Found" else "OK"
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 $status $statusText\r\n" +
            "Content-Type: $contentType\r\n" +
            "Content-Length: ${bodyBytes.size}\r\n" +
            "Connection: close\r\n" +
            "\r\n"

        val out = client.getOutputStream()
        out.write(headers.toByteArray(Charsets.UTF_8))
        out.write(bodyBytes)
        out.flush()
    }

    private companion object {
        const val TOKEN_LENGTH = 12
        const val SOCKET_TIMEOUT_MILLIS = 5_000

        val FORM_HTML = """
            <!doctype html><html><head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Add to Mango TV</title>
            <style>
              body { font-family: sans-serif; max-width: 480px; margin: 48px auto; padding: 0 20px; color: #222; }
              input { width: 100%; box-sizing: border-box; font-size: 16px; padding: 12px;
                      margin: 16px 0; border: 1px solid #ccc; border-radius: 6px; }
              button { width: 100%; font-size: 16px; padding: 12px; border: 0; border-radius: 6px;
                       background: #d9a05b; color: #120d06; font-weight: 600; }
            </style>
            </head><body>
            <h1>Add to Mango TV</h1>
            <p>Paste the add-on's manifest URL below, then tap Add. It will appear on your TV.</p>
            <form>
              <input name="url" type="url" placeholder="https://example.com/manifest.json" autofocus required>
              <button type="submit">Add</button>
            </form>
            </body></html>
        """.trimIndent()

        val RECEIVED_HTML = """
            <!doctype html><html><head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Sent to Mango TV</title>
            <style>body { font-family: sans-serif; max-width: 480px; margin: 48px auto;
                          padding: 0 20px; color: #222; text-align: center; }</style>
            </head><body>
            <h1>Sent</h1>
            <p>Check your TV to finish adding it.</p>
            </body></html>
        """.trimIndent()
    }
}
