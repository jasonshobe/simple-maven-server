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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * {@code S3Storage} is an implementation of {@link Storage} that stores artifacts in an AWS S3
 * bucket.
 */
public class S3Storage implements Storage {

  private final String bucket;
  private final Set<String> repositories;

  /**
   * Creates a new instance of {@code S3Storage}.
   *
   * @param bucket       the name of the S3 bucket.
   * @param repositories the repository names.
   */
  public S3Storage(String bucket, Set<String> repositories) {
    this.bucket = bucket;
    this.repositories = repositories;

    try (S3Client client = createClient()) {
      ListObjectsRequest listRequest = ListObjectsRequest.builder()
          .bucket(bucket)
          .delimiter("/")
          .build();
      Set<String> existing = client.listObjects(listRequest).commonPrefixes().stream()
          .map(CommonPrefix::prefix)
          .map(s -> s.substring(0, s.length() - 1))
          .collect(Collectors.toSet());

      for (String repository : repositories) {
        if (!existing.contains(repository)) {
          PutObjectRequest request = PutObjectRequest.builder()
              .bucket(bucket)
              .key(repository + "/")
              .build();
          client.putObject(request, RequestBody.empty());
        }
      }
    }
  }

  @Override
  public List<String> getRepositories() {
    return new ArrayList<>(repositories);
  }

  @Override
  public List<StorageFile> listDirectory(String repository, String path) {
    try (S3Client client = createClient()) {
      String prefix = getKey(repository, path) + "/";
      ListObjectsRequest request = ListObjectsRequest.builder()
          .bucket(bucket)
          .prefix(prefix)
          .delimiter("/")
          .build();
      ListObjectsResponse response = client.listObjects(request);

      return Stream.concat(
          response.commonPrefixes().stream()
              .map(p -> createStorageDirectory(repository, p.prefix())),
          response.contents().stream()
              .filter(o -> !prefix.equals(o.key()))
              .map(o -> createStorageFile(repository, o)))
          .collect(Collectors.toList());
    }
  }

  @Override
  public boolean exists(String repository, String path) {
    return getFile(repository, path).isPresent();
  }

  @Override
  public Optional<StorageFile> getFile(String repository, String path) {
    int index = path.lastIndexOf('/');
    String parent = null;

    if (index >= 0) {
      parent = path.substring(0, index);
    }

    return listDirectory(repository, parent).stream()
        .filter(f -> f.getRepository().equals(repository) && f.getPath().equals(path))
        .findAny();
  }

  @Override
  public InputStream readFile(String repository, String path) {
    S3Client client = createClient();
    String key = getKey(repository, path);
    GetObjectRequest request = GetObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build();
    return new S3InputStream(client.getObject(request), client);
  }

  @Override
  public void writeFile(String repository, String path, InputStream input) throws IOException {
    Path temp = Files.createTempFile("maven", "tmp");

    try {
      Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
      long length = temp.toFile().length();

      try (S3Client client = createClient();
          InputStream in = Files.newInputStream(temp)) {
        String key = getKey(repository, path);
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();
        client.putObject(request, RequestBody.fromInputStream(in, length));
      }
    } finally {
      temp.toFile().delete();
    }
  }

  @Override
  public void createDirectory(String repository, String path) {
    try (S3Client client = createClient()) {
      String key = getKey(repository, path) + "/";
      PutObjectRequest request = PutObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .build();
      client.putObject(request, RequestBody.empty());
    }
  }

  /**
   * Creates a new S3 client instance.
   *
   * @return a new client.
   */
  private S3Client createClient() {
    return S3Client.builder().build();
  }

  /**
   * Gets the S3 key for the specified repository and file path.
   *
   * @param repository the name of the repository.
   * @param path       the path to the file.
   * @return the S3 key.
   */
  private String getKey(String repository, String path) {
    StringBuilder sb = new StringBuilder(repository);

    if (path != null && !path.isEmpty()) {
      sb.append('/').append(path);
    }

    return sb.toString();
  }

  /**
   * Creates the storage file for a directory.
   *
   * @param repository the name of the repository.
   * @param prefix     the common prefix representing a directory.
   * @return the storage file.
   */
  private StorageFile createStorageDirectory(String repository, String prefix) {
    StorageFile file = new StorageFile();
    file.setDirectory(true);
    file.setRepository(repository);
    file.setPath(prefix.substring(repository.length() + 1, prefix.length() - 1));
    return file;
  }

  /**
   * Creates the storage file for a S3 object.
   *
   * @param repository the name of the repository.
   * @param object     the S3 object.
   * @return the storage file.
   */
  private StorageFile createStorageFile(String repository, S3Object object) {
    StorageFile file = new StorageFile();
    file.setDirectory(false);
    file.setRepository(repository);
    file.setPath(object.key().substring(repository.length() + 1));
    file.setSize(object.size());
    file.setCreated(LocalDateTime.ofInstant(object.lastModified(), ZoneOffset.systemDefault()));
    return file;
  }

  private static final class S3InputStream extends FilterInputStream {

    private final S3Client client;

    public S3InputStream(InputStream in, S3Client client) {
      super(in);
      this.client = client;
    }

    @Override
    public void close() throws IOException {
      try {
        super.close();
      } finally {
        try {
          client.close();
        } catch (Exception ignore) {
        }
      }
    }
  }
}
