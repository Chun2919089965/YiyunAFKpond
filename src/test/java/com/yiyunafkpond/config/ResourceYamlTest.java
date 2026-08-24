package com.yiyunafkpond.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ResourceYamlTest {
    @Test
    void bundledYamlResourcesAreValid() {
        for (String resource : List.of("config.yml", "messages.yml", "plugin.yml", "ponds.yml")) {
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(input, resource + " must be available on the test classpath");
                assertDoesNotThrow(() -> new Yaml().load(input), resource);
            } catch (Exception e) {
                throw new AssertionError("Unable to read " + resource, e);
            }
        }
    }
}
