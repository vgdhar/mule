/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.ws.consumer;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.config.i18n.CoreMessages;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapFault;
import org.w3c.dom.Element;

/**
 * Exception thrown by the Web Services Consumer when processing a SOAP fault. The exception contains the details about the fault.
 */
public class SoapFaultException extends MuleException {

  private final QName faultCode;
  private final QName subCode;
  private final Element detail;

  public SoapFaultException(SoapFault soapFault) {
    super(CoreMessages.createStaticMessage(soapFault.getMessage()), soapFault);
    this.faultCode = soapFault.getFaultCode();
    this.subCode = soapFault.getSubCode();
    this.detail = soapFault.getDetail();
  }

  public QName getFaultCode() {
    return faultCode;
  }

  public QName getSubCode() {
    return subCode;
  }

  public Element getDetail() {
    return detail;
  }
}
