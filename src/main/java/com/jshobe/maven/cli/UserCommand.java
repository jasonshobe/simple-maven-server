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

package com.jshobe.maven.cli;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.jshobe.maven.config.Config;
import com.jshobe.maven.config.MavenConfig;
import com.jshobe.maven.config.UserConfig;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import picocli.CommandLine;

/**
 * {@code UserCommand} is the command line command for adding or updating a user.
 */
@Slf4j
@CommandLine.Command(
    name = "user", description = "Adds or updates a user", mixinStandardHelpOptions = true)
public class UserCommand implements Callable<Integer> {

  @CommandLine.Parameters(
      index = "0", paramLabel = "username",
      description = "The name of the user to create or update")
  String username;

  @Override
  public Integer call() {
    Console console = System.console();

    if (console == null) {
      log.error("No console available");
      return 1;
    }

    char[] password;

    while (true) {
      if ((password = readPassword(console)) != null) {
        break;
      }
    }

    try {
      password = hashPassword(password);
    } catch (Exception e) {
      log.error("Failed to hash password", e);
      return 2;
    }

    try {
      saveUser(username, password);
    } catch (Exception e) {
      log.error("Failed to update configuration file", e);
      return 3;
    }

    return 0;
  }

  /**
   * Reads and verifies a password from the console.
   *
   * @param console the console.
   * @return the password or {@code null} if not verified.
   */
  private char[] readPassword(Console console) {
    char[] password = console.readPassword("%s", "Password:");

    if (password == null || password.length == 0) {
      console.writer().println("You must enter a password");
      return null;
    }

    char[] verify = console.readPassword("%s", "Verify password:");

    if (verify == null || verify.length != password.length || !Arrays.equals(password, verify)) {
      console.writer().println("The passwords don't match");
      return null;
    }

    return password;
  }

  /**
   * Hashes a password using the Bcrypt algorithm.
   *
   * @param password the clear text password.
   * @return the hashed password.
   */
  private char[] hashPassword(char[] password) {
    return BCrypt.withDefaults().hashToChar(6, password);
  }

  /**
   * Saves a user to the configuration file.
   *
   * @param username the user name.
   * @param password the Bcrypt-hashed password.
   * @throws Exception if the configuration could not be updated.
   */
  private void saveUser(String username, char[] password) throws Exception {
    File file = new File("maven.yaml").getAbsoluteFile();
    Config config;

    if (file.exists()) {
      try (InputStream input = new FileInputStream(file)) {
        config = new Yaml().loadAs(input, Config.class);
      }
    } else {
      config = new Config();
    }

    if (config.getMaven() == null) {
      config.setMaven(new MavenConfig());
    }

    List<UserConfig> users = config.getMaven().getUsers();

    if (users == null) {
      users = new ArrayList<>();
      config.getMaven().setUsers(users);
    }

    boolean found = false;

    for (UserConfig user : users) {
      if (username.equals(user.getName())) {
        found = true;
        user.setPassword("bcrypt:" + new String(password));
      }
    }

    if (!found) {
      UserConfig user = new UserConfig();
      user.setName(username);
      user.setPassword("bcrypt:" + new String(password));
      users.add(user);
    }

    try (Writer writer = new OutputStreamWriter(new FileOutputStream(file))) {
      DumperOptions options = new DumperOptions();
      options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
      Representer representer = new SkipNullsRepresenter();
      new Yaml(representer, options).dump(config, writer);
    }
  }

  private static final class SkipNullsRepresenter extends Representer {

    @Override
    protected NodeTuple representJavaBeanProperty(Object javaBean, Property property,
        Object propertyValue, Tag customTag) {
      if (propertyValue == null) {
        return null;
      }

      return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
    }

    @Override
    protected MappingNode representJavaBean(Set<Property> properties, Object javaBean) {
      if (!classTags.containsKey(javaBean.getClass())) {
        addClassTag(javaBean.getClass(), Tag.MAP);
      }

      return super.representJavaBean(properties, javaBean);
    }
  }
}
