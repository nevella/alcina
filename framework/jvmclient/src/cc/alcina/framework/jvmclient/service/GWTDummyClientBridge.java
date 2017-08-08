/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * nick - copied from gwt/junit
 */
package cc.alcina.framework.jvmclient.service;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWTBridge;
import com.google.gwt.dev.About;
import com.google.gwt.junit.GWTMockUtilities;

import cc.alcina.framework.common.client.WrappedRuntimeException;

/**
 * A dummy implementation of {@link GWTBridge}, which instantiates nothing.
 *
 * @see GWTMockUtilities
 */
public class GWTDummyClientBridge extends GWTBridge {
  private static final Logger logger = Logger.getLogger(GWTDummyClientBridge.class.getName());

  /**
   * Returns null.
   */
  @Override
  public <T> T create(Class<?> classLiteral) {
    return null;
  }

  /**
   * Returns the current version of GWT ({@link About#getGwtVersionNum()}).
   */
  @Override
  public String getVersion() {
    return About.getGwtVersionNum();
  }

  /**
   * Returns true.
   */
  @Override
  public boolean isClient() {
    return true;
  }

  /**
   * Logs the message and throwable to the standard logger, with level {@link
   * Level#SEVERE}.
   */
  @Override
  public void log(String message, Throwable e) {
    logger.log(Level.SEVERE, message, e);
  }
  public void install(){
	  try {
		Method m = GWT.class.getDeclaredMethod("setBridge", GWTBridge.class);
		m.setAccessible(true);
		m.invoke(null, this);
	} catch (Exception e) {
		throw new WrappedRuntimeException(e);
	}
	
  }
  
}
