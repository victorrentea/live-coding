package com.github.victorrentea.livecoding.relay

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.ui.TerminalWidget
import com.jediterm.terminal.TtyConnector
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.awt.KeyboardFocusManager
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Lets Walkie Talkie deliver a dictation into **one specific** terminal tool
 * window tab.
 *
 * The relay can be pointed at a terminal and will type every later dictation
 * into it. For Terminal.app it addresses the tty and for tmux the pane, and
 * neither touches the focus. A terminal inside this IDE has neither address, so
 * the relay fell back to the only handle available from outside — the
 * application's pid — and delivered by putting the line on the clipboard,
 * activating the IDE and pressing Cmd+V.
 *
 * **Cmd+V goes wherever the caret is.** Measured here, four deliveries, one
 * variable:
 *
 *  - caret in the bound terminal      -> landed correctly
 *  - focus in another application     -> landed correctly (the app is activated)
 *  - caret in the **editor**          -> pasted into the source file, then Enter
 *  - caret in a second terminal tab   -> landed in the wrong terminal
 *
 * and the relay reported success for all four, because "Cmd+V was sent" is the
 * only fact observable from outside a window. On 2026-08-15 that put a dictation
 * into OwnerRestController.java; the backend hot-compiled it and the endpoint
 * answered 500 until somebody read the file.
 *
 * From outside a window you cannot address a pane; from in here `sendCommandToExecute`
 * is right there. So the relay stops guessing and asks us.
 *
 * The listener is loopback-only and gated on a per-run secret. The relay's Chrome
 * extension has no such gate, and that is defensible there: it hands over a CSS
 * selector. This one **types a line into a shell and presses Enter**.
 */
@Service(Service.Level.APP)
class RelayTerminalService : Disposable {

    private val started = java.util.concurrent.atomic.AtomicBoolean(false)

    private val log = thisLogger()
    private var server: HttpServer? = null
    private var registryFile: Path? = null

    /** Terminals handed to the relay, by the id it was given.
     *
     * Identity is minted here because a terminal widget is not serialisable and
     * its name is not unique — two tabs are both "Local". */
    private val bound = ConcurrentHashMap<Int, TerminalRef>()
    private val nextId = AtomicInteger(1)

    /** The widget itself, not its name.
     *
     * Name was the first shape and it is wrong for the very case this feature
     * exists to fix: two tabs are both "Local", so a lookup by name would hand
     * the line to whichever came first — the exact "landed in the wrong
     * terminal" failure being repaired. Nothing crosses a process boundary here,
     * so there is no reason to reduce a live object to a string. */
    private data class TerminalRef(val project: Project, val widget: TerminalWidget, val name: String)

    fun start() {
        // One listener per IDE process, however many projects are open.
        if (!started.compareAndSet(false, true)) return
        try {
            // Port 0: the OS picks a free one and we publish it. A fixed port
            // would have to be negotiated with every other IDE on the machine.
            val http = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
            val token = ByteArray(16).also { SecureRandom().nextBytes(it) }
                .joinToString("") { "%02x".format(it) }

            http.createContext("/") { exchange -> handle(exchange, token) }
            http.executor = null
            http.start()
            server = http

            publish(http.address.port, token)
            log.info("walkie-talkie bridge listening on ${http.address.port}")
        } catch (e: Exception) {
            log.warn("could not start the walkie-talkie bridge", e)
        }
    }

