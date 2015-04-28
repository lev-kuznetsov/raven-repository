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

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.apache.log4j.Logger.getLogger;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.onami.scheduler.Scheduled;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.google.inject.persist.Transactional;

/**
 * Represents a CRAN package snapshot
 * 
 * @author levk
 *
 */
@Entity
@DiscriminatorValue ("cran")
public class Cran extends Snapshot {
  private static final Logger log = getLogger (Cran.class);

  /**
   * @param pakage
   * @param version
   * @param dependencies
   */
  public Cran (Package pakage, String version, Snapshot... dependencies) {
    super (pakage, version, dependencies);
  }

  /**
   * Required for JPA
   */
  @SuppressWarnings ("unused")
  private Cran () {}

  /**
   * Updates CRAN packages
   * 
   * @author levk
   *
   */
  @Scheduled (cronExpression = "0 0 2 * * ?")
  public static class CranResolver extends Resolver implements Job {

    /**
     * Resolved within the context
     */
    private Map<String, Package> resolved;

    /**
     * Context descriptors
     */
    private Map<String, String> descriptors;

    /* (non-Javadoc)
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext) */
    @Override
    @Transactional
    public synchronized void execute (JobExecutionContext context) throws JobExecutionException {
      try {
        descriptors = asList (IOUtils.toString (new URL ("http://cran.us.r-project.org/src/"
                                                         + "contrib/PACKAGES").openStream ())
                                     .split ("(?m)^$")).stream ()
                                                       .filter (d -> !"".equals (d.trim ()))
                                                       .collect (toMap (d -> {
                                                         String p = d.substring (d.indexOf ("Package:") + 8).trim ();
                                                         return p.substring (0, p.indexOf ('\n')).trim ();
                                                       }, d -> d));
        resolved = new HashMap<> ();
        int count = descriptors.entrySet ().stream ().map (e -> {
          try {
            if (!resolved.containsKey (e.getKey ()))
              resolved.put (e.getKey (), resolve (Cran::new, e.getValue ()));
            return 1;
          } catch (RavenException ex) {
            return 0;
          }
        }).reduce ( (a, b) -> a + b).orElse (0);
        log.info ("Resolved " + count + " out of " + descriptors.size () + " packages for CRAN");
        descriptors = null;
        resolved = null;
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
        Package p = descriptor == null ? super.lookup (name) : resolve (Cran::new, descriptor);
        resolved.put (name, p);
        return p;
      });
    }
  }
}
