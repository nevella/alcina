/* 
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
 */

package cc.alcina.framework.common.client;

import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ImplementationLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.spi.LogWriter;
import cc.alcina.framework.common.client.util.CurrentUtcDateProvider;
import cc.alcina.framework.common.client.util.URLComponentEncoder;

/**
 *
 * @author Nick Reddel
 */

 public class CommonLocator {
	private CommonLocator() {
		super();
	}

	private static CommonLocator theInstance;

	public static CommonLocator get() {
		if (theInstance == null) {
			theInstance = new CommonLocator();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}

	private PropertyAccessor propertyAccessor;

	public void registerPropertyAccessor(PropertyAccessor accessor) {
		this.propertyAccessor = accessor;
	}

	public PropertyAccessor propertyAccessor() {
		return propertyAccessor;
	}

	private CurrentUtcDateProvider currentUtcDateProvider;

	public void registerCurrentUtcDateProvider(CurrentUtcDateProvider setter) {
		this.currentUtcDateProvider = setter;
	}

	public CurrentUtcDateProvider currentUtcDateProvider() {
		return currentUtcDateProvider;
	}

	private ObjectLookup objectLookup;

	public void registerObjectLookup(ObjectLookup ol) {
		this.objectLookup = ol;
	}

	public ObjectLookup objectLookup() {
		return objectLookup;
	}

	private ClassLookup classLookup;

	public void registerClassLookup(ClassLookup cl) {
		this.classLookup = cl;
	}

	public ClassLookup classLookup() {
		return classLookup;
	}

	
	private ImplementationLookup implementationLookup;

	public void registerImplementationLookup(
			ImplementationLookup implementationLookup) {
		this.implementationLookup = implementationLookup;
	}

	public ImplementationLookup implementationLookup() {
		return implementationLookup;
	}
	private URLComponentEncoder urlComponentEncoder;

	public void registerURLComponentEncoder(URLComponentEncoder urlComponentEncoder) {
		this.urlComponentEncoder = urlComponentEncoder;
	}

	public URLComponentEncoder urlComponentEncoder() {
		return urlComponentEncoder;
	}
}
