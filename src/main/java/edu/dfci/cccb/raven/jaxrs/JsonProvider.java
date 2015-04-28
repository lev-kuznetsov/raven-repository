/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.dfci.cccb.raven.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON protocol provider
 * 
 * @author levk
 *
 */
@Provider
@Singleton
public class JsonProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

  /**
   * Mapper
   */
  private @Inject ObjectMapper mapper;

  /* (non-Javadoc)
   * @see javax.ws.rs.ext.MessageBodyWriter#getSize(java.lang.Object,
   * java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[],
   * javax.ws.rs.core.MediaType) */
  @Override
  public long getSize (Object arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
    return -1;
  }

  /* (non-Javadoc)
   * @see javax.ws.rs.ext.MessageBodyWriter#isWriteable(java.lang.Class,
   * java.lang.reflect.Type, java.lang.annotation.Annotation[],
   * javax.ws.rs.core.MediaType) */
  @Override
  public boolean isWriteable (Class<?> type, Type arg1, Annotation[] arg2, MediaType arg3) {
    return mapper.canSerialize (type);
  }

  /* (non-Javadoc)
   * @see javax.ws.rs.ext.MessageBodyWriter#writeTo(java.lang.Object,
   * java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[],
   * javax.ws.rs.core.MediaType, javax.ws.rs.core.MultivaluedMap,
   * java.io.OutputStream) */
  @Override
  public void writeTo (Object value,
                       Class<?> arg1,
                       Type arg2,
                       Annotation[] arg3,
                       MediaType arg4,
                       MultivaluedMap<String, Object> arg5,
                       OutputStream out) throws IOException, WebApplicationException {
    mapper.writeValue (out, value);
  }

  /* (non-Javadoc)
   * @see javax.ws.rs.ext.MessageBodyReader#isReadable(java.lang.Class,
   * java.lang.reflect.Type, java.lang.annotation.Annotation[],
   * javax.ws.rs.core.MediaType) */
  @Override
  public boolean isReadable (Class<?> arg0, Type type, Annotation[] arg2, MediaType arg3) {
    return mapper.canDeserialize (mapper.getTypeFactory ().constructType (type));
  }

  /* (non-Javadoc)
   * @see javax.ws.rs.ext.MessageBodyReader#readFrom(java.lang.Class,
   * java.lang.reflect.Type, java.lang.annotation.Annotation[],
   * javax.ws.rs.core.MediaType, javax.ws.rs.core.MultivaluedMap,
   * java.io.InputStream) */
  @Override
  public Object readFrom (Class<Object> arg0,
                          Type type,
                          Annotation[] arg2,
                          MediaType arg3,
                          MultivaluedMap<String, String> arg4,
                          InputStream src) throws IOException, WebApplicationException {
    return mapper.readValue (src, mapper.getTypeFactory ().constructType (type));
  }
}
