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
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.util.StatusCodes;
import java.util.Optional;

/**
 * {@code GetHandler} handles GET requests.
 */
public class GetHandler implements HttpHandler {

  private final Storage storage;
  private final HttpHandler fileHandler;
  private final HttpHandler directoryHandler;

  /**
   * Creates a new instance of {@code GetHandler}.
   *
   * @param storage the storage provider.
   */
  public GetHandler(Storage storage) {
    this.storage = storage;
    fileHandler = new BlockingHandler(new FileHandler(storage));
    directoryHandler = new BlockingHandler(new DirectoryHandler(storage));
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    String repository = exchange.getAttachment(Keys.REPOSITORY);
    String path = exchange.getAttachment(Keys.PATH);
    Optional<StorageFile> file = storage.getFile(repository, path);

    if (file.isEmpty()) {
      exchange.setStatusCode(StatusCodes.NOT_FOUND);
      exchange.getResponseSender().send("Not found.");
      return;
    }

    exchange.putAttachment(Keys.FILE, file.get());

    if (file.get().isDirectory()) {
      directoryHandler.handleRequest(exchange);
    } else {
      fileHandler.handleRequest(exchange);
    }
  }
}
