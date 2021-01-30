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

package com.jshobe.maven.handler;

import com.jshobe.maven.storage.Storage;
import com.jshobe.maven.storage.StorageFile;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@code DirectoryHandler} handles serving directory listings.
 */
@Slf4j
public class DirectoryHandler implements HttpHandler {

  private final Storage storage;
  private final Template template;
  private final NumberFormat numberFormat;
  private final DateTimeFormatter dateFormat;

  /**
   * Creates a new instance of {@code DirectoryHandler}.
   *
   * @param storage the storage provider.
   */
  public DirectoryHandler(Storage storage) {
    this.storage = storage;
    this.template = createTemplate();
    this.numberFormat = new DecimalFormat("0");
    this.dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    StorageFile directory = exchange.getAttachment(Keys.FILE);
    List<Line> lines = storage.listDirectory(directory.getRepository(), directory.getPath())
        .stream()
        .sorted(Comparator.comparing(StorageFile::isDirectory).thenComparing(StorageFile::getPath))
        .map(this::createLine)
        .collect(Collectors.toList());
    Map<String, Object> data = new HashMap<>();
    data.put("showParent", !directory.getPath().isEmpty());
    data.put("path", directory.getPath().isEmpty() ? "/" : directory.getPath());
    data.put("files", lines);

    exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "text/html");

    try (Writer writer = getWriter(exchange)) {
      template.execute(data, writer);
    }
  }

  /**
   * Creates a directory listing line for the specified file.
   *
   * @param file the file.
   * @return the line.
   */
  private Line createLine(StorageFile file) {
    String link = "/" + file.getRepository() + "/" + file.getPath();

    String label;
    int index = file.getPath().lastIndexOf('/');

    if (index < 0) {
      label = file.getPath();
    } else {
      label = file.getPath().substring(index + 1);
    }

    if (file.isDirectory()) {
      label = label + "/";
      link = link + "/";
    }

    String date;

    if (file.getCreated() == null) {
      date = "";
    } else {
      date = dateFormat.format(file.getCreated().atZone(ZoneId.of("UTC")));
    }

    String size;

    if (file.isDirectory()) {
      size = "-";
    } else {
      size = numberFormat.format(file.getSize());
    }

    Line line = new Line();
    line.setLabel(label);
    line.setLink(link);
    line.setDate(date);
    line.setSize(size);
    return line;
  }

  /**
   * Creates the Mustache template for the directory listing.
   *
   * @return the template.
   */
  private Template createTemplate() {
    try (Reader reader = openTemplate()) {
      return Mustache.compiler().compile(reader);
    } catch (Exception e) {
      log.error("Failed to load the directory listing template", e);
      return Mustache.compiler().compile("Error loading directory listing");
    }
  }

  /**
   * Opens the Mustache template for the directory listing.
   *
   * @return a reader from which the template can be read.
   */
  private Reader openTemplate() {
    return new BufferedReader(new InputStreamReader(
        getClass().getResourceAsStream("directory.html"), StandardCharsets.UTF_8));
  }

  /**
   * Gets the writer for the HTTP response.
   *
   * @param exchange the HTTP request/response exchange.
   * @return the response writer.
   */
  private Writer getWriter(HttpServerExchange exchange) {
    return new PrintWriter(new OutputStreamWriter(
        exchange.getOutputStream(), StandardCharsets.UTF_8));
  }

  @Data
  @NoArgsConstructor
  private static final class Line {

    private String label;
    private String link;
    private String date;
    private String size;
  }
}
