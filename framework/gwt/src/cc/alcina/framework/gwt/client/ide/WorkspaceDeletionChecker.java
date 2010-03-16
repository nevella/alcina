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

package cc.alcina.framework.gwt.client.ide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;

import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Property;

/**
 *
 * @author Nick Reddel
 */

 public class WorkspaceDeletionChecker {
	public static boolean enabled = false;
	@SuppressWarnings("unchecked")
	public boolean checkPropertyRefs(HasIdAndLocalId singleObj) {
		Class<? extends Object> clazz = singleObj.getClass();
		Map<Class<? extends HasIdAndLocalId>, Set<HasIdAndLocalId>> map = TransformManager
				.get().getDomainObjects().getCollnMap();
		String message = "";
		String template = TextProvider.get().getUiObjectText(getClass(),
				"unable-to-delete-detail",
				"Referred to by %1 '%2' [id:%3], property '%4'");
		String msgtitle = TextProvider.get().getUiObjectText(getClass(),
				"unable-to-delete-msg", "Unable to delete - object is ");
		try {
			TextProvider.get().setDecorated(false);
			TextProvider.get().setTrimmed(true);
			for (Class c : map.keySet()) {
				Set<HasIdAndLocalId> objs = (Set) map.get(c);
				if (objs.isEmpty()) {
					continue;
				}
				if (!(objs.iterator().next() instanceof HasIdAndLocalId)) {
					continue;
				}
				BeanDescriptor descriptor = null;
				try {
					descriptor = GwittirBridge.get().getDescriptorForClass(c);
				} catch (Exception e) {
					continue;
				}
				List<Property> checkProperties = new ArrayList<Property>();
				Property[] properties = descriptor.getProperties();
				for (Property p : properties) {
					if (p.getType() == clazz) {
						checkProperties.add(p);
					}
				}
				for (Property p : checkProperties) {
					for (HasIdAndLocalId o : objs) {
						Object pValue = p.getAccessorMethod().invoke(o,
								CommonUtils.EMPTY_OBJECT_ARRAY);
						if (pValue != null && pValue.equals(singleObj)) {
							message += CommonUtils.format(template, CommonUtils
									.simpleClassName(o.getClass()), TextProvider.get()
									.getObjectName(o), o.getId(), TextProvider
									.get().getLabelText(c, p.getName()))
									+ "\n";
						}
					}
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			TextProvider.get().setDecorated(true);
			TextProvider.get().setTrimmed(false);
		}
		if (message.length() > 0) {
			ClientLayerLocator.get().clientBase().showWarning(msgtitle,
					message.replace("\n", "<br>\n"));
		}
		return message.length() == 0;
	}
}
