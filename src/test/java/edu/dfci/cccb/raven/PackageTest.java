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
package edu.dfci.cccb.raven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.jpa.JpaPersistModule;

import edu.dfci.cccb.raven.Cran;
import edu.dfci.cccb.raven.Package;

@RunWith (JukitoRunner.class)
public class PackageTest {

  public static class ptm extends JukitoModule {
    protected void configureTest () {
      install (new ObjectMapperModule ());
      install (new JpaPersistModule ("test"));

      requestInjection (new Object () {
        @Inject
        private void startPersistence (PersistService s) {
          s.start ();
        }
      });
    }
  }

  @Test
  public void jsonWithoutSnapshots (ObjectMapper m) throws Exception {
    assertEquals (m.writeValueAsString (new Package ("foo")), "{\"name\":\"foo\"}");
  }

  @Test
  public void jsonWithSnapshots (ObjectMapper m) throws Exception {
    Package bar = new Package ("bar");
    bar = bar.with (new Cran (bar, "0.1"));
    bar = bar.with (new Cran (bar, "0.2"));
    assertTrue (m.writeValueAsString (bar).startsWith ("{\"name\":\"bar\",\"versions\":{\"0.1\":"));
  }

  @Test
  public void persistenceWithoutSnapshots (EntityManager e, ObjectMapper m) throws Exception {
    e.persist (new Package ("foo"));
    assertEquals (m.writeValueAsString (e.find (Package.class, "foo")), "{\"name\":\"foo\"}");
  }

  @Test
  public void persistenceWithSnapshots (EntityManager e, ObjectMapper m) throws Exception {
    Package bar = new Package ("bar");
    bar = bar.with (new Cran (bar, "0.1"));
    bar = bar.with (new Cran (bar, "0.2"));
    e.persist (bar);
    bar = e.find (Package.class, "bar");
    assertTrue (m.writeValueAsString (bar).startsWith ("{\"name\":\"bar\",\"versions\":{\"0.1\":"));
  }
}
