/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.transport.MessageRequester;
import org.mule.compatibility.core.transport.AbstractMessageRequesterFactory;
import org.mule.runtime.core.api.MuleException;

/**
 * Creates a requester responsible for reading files from disk
 */
public class FileMessageRequesterFactory extends AbstractMessageRequesterFactory {

  @Override
  public MessageRequester create(InboundEndpoint endpoint) throws MuleException {
    return new FileMessageRequester(endpoint);
  }
}
