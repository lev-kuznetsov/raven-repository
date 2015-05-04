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

import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.inject.Inject;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a github hosted package
 * 
 * @author levk
 *
 */
@Entity
@DiscriminatorValue ("github")
public class Github extends Snapshot {

  /**
   * repository
   */
  private @JsonProperty String repository;

  /**
   * commit hash or tag
   */
  private @JsonProperty String commit;

  /**
   * Required for JPA
   */
  @Inject
  private Github () {}

  /**
   * @param pakage
   * @param version
   * @param repository
   * @param commit
   * @param dependencies
   */
  public Github (Package pakage,
                 String version,
                 String repository,
                 String commit,
                 Snapshot... dependencies) {
    super (pakage, version, dependencies);
    this.repository = repository;
    this.commit = commit;
  }

  /**
   * Github webhook receiver
   * 
   * @author levk
   *
   */
  @Path ("/registry/github")
  public static class GithubUpdater extends Resolver {

    /**
     * Github push event
     * 
     * @author levk
     *
     */
    @JsonIgnoreProperties (ignoreUnknown = true)
    public static class GithubPushEvent {
      private @JsonProperty ("ref") String ref;
      private @JsonProperty ("after") String sha;
      private @JsonProperty GithubRepository repository;

      @JsonIgnoreProperties (ignoreUnknown = true)
      private static class GithubRepository {
        private @JsonProperty ("full_name") String name;
      }
    }

    /**
     * @param push
     * @throws IOException
     * @throws MalformedURLException
     * @throws URISyntaxException
     */
    @POST
    public Response update (GithubPushEvent push) throws MalformedURLException, IOException, URISyntaxException {
      if ("refs/heads/master".equals (push.ref)) {
        String target = "https://raw.githubusercontent.com/" + push.repository.name + "/" + push.sha + "/DESCRIPTION";
        HttpURLConnection connection = (HttpURLConnection) new URL (target).openConnection ();
        connection.setRequestProperty ("User-Agent", "dfci-cccb");
        Snapshot[] holder = new Snapshot[] { null };
        resolve ( (Package p, String v, Snapshot... d) -> holder[0] = new Github (p,
                                                                                  v,
                                                                                  push.repository.name,
                                                                                  push.sha,
                                                                                  d),
                 IOUtils.toString (connection.getInputStream ()));
        return (holder[0] == null ? noContent () : created (new URI (Repository.class.getAnnotation (Path.class)
                                                                                     .value ()
                                                                     + "/" + holder[0].pakage ()
                                                                     + "/" + holder[0].version ()))).build ();
      } else
        return status (BAD_REQUEST).entity ("Not a valid package repository").build ();
    }
  }
}
