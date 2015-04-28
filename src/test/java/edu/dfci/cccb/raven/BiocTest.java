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

import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.jpa.JpaPersistModule;

import edu.dfci.cccb.raven.Package;
import edu.dfci.cccb.raven.Svn;

@RunWith (JukitoRunner.class)
public class BiocTest {

  public static class btm extends JukitoModule {
    protected void configureTest () {
      install (new ObjectMapperModule ());
      install (new JpaPersistModule ("test"));

      requestInjection (new Object () {
        @Inject
        private void startPersistence (PersistService s) {
          s.start ();
        }
      });

      bind (Svn.BiocDevelResolver.class);
      bind (Svn.BiocReleaseResolver.class);
    }
  }

  @Test
  public void bioc (Svn.BiocDevelResolver d, Svn.BiocReleaseResolver r, EntityManager e) throws Exception {
    d.execute (null);
    r.execute (null);
    assertTrue (e.find (Package.class, "limma").available ().size () == 2);
  }
}
