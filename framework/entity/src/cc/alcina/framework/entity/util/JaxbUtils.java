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

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class JaxbUtils {
	private static JAXBContext jc = null;

	private static Set<Class> jcClasses = new HashSet<Class>();

	public static void appShutdown() {
		jc = null;
		jcClasses = new HashSet<Class>();
	}

	public static JAXBContext getContext(Collection<Class> classes)
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
