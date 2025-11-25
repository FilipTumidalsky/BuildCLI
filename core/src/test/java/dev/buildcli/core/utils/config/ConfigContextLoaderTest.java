package dev.buildcli.core.utils.config;

import dev.buildcli.core.domain.configs.BuildCLIConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigContextLoaderTest {

    @TempDir
    Path tmp;

    @Test
    @DisplayName("Empty environment yields empty merged config")
    void emptyEnv() {
        BuildCLIConfig all = ConfigContextLoader.getAllConfigs();
        assertNotNull(all);
        assertNull(all.getProperty("nonexistent").orElse(null));
    }
}