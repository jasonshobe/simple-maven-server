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
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;

/**
 * {@code FileHandler} handles serving files.
 */
public class FileHandler implements HttpHandler {

  private final Storage storage;

  /**
   * Creates a new instanceof {@code FileHandler}.
   *
   * @param storage the storage provider.
   */
  public FileHandler(Storage storage) {
    this.storage = storage;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    StorageFile file = exchange.getAttachment(Keys.FILE);
    exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, getContentType(file));
    exchange.getResponseHeaders().add(Headers.CONTENT_LENGTH, file.getSize());

    try (InputStream input = storage.readFile(file.getRepository(), file.getPath());
        OutputStream output = exchange.getOutputStream()) {
      byte[] buffer = new byte[1024];
      int len;

      while ((len = input.read(buffer)) >= 0) {
        output.write(buffer, 0, len);
      }
    }
  }

  /**
   * Gets the content type for a file.
   *
   * @param file the file.
   * @return the content MIME type.
   */
  private String getContentType(StorageFile file) {
    String type = URLConnection.guessContentTypeFromName(file.getPath());
    return type == null ? "application/octet-stream" : type;
  }
}
