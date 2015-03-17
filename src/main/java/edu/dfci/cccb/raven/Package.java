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

import static java.util.Collections.sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonProperty;

@ToString
public class Package {

  private @JsonProperty Map<String, Snapshot> snapshots = new HashMap<> ();

  @Path ("/{version}")
  @GET
  public Snapshot version (@PathParam ("version") String version) throws VersionNotFoundException {
    Snapshot snapshot = snapshots.get (version);
    if (snapshot == null)
      throw new VersionNotFoundException ("Could not locate version " + version + " among " + available ());
    return snapshot;
  }

  @GET
  public List<String> available () {
    List<Entry<String, Snapshot>> ageOrder = new ArrayList<> (snapshots.entrySet ());
    sort (ageOrder, new Comparator<Entry<String, Snapshot>> () {

      @Override
      public int compare (Entry<String, Snapshot> o1, Entry<String, Snapshot> o2) {
        return o1.getValue ().compareTo (o2.getValue ());
      }
    });
    List<String> result = new ArrayList<> ();
    for (Entry<String, Snapshot> entry : ageOrder)
      result.add (entry.getKey ());
    return result;
  }

  public synchronized boolean add (String version, Snapshot snapshot) {
    if (!snapshots.containsKey (version)) {
      snapshots.put (version, snapshot);
      return true;
    }
    return false;
  }
}
