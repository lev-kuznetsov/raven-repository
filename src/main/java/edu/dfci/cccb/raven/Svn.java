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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.apache.log4j.Logger.getLogger;
import static org.jsoup.Jsoup.parse;

import java.io.IOException;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.onami.scheduler.Scheduled;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an SVN hosted package snapshot
 * 
 * @author levk
 *
 */
@Entity
@DiscriminatorValue ("svn")
public class Svn extends Snapshot {
  private static final Logger log = getLogger (Svn.class);

  /**
   * url
   */
  private @JsonProperty String url;

  /**
   * branch, infer trunk if omitted
   */
  private @JsonProperty String branch;

  /**
   * directory, infer root if omitted
   */
  private @JsonProperty String directory;

  /**
   * username, infer no auth if omitted
   */
  private @JsonProperty String username;

  /**
   * password, infer no auth if omitted
   */
  private @JsonProperty String password;

  /**
   * SVN revision
   */
  private @JsonProperty String revision;

  /**
   * @param pakage
   * @param version
   * @param url
   * @param branch
   * @param directory
   * @param username
   * @param password
   * @param revision
   * @param dependencies
   */
  public Svn (Package pakage,
              String version,
              String url,
              String branch,
              String directory,
              String username,
              String password,
              String revision,
              Snapshot... dependencies) {
    super (pakage, version, dependencies);
    this.url = url;
    this.branch = branch;
    this.directory = directory;
    this.username = username;
    this.password = password;
    this.revision = revision;
  }

  /**
   * @param pakage
   * @param version
   * @param url
   * @param branch
   * @param directory
   * @param revision
   * @param dependencies
   */
  public Svn (Package pakage,
              String version,
              String url,
              String branch,
              String directory,
              String revision,
              Snapshot... dependencies) {
    this (pakage, version, url, branch, directory, null, null, revision, dependencies);
  }

  /**
   * @param pakage
   * @param version
   * @param url
   * @param branch
   * @param revision
   * @param dependencies
   */
  public Svn (Package pakage,
              String version,
              String url,
              String branch,
              String revision,
              Snapshot... dependencies) {
    this (pakage, version, url, branch, null, revision, dependencies);
  }

  /**
   * @param pakage
   * @param version
   * @param url
   * @param revision
   * @param dependencies
   */
  public Svn (Package pakage,
              String version,
              String url,
              String revision,
              Snapshot... dependencies) {
    this (pakage, version, url, null, revision, dependencies);
  }

  /**
   * Required for JPA
   */
  @SuppressWarnings ("unused")
  private Svn () {}

  /**
   * Resolves bioconductor packages
   * 
   * @author levk
   *
   */
  private static abstract class BiocResolver extends Resolver implements Job {

    /**
     * Pull urls
     * 
     * @author levk
     *
     */
    @Retention (RUNTIME)
    @Target (TYPE)
    @Inherited
    protected @interface Bioconductor {
      /**
       * @return PACKAGES url
       */
      String packagesUrl ();

      /**
       * @return daily build page url
       */
      String buildPageUrl ();
    }

    /**
     * Resolved within the context
     */
    private Map<String, Package> resolved;

    /**
     * Context descriptors
     */
    private Map<String, String> descriptors;

    /**
     * Context snapshot constructors
     */
    private Map<String, SnapshotCreator> creators;

