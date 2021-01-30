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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.jshobe.maven.storage.cache.Cache;
import com.jshobe.maven.storage.cache.CacheMap;
import com.vdurmont.semver4j.Semver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@code ProxyStorage} is an implementation of {@link Storage} that supports virtual repositories
 * that are proxies for one or more physical repositories.
 */
public class ProxyStorage implements Storage {

  private final Storage storage;
  private final Map<String, Set<String>> proxies;
  private final ObjectMapper mapper;
  private final DateTimeFormatter dateFormat;
  private final CacheMap<Metadata> metadata;

  /**
   * Creates a new instance of {@code ProxyStorage}.
   *
   * @param storage the storage provider.
   * @param cache   the cache.
   * @param proxies a map of virtual repository names to the names of the proxied repositories.
   */
  public ProxyStorage(Storage storage, Cache cache, Map<String, Set<String>> proxies) {
    this.storage = storage;
    this.proxies = proxies;
    this.mapper = new XmlMapper();
    this.dateFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    this.metadata = cache.createCache("metadata", 100L, 1L, TimeUnit.HOURS, this::buildMetadata);
  }

  @Override
  public List<String> getRepositories() throws IOException {
    return Stream.concat(storage.getRepositories().stream(), proxies.keySet().stream())
        .collect(Collectors.toList());
  }

  @Override
  public List<StorageFile> listDirectory(String repository, String path) throws IOException {
    Set<String> proxied = proxies.get(repository);

    if (proxied == null || proxied.isEmpty()) {
      return storage.listDirectory(repository, path);
    }

    List<StorageFile> files = new ArrayList<>();
    Set<String> added = new HashSet<>();

    for (String repo : proxied) {
      Optional<StorageFile> directory = storage.getFile(repo, path);

      if (directory.isPresent() && directory.get().isDirectory()) {
        for (StorageFile file : storage.listDirectory(repo, path)) {
          if (!added.contains(file.getPath())) {
            files.add(file.withRepository(repository));
            added.add(file.getPath());
          }
        }
      }
    }

    for (int i = 0; i < files.size(); i++) {
      StorageFile file = files.get(i);

      if (isMetadata(file.getPath())) {
        files.set(i, getFile(file.getRepository(), file.getPath()).get());
      }
    }

    return files;
  }

