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
package cc.alcina.framework.entity.entityaccess;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.Lob;
import javax.persistence.Transient;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.domaintransform.WrappedObjectProvider;
import cc.alcina.framework.entity.util.JaxbUtils;

/**
 * 
 * @author Nick Reddel
 */
public interface WrappedObject<T extends WrapperPersistable> extends HasId {
    public static final String CONTEXT_CLASSES = WrappedObject.class.getName()
            + ".CONTEXT_CLASSES";

    @Transient
    public T getObject();

    @Transient
    public abstract T getObject(ClassLoader classLoader);

    @Lob
    public abstract String getSerializedXml();

    public abstract IUser getUser();

    public abstract void setObject(T object);

    public abstract void setSerializedXml(String serializedXml);

    public abstract void setUser(IUser user);

    public static class WrappedObjectHelper {
        static List<Class> jaxbSubclasses = null;

        public static <T> T clone(T object) {
            try {
                String s = xmlSerialize(object);
                return (T) xmlDeserialize(object.getClass(), s);
            } catch (Exception e) {
                throw new WrappedRuntimeException(e);
            }
        }

        public static synchronized List<Class> ensureJaxbSubclasses(
                Class addClass) {
            if (jaxbSubclasses == null) {
                jaxbSubclasses = Registry.impl(WrappedObjectProvider.class)
                        .getJaxbSubclasses();
            }
            if (addClass == null) {
                return new ArrayList<Class>(jaxbSubclasses);
            }
            ArrayList<Class> classes = new ArrayList<Class>(jaxbSubclasses);
            classes.add(0, addClass);
            return classes;
        }

        public static synchronized void withoutRegistry() {
            jaxbSubclasses = new ArrayList<>();
        }

        @SuppressWarnings("unchecked")
        public static <T> T xmlDeserialize(Class<T> clazz, String xmlStr) {
            if (xmlStr == null) {
                return null;
            }
            try {
                List<Class> classes = getContextClasses(clazz);
                JAXBContext jc = JaxbUtils.getContext(classes);
                Unmarshaller um = jc.createUnmarshaller();
                StringReader sr = new StringReader(xmlStr);
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
                Collection<Class> jaxbClasses, boolean tight)
                throws JAXBException {
            JAXBContext jc = JaxbUtils.getContext(jaxbClasses);
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

        protected static <T> List<Class> getContextClasses(Class<T> clazz) {
            List<Class> classes = null;
            if (!LooseContext.containsKey(CONTEXT_CLASSES)) {
                classes = ensureJaxbSubclasses(clazz);
            } else {
                classes = new ArrayList<>(
                        (List) LooseContext.get(CONTEXT_CLASSES));
            }
            return classes;
        }
    }
}