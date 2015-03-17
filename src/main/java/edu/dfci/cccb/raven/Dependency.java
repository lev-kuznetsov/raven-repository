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

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static lombok.AccessLevel.PRIVATE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor (access = PRIVATE)
@EqualsAndHashCode
public class Dependency {

  private static final Set<String> IGNORE = new TreeSet<> (asList ("R", "base", "compiler", "tcltk", "parallel",
                                                                   "grid", "tools", "utils", "methods", "stats",
                                                                   "stats4", "splines", "graphics", "datasets",
                                                                   "grDevices"));

  private @Getter final String name;

  private final Set<String> constraints;

  public boolean satisfies (String version) {
    return true; // TODO
  }

  public static Set<Dependency> fromDescriptionEntry (String entry) {
    Map<String, Set<String>> expressions = new HashMap<String, Set<String>> ();
    if (entry != null)
      for (String spec : entry.split (",")) {
        String[] split = spec.split ("[\\(\\)]");
        String pkg = split[0].trim ();
        if (IGNORE.contains (pkg))
          continue;
        Set<String> exprs = expressions.get (pkg);
        if (exprs == null)
          expressions.put (pkg, exprs = new TreeSet<> ());
        if (split.length > 1)
          exprs.add (split[1]);
      }
    Set<Dependency> result = new HashSet<> ();
    for (Entry<String, Set<String>> e : expressions.entrySet ())
      result.add (new Dependency (e.getKey (), e.getValue ()));
    return unmodifiableSet (result);
  }
}