    @Override
    public synchronized void execute (JobExecutionContext context) throws JobExecutionException {
      try {
        String buildPageUrl = getClass ().getAnnotation (Bioconductor.class).buildPageUrl ();
        String packagesUrl = getClass ().getAnnotation (Bioconductor.class).packagesUrl ();
        Element body = parse (new URL (buildPageUrl).openStream (), null, buildPageUrl).body ();
        String svnPath = body.select ("div[class=svn_info] > table[class=svn_info] > tbody "
                                      + "> tr").get (1).select ("span[class=svn_info]").text ();
        String branch =
                        svnPath.contains ("trunk")
                                                  ? null
                                                  : svnPath
                                                           .replaceAll ("https://hedgehog.fhcrc.org/bioconductor/branches/",
                                                                        "")
                                                           .replaceAll ("/madman/Rpacks", "");

        creators =
                   body.select ("table[class=mainrep] > tbody > tr "
                                + "> td[rowspan][style*=padding-left]")
                       .stream ()
                       .collect (toMap (p -> p.select ("b")
                                              .select ("a[href*=package]")
                                              .text (),
                                        p -> {
                                          Elements spec = p.select ("b");
                                          final String name = spec.select ("a[href*=package]")
                                                                  .text ();
                                          String version = spec.text ()
                                                               .replaceAll (name, "")
                                                               .replaceAll ("[^0-9\\.-]",
                                                                            "");
                                          String revision = p.select ("table[class=svn_info]")
                                                             .select ("td[class=svn_info]")
                                                             .select ("span[class=svn_info]")
                                                             .first ()
                                                             .text ();
                                          return (Package pkg, String v, Snapshot... d) -> new Svn (pkg,
                                                                                                    version,
                                                                                                    "https://hedgehog.fhcrc.org/bioconductor",
                                                                                                    branch,
                                                                                                    "madman/Rpacks/"
                                                                                                            + name,
                                                                                                    "readonly",
                                                                                                    "readonly",
                                                                                                    revision,
                                                                                                    d);
                                        }));
        descriptors = asList (IOUtils.toString (new URL (packagesUrl).openStream ())
                                     .split ("(?m)^$")).stream ()
                                                       .filter (d -> !"".equals (d.trim ()))
                                                       .collect (toMap (d -> {
                                                         String p = d.substring (d.indexOf ("Package:") + 8).trim ();
                                                         return p.substring (0, p.indexOf ('\n'));
                                                       }, d -> d));
        resolved = new HashMap<> ();
        int count = descriptors.entrySet ().stream ().filter (e -> creators.containsKey (e.getKey ())).map (e -> {
          try {
            resolved.put (e.getKey (), resolve (creators.get (e.getKey ()), e.getValue ()));
            return 1;
          } catch (RavenException ex) {
            return 0;
          }
        }).reduce ( (a, b) -> a + b).orElse (0);
        log.info ("Resolved " + count + " out of " + descriptors.size () + " packages for bioconductor");
      } catch (IOException e) {
        throw new JobExecutionException (e);
      }
    }

    /* (non-Javadoc)
     * @see edu.dfci.cccb.raven2.Snapshot.Resolver#lookup(java.lang.String) */
    @Override
    protected Package lookup (String name) {
      return ofNullable (resolved.get (name)).orElseGet ( () -> {
        String descriptor = descriptors.get (name);
        Package p = descriptor == null ? super.lookup (name) : resolve (creators.get (name), descriptor);
        resolved.put (name, p);
        return p;
      });
    }
  }

  /**
   * Resolves bioconductor devel packages
   * 
   * @author levk
   *
   */
  @Scheduled (cronExpression = "0 10 2 * * ?")
  @BiocResolver.Bioconductor (packagesUrl = "http://bioconductor.org/packages/devel/bioc/src/contrib/PACKAGES",
                              buildPageUrl = "http://bioconductor.org/checkResults/devel/bioc-LATEST/")
  public static class BiocDevelResolver extends BiocResolver {

    @Override
    public String toString () {
      return "bioconductor devel";
    }
  }

  /**
   * Resolves bioconductor release packages
   * 
   * @author levk
   *
   */
  @Scheduled (cronExpression = "0 20 2 * * ?")
  @BiocResolver.Bioconductor (packagesUrl = "http://bioconductor.org/packages/release/bioc/src/contrib/PACKAGES",
                              buildPageUrl = "http://bioconductor.org/checkResults/release/bioc-LATEST")
  public static class BiocReleaseResolver extends BiocResolver {

    @Override
    public String toString () {
      return "bioconductor release";
    }
  }
}