  @Override
  public boolean exists(String repository, String path) throws IOException {
    Set<String> proxied = proxies.get(repository);

    if (proxied == null || proxied.isEmpty()) {
      return storage.exists(repository, path);
    }

    for (String repo : proxied) {
      if (storage.exists(repo, path)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public Optional<StorageFile> getFile(String repository, String path) throws IOException {
    Set<String> proxied = proxies.get(repository);

    if (proxied == null || proxied.isEmpty()) {
      return storage.getFile(repository, path);
    }

    if (isMetadata(path)) {
      Metadata metadata = getMetadata(repository, path);
      StorageFile file = new StorageFile();
      file.setRepository(repository);
      file.setPath(path);
      file.setCreated(metadata.getCreated());

      if (path.endsWith(".md5")) {
        file.setSize(metadata.getMd5().length);
      } else if (path.endsWith(".sha1")) {
        file.setSize(metadata.getSha1().length);
      } else if (path.endsWith(".sha256")) {
        file.setSize(metadata.getSha256().length);
      } else if (path.endsWith(".sha512")) {
        file.setSize(metadata.getSha512().length);
      } else {
        file.setSize(metadata.size);
      }

      return Optional.of(file);
    } else {
      for (String repo : proxied) {
        Optional<StorageFile> file = storage.getFile(repo, path);

        if (file.isPresent()) {
          return file.map(f -> f.withRepository(repository));
        }
      }
    }

    return Optional.empty();
  }

  @Override
  public InputStream readFile(String repository, String path) throws IOException {
    Set<String> proxied = proxies.get(repository);

    if (proxied == null || proxied.isEmpty()) {
      return storage.readFile(repository, path);
    }

    if (isMetadata(path)) {
      Metadata metadata = getMetadata(repository, path);

      if (path.endsWith(".md5")) {
        return new ByteArrayInputStream(metadata.getMd5());
      } else if (path.endsWith(".sha1")) {
        return new ByteArrayInputStream(metadata.getSha1());
      } else if (path.endsWith(".sha256")) {
        return new ByteArrayInputStream(metadata.getSha256());
      } else if (path.endsWith(".sha512")) {
        return new ByteArrayInputStream(metadata.getSha512());
      } else {
        return new ByteArrayInputStream(mapper.writeValueAsBytes(metadata));
      }
    }

    for (String repo : proxied) {
      Optional<StorageFile> file = storage.getFile(repo, path);

      if (file.isPresent()) {
        return storage.readFile(repo, path);
      }
    }

    throw new IllegalArgumentException(
        "The directory at '" + path + "' in repository '" + repository + "' does not exist");
  }

  @Override
  public void writeFile(String repository, String path, InputStream input) throws IOException {
    Set<String> proxied = proxies.get(repository);

    if (proxied != null && !proxied.isEmpty()) {
      throw new IllegalArgumentException("The repository '" + repository + "' is read-only");
    }

    storage.writeFile(repository, path, input);
  }

  @Override
  public void createDirectory(String repository, String path) throws IOException {
    Set<String> proxied = proxies.get(repository);

    if (proxied != null && !proxied.isEmpty()) {
      throw new IllegalArgumentException("The repository '" + repository + "' is read-only");
    }

    storage.createDirectory(repository, path);
  }

  /**
   * Gets all proxied files at the specified path.
   *
   * @param repository the proxy repository name.
   * @param path       the path to the file.
   * @return the proxied files.
   * @throws IOException if an I/O error prevents the files from being obtained.
   */
  private List<StorageFile> getFiles(String repository, String path) throws IOException {
    List<StorageFile> files = new ArrayList<>();
    Set<String> proxied = proxies.get(repository);

    for (String repo : proxied) {
      Optional<StorageFile> file = storage.getFile(repo, path);
      file.ifPresent(files::add);
    }

    return files;
  }

  /**
   * Determines if the path is for a metadata file.
   *
   * @param path the path to check.
   * @return {@code true} if a metadatafile or {@code false} if not.
   */
  private boolean isMetadata(String path) {
    return path.contains("/maven-metadata.xml");
  }

  /**
   * Gets the metadata for the specified path.
   *
   * @param repository the name of the proxy repository.
   * @param path       the path to the file.
   * @return the metadata.
   */
  private Metadata getMetadata(String repository, String path) {
    String metadataPath = path;

    if (!path.endsWith(".xml")) {
      int index = path.lastIndexOf('.');
      metadataPath = path.substring(0, index);
    }

    return this.metadata.get(repository, metadataPath);
  }

  /**
   * Gets the metadata for the cache.
   *
   * @param repository the name of the repository.
   * @param path       the path to the file.
   * @return the metadata.
   */
  private Metadata buildMetadata(String repository, String path) {
    try {
      String groupId = null;
      String artifactId = null;
      String latest = null;
      Semver release = null;
      Set<Semver> versions = new HashSet<>();
      LocalDateTime created = null;
      OffsetDateTime lastUpdated = null;

      for (StorageFile file : getFiles(repository, path)) {
        Metadata metadata = createMetadata(file);
        groupId = metadata.getGroupId();
        artifactId = metadata.getArtifactId();

        if (created == null || file.getCreated() != null && file.getCreated().isAfter(created)) {
          created = file.getCreated();

          if (metadata.getVersioning() != null && metadata.getVersioning().getLatest() != null) {
            latest = metadata.getVersioning().getLatest();
          }
        }

        if (metadata.getVersioning() != null && metadata.getVersioning().getRelease() != null) {
          Semver version = new Semver(metadata.getVersioning().getRelease());

          if (release == null || version.isGreaterThan(release)) {
            release = version;
          }
        }

        if (metadata.getVersioning() != null && metadata.getVersioning().getLastUpdated() != null) {
          OffsetDateTime time =
              dateFormat.parse(metadata.getVersioning().getLastUpdated(), this::createTime);

          if (lastUpdated == null || time.isAfter(lastUpdated)) {
            lastUpdated = time;
          }
        }

        if (metadata.getVersioning() != null && metadata.getVersioning().getVersion() != null) {
          for (String version : metadata.getVersioning().getVersion()) {
            versions.add(new Semver(version));
          }
        }
      }

      Metadata metadata = new Metadata();
      metadata.setGroupId(groupId);
      metadata.setArtifactId(artifactId);
      metadata.setCreated(created);
      Versioning versioning = new Versioning();
      metadata.setVersioning(versioning);
      versioning.setLatest(latest);

      if (release != null) {
        versioning.setRelease(release.toString());
      }

      if (lastUpdated != null) {
        versioning.setLastUpdated(lastUpdated.format(dateFormat));
      }

      versioning.setVersion(versions.stream()
          .sorted()
          .map(Semver::toString)
          .toArray(String[]::new));

      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      mapper.writeValue(buffer, metadata);
      byte[] data = buffer.toByteArray();
      metadata.setSize(data.length);
      metadata.setMd5(digest(data, "MD5"));
      metadata.setSha1(digest(data, "SHA-1"));
      metadata.setSha256(digest(data, "SHA-256"));
      metadata.setSha512(digest(data, "SHA-512"));

      return metadata;
    } catch (IOException e) {
      throw new RuntimeException("Failed to build metadata", e);
    }
  }

  /**
   * Creates a UTC time from a temporal accessor.
   *
   * @param temporal the temporal accessor.
   * @return the time.
   */
  private OffsetDateTime createTime(TemporalAccessor temporal) {
    return OffsetDateTime.of(
        temporal.get(ChronoField.YEAR),
        temporal.get(ChronoField.MONTH_OF_YEAR),
        temporal.get(ChronoField.DAY_OF_MONTH),
        temporal.get(ChronoField.HOUR_OF_DAY),
        temporal.get(ChronoField.MINUTE_OF_HOUR),
        temporal.get(ChronoField.SECOND_OF_MINUTE),
        0, ZoneOffset.UTC);
  }

  /**
   * Calculates the digest for a file.
   *
   * @param data      the file content.
   * @param algorithm the hash algorithm.
   * @return the hex-encoded digest.
   * @throws IOException if the digest could not be calculated.
   */
  private byte[] digest(byte[] data, String algorithm) throws IOException {
    StringBuilder hex = new StringBuilder();

    try {
      MessageDigest digest = MessageDigest.getInstance(algorithm);

      for (byte b : digest.digest(data)) {
        hex.append(String.format("%02x", ((int) b) & 0xff));
      }
    } catch (Exception e) {
      throw new IOException("Failed to calculate digest", e);
    }

    return hex.toString().getBytes(StandardCharsets.US_ASCII);
  }

  /**
   * Creates the metadata for a file.
   *
   * @param file the file.
   * @return the metadata.
   * @throws IOException if an I/O error prevents the metadata from being created.
   */
  private Metadata createMetadata(StorageFile file) throws IOException {
    try (InputStream input = storage.readFile(file.getRepository(), file.getPath())) {
      return mapper.readValue(input, Metadata.class);
    }
  }

  @Data
  @NoArgsConstructor
  @JacksonXmlRootElement(localName = "metadata")
  public static final class Metadata implements Serializable {

    private String groupId;
    private String artifactId;
    private Versioning versioning;
    @JsonIgnore
    private byte[] md5;
    @JsonIgnore
    private byte[] sha1;
    @JsonIgnore
    private byte[] sha256;
    @JsonIgnore
    private byte[] sha512;
    @JsonIgnore
    private long size;
    @JsonIgnore
    private LocalDateTime created;
  }

  @Data
  @NoArgsConstructor
  public static final class Versioning implements Serializable {

    private String latest;
    private String release;
    private String lastUpdated;
    @JacksonXmlElementWrapper(localName = "versions")
    private String[] version;
  }
}
