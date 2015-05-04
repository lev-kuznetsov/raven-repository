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
import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.MINIMAL_CLASS;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static javax.persistence.CascadeType.ALL;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToMany;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.inject.persist.Transactional;

/**
 * Represents a package snapshot
 * 
 * @author levk
 *
 */
@JsonTypeInfo (use = MINIMAL_CLASS, include = PROPERTY, property = "type")
@JsonInclude (NON_EMPTY)
@Entity
@Inheritance (strategy = InheritanceType.JOINED)
@DiscriminatorColumn
public abstract class Snapshot implements Comparable<Snapshot> {

  /**
   * Date serializer
   * 
   * @author levk
   *
   */
  public static class DateSerializer extends JsonSerializer<LocalDate> {

    @Override
    public void serialize (LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException,
                                                                                              JsonProcessingException {
      gen.writeString (value.toString ());
    }
  }

  /**
   * Primary key
   */
  private @TableGenerator (name = "SNAPSHOT_GEN",
                           table = "ID_GEN",
                           pkColumnName = "GEN_NAME",
                           valueColumnName = "GEN_VAL",
                           allocationSize = 1) @Id @GeneratedValue (strategy = GenerationType.TABLE,
                                                                    generator = "SNAPSHOT_GEN") long id;

  /**
   * Package for this snapshot
   */
  private @Column String pakage;

  /**
   * Version string
   */
  private @Column String version;

  /**
   * Date of the snapshot
   */
  private @Column LocalDate date;

  /**
   * Dependency snapshots
   */
  private @ManyToMany (cascade = ALL) Set<Snapshot> dependencies = new HashSet<Snapshot> ();

  /**
   * @param pakage
   * @param version
   * @param dependencies
   */
  protected Snapshot (Package pakage, String version, Snapshot... dependencies) {
    this.pakage = pakage.name ();
    this.version = version;
    this.dependencies = new HashSet<> (asList (dependencies));
    date = now ();
  }

  /**
   * Required for JPA
   */
  protected Snapshot () {}

  /**
   * @return package name
   */
  @JsonProperty ("package")
  @Transient
  protected String pakage () {
    return pakage;
  }

  /**
   * @return version
   */
  @JsonProperty
  @Transient
  public String version () {
    return version;
  }

  /**
   * @return dependency map
   */
  @JsonProperty
  @Transient
  private Map<String, String> dependencies () {
    return dependencies.stream ().collect (toMap (d -> d.pakage (), d -> d.version));
  }

  /**
   * @return date
   */
  @JsonProperty
  @JsonSerialize (using = DateSerializer.class)
  @Transient
  public LocalDate date () {
    return date;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object) */
  @Override
  public boolean equals (Object obj) {
    return obj instanceof Snapshot
           && pakage ().equals (((Snapshot) obj).pakage ()) && version.equals (((Snapshot) obj).version);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode() */
  @Override
  @Transient
  public int hashCode () {
    return pakage ().hashCode () ^ version.hashCode ();
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object) */
  @Override
  public int compareTo (Snapshot o) {
    int result = o.date.compareTo (date);
    return result == 0 && !equals (o) ? 1 : result;
  }

  // Resolution

  /**
   * Resolution helper
   * 
   * @author levk
   *
   */
  protected abstract static class Resolver {
    /**
     * Do not resolve for these packages
     */
    private static final Set<String> IGNORE = new TreeSet<> (asList ("R", "base", "compiler", "tcltk", "parallel",
                                                                     "grid", "tools", "utils", "methods", "stats",
                                                                     "stats4", "splines", "graphics", "datasets",
                                                                     "grDevices"));

    /**
     * Persist on resolution
     */
    private @Inject Provider<EntityManager> manager;

    /**
     * Functional interface for snapshot creation
     * 
     * @author levk
     *
     */
    @FunctionalInterface
    protected static interface SnapshotCreator {
      Snapshot create (Package pakage, String version, Snapshot... dependencies);
    }

    /**
     * Resolves the snapshot with dependencies (and persists the package if
     * needed)
     * 
     * @param c snapshot creator
     * @param name
     * @param version
     * @param dependencies
     * @throws VersionNotFoundException
     * @throws PackageNotFoundException
     */
    private synchronized Package resolve (SnapshotCreator c,
                                          String name,
                                          String version,
                                          String... dependencies) throws VersionNotFoundException,
                                                                 PackageNotFoundException {
      EntityManager manager = this.manager.get ();
      Package pakage = manager.find (Package.class, name);
      Consumer<Package> save;
      if (pakage == null) {
        pakage = new Package (name);
        save = manager::persist;
      } else
        save = manager::merge;
      if (pakage.available ().containsKey (version))
        return pakage;
      pakage =
               pakage.with (c.create (pakage,
                                      version,
                                      asList (dependencies).stream ()
                                                           .map (p -> lookup (p))
                                                           .map (p -> p.snapshot (p.available ()
                                                                                   .entrySet ()
                                                                                   .stream ()
                                                                                   .findFirst ()
                                                                                   .orElseThrow ( () -> new VersionNotFoundException ("No suitable version "
                                                                                                                                      + "found for package "
                                                                                                                                      + p.name ()))
                                                                                   .getKey ()))
                                                           .collect (toSet ())
                                                           .toArray (new Snapshot[0])));
      save.accept (pakage);
      return pakage;
    }

    /**
     * Parses descriptor and resolves the snapshot
     * 
     * @param c
     * @param descriptor
     * @throws VersionNotFoundException
     * @throws PackageNotFoundException
     */
    @Transactional
    protected synchronized Package resolve (SnapshotCreator c, String descriptor) throws VersionNotFoundException,
                                                                                 PackageNotFoundException {
      Map<String, String> keyValuePairs = new HashMap<> ();
      Pattern keyPattern = compile ("(?m)^[A-Za-z@0-9]+:");
      for (Matcher matcher = keyPattern.matcher (descriptor); matcher.find ();) {
        String key = descriptor.substring (matcher.start (), matcher.end ()).trim ();
        String rest = descriptor.substring (matcher.end ());
        Matcher matcherOfRest = keyPattern.matcher (rest);
        String value = rest.substring (0, matcherOfRest.find () ? matcherOfRest.start () : rest.length ()).trim ();
        keyValuePairs.put (key, value);
      }
      return resolve (c,
                      keyValuePairs.get ("Package:").trim (),
                      keyValuePairs.get ("Version:").trim (),
                      of (keyValuePairs.get ("Depends:"),
                          keyValuePairs.get ("Imports:"),
                          keyValuePairs.get ("LinkingTo:")).map (e -> e == null ? new String[0] : e.split (","))
                                                           .flatMap (a -> asList (a).stream ())
                                                           .map (s -> s.split ("\\(")[0].trim ())
                                                           .filter (s -> !IGNORE.contains (s))
                                                           .collect (toSet ())
                                                           .toArray (new String[0]));
    }

    /**
     * @param name
     * @return package
     */
    protected Package lookup (String name) {
      return ofNullable (manager.get ().find (Package.class, name)).orElseThrow ( () -> new PackageNotFoundException ("No package named "
                                                                                                                      + name));
    }
  }
}
