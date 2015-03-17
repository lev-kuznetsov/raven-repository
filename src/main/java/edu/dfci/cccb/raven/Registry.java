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
import static java.util.Arrays.asList;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;
import static javax.ws.rs.core.Response.noContent;
import static org.jsoup.Jsoup.parse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import lombok.Getter;
import lombok.extern.java.Log;

import org.javatuples.Pair;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;

@Log
@Singleton
@Path ("/registry")
public class Registry {

  private @Inject Repository repository;

  private @Inject AsyncDatastoreService data;

  private @Inject URLFetchService url;

  private @Inject ObjectMapper mapper;

  @JsonIgnoreProperties (ignoreUnknown = true)
  private static class GithubPushEvent {
    private @Getter @JsonProperty ("after") String sha;
    private @JsonProperty GithubRepository repository;

    @JsonIgnoreProperties (ignoreUnknown = true)
    private static class GithubRepository {
      private @JsonProperty ("full_name") String repository;
    }

    public String getRepo () {
      return repository.repository;
    }
  }

  @Path ("/github")
  @POST
  public void github (GithubPushEvent push) throws IOException, InterruptedException {
    String sha = push.getSha ();
    String repo = push.getRepo ();
    HTTPRequest request = new HTTPRequest (new URL ("https://raw.githubusercontent.com/"
                                                    + repo + "/" + sha + "/DESCRIPTION"));
    request.setHeader (new HTTPHeader ("User-Agent", "dfci-cccb"));
    Description description = new Description (new String (url.fetch (request).getContent ()));
    Snapshot snapshot = new Github ().setRepo (repo).setSha (sha);

    resolve (description, snapshot, new HashMap<String, Pair<Description, Snapshot>> (), new HashMap<String, String> ());
  }

  @Path ("/cran")
  @GET
  public Response cran () throws MalformedURLException, InterruptedException, IOException {
    Future<HTTPResponse> cran = url.fetchAsync (new URL ("http://cran.us.r-project.org/src/contrib/PACKAGES"));

    Map<String, Pair<Description, Snapshot>> context = new HashMap<> ();
    try {
      for (String packageDescription : new String (cran.get ().getContent ()).split ("(?m)^$")) {
        Description description = new Description (packageDescription);
        Snapshot snapshot = new Cran ().setName (description.getName ()).setVersion (description.getVersion ());
        context.put (description.getName (), new Pair<> (description, snapshot));
      }
    } catch (ExecutionException e) {
      log.log (WARNING, "Unable to fetch cran package descriptors", e);
      if (e.getCause () instanceof IOException)
        throw (IOException) e.getCause ();
      if (e.getCause () instanceof RuntimeException)
        throw (RuntimeException) e.getCause ();
      else if (e.getCause () instanceof Error)
        throw (Error) e.getCause ();
      else
        throw new UndeclaredThrowableException (e.getCause ());
    }

    Set<String> unresolved = new TreeSet<> ();
    Map<String, String> resolved = new HashMap<> ();
    for (Entry<String, Pair<Description, Snapshot>> pkg : context.entrySet ())
      if (!resolve (pkg.getValue ().getValue0 (), pkg.getValue ().getValue1 (), context, resolved))
        unresolved.add (pkg.getKey ());

    log.info ("Unable to resolve " + unresolved.size () + " cran packages " + unresolved);

    return noContent ().build ();
  }

