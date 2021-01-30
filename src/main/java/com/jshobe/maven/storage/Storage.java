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
import java.util.List;
import java.util.Optional;

/**
 * {@code Storage} provides an interface that provide persistent storage of artifacts.
 */
public interface Storage {

  /**
   * Gets the names of the repositories in storage.
   *
   * @return the list of repository names.
   * @throws IOException if an I/O error occurs that prevents the repositories from being listed.
   */
  List<String> getRepositories() throws IOException;

  /**
   * Lists the contents of a directory.
   *
   * @param repository the name of the repository.
   * @param path       the path to the directory. May be {@code null} or an empty string to list the
   *                   root of the repository.
   * @return the directory contents.
   * @throws IllegalArgumentException if the directory does not exist.
   * @throws IOException              if an I/O error occurs that prevents the contents from being
   *                                  listed.
   */
  List<StorageFile> listDirectory(String repository, String path) throws IOException;

  /**
   * Determines if a file or directory at the specified path exists.
   *
   * @param repository the name of the repository.
   * @param path       the path to the file.
   * @return {@code true} if it exists or {@code false} if it does not.
   * @throws IOException if an I/O error occurs that prevents the file from being obtained.
   */
  boolean exists(String repository, String path) throws IOException;

  /**
   * Gets the file or directory at the specified path, if it exists.
   *
   * @param repository the name of the repository.
   * @param path       the path to the file.
   * @return the {@link StorageFile} for the object at the path.
   * @throws IOException if an I/O error occurs that prevents the file from being obtained.
   */
  Optional<StorageFile> getFile(String repository, String path) throws IOException;

  /**
   * Gets an input stream for the file at the specified path.
   *
   * @param repository the name of the repository.
   * @param path       the path to the file.
   * @return an input stream from which the contents of the file can be read.
   * @throws IllegalArgumentException if the file does not exist or is a directory.
   * @throws IOException              if an I/O error occurs that prevents the file from being
   *                                  opened.
   */
  InputStream readFile(String repository, String path) throws IOException;

  /**
   * Writes the file at the specified path.
   *
   * @param repository the name of the repository.
   * @param path       the path to the file.
   * @param input      the input stream from which the file contents are read.
   * @throws IllegalArgumentException if the parent directory does not exist or the path is for an
   *                                  existing directory.
   * @throws IOException              if an I/O error occurs that prevents the file from being
   *                                  opened.
   */
  void writeFile(String repository, String path, InputStream input) throws IOException;

  /**
   * Creates a directory and its parent directories, if it does not exist.
   *
   * @param repository the name of the repository.
   * @param path       the path to the directory.
   * @throws IOException if an I/O error occurs that prevents the directory from being created.
   */
  void createDirectory(String repository, String path) throws IOException;
}
