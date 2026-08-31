package tvs.mcsb;

import io.javalin.Javalin;

public final class WebServer {
    private static Javalin server;

    private WebServer() {

    }

    public static void start() {
        if (server != null) {
            return;
        }

        if(Utility.logCheck(3)){
            Mcsb.LOGGER.debug("Setting contextClassLoader to modified for webserver");
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(WebServer.class.getClassLoader());

        server = Javalin.create(config -> {config.staticFiles.add("/assets/mcsb/web");})
                .get("/", ctx -> ctx.result("Minecraft Status Bot is running"))
                .get("/health", ctx -> ctx.result("ok"))
                .get("/api/status", ctx -> ctx.json(new StatusResponse(
                        ConfigHelper.nodeName,
                        ConfigHelper.mode
                )))
                
                .start(ConfigHelper.webUIPort);
        
        if(Utility.logCheck(3)){
            Mcsb.LOGGER.debug("Setting contextClassLoader to original");
        }
        Thread.currentThread().setContextClassLoader(originalClassLoader);

        if(Utility.logCheck(1)){
            Mcsb.LOGGER.info("MCSB | Web server started on port {}", ConfigHelper.webUIPort);
        }
    }

    public static void stop() {
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