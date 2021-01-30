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

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.configuration2.MapConfiguration;

/**
 * {@code EnvConfiguration} is a configuration implementation that loads the properties from the
 * environment variables. It differs from {@link org.apache.commons.configuration2.EnvironmentConfiguration}
 * in that it converts properties from snake case (e.g. {@code ENV_VAR_NAME}) to lower-case,
 * dot-separated names (e.g. {@code env.var.name}).
 */
public class EnvConfiguration extends MapConfiguration {

  /**
   * Creates a new instance of {@code EnvConfiguration}.
   */
  public EnvConfiguration() {
    super(loadEnvironment());
  }

  /**
   * Loads the environment variables.
   *
   * @return the map of environment variables.
   */
  private static Map<String, Object> loadEnvironment() {
    Map<String, Object> map = new HashMap<>();

    for (Map.Entry<String, String> e : System.getenv().entrySet()) {
      String key = e.getKey().toLowerCase().replace('_', '.');
      map.put(key, e.getValue());
    }

    return map;
  }

  @Override
  protected void addPropertyDirect(String key, Object value) {
    throw new UnsupportedOperationException("Configuration is read-only");
  }

  @Override
  protected void clearPropertyDirect(String key) {
    throw new UnsupportedOperationException("Configuration is read-only");
  }

  @Override
  protected void clearInternal() {
    throw new UnsupportedOperationException("Configuration is read-only");
  }
}
