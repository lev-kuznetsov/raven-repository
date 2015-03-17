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

import static com.google.appengine.api.datastore.KeyFactory.createKey;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import lombok.SneakyThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Text;

@Path ("/repository")
@Singleton
public class Repository {

  private @Inject AsyncDatastoreService data;

  private @Inject ObjectMapper mapper;

  @Path ("/{package}")
  @SneakyThrows (IOException.class)
  public Package get (final @PathParam ("package") String name) throws PackageNotFoundException,
                                                               InterruptedException {
    try {
      return mapper.readValue (((Text) data.get (createKey ("package", name)).get ().getProperty ("payload")).getValue (),
                               Package.class);
    } catch (ExecutionException e) {
      if (e.getCause () instanceof EntityNotFoundException)
        throw new PackageNotFoundException ("Could not find package " + name);
      else if (e.getCause () instanceof RuntimeException)
        throw (RuntimeException) e.getCause ();
      else if (e.getCause () instanceof Error)
        throw (Error) e.getCause ();
      else
        throw new UndeclaredThrowableException (e.getCause ());
    }
  }
}
