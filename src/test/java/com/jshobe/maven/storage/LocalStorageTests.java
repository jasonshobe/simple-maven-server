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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalStorageTests {

  @TempDir
  Path root;

  private final Set<String> repositories = new LinkedHashSet<>(
      Arrays.asList("releases", "snapshots"));

  @Nested
  @DisplayName("Given an empty root folder")
  class GivenAnEmptyRootFolder {

    @Test
    @DisplayName("then the repository folders should be created")
    void thenTheRepositoryFoldersShouldBeCreated() throws Exception {
      int count;

      try (Stream<Path> files = Files.walk(root)) {
        count = (int) files.filter(p -> !p.equals(root)).count();
      }

      assertEquals(0, count);

      new LocalStorage(root, repositories);

      try (Stream<Path> files = Files.walk(root)) {
        count = (int) files.filter(p -> !p.equals(root)).count();
      }

      assertEquals(2, count);
      assertTrue(Files.isDirectory(root.resolve("releases")));
      assertTrue(Files.isDirectory(root.resolve("snapshots")));
    }
  }

  @Nested
  @DisplayName("Given a root with folders")
  class GivenARootWithFolders {

    private LocalStorage storage;
    private StorageFile rootFile;
    private StorageFile mavenFolder;
    private StorageFile metadataFile;
    private byte[] metadataContent;

    @BeforeEach
    void setUp() throws Exception {
      Files.createDirectories(root.resolve("releases"));
      Files.createDirectories(root.resolve("snapshots"));

      Path path = root.resolve("releases/com/jshobe/maven");
      Files.createDirectories(path);
      mavenFolder = new StorageFile();
      mavenFolder.setDirectory(true);
      mavenFolder.setRepository("releases");
      mavenFolder.setPath("com/jshobe/maven");
      mavenFolder.setSize(0L);
      Instant created = Files.getLastModifiedTime(path).toInstant();
      mavenFolder.setCreated(LocalDateTime.ofInstant(created, ZoneId.systemDefault()));

      path = root.resolve("releases/metadata.xml");
      byte[] data = new byte[2048];
      new Random(System.currentTimeMillis()).nextBytes(data);
      Files.copy(new ByteArrayInputStream(data), path);
      rootFile = new StorageFile();
      rootFile.setDirectory(false);
      rootFile.setRepository("releases");
      rootFile.setPath("metadata.xml");
      rootFile.setSize(path.toFile().length());
      created = Files.getLastModifiedTime(path).toInstant();
      rootFile.setCreated(LocalDateTime.ofInstant(created, ZoneId.systemDefault()));

      path = root.resolve("releases/com/jshobe/metadata.xml");
      metadataContent = new byte[2048];
      new Random(System.currentTimeMillis()).nextBytes(metadataContent);
      Files.copy(new ByteArrayInputStream(metadataContent), path);
      metadataFile = new StorageFile();
      metadataFile.setDirectory(false);
      metadataFile.setRepository("releases");
      metadataFile.setPath("com/jshobe/metadata.xml");
      metadataFile.setSize(path.toFile().length());
      created = Files.getLastModifiedTime(path).toInstant();
      metadataFile.setCreated(LocalDateTime.ofInstant(created, ZoneId.systemDefault()));

      storage = new LocalStorage(root, repositories);
    }

    @Test
    @DisplayName("then correct repositories should be returned")
    void thenCorrectRepositoriesShouldBeReturned() {
      List<String> expected = new ArrayList<>(repositories);
      assertEquals(expected, storage.getRepositories());
    }

    @Test
    @DisplayName("then folder contents should be returned")
    void thenFolderContentsShouldBeReturned() throws Exception {
      List<StorageFile> expected = new ArrayList<>(Arrays.asList(mavenFolder, metadataFile));
      List<StorageFile> actual = storage.listDirectory("releases", "com/jshobe");
      assertEquals(expected, actual);
    }

    @Test
    @DisplayName("then list directory with invalid path should throw exception")
    void thenListDirectoryWithInvalidPathShouldThrowException() {
      assertThrows(IllegalArgumentException.class,
          () -> storage.listDirectory("releases", "missing"));
    }

    @Test
    @DisplayName("then file should be returned")
    void thenFileShouldBeReturned() {
      Optional<StorageFile> actual = storage.getFile("releases", "com/jshobe/metadata.xml");
      assertNotNull(actual);
      assertTrue(actual.isPresent());
      assertEquals(metadataFile, actual.get());
    }

    @Test
    @DisplayName("then root file should be created")
    void thenRootFileShouldBeReturned() {
      Optional<StorageFile> actual = storage.getFile("releases", "metadata.xml");
      assertNotNull(actual);
      assertTrue(actual.isPresent());
      assertEquals(rootFile, actual.get());
    }

    @Test
    @DisplayName("then missing file should be empty")
    void thenMissingFileShouldBeEmpty() {
      Optional<StorageFile> actual = storage.getFile("releases", "com/jshobe/missing.pom");
      assertNotNull(actual);
      assertFalse(actual.isPresent());
    }

    @Test
    @DisplayName("then existing file should exist")
    void thenExistingFileShouldExist() {
      boolean actual = storage.exists("releases", "com/jshobe/metadata.xml");
      assertTrue(actual);
    }

    @Test
    @DisplayName("then missing file should not exist")
    void thenMissingFileShouldNotExist() {
      boolean actual = storage.exists("releases", "com/jshobe/missing.pom");
      assertFalse(actual);
    }

    @Test
    @DisplayName("then read file should provide content")
    void thenReadFileShouldProvideContent() throws Exception {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();

      try (InputStream input = storage.readFile("releases", "com/jshobe/metadata.xml")) {
        byte[] bytes = new byte[1024];
        int len;

        while ((len = input.read(bytes)) >= 0) {
          buffer.write(bytes, 0, len);
        }

        byte[] actual = buffer.toByteArray();
        assertArrayEquals(metadataContent, actual);
      }
    }

    @Test
    @DisplayName("then read file with invalid path should throw exception")
    void thenReadFileWithInvalidPathShouldThrowException() {
      assertThrows(IllegalArgumentException.class,
          () -> storage.readFile("releases", "missing.pom"));
    }

    @Test
    @DisplayName("then read file with directory path should throw exception")
    void thenReadFileWithDirectoryPathShouldThrowException() {
      assertThrows(IllegalArgumentException.class, () -> storage.readFile("releases", "com"));
    }

    @Test
    @DisplayName("then write file should put content")
    void thenWriteFileShouldPutContent() throws Exception {
      byte[] expected = new byte[2048];
      new Random(System.currentTimeMillis()).nextBytes(expected);

      storage.writeFile("releases", "com/jshobe/maven/metadata.xml",
          new ByteArrayInputStream(expected));

      Path path = root.resolve("releases/com/jshobe/maven/metadata.xml");
      assertTrue(Files.isRegularFile(path));

      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      Files.copy(path, buffer);
      byte[] actual = buffer.toByteArray();
      assertArrayEquals(expected, actual);
    }

    @Test
    @DisplayName("then write file with directory path should throw exception")
    void thenWriteFileWithDirectoryPathShouldThrowException() {
      assertThrows(IllegalArgumentException.class,
          () -> storage.writeFile("releases", "com", new ByteArrayInputStream(new byte[0])));
    }

    @Test
    @DisplayName("then write file with missing parent should throw exception")
    void thenWriteFileWithMissingParentShouldThrowException() {
      assertThrows(IllegalArgumentException.class,
          () -> storage.writeFile("releases", "org/metadata.xml", new ByteArrayInputStream(new byte[0])));
    }

    @Test
    @DisplayName("then write file with file parent path should throw exception")
    void thenWriteFileWithFileParentPathShouldThrowException() {
      assertThrows(IllegalArgumentException.class, () -> storage
          .writeFile("releases", "metadata.xml/file.txt", new ByteArrayInputStream(new byte[0])));
    }

    @Test
    @DisplayName("then create directory should create folder")
    void thenCreateDirectoryShouldCreateFolder() throws Exception {
      storage.createDirectory("releases", "com/jshobe/folder");
      Path path = root.resolve("releases/com/jshobe/folder");
      assertTrue(Files.isDirectory(path));
    }

    @Test
    @DisplayName("then create directory with invalid repository should throw exception")
    void thenCreateDirectoryWithInvalidRepositoryShouldThrowException() {
      assertThrows(IllegalArgumentException.class, () -> storage.createDirectory("missing", "com"));
    }
  }
}
