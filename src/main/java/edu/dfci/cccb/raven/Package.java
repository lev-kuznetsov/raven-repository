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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static java.time.LocalDate.parse;
import static java.util.Arrays.asList;
import static java.util.Collections.reverseOrder;
import static java.util.stream.Collectors.toMap;
import static javax.persistence.CascadeType.ALL;
import static javax.persistence.InheritanceType.JOINED;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.OneToMany;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a package
 * 
 * @author levk
 *
 */
@Entity
@Inheritance (strategy = JOINED)
@JsonInclude (NON_EMPTY)
public class Package {

  /**
   * Package name
   */
  private @Id String name;

  /**
   * Snapshots of the package versions
   */
  private @OneToMany (cascade = ALL) Set<Snapshot> snapshots = new HashSet<> ();

  /**
   * @param snapshots
   */
  public Package (String name) {
    this.name = name;
  }

  /**
   * Required for JPA
   */
  @SuppressWarnings ("unused")
  private Package () {}

  /**
   * @param snapshots
   * @return package with new snapshots
   */
  public Package with (Snapshot... snapshots) {
    Package result = new Package (name);
    result.snapshots.addAll (this.snapshots);
    result.snapshots.addAll (asList (snapshots));
    return result;
  }

  /**
   * @return name
   */
  @JsonProperty
  public String name () {
    return name;
  }

  /**
   * @param spec version name or ISO date
   * @return snapshot
   * @throws VersionNotFoundException
   */
  @GET
  @Path ("/{spec}")
  public Snapshot snapshot (@PathParam ("spec") String spec) throws VersionNotFoundException {
    Optional<Snapshot> result = snapshots.stream ().filter (s -> spec.equals (s.version ())).findAny ();
    if (result.isPresent ())
      return result.get ();
    try {
      LocalDate date = parse (spec);
      return snapshots.stream ().sorted (reverseOrder ())
                      .filter (s -> !date.isBefore (s.date ())).findFirst ()
                      .orElseThrow ( () -> {
                        return new VersionNotFoundException ("Unable to find version dated before " + date);
                      });
    } catch (DateTimeParseException e) {
      throw new VersionNotFoundException ("Unable to find version " + spec);
    }
  }

  /**
   * @return available versions with corresponding snapshot dates
   */
  @GET
  @JsonProperty ("versions")
  public Map<String, String> available () {
    return snapshots.stream ().collect (toMap (s -> s.version (), s -> s.date ().toString (), (u, v) -> {
      throw new IllegalArgumentException ("Duplicate version");
    }, LinkedHashMap::new));
  }
}
