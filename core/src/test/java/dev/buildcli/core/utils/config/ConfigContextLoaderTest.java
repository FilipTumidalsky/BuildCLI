package dev.buildcli.core.utils.config;

import dev.buildcli.core.domain.configs.BuildCLIConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
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
    }

    @AfterEach
    void tearDown() {
        if (origUserDir != null) System.setProperty("user.dir", origUserDir);
        if (origUserHome != null) System.setProperty("user.home", origUserHome);
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
}
