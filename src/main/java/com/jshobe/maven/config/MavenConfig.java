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

package com.jshobe.maven.config;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@code MavenConfig} contains the Maven repository configuration properties.
 */
@Data
@NoArgsConstructor
public class MavenConfig {

  /**
   * The storage configuration.
   */
  private StorageConfig storage;

  /**
   * The HTTP port number.
   */
  private int port = 8080;

  /**
   * The list of hosted repository names.
   */
  private List<String> repositories;

  /**
   * The list of users.
   */
  private List<UserConfig> users;

  /**
   * The list of proxy repositories.
   */
  private List<ProxyConfig> proxies;
}
