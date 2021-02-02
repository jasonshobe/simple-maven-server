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

import java.io.InputStream;
import java.io.Serializable;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.mockito.ArgumentMatcher;
import org.mockito.internal.matchers.ContainsExtraTypeInfo;
import org.mockito.internal.matchers.Equality;
import software.amazon.awssdk.core.sync.RequestBody;

public class RequestBodyMatcher implements ArgumentMatcher<RequestBody>, ContainsExtraTypeInfo,
    Serializable {

  private final RequestBody wanted;
  private RequestBody cached;
  private byte[] cachedContent;

  public RequestBodyMatcher(RequestBody wanted) {
    this.wanted = wanted;
  }

  @Override
  public boolean matches(RequestBody argument) {
    if (wanted == null && argument == null) {
      return true;
    } else if (wanted != null && argument != null) {
      return Objects.equals(wanted.contentType(), argument.contentType())
          && wanted.contentLength() == argument.contentLength() && inputMatches(argument);
    }

    return false;
  }

  @Override
  public String toStringWithType() {
    StringBuilder sb = new StringBuilder("(RequestBody) ");

    if (wanted == null) {
      sb.append("null");
    } else {
      sb.append("{contentType=").append(wanted.contentType())
          .append(", contentLength=").append(wanted.contentLength())
          .append('}');
    }

    return sb.toString();
  }

  @Override
  public boolean typeMatches(Object target) {
    return wanted != null && target != null && target.getClass() == wanted.getClass();
  }

  private boolean inputMatches(RequestBody target) {
    byte[] wantedContent;
    byte[] targetContent;

    try (InputStream input = wanted.contentStreamProvider().newStream()) {
      wantedContent = IOUtils.toByteArray(input);
    } catch (Exception ignore) {
      wantedContent = new byte[0];
    }

    if (target == cached) {
      targetContent = cachedContent;
    } else {
      try (InputStream input = target.contentStreamProvider().newStream()) {
        targetContent = IOUtils.toByteArray(input);
      } catch (Exception ignore) {
        targetContent = new byte[0];
      }

      cached = target;
      cachedContent = targetContent;
    }

    return Equality.areEqual(wantedContent, targetContent);
  }
}
