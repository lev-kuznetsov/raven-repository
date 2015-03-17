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

import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.annotation.JsonProperty;

@Accessors (fluent = false, chain = true)
@ToString
public class Svn extends Snapshot {

  private @Setter @JsonProperty String url;

  private @Setter @JsonProperty (required = false) String branch;

  private @Setter @JsonProperty (required = false) String dir;

  private @Setter @JsonProperty (required = false) String user;

  private @Setter @JsonProperty (required = false) String pw;

  private @Setter @JsonProperty String revision;
}
