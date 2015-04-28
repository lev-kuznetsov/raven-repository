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

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.google.inject.persist.Transactional;

/**
 * Repository
 *
 * @author levk
 *
 */
@Path ("/repository")
public class Repository {

  /**
   * JPA DA
   */
  private @Inject Provider<EntityManager> manager;

  /**
   * @param name
   * @return package
   * @throws PackageNotFoundException
   */
  @Path ("/{package}")
  @Transactional
  public Package get (@PathParam ("package") String name) throws PackageNotFoundException {
    try {
      return ofNullable (manager.get ().find (Package.class, name)).orElseThrow ( () -> new PackageNotFoundException ("No package named "
                                                                                                                      + name
                                                                                                                      + " found"));
    } catch (IllegalArgumentException e) {
      throw new PackageNotFoundException ("No package named " + name + " found");
    }
  }

  /**
   * @return package names
   */
  @GET
  @Transactional
  public Collection<String> available () {
    return manager.get ().createQuery ("SELECT o FROM Package o", Package.class)
                  .getResultList ().stream ().map (p -> p.name ()).collect (toList ());
  }
}
