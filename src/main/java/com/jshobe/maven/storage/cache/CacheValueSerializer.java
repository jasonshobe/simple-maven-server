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

package com.jshobe.maven.storage.cache;

import java.io.IOException;
import java.io.Serializable;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.elsa.ElsaSerializerPojo;
import org.mapdb.serializer.GroupSerializerObjectArray;

/**
 * {@code CacheValueSerializer} is a cache serializer for instances of {@link CacheValue}.
 *
 * @param <V> the type of value.
 */
public class CacheValueSerializer<V extends Serializable> extends
    GroupSerializerObjectArray<CacheValue<V>> {

  private final ElsaSerializerPojo serializer = new ElsaSerializerPojo();

  @Override
  public void serialize(DataOutput2 out, CacheValue<V> value) throws IOException {
    serializer.serialize(out, value);
  }

  @Override
  public CacheValue<V> deserialize(DataInput2 input, int available) throws IOException {
    return serializer.deserialize(input);
  }
}
