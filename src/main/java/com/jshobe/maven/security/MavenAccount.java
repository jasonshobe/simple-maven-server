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

package com.jshobe.maven.security;

import io.undertow.security.idm.Account;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import lombok.Data;

/**
 * {@code MavenAccount} is a simple implementation of {@link Account}.
 */
@Data
public class MavenAccount implements Account {

  private final Principal principal;
  private final Set<String> roles = Collections.emptySet();
}
