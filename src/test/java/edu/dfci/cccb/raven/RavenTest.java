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

import javax.inject.Singleton;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;

import edu.dfci.cccb.raven.Registry;

/**
 * @author levk
 */
public class RavenTest {

  private final LocalServiceTestHelper helper = new LocalServiceTestHelper ();
  private long before;

  @Before
  public void setup () {
    helper.setUp ();
    before = System.currentTimeMillis ();
  }

  @After
  public void cleanup () {
    System.out.println ((System.currentTimeMillis () - before) + "ms elapsed");
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService ();
    int count = 0;
    for (Entity e : ds.prepare (new Query ("package")).asIterable ()) {
      ds.delete (e.getKey ());
      count++;
    }
    System.out.println ("cleaned up " + count + " packages");
    count = 0;
    helper.tearDown ();
  }

  @Test
  public void cran () throws Exception {
    Injector i = Guice.createInjector (new Module () {
      @Provides
      @Singleton
      public AsyncDatastoreService service () {
        return DatastoreServiceFactory.getAsyncDatastoreService ();
      }

      @Provides
      @Singleton
      public URLFetchService url () {
        return URLFetchServiceFactory.getURLFetchService ();
      }

      public void configure (Binder binder) {
        binder.install (new ObjectMapperModule ());
        binder.bind (Registry.class);
      }
    });

    Registry r = i.getInstance (Registry.class);
    r.cran ();
    r.biocRelease ();
    r.biocDevel ();
  }

  @Test
  public void biocDevel () throws Exception {
    Injector i = Guice.createInjector (new Module () {
      @Provides
      @Singleton
      public AsyncDatastoreService service () {
        return DatastoreServiceFactory.getAsyncDatastoreService ();
      }

      @Provides
      @Singleton
      public URLFetchService url () {
        return URLFetchServiceFactory.getURLFetchService ();
      }

      public void configure (Binder binder) {
        binder.install (new ObjectMapperModule ());
        binder.bind (Registry.class);
      }
    });

    Registry r = i.getInstance (Registry.class);
    r.biocDevel ();
  }

  @Test
  public void biocRelease () throws Exception {
    Injector i = Guice.createInjector (new Module () {
      @Provides
      @Singleton
      public AsyncDatastoreService service () {
        return DatastoreServiceFactory.getAsyncDatastoreService ();
      }

      @Provides
      @Singleton
      public URLFetchService url () {
        return URLFetchServiceFactory.getURLFetchService ();
      }

      public void configure (Binder binder) {
        binder.install (new ObjectMapperModule ());
        binder.bind (Registry.class);
      }
    });

    Registry r = i.getInstance (Registry.class);
    r.biocRelease ();
  }
}
