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

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import java.security.Principal;
import java.util.Arrays;
import java.util.Map;

/**
 * {@code MavenIdentityManager} is an implementation of {@link IdentityManager} based on a static
 * map of user names and passwords.
 */
public class MavenIdentityManager implements IdentityManager {

  private final Map<String, char[]> credentials;

  /**
   * Creates a new instance of {@code MavenIdentityManager}.
   *
   * @param credentials the map of user credentials.
   */
  public MavenIdentityManager(Map<String, char[]> credentials) {
    this.credentials = credentials;
  }

  @Override
  public Account verify(Account account) {
    return account;
  }

  @Override
  public Account verify(String id, Credential credential) {
    Account account = getAccount(id);

    if (account != null && verifyCredential(account, credential)) {
      return account;
    }

    return null;
  }

  @Override
  public Account verify(Credential credential) {
    return null;
  }

  /**
   * Gets the account for the specified user ID.
   *
   * @param id the user ID.
   * @return the account or {@code null} if the user does not exist.
   */
  private Account getAccount(String id) {
    char[] password = credentials.get(id);

    if (password != null) {
      Principal principal = new MavenPrincipal(id);
      return new MavenAccount(principal);
    }

    return null;
  }

  /**
   * Verifies the credentials for an account.
   *
   * @param account    the account.
   * @param credential the credential to verify.
   * @return {@code true} if verified or {@code false} if not.
   */
  private boolean verifyCredential(Account account, Credential credential) {
    if (credential instanceof PasswordCredential) {
      char[] password = ((PasswordCredential) credential).getPassword();
      char[] expected = credentials.get(account.getPrincipal().getName());
      char[] actual = BCrypt.withDefaults().hashToChar(6, password);
      return Arrays.equals(expected, actual);
    }

    return false;
  }
}
