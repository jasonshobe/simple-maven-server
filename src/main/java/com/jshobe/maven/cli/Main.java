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

import picocli.CommandLine;

/**
 * {@code Main} provides the main entry point of the application.
 */
@CommandLine.Command(
    name = "simple-maven-server", mixinStandardHelpOptions = true,
    version = "simple-maven-server 1.0.0", description = "A simple Maven repository server",
    subcommands = {StartCommand.class, StopCommand.class, UserCommand.class})
public class Main {

  /**
   * The main entry point of the application.
   *
   * @param args the command line arguments.
   */
  public static void main(String[] args) {
    int exitCode = new CommandLine(new Main()).execute(args);

    if(exitCode != 0) {
      System.exit(exitCode);
    }
  }
}
