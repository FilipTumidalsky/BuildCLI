package dev.buildcli.core.utils.config;

import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.exceptions.ConfigException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import static dev.buildcli.core.constants.ConfigDefaultConstants.BUILD_CLI_CONFIG_FILE_NAME;
import static dev.buildcli.core.constants.ConfigDefaultConstants.BUILD_CLI_CONFIG_GLOBAL_FILE;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigsOperationsUtils.
 */
class ConfigsOperationsUtilsTest {

    @TempDir
    Path tempDir;

    private String originalUserDir;

    @BeforeEach
    void setUp() {
        // Redirect working directory to tempDir
        // so the local config file is created only in the temporary folder.
        originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.dir", originalUserDir);
    }

    @Test
    void getLocal_shouldReturnEmpty_whenLocalConfigFileDoesNotExist() {
        // GIVEN - the file BUILD_CLI_CONFIG_FILE_NAME does not exist in tempDir

        // WHEN
        Optional<BuildCLIConfig> result = ConfigsOperationsUtils.getLocal();

        // THEN
        assertTrue(result.isEmpty(), "Expected empty Optional when local config file does not exist");
    }

    @Test
    void getLocal_shouldLoadConfigAndSetLocalFlag_whenFileExists() throws IOException {
        // GIVEN
        File localFile = new File(BUILD_CLI_CONFIG_FILE_NAME);
        assertTrue(localFile.createNewFile(), "Precondition: local config file should be created");

        BuildCLIConfig mockConfig = Mockito.mock(BuildCLIConfig.class);

        // Mock the static method BuildCLIConfig.from(File)
        try (MockedStatic<BuildCLIConfig> mockedStatic =
                     Mockito.mockStatic(BuildCLIConfig.class)) {

            mockedStatic
                    .when(() -> BuildCLIConfig.from(Mockito.any(File.class)))
                    .thenReturn(mockConfig);

            // WHEN
            Optional<BuildCLIConfig> result = ConfigsOperationsUtils.getLocal();

            // THEN
            assertTrue(result.isPresent(), "Config should be present when file exists");
            assertSame(mockConfig, result.get(), "Should return the same instance created by BuildCLIConfig.from()");
            Mockito.verify(mockConfig).setLocal(true);
        }
    }

    @Test
    void getGlobal_shouldLoadConfigAndSetLocalFalse_whenGlobalFileExists() throws IOException {
        // GIVEN
        File globalFile = BUILD_CLI_CONFIG_GLOBAL_FILE.toFile();
        File parent = globalFile.getParentFile();
        if (parent != null && !parent.exists()) {
            assertTrue(parent.mkdirs(), "Precondition: parent dir for global config should be created");
        }
        if (!globalFile.exists()) {
            assertTrue(globalFile.createNewFile(), "Precondition: global config file should be created");
        }

        BuildCLIConfig mockConfig = Mockito.mock(BuildCLIConfig.class);

        try (MockedStatic<BuildCLIConfig> mockedStatic =
                     Mockito.mockStatic(BuildCLIConfig.class)) {

            mockedStatic
                    .when(() -> BuildCLIConfig.from(Mockito.any(File.class)))
                    .thenReturn(mockConfig);

            // WHEN
            Optional<BuildCLIConfig> result = ConfigsOperationsUtils.getGlobal();

            // THEN
            assertTrue(result.isPresent(), "Global config should be present when file exists");
            assertSame(mockConfig, result.get());
            Mockito.verify(mockConfig).setLocal(false);
        }
    }

    @Test
    void set_shouldCreateLocalConfigFile_evenWhenNoProperties() throws IOException {
        // GIVEN
        BuildCLIConfig mockConfig = Mockito.mock(BuildCLIConfig.class);
        Mockito.when(mockConfig.isLocal()).thenReturn(true);
        Mockito.when(mockConfig.getProperties()).thenReturn(Collections.emptySet());

        File localFile = new File(BUILD_CLI_CONFIG_FILE_NAME);
        assertFalse(localFile.exists(), "Precondition: local config file should not exist yet");

        // WHEN
        ConfigsOperationsUtils.set(mockConfig);

        // THEN
        assertTrue(localFile.exists(), "Local config file should be created by set()");
        String content = Files.readString(localFile.toPath());

        // Since properties is an empty set, the content should be an empty string or just newlines
        assertTrue(content.isEmpty() || content.matches("\\R+"),
                "Content should be empty (or only newlines) when there are no properties");
    }

    @Test
    void set_shouldWrapIOExceptionIntoConfigException_whenWriteFails() {
        // GIVEN
        BuildCLIConfig mockConfig = Mockito.mock(BuildCLIConfig.class);
        Mockito.when(mockConfig.isLocal()).thenReturn(true);
        Mockito.when(mockConfig.getProperties()).thenReturn(Collections.emptySet());

        // Create a DIRECTORY with the same name as the config file
        // -> Files.writeString() will throw IOException for this path.
        File asDirectory = new File(BUILD_CLI_CONFIG_FILE_NAME);
        assertTrue(asDirectory.mkdir(), "Precondition: directory with config file name should exist");

        // WHEN / THEN
        ConfigException ex = assertThrows(
                ConfigException.class,
                () -> ConfigsOperationsUtils.set(mockConfig),
                "Expected ConfigException when write fails"
        );

        assertEquals("Error writing config file", ex.getMessage());
    }
}
