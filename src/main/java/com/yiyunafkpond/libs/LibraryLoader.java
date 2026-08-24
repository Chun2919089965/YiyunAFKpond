package com.yiyunafkpond.libs;

import com.yiyunafkpond.YiyunAFKpond;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Downloads optional database libraries on demand and exposes them through a
 * dedicated class loader. Downloaded files are cached in the plugin data folder.
 */
public final class LibraryLoader {
    private static final List<String> DEFAULT_REPOSITORIES = List.of(
            "https://maven.aliyun.com/repository/central/",
            "https://repo.huaweicloud.com/repository/maven/",
            "https://repo.maven.apache.org/maven2/"
    );

    private static final Library MYSQL = new Library(
            "MySQL Connector/J",
            "com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar",
            "mysql-connector-j-8.0.33.jar",
            "e2a3b2fc726a1ac64e998585db86b30fa8bf3f706195b78bb77c5f99bf877bd9"
    );
    private static final Library PROTOBUF = new Library(
            "Protobuf",
            "com/google/protobuf/protobuf-java/3.21.9/protobuf-java-3.21.9.jar",
            "protobuf-java-3.21.9.jar",
            "1b78b4a76a71512debfdff8f8fc5aef6bfd459f65758fecf7aff245e6e6301e4"
    );
    private static final Library SQLITE = new Library(
            "SQLite JDBC",
            "org/xerial/sqlite-jdbc/3.43.2.1/sqlite-jdbc-3.43.2.1.jar",
            "sqlite-jdbc-3.43.2.1.jar",
            "fa206c2b1a651d4cdf02374611c8ce0bd1183e4b0296c2e413594b4b7b72c1ac"
    );
    private static final Library HIKARI = new Library(
            "HikariCP",
            "com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar",
            "HikariCP-5.1.0.jar",
            "a47a6ee62379694ee52c30036f0931b72f9aee2a801d590341ed82bd839e2134"
    );
    private static final Library SLF4J = new Library(
            "SLF4J API",
            "org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar",
            "slf4j-api-1.7.36.jar",
            "d3ef575e3e4979678dc01bf1dcce51021493b4d11fb7f1be8ad982877c16a1c0"
    );

    private static volatile URLClassLoader libraryClassLoader;
    private static volatile boolean hikariLoaded;

    private final YiyunAFKpond plugin;
    private final Path libsFolder;

    public LibraryLoader(YiyunAFKpond plugin) {
        this.plugin = plugin;
        this.libsFolder = plugin.getDataFolder().toPath().resolve("libs");
    }

    public boolean loadStorageLibraries(String storageType, boolean useConnectionPool) {
        String normalizedType = storageType == null ? "yaml" : storageType.toLowerCase(Locale.ROOT);
        List<Library> required = new ArrayList<>();
        if ("sqlite".equals(normalizedType)) {
            required.add(SQLITE);
            required.add(SLF4J);
        } else if ("mysql".equals(normalizedType)) {
            required.add(MYSQL);
            required.add(PROTOBUF);
        } else {
            return true;
        }

        try {
            Files.createDirectories(libsFolder);
            List<URL> urls = new ArrayList<>();
            for (Library library : required) {
                Path file = ensureLibrary(library);
                urls.add(file.toUri().toURL());
            }
            boolean poolLibrariesLoaded = false;
            if ("mysql".equals(normalizedType) && useConnectionPool) {
                try {
                    urls.add(ensureLibrary(HIKARI).toUri().toURL());
                    urls.add(ensureLibrary(SLF4J).toUri().toURL());
                    poolLibrariesLoaded = true;
                } catch (IOException e) {
                    plugin.getLogger().warning(
                            "Connection pool libraries are unavailable; MySQL will use a direct connection: "
                                    + e.getMessage()
                    );
                }
            }

            closeClassLoader();
            libraryClassLoader = new URLClassLoader(
                    urls.toArray(URL[]::new),
                    LibraryLoader.class.getClassLoader()
            );
            verifyRequiredClasses(normalizedType, poolLibrariesLoaded);
            hikariLoaded = poolLibrariesLoaded;
            plugin.getLogger().info("Database libraries loaded from " + libsFolder.toAbsolutePath());
            return true;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            hikariLoaded = false;
            closeClassLoader();
            plugin.getLogger().severe("Unable to download or load database libraries: " + e.getMessage());
            return false;
        }
    }

    public boolean loadHikariCP() {
        return loadStorageLibraries("mysql", true);
    }

