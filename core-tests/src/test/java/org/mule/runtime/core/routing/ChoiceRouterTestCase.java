/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.routing.RoutePathNotFoundException;
import org.mule.runtime.core.management.stats.RouterStatistics;
import org.mule.runtime.core.routing.filters.EqualsFilter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import org.junit.Test;

public class ChoiceRouterTestCase extends AbstractMuleContextTestCase {

  private ChoiceRouter choiceRouter;

  public ChoiceRouterTestCase() {
    setDisposeContextPerClass(true);
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    choiceRouter = new ChoiceRouter();
  }

  @Test
  public void testNoRoute() throws Exception {
    try {
      choiceRouter.process(fooEvent());
      fail("should have got a MuleException");
    } catch (MuleException me) {
      assertTrue(me instanceof RoutePathNotFoundException);
    }
  }

  @Test
  public void testOnlyDefaultRoute() throws Exception {
    choiceRouter.setDefaultRoute(new TestMessageProcessor("default"));
    assertEquals("foo:default", choiceRouter.process(fooEvent()).getMessageAsString(muleContext));
  }

  @Test
  public void testNoMatchingNorDefaultRoute() throws Exception {
    try {
      choiceRouter.addRoute(new TestMessageProcessor("bar"), new EqualsFilter("zap"));
      choiceRouter.process(fooEvent());
      fail("should have got a MuleException");
    } catch (MuleException me) {
      assertTrue(me instanceof RoutePathNotFoundException);
    }
  }

  @Test
  public void testNoMatchingRouteWithDefaultRoute() throws Exception {
    choiceRouter.addRoute(new TestMessageProcessor("bar"), new EqualsFilter("zap"));
    choiceRouter.setDefaultRoute(new TestMessageProcessor("default"));
    assertEquals("foo:default", choiceRouter.process(fooEvent()).getMessageAsString(muleContext));
  }

  @Test
  public void testMatchingRouteWithDefaultRoute() throws Exception {
    choiceRouter.addRoute(new TestMessageProcessor("bar"), new EqualsFilter("zap"));
    choiceRouter.setDefaultRoute(new TestMessageProcessor("default"));
    assertEquals("zap:bar", choiceRouter.process(zapEvent()).getMessageAsString(muleContext));
  }

  @Test
  public void testMatchingRouteWithStatistics() throws Exception {
    choiceRouter.addRoute(new TestMessageProcessor("bar"), new EqualsFilter("zap"));
    choiceRouter.setRouterStatistics(new RouterStatistics(RouterStatistics.TYPE_OUTBOUND));
    assertEquals("zap:bar", choiceRouter.process(zapEvent()).getMessageAsString(muleContext));
  }

  @Test
  public void testAddAndDeleteRoute() throws Exception {
    try {
      TestMessageProcessor mp = new TestMessageProcessor("bar");
      choiceRouter.addRoute(mp, new EqualsFilter("zap"));
      choiceRouter.removeRoute(mp);
      choiceRouter.setRouterStatistics(new RouterStatistics(RouterStatistics.TYPE_OUTBOUND));
      choiceRouter.process(zapEvent());
      fail("should have got a MuleException");
    } catch (MuleException me) {
      assertTrue(me instanceof RoutePathNotFoundException);
    }
  }

  @Test
  public void testUpdateRoute() throws Exception {
    TestMessageProcessor mp = new TestMessageProcessor("bar");
    choiceRouter.addRoute(mp, new EqualsFilter("paz"));
    choiceRouter.updateRoute(mp, new EqualsFilter("zap"));
    assertEquals("zap:bar", choiceRouter.process(zapEvent()).getMessageAsString(muleContext));
  }

  protected Event fooEvent() throws MuleException {
    return eventBuilder().message(InternalMessage.of("foo")).build();
  }

  protected Event zapEvent() throws MuleException {
    return eventBuilder().message(InternalMessage.of("zap")).build();
  }

  @Test
  public void testRemovingUpdatingMissingRoutes() {
    choiceRouter.updateRoute(new TestMessageProcessor("bar"), new EqualsFilter("zap"));
    choiceRouter.removeRoute(new TestMessageProcessor("rab"));
  }

}
