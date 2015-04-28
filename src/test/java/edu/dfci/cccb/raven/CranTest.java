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

import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.jpa.JpaPersistModule;

@RunWith (JukitoRunner.class)
public class CranTest {

  public static class ctm extends JukitoModule {
    protected void configureTest () {
      install (new ObjectMapperModule ());
      install (new JpaPersistModule ("test"));

      requestInjection (new Object () {
        @Inject
        private void startPersistence (PersistService s) {
          s.start ();
        }
      });

      bind (Cran.CranResolver.class);
      bind (Repository.class);
    }
  }

  @Test
  public void cran (Cran.CranResolver c, EntityManager e, Repository r) throws Exception {
    c.execute (null);
    assertNotNull (e.find (Package.class, "NMF"));
  }
}