    private Path ensureLibrary(Library library) throws IOException, InterruptedException {
        Path target = libsFolder.resolve(library.fileName());
        if (Files.isRegularFile(target) && checksumMatches(target, library.sha256())) {
            plugin.getLogger().info("Using cached library: " + library.fileName());
            return target;
        }

        FileConfiguration config = plugin.getConfigManager().getConfig();
        int connectTimeout = Math.max(1000, config.getInt("libraries.download.connect-timeout-ms", 10000));
        int readTimeout = Math.max(1000, config.getInt("libraries.download.read-timeout-ms", 60000));
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        Path temporary = libsFolder.resolve(library.fileName() + ".part");
        IOException lastFailure = null;
        for (String repository : repositories(config)) {
            String downloadUrl = normalizeRepository(repository) + library.mavenPath();
            plugin.getLogger().info("Downloading " + library.name() + " from " + downloadUrl);
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUrl))
                        .timeout(Duration.ofMillis(readTimeout))
                        .header("User-Agent", "YiyunAFKpond/" + plugin.getDescription().getVersion())
                        .GET()
                        .build();
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream body = response.body()) {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new IOException("HTTP " + response.statusCode());
                    }
                    Files.copy(body, temporary, StandardCopyOption.REPLACE_EXISTING);
                }
                if (!checksumMatches(temporary, library.sha256())) {
                    throw new IOException("SHA-256 checksum mismatch");
                }
                moveIntoPlace(temporary, target);
                plugin.getLogger().info("Downloaded library: " + library.fileName());
                return target;
            } catch (IllegalArgumentException | IOException e) {
                lastFailure = new IOException(downloadUrl + " (" + e.getMessage() + ")", e);
                Files.deleteIfExists(temporary);
                plugin.getLogger().warning("Library mirror failed: " + lastFailure.getMessage());
            }
        }

        throw new IOException("All download mirrors failed for " + library.name(), lastFailure);
    }

    private List<String> repositories(FileConfiguration config) {
        List<String> configured = config.getStringList("libraries.download.repositories");
        if (configured.isEmpty()) {
            return DEFAULT_REPOSITORIES;
        }
        List<String> repositories = configured.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
        return repositories.isEmpty() ? DEFAULT_REPOSITORIES : repositories;
    }

    private String normalizeRepository(String repository) {
        String trimmed = repository.trim();
        return trimmed.endsWith("/") ? trimmed : trimmed + "/";
    }

    private void verifyRequiredClasses(String storageType, boolean useConnectionPool) throws ClassNotFoundException {
        if ("sqlite".equals(storageType)) {
            Class.forName("org.sqlite.JDBC", true, libraryClassLoader);
            return;
        }
        Class.forName("com.mysql.cj.jdbc.Driver", true, libraryClassLoader);
        if (useConnectionPool) {
            Class.forName("com.zaxxer.hikari.HikariConfig", true, libraryClassLoader);
            Class.forName("com.zaxxer.hikari.HikariDataSource", true, libraryClassLoader);
        }
    }

    public static Connection connect(String driverClassName, String url, Properties properties) throws Exception {
        ClassLoader loader = getLibraryClassLoader();
        Class<?> driverClass = Class.forName(driverClassName, true, loader);
        Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
        Connection connection = driver.connect(url, properties);
        if (connection == null) {
            throw new SQLException(driverClassName + " did not accept URL: " + url);
        }
        return connection;
    }

    public static boolean isHikariCPAvailable() {
        if (!hikariLoaded) {
            return false;
        }
        try {
            Class.forName("com.zaxxer.hikari.HikariConfig", false, getLibraryClassLoader());
            Class.forName("com.zaxxer.hikari.HikariDataSource", false, getLibraryClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    public static boolean isHikariLoaded() {
        return hikariLoaded;
    }

    public static ClassLoader getHikariClassLoader() {
        return getLibraryClassLoader();
    }

    public static ClassLoader getLibraryClassLoader() {
        URLClassLoader loader = libraryClassLoader;
        return loader == null ? LibraryLoader.class.getClassLoader() : loader;
    }

    public static synchronized void closeClassLoader() {
        URLClassLoader loader = libraryClassLoader;
        libraryClassLoader = null;
        hikariLoaded = false;
        if (loader != null) {
            try {
                loader.close();
            } catch (IOException ignored) {
            }
        }
    }

    private boolean checksumMatches(Path file, String expected) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest()).equalsIgnoreCase(expected);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private void moveIntoPlace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private record Library(String name, String mavenPath, String fileName, String sha256) {
    }
}
