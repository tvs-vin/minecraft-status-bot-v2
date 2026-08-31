package tvs.mcsb;

import java.io.IOException;
import java.nio.file.Path;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Properties;

// Imports

import net.fabricmc.loader.api.FabricLoader;

public class ConfigHelper {

    // Config Values

    // Basic Values, always needed
    public static String mode = "Standalone"; // Standalone Manager Worker Hybrid
    public static String nodeName = "New Server"; // Gives name to the node
    
    // Manager related values - Only used in  Worker and Hybrid modes
    public static String managerAddress; // IPV4
    public static String authToken; // Token to authinticate with the manager. Can be disabled
    
    // Feature toggles - just planned for now
    public static boolean webUI = false; // Web UI enabler.
    public static boolean discordIntegration = false; // Discord integration enabler.
    public static boolean chatMonitoring = false; // Chat monitoring enabler. Used for features like having chat be copied into a channel or view on WebUI
    public static boolean hardwareMonitoring = false; // Hardware monitoring to see stats like ram, cpu, and storage usage.

    // WebUI values
    public static int webUIPort = 8080; // Port for the Web UI

    // Debug values
    public static boolean debugEnabled = false; // Enables debug features and extra logs. No debug toggle will work with this disabled. Also sets log level to 1
    public static int logLevel = 1; // Int that controls the amount of stuff being logged based on importance.
    
    

    private static final Path FILE =
        FabricLoader.getInstance()
                .getConfigDir()
                .resolve("mcsb.properties");

    public static void init() {
        // Initializes any config related items
        Properties properties = new Properties();

        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                properties.load(reader);
            } catch (IOException | RuntimeException exception) {
                Mcsb.LOGGER.warn("Could not read config file {}", FILE, exception);
            }
        }

        debugEnabled = Boolean.parseBoolean(
                properties.getProperty("debugEnabled", "false")
        );
        mode = properties.getProperty(
                "mode",
                "Standalone"
        );
        managerAddress = properties.getProperty(
                "managerAddress",
                "localhost"
        );
        authToken = properties.getProperty(
                "authToken",
                ""
        );
        nodeName = properties.getProperty(
                "nodeName",
                "defaultNode"
        );
        webUI = Boolean.parseBoolean(
                properties.getProperty("webUI", "false")
        );
        discordIntegration = Boolean.parseBoolean(
                properties.getProperty("discordIntegration", "false")
        );
        chatMonitoring = Boolean.parseBoolean(
                properties.getProperty("chatMonitoring", "false")
        );
        hardwareMonitoring = Boolean.parseBoolean(
                properties.getProperty("hardwareMonitoring", "false")
        );
        logLevel = Integer.parseInt(
                properties.getProperty("logLevel", "1")
        );
        webUIPort = Integer.parseInt(
                properties.getProperty("webUIPort", "8080")
        );

        saveDefaults(properties);

    }

    private static void saveDefaults(Properties properties) {
        try {
            Files.createDirectories(FILE.getParent());

            try (Writer writer = Files.newBufferedWriter(FILE)) {
                properties.setProperty("debugEnabled", Boolean.toString(debugEnabled));
                properties.setProperty("mode", mode);
                properties.setProperty("managerAddress", managerAddress);
                properties.setProperty("authToken", authToken);
                properties.setProperty("nodeName", nodeName);
                properties.setProperty("webUI", Boolean.toString(webUI));
                properties.setProperty("discordIntegration", Boolean.toString(discordIntegration));
                properties.setProperty("chatMonitoring", Boolean.toString(chatMonitoring));
                properties.setProperty("hardwareMonitoring", Boolean.toString(hardwareMonitoring));
                properties.setProperty("logLevel", Integer.toString(logLevel));
                properties.setProperty("webUIPort", Integer.toString(webUIPort));
                properties.store(writer, "Minecraft Status Bot configuration - See WIKI for usecase");
            }
        } catch (IOException exception) {
            Mcsb.LOGGER.error("MCSB | Could not write config file | {}", FILE, exception);
        }
    }

    

}
