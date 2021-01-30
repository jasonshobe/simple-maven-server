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

import com.sun.tools.attach.VirtualMachine;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

/**
 * {@code StopCommand} is the command line command for stopping the server.
 */
@CommandLine.Command(
    name = "stop", description = "Stops the server", mixinStandardHelpOptions = true)
@Slf4j
public class StopCommand implements Callable<Integer> {

  private static final String CONNECTOR_ADDRESS =
      "com.sun.management.jmxremote.localConnectorAddress";

  @Override
  public Integer call() {
    try {
      String pid = getPid();
      stopServer(pid);
    } catch (Exception e) {
      log.error("Failed to stop the server", e);
      return 1;
    }

    return 0;
  }

  /**
   * Gets the PID of the server process.
   *
   * @return the PID.
   * @throws Exception if an error prevents the PID from being obtained.
   */
  private String getPid() throws Exception {
    Path path = Paths.get(System.getProperty("user.dir"), "maven.pid").toAbsolutePath();

    if (Files.exists(path)) {
      try (InputStream input = Files.newInputStream(path)) {
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(input, StandardCharsets.US_ASCII));
        return reader.readLine();
      } finally {
        Files.delete(path);
      }
    } else {
      throw new IllegalStateException("PID file does not exist");
    }
  }

  /**
   * Stops the Maven repository server.
   *
   * @param pid the PID of the server process.
   * @throws Exception if an error prevented the server from being stopped.
   */
  private void stopServer(String pid) throws Exception {
    VirtualMachine vm = VirtualMachine.attach(pid);
    String address = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);

    if (address == null) {
      vm.startLocalManagementAgent();
      address = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
    }

    JMXServiceURL url = new JMXServiceURL(address);
    MBeanServerConnection mbsc = JMXConnectorFactory.connect(url, null).getMBeanServerConnection();
    ObjectName name = new ObjectName("com.jshobe.maven:type=ServerControl");
    ServerControlMBean mbean = JMX.newMBeanProxy(mbsc, name, ServerControlMBean.class);
    mbean.stopServer();
  }
}
