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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code LocalStorage} is an implementation of {@link Storage} that stores artifacts in a local
 * file system.
 */
public class LocalStorage implements Storage {

  private final Path root;
  private final Set<String> repositories;

  /**
   * Creates a new instance of {@code LocalStorage}.
   *
   * @param root         the path to the directory where the artifacts will be stored.
   * @param repositories the repository names.
   * @throws IOException if an I/O error prevented the storage from being initialized.
   */
  public LocalStorage(Path root, Set<String> repositories) throws IOException {
    this.root = root.toAbsolutePath();
    this.repositories = repositories;

    for (String repository : repositories) {
      Path path = root.resolve(repository);
      Files.createDirectories(path);
    }
  }

  @Override
  public List<String> getRepositories() {
    return new ArrayList<>(repositories);
  }

  @Override
  public List<StorageFile> listDirectory(String repository, String path) throws IOException {
    Path directoryPath = getPath(repository, path);

    if (directoryPath == null) {
      throw new IllegalArgumentException(
          "The directory at '" + path + "' in repository '" + repository + "' does not exist");
    }

    return Files.list(directoryPath)
        .map(this::createStorageFile)
        .collect(Collectors.toList());
  }

  @Override
  public boolean exists(String repository, String path) {
    return getPath(repository, path) != null;
  }

  @Override
  public Optional<StorageFile> getFile(String repository, String path) {
    return Optional.ofNullable(getPath(repository, path)).map(this::createStorageFile);
  }

  @Override
  public InputStream readFile(String repository, String path) throws IOException {
    Path filePath = getPath(repository, path);

    if (filePath == null) {
      throw new IllegalArgumentException(
          "The directory at '" + path + "' in repository '" + repository + "' does not exist");
    }

    if (Files.isDirectory(filePath)) {
      throw new IllegalArgumentException(
          "The path '" + path + "' in repository '" + repository + "' is a directory");
    }

    return Files.newInputStream(filePath);
  }

  @Override
  public void writeFile(String repository, String path, InputStream input) throws IOException {
    Path filePath = getPath(repository, path, true);
    assert filePath != null;

    if (Files.isDirectory(filePath)) {
      throw new IllegalArgumentException(
          "The path '" + path + "' in repository '" + repository + "' is a directory");
    }

    if (!Files.exists(filePath.getParent())) {
      throw new IllegalArgumentException(
          "The parent of path '" + path + "' in repository '" + repository +
              "' does not exist");
    }

    if (!Files.isDirectory(filePath.getParent())) {
      throw new IllegalArgumentException(
          "The parent of path '" + path + "' in repository '" + repository +
              "' is not a directory");
    }

    Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
  }

  @Override
  public void createDirectory(String repository, String path) throws IOException {
    if (getPath(repository, null) == null) {
      throw new IllegalArgumentException("The repository '" + repository + "' does not exist");
    }

    Path directoryPath = getPath(repository, path, true);
    assert directoryPath != null;
    Files.createDirectories(directoryPath);
  }

  /**
   * Gets the {@link Path} for a specified repository and file path.
   *
   * @param repository the name of the repository.
   * @param path       the path to the file.
   * @return the path or {@code null} if it does not exist.
   */
  private Path getPath(String repository, String path) {
    return getPath(repository, path, false);
  }

  /**
   * Gets the {@link Path} for a specified repository and file path.
   *
   * @param repository the name of the repository.
   * @param path       the path to the file.
   * @param force      a flag that indicates that a path should be returned even if it does not
   *                   exist.
   * @return the path or {@code null} if it does not exist.
   */
  private Path getPath(String repository, String path, boolean force) {
    Path repositoryPath = root.resolve(repository);
    Path filePath;

    if (path == null || path.isEmpty()) {
      filePath = repositoryPath;
    } else {
      filePath = repositoryPath.resolve(path);
    }

    return force || Files.exists(filePath) ? filePath : null;
  }

  /**
   * Creates a {@link StorageFile} instance for a file path.
   *
   * @param path the file path.
   * @return the storage file.
   */
  private StorageFile createStorageFile(Path path) {
    Path relativePath = root.relativize(path);
    Path repositoryPath = relativePath.getName(0);
    Path filePath = repositoryPath.relativize(relativePath);

    StorageFile file = new StorageFile();
    file.setDirectory(Files.isDirectory(path));
    file.setRepository(repositoryPath.getFileName().toString());
    file.setPath(toString(filePath));
    file.setSize(file.isDirectory() ? 0L : path.toFile().length());
    file.setCreated(getDateCreated(path));
    return file;
  }

  /**
   * Converts a path to a string.
   *
   * @param path the path to convert.
   * @return the string representation of the path.
   */
  private String toString(Path path) {
    StringBuilder sb = new StringBuilder();

    for (Path name : path) {
      if (sb.length() > 0) {
        sb.append('/');
      }

      sb.append(name.toString());
    }

    return sb.toString();
  }

  /**
   * Gets the date and time at which a file was created.
   *
   * @param path the path to the file.
   * @return the creation date and time.
   */
  private LocalDateTime getDateCreated(Path path) {
    Instant created = Instant.ofEpochMilli(path.toFile().lastModified());
    return LocalDateTime.ofInstant(created, ZoneOffset.systemDefault());
  }
}
