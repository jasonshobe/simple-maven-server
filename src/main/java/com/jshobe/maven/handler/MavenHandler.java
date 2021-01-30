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
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import java.util.Collections;
import java.util.List;

/**
 * {@code MavenHandler} is the base HTTP handler for the Maven server.
 */
public class MavenHandler implements HttpHandler {

  private final HttpHandler get;
  private final HttpHandler put;

  /**
   * Creates a new instance of {@code MavenHandler}.
   *
   * @param storage         the storage provider.
   * @param identityManager the identity manager for the users that are allowed to write to the
   *                        repository.
   */
  public MavenHandler(Storage storage, IdentityManager identityManager) {
    get = new GetHandler(storage);
    put = createPutHandler(storage, identityManager);
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    String repository = getRepository(exchange);
    String path = getPath(exchange);

    if (repository.isEmpty()) {
      exchange.setStatusCode(StatusCodes.NOT_FOUND);
      exchange.getResponseSender().send("Not found.");
      return;
    }

    exchange.putAttachment(Keys.REPOSITORY, repository);
    exchange.putAttachment(Keys.PATH, path);

    if (exchange.getRequestMethod().equalToString("GET")) {
      get.handleRequest(exchange);
    } else if (exchange.getRequestMethod().equalToString("PUT")) {
      put.handleRequest(exchange);
    } else {
      exchange.setStatusCode(StatusCodes.METHOD_NOT_ALLOWED);
      exchange.getResponseHeaders().add(Headers.ALLOW, "GET, PUT");
      exchange.getResponseSender().send("Method not allowed.");
    }
  }

  /**
   * Creates the handler for PUT requests.
   *
   * @param storage         the storage provider.
   * @param identityManager the identity manager for the users that are allowed to write to the
   *                        repository.
   * @return the PUT handler.
   */
  private HttpHandler createPutHandler(Storage storage, IdentityManager identityManager) {
    HttpHandler handler = new PutHandler(storage);
    handler = new AuthenticationCallHandler(handler);
    handler = new AuthenticationConstraintHandler(handler);
    List<AuthenticationMechanism> mechanisms =
        Collections.singletonList(new BasicAuthenticationMechanism("InetSoft Repository"));
    handler = new AuthenticationMechanismsHandler(handler, mechanisms);
    handler = new SecurityInitialHandler(AuthenticationMode.PRO_ACTIVE, identityManager, handler);
    return handler;
  }

  /**
   * Gets the name of the requested repository.
   *
   * @param exchange the HTTP request/response exchange.
   * @return the repository name.
   */
  private String getRepository(HttpServerExchange exchange) {
    String path = getRequestPath(exchange);
    int index = path.indexOf('/');

    if (index < 0) {
      return path;
    } else {
      return path.substring(0, index);
    }
  }

  /**
   * Gets the path to the requested file.
   *
   * @param exchange the HTTP request/response exchange.
   * @return the file path.
   */
  private String getPath(HttpServerExchange exchange) {
    String path = getRequestPath(exchange);
    int index = path.indexOf('/');

    if (index < 0) {
      return "";
    } else {
      return path.substring(index + 1);
    }
  }

  /**
   * Gets the request path.
   *
   * @param exchange the HTTP request/response exchange.
   * @return the request path.
   */
  private String getRequestPath(HttpServerExchange exchange) {
    String path = exchange.getRequestPath();

    if (path.startsWith("/")) {
      path = path.substring(1);
    }

    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }

    return path;
  }
}