  @Path ("/bioc-devel")
  @GET
  public Response biocDevel () throws MalformedURLException, InterruptedException, IOException {
    Map<String, Pair<Description, Snapshot>> context = new HashMap<> ();
    Map<String, Pair<String, String>> packageToVersionAndRevision = new HashMap<> ();
    Future<HTTPResponse> biocDevelPackages = url.fetchAsync (new URL ("http://bioconductor.org/packages/devel"
                                                                      + "/bioc/src/contrib/PACKAGES"));
    Element body = parse (new ByteArrayInputStream (url.fetch (new URL ("http://bioconductor.org/checkResults/devel/"
                                                                        + "bioc-LATEST/"))
                                                       .getContent ()),
                          null, "http://bioconductor.org/checkResults/devel/bioc-LATEST/").body ();
    try {
      for (Element packageEntry : body.select ("table[class=mainrep] > tbody > tr "
                                               + "> td[rowspan][style*=padding-left]")) {
        Elements spec = packageEntry.select ("b");
        String name = spec.select ("a[href*=package]").text ();
        String version = spec.text ().replaceAll (name, "").replaceAll ("[^0-9\\.-]", "");
        String revision = packageEntry.select ("table[class=svn_info]")
                                      .select ("td[class=svn_info]")
                                      .select ("span[class=svn_info]")
                                      .first ()
                                      .text ();
        packageToVersionAndRevision.put (name, new Pair<> (version, revision));
      }
      for (String unparsedDescriptor : new String (biocDevelPackages.get ().getContent ()).split ("(?m)^$")) {
        if ("".equals (unparsedDescriptor.trim ()))
          continue;
        Description d = new Description (unparsedDescriptor);
        Pair<String, String> versionAndRevision = packageToVersionAndRevision.get (d.getName ());
        if (versionAndRevision == null) {
          log.fine ("no build info on bioc:" + d.getName () + ":devel");
          continue;
        }
        Snapshot snapshot = new Svn ().setUrl ("https://hedgehog.fhcrc.org/bioconductor")
                                      .setDir ("madman/Rpacks/" + d.getName ())
                                      .setUser ("readonly")
                                      .setPw ("readonly")
                                      .setRevision (versionAndRevision.getValue1 ());
        context.put (d.getName (), new Pair<> (d, snapshot));
      }

      if (context.size () < 50) // this happens if they fuck with the build page
                                // layout
        log.severe ("Very low package count for bioc-devel");

      Set<String> unresolved = new TreeSet<> ();
      Map<String, String> resolved = new HashMap<> ();
      for (Entry<String, Pair<Description, Snapshot>> pkg : context.entrySet ())
        if (!resolve (pkg.getValue ().getValue0 (), pkg.getValue ().getValue1 (), context, resolved))
          unresolved.add (pkg.getKey ());

      log.info ("Unable to resolve " + unresolved.size () + " bioc:devel packages " + unresolved);

      return noContent ().build ();
    } catch (ExecutionException e) {
      log.log (WARNING, "Unable to fetch bioc devel package descriptors", e);
      if (e.getCause () instanceof IOException)
        throw (IOException) e.getCause ();
      if (e.getCause () instanceof RuntimeException)
        throw (RuntimeException) e.getCause ();
      else if (e.getCause () instanceof Error)
        throw (Error) e.getCause ();
      else
        throw new UndeclaredThrowableException (e.getCause ());
    }
  }

  @Path ("/bioc-release")
  @GET
  public Response biocRelease () throws MalformedURLException, InterruptedException, IOException {
    Map<String, Pair<Description, Snapshot>> context = new HashMap<> ();
    Map<String, Pair<String, String>> packageToVersionAndRevision = new HashMap<> ();
    Future<HTTPResponse> biocReleasePackages = url.fetchAsync (new URL ("http://bioconductor.org/packages/release"
                                                                        + "/bioc/src/contrib/PACKAGES"));
    Element body = parse (new ByteArrayInputStream (url.fetch (new URL ("http://bioconductor.org/checkResults/release"
                                                                        + "/bioc-LATEST")).getContent ()), null,
                          "http://bioconductor.org/checkResults/release/bioc-LATEST").body ();
    String branch = body.select ("div[class=svn_info] > table[class=svn_info] "
                                 + "> tbody > tr")
                        .get (1)
                        .select ("span[class=svn_info]")
                        .text ()
                        .replaceAll ("https://hedgehog.fhcrc.org/bioconductor/branches/", "")
                        .replaceAll ("/madman/Rpacks", "");
    try {
      for (Element packageEntry : body.select ("table[class=mainrep] > tbody > tr "
                                               + "> td[rowspan][style*=padding-left]")) {
        Elements spec = packageEntry.select ("b");
        String name = spec.select ("a[href*=package]").text ();
        String version = spec.text ().replaceAll (name, "").replaceAll ("[^0-9\\.-]", "");
        String revision = packageEntry.select ("table[class=svn_info]")
                                      .select ("td[class=svn_info]")
                                      .select ("span[class=svn_info]")
                                      .first ()
                                      .text ();
        packageToVersionAndRevision.put (name, new Pair<> (version, revision));
      }
      for (String unparsedDescriptor : new String (biocReleasePackages.get ().getContent ()).split ("(?m)^$")) {
        if ("".equals (unparsedDescriptor.trim ()))
          continue;
        Description d = new Description (unparsedDescriptor);
        Pair<String, String> versionAndRevision = packageToVersionAndRevision.get (d.getName ());
        if (versionAndRevision == null) {
          log.fine ("no build info on bioc:" + d.getName () + ":release");
          continue;
        }
        Snapshot snapshot = new Svn ().setUrl ("https://hedgehog.fhcrc.org/bioconductor")
                                      .setDir ("madman/Rpacks/" + d.getName ())
                                      .setBranch (branch)
                                      .setUser ("readonly")
                                      .setPw ("readonly")
                                      .setRevision (versionAndRevision.getValue1 ());
        context.put (d.getName (), new Pair<> (d, snapshot));
      }

      if (context.size () < 50) // this happens if they fuck with the build page
                                // layout
        log.severe ("Very low package count for bioc-release");

      Set<String> unresolved = new TreeSet<> ();
      Map<String, String> resolved = new HashMap<> ();
      for (Entry<String, Pair<Description, Snapshot>> pkg : context.entrySet ())
        if (!resolve (pkg.getValue ().getValue0 (), pkg.getValue ().getValue1 (), context, resolved))
          unresolved.add (pkg.getKey ());

      log.info ("Unable to resolve " + unresolved.size () + " bioc:release packages " + unresolved);

      return noContent ().build ();
    } catch (ExecutionException e) {
      log.log (WARNING, "Unable to fetch bioc devel package descriptors", e);
      if (e.getCause () instanceof IOException)
        throw (IOException) e.getCause ();
      if (e.getCause () instanceof RuntimeException)
        throw (RuntimeException) e.getCause ();
      else if (e.getCause () instanceof Error)
        throw (Error) e.getCause ();
      else
        throw new UndeclaredThrowableException (e.getCause ());
    }
  }

