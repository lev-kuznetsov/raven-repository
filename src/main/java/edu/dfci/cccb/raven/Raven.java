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

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getAsyncDatastoreService;
import static com.google.appengine.api.urlfetch.URLFetchServiceFactory.getURLFetchService;
import static com.google.common.collect.ImmutableMap.of;

import javax.inject.Singleton;

import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class Raven implements Module {

  @Provides
  @Singleton
  public AsyncDatastoreService data () {
    return getAsyncDatastoreService ();
  }

  @Provides
  @Singleton
  public URLFetchService url () {
    return getURLFetchService ();
  }

  @Override
  public void configure (Binder binder) {
    binder.install (new ObjectMapperModule ());

    binder.install (new ServletModule () {

      @Override
      protected void configureServlets () {
        serve ("/*").with (GuiceContainer.class, of ("com.sun.jersey.config.property.packages",
                                                     "edu.dfci.cccb.raven"));
      }
    });
  }
}
