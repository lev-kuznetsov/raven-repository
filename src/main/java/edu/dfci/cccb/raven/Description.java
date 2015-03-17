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

import static edu.dfci.cccb.raven.Dependency.fromDescriptionEntry;
import static java.util.regex.Pattern.compile;
import static lombok.AccessLevel.PRIVATE;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.apache.commons.io.IOUtils;

@Getter
@RequiredArgsConstructor (access = PRIVATE)
@EqualsAndHashCode (of = "name")
public class Description {

  private final String name;
  private final String version;
  private final Set<Dependency> depends;
  private final Set<Dependency> imports;
  private final Set<Dependency> linksTo;

  public Description (String descriptor) {
    Map<String, String> keyValuePairs = new HashMap<> ();
    Pattern keyPattern = compile ("(?m)^[A-Za-z@0-9]+:");
    for (Matcher matcher = keyPattern.matcher (descriptor); matcher.find ();) {
      String key = descriptor.substring (matcher.start (), matcher.end ()).trim ();
      String rest = descriptor.substring (matcher.end ());
      Matcher matcherOfRest = keyPattern.matcher (rest);
      String value = rest.substring (0, matcherOfRest.find () ? matcherOfRest.start () : rest.length ()).trim ();
      keyValuePairs.put (key, value);
    }
    name = keyValuePairs.get ("Package:").trim ();
    version = keyValuePairs.get ("Version:").trim ();
    depends = fromDescriptionEntry (keyValuePairs.get ("Depends:"));
    imports = fromDescriptionEntry (keyValuePairs.get ("Imports:"));
    linksTo = fromDescriptionEntry (keyValuePairs.get ("LinkingTo:"));
  }

  public Description (InputStream descriptor) throws IOException {
    this (IOUtils.toString (descriptor));
  }
}
