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

import java.io.Serializable;
import java.util.function.BiFunction;
import org.mapdb.HTreeMap;

/**
 * {@code CacheMap} is a map of values within the cache.
 *
 * @param <V> the type of value.
 */
public class CacheMap<V extends Serializable> {

  private final HTreeMap<CacheKey, CacheValue<V>> map;
  private final BiFunction<String, String, V> loader;

  /**
   * Creates a new instance of {@code CacheMap}.
   *
   * @param map    the map that backs this cache.
   * @param loader the function that creates values missing from the cache.
   */
  CacheMap(HTreeMap<CacheKey, CacheValue<V>> map, BiFunction<String, String, V> loader) {
    this.map = map;
    this.loader = loader;
  }

  /**
   * Gets a value from the cache.
   *
   * @param repository the name of the repository.
   * @param path       the path to the file.
   * @return the cached value or {@code null} if it does not exist.
   */
  public V get(String repository, String path) {
    CacheKey key = new CacheKey(repository, path);
    CacheValue<V> value = map
        .computeIfAbsent(key, k -> new CacheValue<>(loader.apply(k.getRepository(), k.getPath())));
    return value.getValue();
  }

  /**
   * Removes a value from the cache.
   *
   * @param repository the name of the repository.
   * @param path       the path to the file.
   * @return the removed value or {@code null} if there was none.
   */
  public V invalidate(String repository, String path) {
    CacheKey key = new CacheKey(repository, path);
    CacheValue<V> value = map.remove(key);
    return value == null ? null : value.getValue();
  }
}
