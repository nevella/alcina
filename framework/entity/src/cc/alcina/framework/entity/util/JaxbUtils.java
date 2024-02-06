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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;

/**
 * @author Nick Reddel
 */
@Registration(ClearStaticFieldsOnAppShutdown.class)
public class JaxbUtils {
	private static JaxbUtils singleton;

	public static final String CONTEXT_CLASSES = JaxbUtils.class.getName()
			+ ".CONTEXT_CLASSES";

	static List<Class> jaxbSubclasses = null;

	public static <T> T clone(T object) {
		try {
			String s = xmlSerialize(object);
			return (T) xmlDeserialize(object.getClass(), s);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static synchronized List<Class>
			ensureJaxbSubclasses(Class addClass) {
		if (jaxbSubclasses == null) {
			jaxbSubclasses = Registry.query(JaxbContextRegistration.class)
					.untypedRegistrations().collect(Collectors.toList());
		}
		if (addClass == null) {
			return new ArrayList<Class>(jaxbSubclasses);
		}
		ArrayList<Class> classes = new ArrayList<Class>(jaxbSubclasses);
		classes.add(0, addClass);
		return classes;
	}

	public static JaxbUtils get() {
		if (singleton == null) {
			singleton = new JaxbUtils();
			Registry.register().singleton(JaxbUtils.class, singleton);
		}
		return singleton;
	}

	public static JAXBContext getContext() throws JAXBException {
		return get().getContext0(ensureJaxbSubclasses(null));
	}

	public static JAXBContext getContext(Collection<Class> classes)
			throws JAXBException {
		return get().getContext0(classes);
	}

	protected static <T> List<Class> getContextClasses(Class<T> clazz) {
		List<Class> classes = null;
		if (!LooseContext.containsKey(JaxbUtils.CONTEXT_CLASSES)) {
			classes = ensureJaxbSubclasses(clazz);
		} else {
			classes = new ArrayList<>(
					(List) LooseContext.get(JaxbUtils.CONTEXT_CLASSES));
		}
		return classes;
	}

	public static synchronized void withoutRegistry() {
		jaxbSubclasses = new ArrayList<>();
	}

	public static <T> T xmlDeserialize(Class<T> clazz, String xmlStr) {
		if (xmlStr == null) {
			return null;
		}
		try {
			String preProcessed = Registry
					.optional(JaxbUtils.PreProcessor.class)
					.map(pp -> pp.preprocess(xmlStr)).orElse(xmlStr);
			List<Class> classes = getContextClasses(clazz);
			JAXBContext jc = getContext(classes);
			Unmarshaller um = jc.createUnmarshaller();
			StringReader sr = new StringReader(preProcessed);
			return (T) um.unmarshal(sr);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static String xmlSerialize(Object object) {
		List<Class> classes = getContextClasses(object.getClass());
		try {
			return xmlSerialize(object, classes);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static String xmlSerialize(Object object,
			Collection<Class> jaxbClasses) throws JAXBException {
		return xmlSerialize(object, jaxbClasses, false);
	}

	public static String xmlSerialize(Object object,
			Collection<Class> jaxbClasses, boolean tight) throws JAXBException {
		JAXBContext jc = getContext(jaxbClasses);
		Marshaller m = jc.createMarshaller();
		if (!tight) {
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		}
		StringWriter s = new StringWriter();
		m.marshal(object, s);
		return s.toString();
	}

	public static String xmlSerializeTight(Object object) {
		List<Class> classes = getContextClasses(object.getClass());
		try {
			return xmlSerialize(object, classes, true);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private JAXBContext jc = null;

	private Set<Class> jcClasses = new HashSet<Class>();

	private JaxbUtils() {
	}

	private JAXBContext getContext0(Collection<Class> classes)
			throws JAXBException {
		if (jc == null || !jcClasses.containsAll(classes)) {
			synchronized (jcClasses) {
				Map<String, String> emptyProps = new HashMap<String, String>();
				jcClasses.addAll(classes);
				Class[] clazzez = (Class[]) jcClasses
						.toArray(new Class[jcClasses.size()]);
				try {
					jc = JAXBContext.newInstance(clazzez, emptyProps);
				} catch (RuntimeException e) {
					jcClasses = new HashSet<Class>();
					throw e;
				}
			}
		}
		return jc;
	}

	@Registration(JaxbUtils.PreProcessor.class)
	public abstract static class PreProcessor {
		public abstract String preprocess(String xmlStr);
	}
}
