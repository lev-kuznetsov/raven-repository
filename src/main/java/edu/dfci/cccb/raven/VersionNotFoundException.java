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

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * Thrown when a non existing version of a package is requested
 * 
 * @author levk
 *
 */
public class VersionNotFoundException extends RavenException {
  private static final long serialVersionUID = 1L;

  /**
   */
  public VersionNotFoundException () {}

  /**
   * @param message
   */
  public VersionNotFoundException (String message) {
    super (message);
  }

  /**
   * @param cause
   */
  public VersionNotFoundException (Throwable cause) {
    super (cause);
  }

  /**
   * @param message
   * @param cause
   */
  public VersionNotFoundException (String message, Throwable cause) {
    super (message, cause);
  }

  /**
   * @param message
   * @param cause
   * @param enableSuppression
   * @param writableStackTrace
   */
  public VersionNotFoundException (String message,
                                   Throwable cause,
                                   boolean enableSuppression,
                                   boolean writableStackTrace) {
    super (message, cause, enableSuppression, writableStackTrace);
  }

  /* (non-Javadoc)
   * @see edu.dfci.cccb.raven2.RavenException#response() */
  @Override
  public ResponseBuilder response () {
    return status (NOT_FOUND);
  }
}
