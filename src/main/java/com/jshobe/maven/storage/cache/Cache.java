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

package com.jshobe.maven.storage.cache;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import org.mapdb.DB;
import org.mapdb.DB.HashMapMaker;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

/**
 * {@code Cache} provides in-memory caching that overflows to disk.
 */
public class Cache implements AutoCloseable {

  private final DB diskDb;
  private final DB memoryDb;

  /**
   * Creates a new instance of {@code Cache}.
   *
   * @param path    the path to the on-disk cache.
   * @param rebuild {@code true} to discard any existing entries on-disk or {@code false} to retain
   *                existing entries on-disk.
   * @throws IOException if an I/O error occurs.
   */
  public Cache(Path path, boolean rebuild) throws IOException {
    if (rebuild && Files.exists(path.getParent())) {
      try (Stream<Path> paths = Files.walk(path.getParent())) {
        paths.sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
      }
    }

    Files.createDirectories(path.getParent());
    diskDb = DBMaker
        .fileDB(path.toFile())
        .closeOnJvmShutdown()
        .make();
    memoryDb = DBMaker.memoryDB().make();
  }

  @Override
  public void close() {
    memoryDb.close();
    diskDb.close();
  }

  /**
   * Creates a cache map.
   *
   * @param name       the name of the cache.
   * @param maxSize    the maximum size of the in-memory cache.
   * @param expireTime the in-memory cache expiration time.
   * @param expireUnit the time unit for the in-memory cache expiration.
   * @param loader     the cache data loader.
   * @param <V>        the type of the value.
   * @return a new cache map.
   */
  public <V extends Serializable> CacheMap<V> createCache(String name, long maxSize,
      long expireTime, TimeUnit expireUnit, BiFunction<String, String, V> loader) {
    CacheKeySerializer keySerializer = new CacheKeySerializer();
    CacheValueSerializer<V> valueSerializer = new CacheValueSerializer<>();
    HTreeMap<CacheKey, CacheValue<V>> diskMap =
        diskDb.hashMap(name, keySerializer, valueSerializer).createOrOpen();
    HashMapMaker<CacheKey, CacheValue<V>> maker =
        memoryDb.hashMap(name, keySerializer, valueSerializer);

    if (maxSize > 0L) {
      maker = maker.expireMaxSize(maxSize);
    }

    if (expireTime >= 0L && expireUnit != null) {
      maker.expireAfterGet(expireTime, expireUnit);
    }

    HTreeMap<CacheKey, CacheValue<V>> memoryMap = maker.expireOverflow(diskMap).createOrOpen();
    return new CacheMap<>(memoryMap, loader);
  }
}
