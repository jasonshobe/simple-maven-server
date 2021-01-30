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

import com.jshobe.maven.MavenServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * {@code StartCommand} is the command line command for starting the server.
 */
@CommandLine.Command(
    name = "start", description = "Starts the server", mixinStandardHelpOptions = true)
@Slf4j
public class StartCommand implements Callable<Integer> {

  @Option(names = {"-r", "--rebuild-index"}, description = "Rebuilds the index")
  boolean rebuildIndex;

  @Override
  public Integer call() {
    try {
      MavenServer server = new MavenServer(rebuildIndex);
      server.start();
      writePidFile();
      registerMBean(server);
    } catch (Exception e) {
      log.error("Failed to start server", e);
      return 1;
    }

    return 0;
  }

  /**
   * Writes the current PID to a file.
   *
   * @throws IOException if an I/O error occurs.
   */
  private void writePidFile() throws IOException {
    long pid = ProcessHandle.current().pid();
    Path path = Paths.get(System.getProperty("user.dir"), "maven.pid").toAbsolutePath();

    try (OutputStream output = Files.newOutputStream(path)) {
      PrintWriter writer =
          new PrintWriter(new OutputStreamWriter(output, StandardCharsets.US_ASCII));
      writer.println(pid);
      writer.flush();
    }
  }

  /**
   * Registers the server control MBean.
   *
   * @param server the Maven repository server.
   * @throws Exception if the bean could not be registered.
   */
  private void registerMBean(MavenServer server) throws Exception {
    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    ObjectName name = new ObjectName("com.jshobe.maven:type=ServerControl");
    ServerControl mbean = new ServerControl(server);
    mbs.registerMBean(mbean, name);
  }
}
