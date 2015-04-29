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

import static com.google.common.collect.ImmutableMap.of;
import static com.google.inject.Guice.createInjector;
import static java.lang.System.getProperties;
import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static java.util.logging.Level.FINEST;
import static java.util.logging.LogManager.getLogManager;
import static java.util.logging.Logger.getLogger;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;

import org.apache.onami.scheduler.QuartzModule;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.persist.PersistFilter;
import com.google.inject.persist.jpa.JpaPersistModule;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

import edu.dfci.cccb.raven.Cran.CranResolver;
import edu.dfci.cccb.raven.Svn.BiocDevelResolver;
import edu.dfci.cccb.raven.Svn.BiocReleaseResolver;

/**
 * Configures the application server
 *
 * @author levk
 *
 */
public class Raven extends GuiceServletContextListener implements Module {

  /**
   * Application package for component scans
   */
  private static final String APPLICATION_PACKAGE = Raven.class.getPackage ().getName ();

  /* (non-Javadoc)
   * @see com.google.inject.Module#configure(com.google.inject.Binder) */
  @Override
  public void configure (Binder binder) {
    // Import configuration as system properties
    getProperties ().putAll (getenv ());
    try {
      getProperties ().load (getClass ().getResourceAsStream ("/profile.properties"));
    } catch (IOException e) {}

    // Configures deps using java.util.logging to log out to slf4j
    getLogManager ().reset ();
    SLF4JBridgeHandler.install ();
    getLogger ("global").setLevel (FINEST);

    // Inject context listener
    binder.requestInjection (this);

    // JSON
    binder.install (new ObjectMapperModule ());

    // JPA
    binder.install (new JpaPersistModule (getProperty ("persistence.unit.name")));

    // Quartz
    binder.install (new QuartzModule () {

      @Override
      protected void schedule () {
        scheduleJob (CranResolver.class);
        scheduleJob (BiocDevelResolver.class);
        scheduleJob (BiocReleaseResolver.class);

        requestInjection (Raven.this);
      }
    });

    // REST
    binder.install (new ServletModule () {

      @Override
      protected void configureServlets () {
        serve ("/*").with (GuiceContainer.class, of ("com.sun.jersey.config.property.packages",
                                                     APPLICATION_PACKAGE));
        filter ("/*").through (PersistFilter.class);
      }
    });
  }

  /* (non-Javadoc)
   * @see com.google.inject.servlet.GuiceServletContextListener#getInjector() */
  @Override
  protected Injector getInjector () {
    return createInjector (this);
  }

  /**
   * For cleanup, injected after the injector is created
   */
  private @Inject Scheduler scheduler;

  /* (non-Javadoc)
   * @see
   * com.google.inject.servlet.GuiceServletContextListener#contextDestroyed
   * (javax.servlet.ServletContextEvent) */
  @Override
  public void contextDestroyed (ServletContextEvent servletContextEvent) {
    try {
      scheduler.shutdown (true);
    } catch (SchedulerException e) {
      throw new RuntimeException (e);
    } finally {
      super.contextDestroyed (servletContextEvent);
    }
  }
}
