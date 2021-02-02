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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3 Storage Tests")
class S3StorageTests {

  @Mock
  private S3Client client;
  private final String bucket = "bucket";
  private final Set<String> repositories =
      new LinkedHashSet<>(Arrays.asList("releases", "snapshots"));

  private static Instant toInstant(LocalDateTime time) {
    return time.toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now()));
  }

  @Nested
  @DisplayName("Given an empty bucket")
  class GivenAnEmptyBucket {

    @Test
    @DisplayName("then the repository folders should be created")
    void thenTheRepositoryFoldersShouldBeCreated() {
      ListObjectsRequest list = ListObjectsRequest.builder()
          .bucket(bucket)
          .delimiter("/")
          .build();
      ListObjectsResponse response = ListObjectsResponse.builder()
          .commonPrefixes(Collections.emptyList())
          .build();
      PutObjectRequest put1 = PutObjectRequest.builder()
          .bucket(bucket)
          .key("releases/")
          .build();
      PutObjectRequest put2 = PutObjectRequest.builder()
          .bucket(bucket)
          .key("snapshots/")
          .build();

      when(client.listObjects(eq(list))).thenReturn(response);

      new S3Storage(bucket, repositories, () -> client);
      verify(client).listObjects(eq(list));
      verify(client).putObject(eq(put1), any(RequestBody.class));
      verify(client).putObject(eq(put2), any(RequestBody.class));
      verify(client).close();
      verifyNoMoreInteractions(client);
    }
  }

  @Nested
  @DisplayName("Given a bucket with folders")
  class GivenABucketWithFolders {

    private S3Storage storage;

    @BeforeEach
    void setUp() {
      ListObjectsRequest request = ListObjectsRequest.builder()
          .bucket(bucket)
          .delimiter("/")
          .build();
      ListObjectsResponse response = ListObjectsResponse.builder()
          .commonPrefixes(
              CommonPrefix.builder().prefix("releases/").build(),
              CommonPrefix.builder().prefix("snapshots/").build())
          .build();

      when(client.listObjects(eq(request))).thenReturn(response);
      storage = new S3Storage(bucket, repositories, () -> client);
      verify(client).listObjects(eq(request));
      verify(client).close();
    }

    @Test
    @DisplayName("then repository folders should not be created")
    void thenRepositoryFoldersShouldNotBeCreated() {
      verifyNoMoreInteractions(client);
    }

    @Test
    @DisplayName("then correct repositories should be returned")
    void thenCorrectRepositoriesShouldBeReturned() {
      List<String> expected = new ArrayList<>(repositories);
      assertEquals(expected, storage.getRepositories());
      verifyNoMoreInteractions(client);
    }

    @Test
    @DisplayName("then folder contents should be returned")
    void thenFolderContentsShouldBeReturned() {
      LocalDateTime modified = LocalDateTime.of(2021, 1, 31, 12, 30, 0);
      Instant modifiedTs = toInstant(modified);
      long size = 1234L;
      ListObjectsRequest request = ListObjectsRequest.builder()
          .bucket(bucket)
          .prefix("releases/com/jshobe/")
          .delimiter("/")
          .build();
      ListObjectsResponse response = ListObjectsResponse.builder()
          .commonPrefixes(CommonPrefix.builder().prefix("releases/com/jshobe/maven/").build())
          .contents(
              S3Object.builder()
                  .key("releases/com/jshobe/")
                  .eTag("DUMMY")
                  .size(0L)
                  .lastModified(modifiedTs)
                  .build(),
              S3Object.builder()
                  .key("releases/com/jshobe/metadata.xml")
                  .eTag("DUMMY")
                  .size(size)
                  .lastModified(modifiedTs)
                  .build())
          .build();

      when(client.listObjects(eq(request))).thenReturn(response);

      List<StorageFile> expected = new ArrayList<>(Arrays.asList(
          new StorageFile(true, "releases", "com/jshobe/maven", 0L, null),
          new StorageFile(false, "releases", "com/jshobe/metadata.xml", size, modified)));
      List<StorageFile> actual = storage.listDirectory("releases", "com/jshobe");

      assertEquals(expected, actual);

      verify(client).listObjects(eq(request));
      verify(client, times(2)).close();
      verifyNoMoreInteractions(client);
    }

    @Test
    @DisplayName("then file should be returned")
    void thenFileShouldBeReturned() {
      LocalDateTime modified = LocalDateTime.of(2021, 1, 31, 12, 30, 0);
      Instant modifiedTs = toInstant(modified);
      long size = 1234L;
      ListObjectsRequest request = ListObjectsRequest.builder()
          .bucket(bucket)
          .prefix("releases/com/jshobe/")
          .delimiter("/")
          .build();
      ListObjectsResponse response = ListObjectsResponse.builder()
          .commonPrefixes(CommonPrefix.builder().prefix("releases/com/jshobe/maven/").build())
          .contents(
              S3Object.builder()
                  .key("releases/com/jshobe/")
                  .eTag("DUMMY")
                  .size(0L)
                  .lastModified(modifiedTs)
                  .build(),
              S3Object.builder()
                  .key("releases/com/jshobe/metadata.xml")
                  .eTag("DUMMY")
                  .size(size)
                  .lastModified(modifiedTs)
                  .build())
          .build();

      when(client.listObjects(eq(request))).thenReturn(response);

      Optional<StorageFile> actual = storage.getFile("releases", "com/jshobe/metadata.xml");
      StorageFile expected = new StorageFile(false, "releases", "com/jshobe/metadata.xml", size,
          modified);

      assertNotNull(actual);
      assertTrue(actual.isPresent());
      assertEquals(expected, actual.get());

      verify(client).listObjects(eq(request));
      verify(client, times(2)).close();
      verifyNoMoreInteractions(client);
    }

    @Test
    @DisplayName("then root file should be returned")
    void thenRootFileShouldBeReturned() {
      LocalDateTime modified = LocalDateTime.of(2021, 1, 31, 12, 30, 0);
      Instant modifiedTs = toInstant(modified);
      long size = 1234L;
      ListObjectsRequest request = ListObjectsRequest.builder()
          .bucket(bucket)
          .prefix("releases/")
          .delimiter("/")
          .build();
      ListObjectsResponse response = ListObjectsResponse.builder()
          .commonPrefixes(CommonPrefix.builder().prefix("releases/com/").build())
          .contents(
              S3Object.builder()
                  .key("releases/")
                  .eTag("DUMMY")
                  .size(0L)
                  .lastModified(modifiedTs)
                  .build(),
              S3Object.builder()
                  .key("releases/metadata.xml")
                  .eTag("DUMMY")
                  .size(size)
                  .lastModified(modifiedTs)
                  .build())
          .build();

      when(client.listObjects(eq(request))).thenReturn(response);

      Optional<StorageFile> actual = storage.getFile("releases", "metadata.xml");
      StorageFile expected = new StorageFile(false, "releases", "metadata.xml", size, modified);

      assertNotNull(actual);
      assertTrue(actual.isPresent());
      assertEquals(expected, actual.get());

      verify(client).listObjects(eq(request));
      verify(client, times(2)).close();
      verifyNoMoreInteractions(client);
    }

    @Test
    @DisplayName("then missing file should be empty")
    void thenMissingFileShouldBeEmpty() {
      LocalDateTime modified = LocalDateTime.of(2021, 1, 31, 12, 30, 0);
      Instant modifiedTs = toInstant(modified);
      long size = 1234L;
      ListObjectsRequest request = ListObjectsRequest.builder()
          .bucket(bucket)
          .prefix("releases/com/jshobe/")
          .delimiter("/")
          .build();
      ListObjectsResponse response = ListObjectsResponse.builder()
          .commonPrefixes(CommonPrefix.builder().prefix("releases/com/jshobe/maven/").build())
          .contents(
              S3Object.builder()
                  .key("releases/com/jshobe/")
                  .eTag("DUMMY")
                  .size(0L)
                  .lastModified(modifiedTs)
                  .build(),
              S3Object.builder()
                  .key("releases/com/jshobe/metadata.xml")
                  .eTag("DUMMY")
                  .size(size)
                  .lastModified(modifiedTs)
                  .build())
          .build();

      when(client.listObjects(eq(request))).thenReturn(response);

      Optional<StorageFile> actual = storage.getFile("releases", "com/jshobe/missing.pom");

      assertNotNull(actual);
      assertFalse(actual.isPresent());

      verify(client).listObjects(eq(request));
      verify(client, times(2)).close();
      verifyNoMoreInteractions(client);
    }

    @Test
    @DisplayName("then existing file should return true")
    void thenExistingFileShouldExist() {
      LocalDateTime modified = LocalDateTime.of(2021, 1, 31, 12, 30, 0);
      Instant modifiedTs = toInstant(modified);
      long size = 1234L;
      ListObjectsRequest request = ListObjectsRequest.builder()
          .bucket(bucket)
          .prefix("releases/com/jshobe/")
          .delimiter("/")
          .build();
      ListObjectsResponse response = ListObjectsResponse.builder()
          .commonPrefixes(CommonPrefix.builder().prefix("releases/com/jshobe/maven/").build())
          .contents(
              S3Object.builder()
                  .key("releases/com/jshobe/")
                  .eTag("DUMMY")
                  .size(0L)
                  .lastModified(modifiedTs)
                  .build(),
              S3Object.builder()
                  .key("releases/com/jshobe/metadata.xml")
                  .eTag("DUMMY")
                  .size(size)
                  .lastModified(modifiedTs)
                  .build())
          .build();

      when(client.listObjects(eq(request))).thenReturn(response);

      boolean actual = storage.exists("releases", "com/jshobe/metadata.xml");
      assertTrue(actual);

      verify(client).listObjects(eq(request));
      verify(client, times(2)).close();
      verifyNoMoreInteractions(client);
    }

    @Test
    @DisplayName("then missing file should exist")
    void thenMissingFileShouldNotExist() {
      LocalDateTime modified = LocalDateTime.of(2021, 1, 31, 12, 30, 0);
      Instant modifiedTs = toInstant(modified);
      long size = 1234L;
      ListObjectsRequest request = ListObjectsRequest.builder()
          .bucket(bucket)
          .prefix("releases/com/jshobe/")
          .delimiter("/")
          .build();
      ListObjectsResponse response = ListObjectsResponse.builder()
          .commonPrefixes(CommonPrefix.builder().prefix("releases/com/jshobe/maven/").build())
          .contents(
              S3Object.builder()
                  .key("releases/com/jshobe/")
                  .eTag("DUMMY")
                  .size(0L)
                  .lastModified(modifiedTs)
                  .build(),
              S3Object.builder()
                  .key("releases/com/jshobe/metadata.xml")
                  .eTag("DUMMY")
                  .size(size)
                  .lastModified(modifiedTs)
                  .build())
          .build();

      when(client.listObjects(eq(request))).thenReturn(response);

      boolean actual = storage.exists("releases", "com/jshobe/missing.pom");
      assertFalse(actual);

      verify(client).listObjects(eq(request));
      verify(client, times(2)).close();
      verifyNoMoreInteractions(client);
    }

    @Test
    @DisplayName("then read file should provide content")
    void thenReadFileShouldProvideContent() throws Exception {
      GetObjectRequest request = GetObjectRequest.builder()
          .bucket(bucket)
          .key("releases/com/jshobe/metadata.xml")
          .build();
      byte[] expected = randomData();
      ByteArrayInputStream responseBuffer = new ByteArrayInputStream(expected);
      AbortableInputStream abortableInput = AbortableInputStream.create(responseBuffer, () -> {
      });
      GetObjectResponse response = GetObjectResponse.builder()
          .build();
      ResponseInputStream<GetObjectResponse> responseInput =
          new ResponseInputStream<>(response, abortableInput);

      when(client.getObject(eq(request))).thenReturn(responseInput);

      ByteArrayOutputStream buffer = new ByteArrayOutputStream();

      try (InputStream input = storage.readFile("releases", "com/jshobe/metadata.xml")) {
        byte[] bytes = new byte[1024];
        int len;

        while ((len = input.read(bytes)) >= 0) {
          buffer.write(bytes, 0, len);
        }
      }

      byte[] actual = buffer.toByteArray();
      assertArrayEquals(expected, actual);

      verify(client).getObject(eq(request));
      verify(client, times(2)).close();
      verifyNoMoreInteractions(client);
    }

    @Test
    @DisplayName("then write file should put content")
    void thenWriteFileShouldPutContent() throws Exception {
      PutObjectRequest request = PutObjectRequest.builder()
          .bucket(bucket)
          .key("releases/com/jshobe/metadata.xml")
          .build();
      byte[] data = randomData();
      RequestBody expected = RequestBody.fromInputStream(new ByteArrayInputStream(data), 2048L);
      RequestBodyMatcher matcher = new RequestBodyMatcher(expected);

      when(client.putObject(eq(request), argThat(matcher))).thenReturn(null);

      storage.writeFile("releases", "com/jshobe/metadata.xml", new ByteArrayInputStream(data));

      verify(client).putObject(eq(request), argThat(matcher));
      verify(client, times(2)).close();
      verifyNoMoreInteractions(client);
    }

    @Test
    @DisplayName("then create directory should create folder")
    void thenCreateDirectoryShouldCreateFolder() {
      PutObjectRequest request = PutObjectRequest.builder()
          .bucket(bucket)
          .key("releases/com/jshobe/folder/")
          .build();
      RequestBody expected = RequestBody.empty();
      RequestBodyMatcher matcher = new RequestBodyMatcher(expected);

      when(client.putObject(eq(request), argThat(matcher))).thenReturn(null);

      storage.createDirectory("releases", "com/jshobe/folder");

      verify(client).putObject(eq(request), argThat(matcher));
      verify(client, times(2)).close();
      verifyNoMoreInteractions(client);
    }

    private byte[] randomData() {
      byte[] data = new byte[2048];
      Random random = new Random(System.currentTimeMillis());
      random.nextBytes(data);
      return data;
    }
  }
}
