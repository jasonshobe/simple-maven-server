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

import java.io.File;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;

/**
 * {@code MavenConfiguration} provides the configuration options for the Maven server. It check for
 * properties in the following order:
 *
 * <ol>
 *    <li>System properties</li>
 *    <li>Environment variables</li>
 *    <li>The {@code maven.yaml} file</li>
 * </ol>
 * <p>
 * Note that environment variables are converted from snake case to lower case, dot-separated
 * strings. For example, the {@code maven.storage.s3} property corresponds to the
 * {@code MAVEN_STORAGE_S3} environment variable.
 */
@Slf4j
public class MavenConfiguration extends CompositeConfiguration {

  /**
   * Creates a new instance of {@code MavenConfiguration}.
   */
  public MavenConfiguration() {
    addConfiguration(new SystemConfiguration());
    addConfiguration(new EnvConfiguration());
    addConfigurationFile();
    addDefaultProperties();
  }

  /**
   * Adds the configuration file.
   */
  private void addConfigurationFile() {
    try {
      File file = new File("maven.yaml");
      addConfiguration(new Configurations().fileBased(YAMLConfiguration.class, file));
    } catch (Exception e) {
      log.error("Failed to load configuration file", e);
    }
  }

  /**
   * Adds the default properties.
   */
  private void addDefaultProperties() {
    YAMLConfiguration configuration = new YAMLConfiguration();
    org.apache.commons.configuration2.io.FileHandler handler =
        new org.apache.commons.configuration2.io.FileHandler(configuration);

    try (InputStream input = getClass().getResourceAsStream("/maven.yaml")) {
      handler.load(input);
    } catch (Exception e) {
      log.error("Failed to load default properties", e);
    }

    addConfiguration(configuration);
  }
}
