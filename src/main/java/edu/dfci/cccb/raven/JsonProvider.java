/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */

package edu.dfci.cccb.raven;

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

@Provider
@Singleton
public class JsonProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

  private @Inject ObjectMapper mapper;

  @Override
  public long getSize (Object arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
    return -1;
  }

  @Override
  public boolean isWriteable (Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
    return true;
  }

  @Override
  public boolean isReadable (Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
    return true;
  }

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

  @Override
  public Object readFrom (Class<Object> arg0,
                          Type type,
                          Annotation[] arg2,
                          MediaType arg3,
                          MultivaluedMap<String, String> arg4,
                          InputStream in) throws IOException, WebApplicationException {
    return mapper.readValue (in, mapper.getTypeFactory ().constructType (type));
  }
}
