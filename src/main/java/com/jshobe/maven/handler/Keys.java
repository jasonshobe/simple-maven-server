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

import com.jshobe.maven.storage.StorageFile;
import io.undertow.util.AttachmentKey;

/**
 * {@code Keys} provides the attachment keys used by the Maven HTTP handlers.
 */
public interface Keys {

  /**
   * Key for the repository name.
   */
  AttachmentKey<String> REPOSITORY = AttachmentKey.create(String.class);

  /**
   * Key for the file path.
   */
  AttachmentKey<String> PATH = AttachmentKey.create(String.class);

  /**
   * Key for the storage file.
   */
  AttachmentKey<StorageFile> FILE = AttachmentKey.create(StorageFile.class);
}
