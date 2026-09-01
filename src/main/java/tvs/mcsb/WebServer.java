package tvs.mcsb;

import io.javalin.Javalin;
import io.javalin.util.JavalinLogger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WebServer {
    private static volatile Javalin server;
    private static boolean starting;
    private static final ExecutorService SERVER_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "MCSB-WebServer");
        thread.setDaemon(true);
        return thread;
    });

    private WebServer() {

    }

    public static synchronized void start() {
        if (server != null || starting) {
            return;
        }
        starting = true;
        SERVER_EXECUTOR.execute(WebServer::startServer);
    }

    private static void startServer() {
        if(Utility.logCheck(3)){
            Mcsb.LOGGER.debug("Setting contextClassLoader to modified for webserver");
        }

        JavalinLogger.enabled = false;
        JavalinLogger.startupInfo = false;

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(WebServer.class.getClassLoader());

        try {
            server = Javalin.create(config -> {config.staticFiles.add("/assets/mcsb/web");})
                    .get("/", ctx -> ctx.result("Minecraft Status Bot is running"))
                    .get("/health", ctx -> ctx.result("ok"))
                    .get("/api/status", ctx -> ctx.json(new StatusResponse(
                            ConfigHelper.nodeName,
                            ConfigHelper.mode
                    )))
                    .start(ConfigHelper.webUIPort);

            if(Utility.logCheck(1)){
                Mcsb.LOGGER.info("MCSB | Web server started on port {}", ConfigHelper.webUIPort);
            }
        } finally {
            if(Utility.logCheck(3)){
                Mcsb.LOGGER.debug("Setting contextClassLoader to original");
            }
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            synchronized (WebServer.class) {
                starting = false;
            }
        }
    }

    public static void stop() {
        SERVER_EXECUTOR.execute(WebServer::stopServer);
    }

    private static void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
            if(Utility.logCheck(1)){
                Mcsb.LOGGER.info("MCSB | Web server stopped");
            }
        }
    }

    private record StatusResponse(String nodeName, String mode) {
    }
}
