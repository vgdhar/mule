/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;


import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.extension.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.extension.http.api.HttpConstants.Methods.POST;
import static org.mule.extension.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.extension.http.api.HttpConstants;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.exception.TemplateOnErrorHandler;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.rule.DynamicPort;

import java.nio.charset.Charset;

import org.apache.cxf.interceptor.Fault;
import org.junit.Rule;
import org.junit.Test;


public class OnErrorContinueTestCase extends AbstractCxfOverHttpExtensionTestCase {

  private static final String requestPayload =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
          "           xmlns:hi=\"http://example.cxf.module.runtime.mule.org/\">\n" +
          "<soap:Body>\n" +
          "<hi:sayHi>\n" +
          "    <arg0>Hello</arg0>\n" +
          "</hi:sayHi>\n" +
          "</soap:Body>\n" +
          "</soap:Envelope>";

  private static final String requestFaultPayload =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
          "           xmlns:hi=\"http://cxf.module.runtime.mule.org/\">\n" +
          "<soap:Body>\n" +
          "<hi:sayHi>\n" +
          "    <arg0>Hello</arg0>\n" +
          "</hi:sayHi>\n" +
          "</soap:Body>\n" +
          "</soap:Envelope>";

  public static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions()
      .method(POST.name()).disableStatusCodeValidation().build();

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "on-error-continue-conf.xml";
  }

  @Test
  public void testFaultInCxfServiceWithCatchExceptionStrategy() throws Exception {
    InternalMessage request = InternalMessage.builder().payload(requestFaultPayload).build();
    MuleClient client = muleContext.getClient();
    InternalMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/testServiceWithFaultCatchException",
                                           request, HTTP_REQUEST_OPTIONS)
        .getRight();
    assertNotNull(response);
    assertEquals(String.valueOf(OK.getStatusCode()), response.getInboundProperty(HTTP_STATUS_PROPERTY).toString());
    assertTrue(getPayloadAsString(response).contains("Anonymous"));
  }

  @Test
  public void testFaultInCxfServiceWithCatchExceptionStrategyRethrown() throws Exception {
    InternalMessage request = InternalMessage.builder().payload(requestFaultPayload).build();
    MuleClient client = muleContext.getClient();
    InternalMessage response =
        client.send("http://localhost:" + dynamicPort.getNumber() + "/testServiceWithFaultCatchExceptionRethrown", request,
                    HTTP_REQUEST_OPTIONS)
            .getRight();
    assertNotNull(response);
    assertEquals(String.valueOf(HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR.getStatusCode()),
                 response.getInboundProperty(HTTP_STATUS_PROPERTY).toString());
    assertTrue(getPayloadAsString(response).contains("<faultstring>"));
  }

  @Test
  public void testExceptionThrownInTransformerWithCatchExceptionStrategy() throws Exception {
    InternalMessage request = InternalMessage.builder().payload(requestPayload).build();
    MuleClient client = muleContext.getClient();
    InternalMessage response =
        client.send("http://localhost:" + dynamicPort.getNumber() + "/testTransformerExceptionCatchException",
                    request, HTTP_REQUEST_OPTIONS)
            .getRight();
    assertNotNull(response);
    assertEquals(String.valueOf(OK.getStatusCode()), response.getInboundProperty(HTTP_STATUS_PROPERTY).toString());
    assertTrue(getPayloadAsString(response).contains("APPEND"));
  }

  @Test
  public void testClientWithSOAPFaultCatchException() throws Exception {
    Event event = flowRunner("FlowWithClientAndSOAPFaultCatchException").withPayload("hello").run();
    assertNotNull(event);
    assertThat(event.getError().isPresent(), is(false));
  }

  @Test
  public void testClientWithSOAPFaultCatchExceptionRedirect() throws Exception {
    Event event = flowRunner("FlowWithClientAndSOAPFaultCatchExceptionRedirect").withPayload("TEST").run();
    assertNotNull(event);
    assertNotNull(event.getMessage());
    assertThat(getPayloadAsString(event.getMessage()), containsString("TEST"));
    assertThat(event.getError().isPresent(), is(false));
  }

  @Test
  public void testClientWithTransformerExceptionCatchException() throws Exception {
    InternalMessage response =
        flowRunner("FlowWithClientAndTransformerExceptionCatchException").withPayload("hello").run().getMessage();
    assertNotNull(response);
    assertTrue(getPayloadAsString(response).contains(" Anonymous"));
  }

  @Test
  public void testServerClientProxyWithTransformerExceptionCatchStrategy() throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage result =
        client.send("http://localhost:" + dynamicPort.getNumber() + "/testProxyWithTransformerExceptionCatchStrategy",
                    InternalMessage.of(requestPayload), HTTP_REQUEST_OPTIONS)
            .getRight();
    String resString = getPayloadAsString(result);
    assertEquals(String.valueOf(OK.getStatusCode()), result.getInboundProperty(HTTP_STATUS_PROPERTY).toString());
    assertTrue(resString.contains("Anonymous"));
  }

  public static class ProxyCustomProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      String payload =
          "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
              + "<soap:Body>"
              + "<ns2:sayHiResponse xmlns:ns2=\"http://example.cxf.module.runtime.mule.org/\">"
              + "<return>Hello Anonymous</return>"
              + "</ns2:sayHiResponse>"
              + "</soap:Body>"
              + "</soap:Envelope>";
      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload(payload).build()).build();
    }
  }

  public static class RethrowFaultProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      throw new Fault(event.getError().get().getException().getCause());
    }
  }

  public static class RethrowExceptionStrategy extends TemplateOnErrorHandler {

    @Override
    protected Event nullifyExceptionPayloadIfRequired(Event event) {
      return event;
    }

    @Override
    protected Event afterRouting(MessagingException exception, Event event) {
      return event;
    }

    @Override
    protected Event beforeRouting(MessagingException exception, Event event) {
      return event;
    }
  }

  public static class CxfTransformerThrowsExceptions extends AbstractTransformer {

    @Override
    protected Object doTransform(Object src, Charset enc) throws TransformerException {
      throw new TransformerException(CoreMessages.failedToBuildMessage());
    }

  }

}
