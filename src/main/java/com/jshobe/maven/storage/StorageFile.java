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

package com.jshobe.maven.storage;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

/**
 * {@code StorageFile} represents a file in storage.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageFile implements Serializable {

  /**
   * A flag indicating if the storage object is a directory.
   */
  private boolean directory;

  /**
   * The name of the repository that contains the storage object.
   */
  @With
  private String repository;

  /**
   * The path to the storage object.
   */
  private String path;

  /**
   * The size of the storage object in bytes.
   */
  private long size;

  /**
   * The date and time at which the storage object was created.
   */
  private LocalDateTime created;
}
