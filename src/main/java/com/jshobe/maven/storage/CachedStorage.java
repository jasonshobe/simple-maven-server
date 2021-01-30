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

package com.jshobe.maven.storage;

import com.jshobe.maven.storage.cache.Cache;
import com.jshobe.maven.storage.cache.CacheMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * {@code CachedStorage} is an implementation of {@link Storage} that wraps another storage instance
 * and caches the results of its query methods.
 */
public class CachedStorage implements Storage {

  private final Storage storage;
  private final CacheMap<StorageFile[]> directories;
  private final CacheMap<StorageFile> files;

  /**
   * Creates a new instance of {@code CachedStorage}.
   *
   * @param storage the wrapped storage.
   * @param cache   the cache.
   */
  public CachedStorage(Storage storage, Cache cache) {
    this.storage = storage;
    directories = cache.createCache(
        "directories", 100L, 1L, TimeUnit.HOURS, this::fetchDirectoryList);
    files = cache.createCache(
        "files", 1000L, 1L, TimeUnit.HOURS, this::fetchFile);
  }

  @Override
  public List<String> getRepositories() throws IOException {
    return storage.getRepositories();
  }

  @Override
  public List<StorageFile> listDirectory(String repository, String path) {
    StorageFile[] files = directories.get(repository, path);

    if (files == null) {
      return null;
    }

    return Arrays.asList(files);
  }

  @Override
  public boolean exists(String repository, String path) {
    return getFile(repository, path).isPresent();
  }

  @Override
  public Optional<StorageFile> getFile(String repository, String path) {
    return Optional.ofNullable(files.get(repository, path));
  }

  @Override
  public InputStream readFile(String repository, String path) throws IOException {
    return storage.readFile(repository, path);
  }

  @Override
  public void writeFile(String repository, String path, InputStream input) throws IOException {
    storage.writeFile(repository, path, input);
    directories.invalidate(repository, getParentPath(path));
    files.invalidate(repository, path);
  }

  @Override
  public void createDirectory(String repository, String path) throws IOException {
    storage.createDirectory(repository, path);
    directories.invalidate(repository, getParentPath(path));
  }

  /**
   * Gets the directory contents for the cache.
   *
   * @param repository the name of the repository.
   * @param path       the path to the file.
   * @return the directory contents.
   */
  private StorageFile[] fetchDirectoryList(String repository, String path) {
    try {
      List<StorageFile> files = storage.listDirectory(repository, path);

      if (files == null) {
        return null;
      }

      return files.toArray(new StorageFile[0]);
    } catch (IOException e) {
      throw new RuntimeException("Failed to list directory", e);
    }
  }

  /**
   * Gets a file for the cache.
   *
   * @param repository the name of the repository.
   * @param path       the path to the file.
   * @return the file or {@code null} if it does not exist.
   */
  private StorageFile fetchFile(String repository, String path) {
    try {
      return storage.getFile(repository, path).orElse(null);
    } catch (IOException e) {
      throw new RuntimeException("Failed to get file", e);
    }
  }

  /**
   * Gets the parent path of a path.
   *
   * @param path the path.
   * @return the parent path.
   */
  private String getParentPath(String path) {
    int index = path.lastIndexOf('/');

    if (index < 0) {
      return "";
    }

    return path.substring(0, index);
  }
}
