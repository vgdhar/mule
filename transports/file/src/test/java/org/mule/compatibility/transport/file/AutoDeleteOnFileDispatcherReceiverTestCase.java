/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.message.DefaultEventBuilder.EventImplementation.setCurrentEvent;

import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.registry.MuleRegistryTransportHelper;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.util.FileUtils;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import java.io.File;
import java.io.InputStream;

import org.junit.Test;

public class AutoDeleteOnFileDispatcherReceiverTestCase extends AbstractMuleContextEndpointTestCase {

  private File validMessage;
  private String tempDirName = "input";
  File tempDir;
  Connector connector;

  @Test
  public void testAutoDeleteFalseOnDispatcher() throws Exception {
    ((FileConnector) connector).setAutoDelete(false);

    setCurrentEvent(testEvent());

    InternalMessage message =
        muleContext.getClient().request(getTestEndpointURI() + "/" + tempDirName + "?connector=FileConnector", 50000).getRight()
            .get();
    // read the payload into a string so the file is deleted on InputStream.close()
    assertNotNull(getPayloadAsString(message));

    File[] files = tempDir.listFiles();
    assertTrue(files.length > 0);
    for (File file : files) {
      assertTrue(file.getName().equals(message.getInboundProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME)));
      file.delete();
    }
  }

  @Test
  public void testAutoDeleteTrueOnDispatcher() throws Exception {
    ((FileConnector) connector).setAutoDelete(true);

    setCurrentEvent(testEvent());

    InternalMessage message = muleContext.getClient().request(getTestEndpointURI() + "/" + tempDirName, 50000).getRight().get();
    assertNotNull(message.getPayload().getValue());
    assertTrue(message.getPayload().getValue() instanceof InputStream);

    // Auto-delete happens after FileInputStream.close() when streaming. Streaming is default.
    assertTrue(tempDir.listFiles().length > 0);
    ((InputStream) message.getPayload().getValue()).close();
    // Give file-system some time (annoying but necessary wait apparently due to OS caching?)
    Prober prober = new PollingProber(1000, 100);
    prober.check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return tempDir.listFiles().length == 0;
      }

      @Override
      public String describeFailure() {
        return "File was not deleted from temp directory";
      }
    });
    assertTrue(tempDir.listFiles().length == 0);


  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    // The working directory is deleted on tearDown
    tempDir = FileUtils.newFile(muleContext.getConfiguration().getWorkingDirectory(), tempDirName);
    tempDir.deleteOnExit();
    if (!tempDir.exists()) {
      tempDir.mkdirs();
    }
    validMessage = File.createTempFile("hello", ".txt", tempDir);
    assertNotNull(validMessage);
    connector = getConnector();
    connector.start();
  }

  @Override
  protected void doTearDown() throws Exception {
    // TestConnector dispatches events via the test: protocol to test://test
    // endpoints, which seems to end up in a directory called "test" :(
    FileUtils.deleteTree(FileUtils.newFile(getTestConnector().getProtocol()));
    super.doTearDown();
  }

  public Connector getConnector() throws Exception {
    Connector connector = new FileConnector(muleContext);
    connector.setName("FileConnector");
    MuleRegistryTransportHelper.registerConnector(muleContext.getRegistry(), connector);
    return connector;
  }

  public String getTestEndpointURI() {
    return "file://" + muleContext.getConfiguration().getWorkingDirectory();
  }
}
