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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Lob;
import javax.persistence.Transient;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import cc.alcina.framework.common.client.entity.GwtPersistableObject;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.entity.datatransform.EntityLayerLocator;
import cc.alcina.framework.entity.util.JaxbUtils;


/**
 *
 * @author Nick Reddel
 */

 public interface WrappedObject<T extends GwtPersistableObject> extends HasId {
	@Transient
	public  T getObject();
	@Transient
	public abstract T getObject(ClassLoader classLoader);

	public abstract void setObject(T object);

	public abstract String getKey();

	public abstract void setKey(String key);

	@Lob
	public abstract String getSerializedXml();

	public abstract void setSerializedXml(String serializedXml);

	public abstract void setUser(IUser user);

	public abstract IUser getUser();

	public static class WrappedObjectHelper {
		public static String xmlSerialize(Object object) throws JAXBException {
			List<Class> classes = EntityLayerLocator.get().wrappedObjectProvider()
					.getJaxbSubclasses();
			Map<String, String> emptyProps = new HashMap<String, String>();
			classes.add(0, object.getClass());
			JAXBContext jc = JaxbUtils.getContext(classes);
			Marshaller m = jc.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			StringWriter s = new StringWriter();
			m.marshal(object, s);
			return s.toString();
		}
		@SuppressWarnings("unchecked")
		public static <T> T xmlDeserialize(Class<T> clazz, String xmlStr) throws JAXBException {
			List<Class> classes = EntityLayerLocator.get().wrappedObjectProvider()
					.getJaxbSubclasses();
			classes.add(0, clazz);
			JAXBContext jc = JaxbUtils.getContext(classes);
			Unmarshaller um = jc.createUnmarshaller();
			StringReader sr = new StringReader(xmlStr);
			return (T) um.unmarshal(sr);
		}
	}
}