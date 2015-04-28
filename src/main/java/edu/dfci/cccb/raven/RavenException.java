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
import static com.google.common.collect.ImmutableMap.of;
import static javax.ws.rs.core.Response.serverError;

import java.util.Map;

import javax.ws.rs.core.Response.ResponseBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Common superclass for failures
 * 
 * @author levk
 *
 */
@JsonInclude (NON_EMPTY)
public class RavenException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   */
  public RavenException () {}

  /**
   * @param message
   */
  public RavenException (String message) {
    super (message);
  }

  /**
   * @param cause
   */
  public RavenException (Throwable cause) {
    super (cause);
  }

  /**
   * @param message
   * @param cause
   */
  public RavenException (String message, Throwable cause) {
    super (message, cause);
  }

  /**
   * @param message
   * @param cause
   * @param enableSuppression
   * @param writableStackTrace
   */
  public RavenException (String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super (message, cause, enableSuppression, writableStackTrace);
  }

  /* (non-Javadoc)
   * @see java.lang.Throwable#getMessage() */
  @JsonProperty ("message")
  @Override
  public String getMessage () {
    return super.getMessage ();
  }

  /**
   * @return type
   */
  @JsonProperty
  public String getType () {
    return getClass ().getSimpleName ();
  }

  /**
   * @return cause
   */
  @JsonProperty ("cause")
  private Map<String, ?> cause () {
    Throwable cause = getCause ();
    if (cause == null)
      return null;
    else {
      StackTraceElement at = null;
      for (StackTraceElement element : cause.getStackTrace ())
        if (element.getClassName ().startsWith (RavenException.class.getPackage ().getName ())) {
          at = element;
          break;
        }
      if (at == null)
        at = cause.getStackTrace ()[0];
      return of ("type", cause.getClass ().getSimpleName (), "message", cause.getMessage (), "@", at);
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Throwable#getCause() */
  @Override
  @JsonIgnore
  public synchronized Throwable getCause () {
    return super.getCause ();
  }

  /* (non-Javadoc)
   * @see java.lang.Throwable#getLocalizedMessage() */
  @Override
  @JsonIgnore
  public String getLocalizedMessage () {
    return super.getLocalizedMessage ();
  }

  /* (non-Javadoc)
   * @see java.lang.Throwable#getStackTrace() */
  @Override
  @JsonIgnore
  public StackTraceElement[] getStackTrace () {
    return super.getStackTrace ();
  }

  /**
   * @return response builder
   */
  public ResponseBuilder response () {
    return serverError ();
  }

  /**
   * @return entity
   */
  public Object asEntity () {
    return this;
  }
}