    /**
     * Announce the listener where the relay looks for it.
     *
     * The IDE runs the plugin in its own process, so the pid published here *is*
     * the pid the relay sees in front — unlike VS Code, where the extension host
     * is a child process and the relay has to disambiguate by which window is
     * focused. The relay uses the same `/ping` answer either way.
     */
    private fun publish(port: Int, token: String) {
        val dir = Path.of(System.getProperty("user.home"), ".walkie-talkie", "ide")
        Files.createDirectories(dir)
        val pid = ProcessHandle.current().pid()
        val file = dir.resolve("intellij-$pid.json")
        Files.writeString(file, """{"app":"intellij","port":$port,"token":"$token","pid":$pid}""")
        runCatching { Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------")) }
        registryFile = file
    }

    override fun dispose() {
        server?.stop(0)
        server = null
        registryFile?.let { runCatching { Files.deleteIfExists(it) } }
        registryFile = null
    }

    private fun handle(exchange: HttpExchange, token: String) {
        try {
            if (exchange.requestHeaders.getFirst("x-relay-token") != token) {
                return respond(exchange, 403, """{"ok":false,"error":"bad token"}""")
            }
            when (exchange.requestURI.path) {
                // **On the EDT, all of it.** The listener has a thread of its
                // own, and every question below — which window is active, which
                // tab is selected, write into a widget — is one the platform
                // answers only from the Event Dispatch Thread. Answering it
                // anywhere else throws `Access is allowed from Event Dispatch
                // Thread (EDT) only`, which is exactly how the first bind
                // failed: silently, with the relay falling back to the paste
                // path this whole class exists to retire.
                "/ping" -> onEdt { ping(exchange) }
                "/state" -> onEdt { state(exchange) }
                "/bind" -> onEdt { bind(exchange) }
                "/send" -> onEdt { send(exchange) }
                "/unbind" -> {
                    idParam(exchange)?.let { bound.remove(it) }
                    respond(exchange, 200, """{"ok":true}""")
                }
                else -> respond(exchange, 404, """{"ok":false}""")
            }
        } catch (e: Exception) {
            log.warn("bridge request failed", e)
            runCatching { respond(exchange, 500, """{"ok":false}""") }
        }
    }

    /** Run on the EDT and wait, so the HTTP response is written only once the
     *  answer exists. `invokeAndWait` and not `invokeLater`: the caller is
     *  holding an open socket that has to be answered. */
    private fun onEdt(block: () -> Unit) =
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait(block)

    /**
     * Is this the window Victor is looking at?
     *
     * With two IDEs open there are two listeners, and the only one that can be
     * the window he pressed the key in front of is the focused one. AWT's active
     * window is the honest answer: it is null for every process that is not the
     * one holding the keyboard.
     */
    private fun ping(exchange: HttpExchange) {
        val focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow != null
        respond(exchange, 200, """{"ok":true,"app":"intellij","focused":$focused}""")
    }

    /** Point me at the terminal that is selected right now. */
    private fun bind(exchange: HttpExchange) {
        val project = focusedProject()
            ?: return respond(exchange, 409, """{"ok":false,"error":"no open project"}""")
        // **The selected tab, not the first one.** `terminalWidgets` is a set in
        // no useful order, and Victor presses the key while looking at one
        // particular tab. The content manager is the only thing that knows which.
        val manager = TerminalToolWindowManager.getInstance(project)
        val selected = manager.toolWindow?.contentManager?.selectedContent
        val widget = selected?.let { TerminalToolWindowManager.findWidgetByContent(it) }
            ?: manager.terminalWidgets.firstOrNull()
            ?: return respond(exchange, 409, """{"ok":false,"error":"no terminal open"}""")

        val id = nextId.getAndIncrement()
        val name = widget.terminalTitle.buildTitle()
        bound[id] = TerminalRef(project, widget, name)

        // The **shell's** pid, and the reason IDE targets can finally be guarded.
        // From it the relay resolves a tty and asks the question it already asks
        // of a Terminal.app tab: is a shell sitting at a prompt? A dictation
        // typed at a prompt is not a prompt, it is a command.
        val connector = connectorOf(widget)
        val pid = runCatching { connector?.let { shellPid(it) } }.getOrNull()
        // **The directory, asked of the IDE rather than of the process.** A pid
        // is not always there to ask: this IDE runs its terminal in a backend
        // process, so the connector is a `BackendTtyConnector` with no `Process`
        // behind it and `shellPID` is null however hard it is reflected over.
        // The widget still knows where its shell is, and that is the whole of
        // what the relay wants the pid for — the folder on the chip beside the
        // cursor. (The shell guard genuinely needs the pid and stays off without
        // it; `commandRunning` is reported so it can be judged from data rather
        // than from a guess.)
        //
        // Both are read reflectively: they are default methods added to
        // `TerminalWidget` after the platform version this plugin compiles
        // against, and calling them directly would trade a null on old IDEs for
        // a plugin that does not build at all.
        val cwd = call(widget, "getCurrentDirectory") as? String
        log.info("bind #$id ${widget.javaClass.name} connector=${connector?.javaClass?.name} " +
                 "shellPID=$pid cwd=$cwd")
        respond(exchange, 200,
            """{"ok":true,"id":$id,"name":${quote(name)},"shellPID":${pid ?: "null"},""" +
            """"cwd":${cwd?.let { quote(it) } ?: "null"}}""")
    }

    /**
     * Where a bound terminal is **now**, and whether anything is running in it.
     *
     * The relay re-reads the folder every few seconds because Victor `cd`s
     * between repos inside one session, and a chip still naming the folder he
     * bound in an hour ago says the words are going somewhere they are not.
     * `bind` answers this once; this answers it again without rebinding.
     */
    private fun state(exchange: HttpExchange) {
        val id = idParam(exchange)
            ?: return respond(exchange, 400, """{"ok":false,"error":"expected ?id=N"}""")
        val ref = bound[id]
            ?: return respond(exchange, 404, """{"ok":false,"error":"that terminal is gone"}""")
        if (ref.widget !in TerminalToolWindowManager.getInstance(ref.project).terminalWidgets) {
            bound.remove(id)
            return respond(exchange, 404, """{"ok":false,"error":"that terminal is gone"}""")
        }
        // **`getCurrentDirectory` only.** `isCommandRunning` was read here too,
        // as a candidate shell guard for targets with no pid, and it asks the
        // platform a question that needs a read action: every call logged a
        // `softAssertNoReadAccess` stack trace into idea.log. A guard is not
        // worth a Throwable per bind, and the folder is what the relay came for.
        val cwd = call(ref.widget, "getCurrentDirectory") as? String
        respond(exchange, 200,
            """{"ok":true,"cwd":${cwd?.let { quote(it) } ?: "null"}}""")
    }

    private fun send(exchange: HttpExchange) {
        val body = exchange.requestBody.readAllBytes().decodeToString()
        val id = Regex(""""id"\s*:\s*(\d+)""").find(body)?.groupValues?.get(1)?.toIntOrNull()
        val line = Regex(""""line"\s*:\s*"((?:[^"\\]|\\.)*)"""").find(body)?.groupValues?.get(1)
            ?.replace("\\\"", "\"")?.replace("\\\\", "\\")
        if (id == null || line.isNullOrEmpty()) {
            return respond(exchange, 400, """{"ok":false,"error":"expected {id, line}"}""")
        }
        val ref = bound[id]
            ?: return respond(exchange, 404, """{"ok":false,"error":"that terminal is gone"}""")

        // Still open? A tab the relay points at can be closed while it points at
        // it, and an honest 404 beats a line written into a disposed widget.
        if (ref.widget !in TerminalToolWindowManager.getInstance(ref.project).terminalWidgets) {
            bound.remove(id)
            return respond(exchange, 404, """{"ok":false,"error":"that terminal is gone"}""")
        }
        val widget = ref.widget

        // Straight into that terminal's pty: no window is activated, no focus
        // moves, the caret stays where Victor left it, and his clipboard stays
        // his own. The relay flattens the message to one line before it ever
        // gets here, so this writes exactly one.
        //
        // **The Return is written by hand, as `\r`, instead of letting
        // `sendCommandToExecute` append one.** That appends `\n` — and in a TUI
        // in raw mode `\n` is not Enter, it is *insert a newline*, the very
        // convention Claude Code uses for a multi-line prompt. The dictation then
        // sits in the prompt until Victor presses Return himself, which is
        // exactly what he reported. The tty paths never had it: tmux's
        // `send-keys Enter` and Terminal.app's `do script` press a real Return.
        //
        // **And as a second write, a beat later**, for the other half: a TUI that
        // reads `text\r` in one chunk treats the whole thing as a paste and keeps
        // the Return as text. A separate write is a keypress — which is why the
        // tmux path has always been two calls.
        val tty = connectorOf(widget)
        if (tty == null) {
            // No connector to write to (a widget still starting up). The old
            // route is worse at submitting but better than dropping the line.
            widget.sendCommandToExecute(line)
        } else {
            tty.write(line)
            AppExecutorUtil.getAppScheduledExecutorService().schedule(
                { runCatching { tty.write("\r") } }, 120, TimeUnit.MILLISECONDS)
        }
        respond(exchange, 200, """{"ok":true,"name":${quote(ref.name)}}""")
    }

    /** The project whose window holds the keyboard, not simply the first open.
     *
     * Two projects are two frames, and with the wrong one chosen the relay binds
     * a terminal in a repo Victor is not looking at — the same class of mistake
     * as picking the wrong tab, one level up. */
    private fun focusedProject(): Project? {
        val open = ProjectManager.getInstance().openProjects.filter { !it.isDisposed }
        return open.firstOrNull {
            com.intellij.openapi.wm.WindowManager.getInstance().suggestParentWindow(it)?.isActive == true
        } ?: open.firstOrNull()
    }

    private fun idParam(exchange: HttpExchange): Int? =
        exchange.requestURI.query?.split('&')
            ?.firstOrNull { it.startsWith("id=") }?.removePrefix("id=")?.toIntOrNull()

    private fun respond(exchange: HttpExchange, code: Int, body: String) {
        val bytes = body.toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(code, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    /**
     * The shell behind a terminal widget, by reflection.
     *
     * The connector is a `PtyProcessTtyConnector` wrapping a `PtyProcess`, whose
     * `pid()` is what the relay needs — but that class has moved package between
     * platform versions and its process field is not public in any of them. A
     * hard import would pin the plugin to one IDE build for a value that is a
     * *nice-to-have*: without it the relay simply reports the target as
     * unguarded, which is what it did for every IDE target until now. Failing
     * soft is therefore the correct shape, and reflection is what failing soft
     * costs.
     */
    /** A no-arg call by name, for platform API newer than we compile against. */
    private fun call(target: Any, method: String): Any? = runCatching {
        target.javaClass.methods.firstOrNull { it.name == method && it.parameterCount == 0 }
            ?.also { it.isAccessible = true }
            ?.invoke(target)
    }.getOrNull()

    /**
     * The connector behind a terminal tab, **whichever route created the tab**.
     *
     * `TerminalWidget.getTtyConnector()` reads a `TtyConnectorAccessor` that is
     * only populated when the tab was connected through the new-widget API.
     * Measured on this Mac (IntelliJ 2026.2, `terminalEngine=CLASSIC`): a normal
     * terminal tab answers **null** there, and null cost two things at once —
     * `bind` reported `shellPID: null`, so the chip beside the cursor fell back
     * to the app's name and the shell guard was off, and `send` fell into the
     * `sendCommandToExecute` branch, which appends `\n`. In a TUI in raw mode
     * `\n` is not Enter, so every dictation sat unsent in the prompt — the
     * exact symptom the `\r` write was added to fix, in a build that already
     * had the fix.
     *
     * The classic widget underneath is still a JediTerm one and still holds a
     * real `ProcessTtyConnector`, which is both the thing to write `\r` to and
     * the only object here that knows the shell's `Process`.
     */
    private fun connectorOf(widget: TerminalWidget): TtyConnector? =
        widget.ttyConnector
            ?: runCatching { JBTerminalWidget.asJediTermWidget(widget)?.processTtyConnector }.getOrNull()

    private fun shellPid(connector: Any): Long? = runCatching {
        // A getter first — JediTerm's ProcessTtyConnector exposes one and a
        // public method is the part of a class least likely to move.
        generateSequence(connector.javaClass) { it.superclass }
            .flatMap { it.declaredMethods.asSequence() }
            .firstOrNull { it.name == "getProcess" && it.parameterCount == 0 }
            ?.also { it.isAccessible = true }
            ?.invoke(connector)
            ?.let { return (it as? Process)?.pid() }

        generateSequence(connector.javaClass) { it.superclass }
            .flatMap { it.declaredFields.asSequence() }
            .firstOrNull { it.name == "myProcess" || it.name == "process" }
            ?.also { it.isAccessible = true }
            ?.get(connector)
            ?.let { (it as? Process)?.pid() }
    }.onFailure { log.info("no shell pid from ${connector.javaClass.name}: ${it.message}") }.getOrNull()

    private fun quote(s: String) =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\""
}


/**
 * Start the bridge as soon as any project opens.
 *
 * A project activity rather than an application listener because the terminal
 * tool window is a per-project thing and there is nothing worth listening on
 * before one exists. The service guards itself against running twice.
 */
class RelayTerminalStarter : ProjectActivity {
    override suspend fun execute(project: Project) {
        service<RelayTerminalService>().start()
    }
}
