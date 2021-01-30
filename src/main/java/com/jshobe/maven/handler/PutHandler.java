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
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import java.io.InputStream;

/**
 * {@code PutHandler} handles PUT requests.
 */
public class PutHandler implements HttpHandler {

  private final Storage storage;

  /**
   * Creates a new instance of {@code PutHandler}.
   *
   * @param storage the storage provider.
   */
  public PutHandler(Storage storage) {
    this.storage = storage;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    String repository = exchange.getAttachment(Keys.REPOSITORY);
    String path = exchange.getAttachment(Keys.PATH);
    String parentPath = getParentPath(path);

    if (parentPath != null && !storage.exists(repository, parentPath)) {
      storage.createDirectory(repository, parentPath);
    }

    exchange.startBlocking();

    if (exchange.isInIoThread()) {
      exchange.dispatch(this::writeFile);
    } else {
      this.writeFile(exchange);
    }
  }

  private void writeFile(HttpServerExchange exchange) throws Exception {
    String repository = exchange.getAttachment(Keys.REPOSITORY);
    String path = exchange.getAttachment(Keys.PATH);

    try (InputStream input = exchange.getInputStream()) {
      storage.writeFile(repository, path, input);
    }
  }

  private String getParentPath(String path) {
    int index = path.lastIndexOf('/');

    if (index < 0) {
      return null;
    }

    return path.substring(0, index);
  }
}