  private boolean resolve (Description description,
                           Snapshot snapshot,
                           Map<String, Pair<Description, Snapshot>> context,
                           Map<String, String> resolved) throws InterruptedException {
    if (description.getName ().equals ("NMF"))
      System.out.println ("yelp");
    if (resolved.containsKey (description.getName ()))
      return true;
    else {
      Package pkg;
      try {
        pkg = repository.get (description.getName ());
        if (pkg.available ().contains (description.getVersion ())) {
          resolved.put (description.getName (), description.getVersion ());
          return true;
        }
      } catch (PackageNotFoundException e) {
        pkg = new Package ();
      }

      for (Pair<Map<String, String>, Set<Dependency>> dependencies : asList (new Pair<> (snapshot.getDepends (),
                                                                                         description.getDepends ()),
                                                                             new Pair<> (snapshot.getImports (),
                                                                                         description.getImports ()),
                                                                             new Pair<> (snapshot.getLinksTo (),
                                                                                         description.getLinksTo ()))) {
        for (Dependency dependency : dependencies.getValue1 ()) {
          String dependencyName = dependency.getName ();
          String dependencyVersion = resolved.get (dependencyName);
          // not resolved, try context
          if (dependencyVersion == null || !dependency.satisfies (dependencyVersion)) {
            Pair<Description, Snapshot> inContext = context.get (dependencyName);
            if (inContext == null) // not in context, try repo
              try {
                Package dependencyPackage = repository.get (dependencyName);
                for (String current : dependencyPackage.available ())
                  if (dependency.satisfies (current)) {
                    dependencyVersion = current;
                    break;
                  }
                if (dependencyVersion == null) {
                  log.fine ("Unable to find a satisfactory dependency version of "
                            + dependencyName + " for " + description.getName ());
                  return false;
                }
              } catch (PackageNotFoundException e) {
                log.fine ("Unable to resolve dependency on " + dependencyName + " for " + description.getName ());
                return false;
              }
            else if (!resolve (inContext.getValue0 (), inContext.getValue1 (), context, resolved)) {
              log.fine ("Unable to resolve transitive dependency on "
                        + dependencyName + " for " + description.getName ());
              return false;
            }
          }
          // resolved
          dependencies.getValue0 ().put (dependencyName, dependencyVersion);
        }
      }
      resolved.put (description.getName (), description.getVersion ());

      if (pkg.add (description.getVersion (), snapshot)) {
        Entity entity = new Entity (createKey ("package", description.getName ()));
        try {
          entity.setProperty ("payload", new Text (mapper.writeValueAsString (pkg)));
          data.put (entity);
        } catch (JsonProcessingException e) {
          log.log (FINE,
                   "Unable to update package entity " + entity + " for cran package " + description.getName (),
                   e);
          return false;
        }
      }
    }

    return true;
  }
}
