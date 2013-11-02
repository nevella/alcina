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
package cc.alcina.framework.entity.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * 
 * @author Nick Reddel
 */
public class JaxbUtils {
	private JAXBContext jc = null;

	private JaxbUtils() {
	}

	private Set<Class> jcClasses = new HashSet<Class>();

	public static JaxbUtils get() {
		JaxbUtils singleton = Registry.checkSingleton(JaxbUtils.class);
		if (singleton == null) {
			singleton = new JaxbUtils();
			Registry.registerSingleton(JaxbUtils.class, singleton);
		}
		return singleton;
	}

	public static JAXBContext getContext(Collection<Class> classes)
			throws JAXBException {
		return get().getContext0(classes);
	}

	private JAXBContext getContext0(Collection<Class> classes)
			throws JAXBException {
		if (jc == null || !jcClasses.containsAll(classes)) {
			Map<String, String> emptyProps = new HashMap<String, String>();
			jcClasses.addAll(classes);
			Class[] clazzez = (Class[]) jcClasses.toArray(new Class[jcClasses
					.size()]);
			try {
				jc = JAXBContext.newInstance(clazzez, emptyProps);
			} catch (RuntimeException e) {
				jcClasses = new HashSet<Class>();
				throw e;
			}
		}
		return jc;
	}
}
