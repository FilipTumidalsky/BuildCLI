package dev.buildcli.core.utils.config;

import dev.buildcli.core.domain.configs.BuildCLIConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigContextLoaderTest {

    @TempDir
    Path tmp;

    private String origUserDir;
    private String origUserHome;

    @BeforeEach
    void setUp() throws Exception {
        origUserDir = System.getProperty("user.dir");
        origUserHome = System.getProperty("user.home");

        System.setProperty("user.dir", tmp.toString());
        System.setProperty("user.home", tmp.resolve("home").toString());
        Files.createDirectories(Path.of(System.getProperty("user.home")));

        resetStaticCache();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (origUserDir != null) System.setProperty("user.dir", origUserDir);
        if (origUserHome != null) System.setProperty("user.home", origUserHome);
        resetStaticCache();
    }

    private static void resetStaticCache() throws Exception {
        try {
            Field local = ConfigContextLoader.class.getDeclaredField("localConfig");
            Field global = ConfigContextLoader.class.getDeclaredField("globalConfig");
            Field merged = ConfigContextLoader.class.getDeclaredField("mergedConfig");
            local.setAccessible(true);
            global.setAccessible(true);
            merged.setAccessible(true);
            local.set(null, null);
            global.set(null, null);
            merged.set(null, null);
        } catch (NoSuchFieldException ignored) {
            // fields nemusia existovať — nevadí
        }
    }

    private static void writeProperties(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        try (var out = new OutputStreamWriter(new FileOutputStream(file.toFile()), StandardCharsets.UTF_8)) {
            out.write(content);
        }
    }

    private static Path localConfig(Path base) {
        return base.resolve("buildcli.properties");
    }

    private static Path globalConfig(Path home) {
        return home.resolve(".buildcli").resolve("config").resolve("buildcli.properties");
    }

    @Test
    @DisplayName("Empty environment yields empty merged config")
    void emptyEnv() {
        BuildCLIConfig all = ConfigContextLoader.getAllConfigs();
        assertNotNull(all);
        assertNull(all.getProperty("nonexistent").orElse(null));
    }

    @Test
    @DisplayName("Reads local buildcli.properties when present")
    void readsLocal() throws Exception {
        writeProperties(localConfig(tmp), "foo=local\nbar=1\n");
        BuildCLIConfig local = ConfigContextLoader.getLocalConfig();
        assertTrue(local.isLocal());
        assertEquals("local", local.getProperty("foo").orElse(null));

        BuildCLIConfig all = ConfigContextLoader.getAllConfigs();
        assertEquals("local", all.getProperty("foo").orElse(null));
        assertEquals("1", all.getProperty("bar").orElse(null));
    }

    @Test
    @DisplayName("Reads global config from user.home when present")
    void readsGlobal() throws Exception {
        Path home = Path.of(System.getProperty("user.home"));
        writeProperties(globalConfig(home), "g1=global\nsame=G\n");

        BuildCLIConfig global = ConfigContextLoader.getGlobalConfig();
        assertFalse(global.isLocal());
        assertEquals("global", global.getProperty("g1").orElse(null));

        BuildCLIConfig all = ConfigContextLoader.getAllConfigs();
        assertEquals("global", all.getProperty("g1").orElse(null));
        assertEquals("G", all.getProperty("same").orElse(null));
    }
}
