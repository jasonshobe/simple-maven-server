/*
 * This file is part of Simple Maven Server.
 * Copyright (C) 2021  Jason Shobe
 *
 * Simple Maven Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Simple Maven Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Simple Maven Server.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.jshobe.maven;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.jshobe.maven.config.MavenConfiguration;
import com.jshobe.maven.handler.MavenHandler;
import com.jshobe.maven.security.MavenIdentityManager;
import com.jshobe.maven.storage.CachedStorage;
import com.jshobe.maven.storage.LocalStorage;
import com.jshobe.maven.storage.ProxyStorage;
import com.jshobe.maven.storage.S3Storage;
import com.jshobe.maven.storage.Storage;
import com.jshobe.maven.storage.cache.Cache;
import io.undertow.Undertow;
import io.undertow.security.idm.IdentityManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;

/**
 * {@code MavenServer} servers a Maven repository.
 */
@Slf4j
public class MavenServer {

  private final Configuration config;
  private final Cache cache;
  private final Undertow server;

  /**
   * Create a new instance of {@code MavenServer}.
   *
   * @param rebuildIndex {@code true} if the index should be rebuilt or {@code false} if any cached
   *                     index data should be reused.
   * @throws Exception if an error prevented the server from being initialized.
   */
  public MavenServer(boolean rebuildIndex) throws Exception {
    this.config = new MavenConfiguration();
    this.cache = createCache(rebuildIndex);
    this.server = Undertow.builder()
        .addHttpListener(getPort(), "0.0.0.0")
        .setHandler(new MavenHandler(createStorage(), createIdentityManager()))
        .build();
  }

  /**
   * Starts the HTTP server.
   */
  public void start() {
    server.start();
  }

  /**
   * Stops the HTTP server.
   */
  public void stop() {
    server.stop();
    cache.close();
  }

  /**
   * Creates the cache.
   *
   * @param rebuildIndex {@code true} if the index should be rebuilt or {@code false} if not.
   * @return the cache.
   * @throws IOException if an I/O error occurs that prevents the cache from being created.
   */
  private Cache createCache(boolean rebuildIndex) throws IOException {
    Path path =
        Paths.get(config.getString("maven.storage.cache")).resolve("db").toAbsolutePath();
    return new Cache(path, rebuildIndex);
  }

  /**
   * Creates the storage provider.
   *
   * @return a new storage provider.
   * @throws IOException if an I/O error occurs that prevents the storage from being created.
   */
  private Storage createStorage() throws IOException {
    Storage storage = null;
    Set<String> repositories = new HashSet<>();

    if (config.containsKey("maven.repositories")) {
      repositories.addAll(config.getList(String.class, "maven.repositories"));
    }

    if (config.getBoolean("maven.storage.s3.enabled") &&
        config.containsKey("maven.storage.s3.bucket")) {
      storage = new S3Storage(config.getString("maven.storage.bucket"), repositories);
    }

    if (storage == null) {
      Path path = Paths.get(config.getString("maven.storage.directory"));
      Files.createDirectories(path);
      storage = new LocalStorage(path, repositories);
    }

    storage = new CachedStorage(storage, cache);

    if (config.containsKey("maven.proxies.name")) {
      Map<String, Set<String>> proxies = new HashMap<>();
      List<String> names = config.getList(String.class, "maven.proxies.name");

      for (int i = 0; i < names.size(); i++) {
        List<String> proxied =
            config.getList(String.class, "maven.proxies(" + i + ").repositories");

        if (!proxied.isEmpty()) {
          proxies.put(names.get(i), new HashSet<>(proxied));
        }
      }

      storage = new ProxyStorage(storage, cache, proxies);
    }

    return storage;
  }

  /**
   * Gets the HTTP port number.
   *
   * @return the port number.
   */
  private int getPort() {
    return config.getInt("maven.port");
  }

  /**
   * Creates the identity manager.
   *
   * @return the identity manager.
   */
  private IdentityManager createIdentityManager() {
    Map<String, char[]> credentials = new HashMap<>();

    if (config.containsKey("maven.users.name")) {
      List<String> names = config.getList(String.class, "maven.users.name");

      for (int i = 0; i < names.size(); i++) {
        String password = config.getString("maven.users(" + i + ").password");

        if (password != null) {
          char[] hashed;

          if (password.startsWith("bcrypt:")) {
            hashed = password.substring(7).toCharArray();
          } else {
            hashed = BCrypt.withDefaults().hashToChar(6, password.toCharArray());
          }

          credentials.put(names.get(i), hashed);
        }
      }
    }

    return new MavenIdentityManager(credentials);
  }
}
