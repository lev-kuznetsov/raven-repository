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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.MINIMAL_CLASS;
import static java.lang.System.currentTimeMillis;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo (use = MINIMAL_CLASS, include = PROPERTY)
@JsonInclude (NON_EMPTY)
@ToString
public abstract class Snapshot implements Comparable<Snapshot> {

  private @Getter @JsonProperty Map<String, String> depends = new HashMap<> ();

  private @Getter @JsonProperty Map<String, String> imports = new HashMap<> ();

  private @Getter @JsonProperty Map<String, String> linksTo = new HashMap<> ();

  private @JsonProperty long timestamp = currentTimeMillis ();

  @Override
  public int compareTo (Snapshot o) {
    return (int) (o.timestamp - timestamp);
  }
}
