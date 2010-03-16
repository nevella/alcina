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

package cc.alcina.framework.gwt.client.ide.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.DisplayInfo;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.VisualiserInfo;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.gwt.client.ide.provider.PropertyCollectionProvider;
import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.TreeItem;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class NodeFactory {
	protected static final StandardDataImages images = GWT
			.create(StandardDataImages.class);

	protected NodeFactory() {
		super();
	}

	private static NodeFactory theInstance;

	public static NodeFactory get() {
		if (theInstance == null) {
			theInstance = new NodeFactory();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}

	public void registerNodeFactory(NodeFactory nodeFactory) {
		theInstance = nodeFactory;
	}

	private Set<SourcesPropertyChangeEvents> childlessBindables = new HashSet<SourcesPropertyChangeEvents>();
	@SuppressWarnings("unchecked")
	protected DomainNode createDomainNode(SourcesPropertyChangeEvents domainObject) {
		return new DomainNode(domainObject);
	}

	public DomainNode getNodeForDomainObject(SourcesPropertyChangeEvents domainObject) {
		DomainNode dn = createDomainNode(domainObject);
		if (childlessBindables.contains(domainObject.getClass())) {
			return dn;
		}
		boolean isChildlessPoorThing = true;
		ClientBeanReflector bi = ClientReflector.get().beanInfoForClass(
				domainObject.getClass());
		Collection<ClientPropertyReflector> prs = bi.getPropertyReflectors()
				.values();
		Class<? extends Object> c = domainObject.getClass();
		ObjectPermissions op = bi.getAnnotation(ObjectPermissions.class);
		BeanInfo beanInfo = bi.getAnnotation(BeanInfo.class);
		Map<Integer, TreeItem> createdNodes = new HashMap<Integer, TreeItem>();
		for (ClientPropertyReflector pr : prs) {
			PropertyPermissions pp = pr
					.getAnnotation(PropertyPermissions.class);
			VisualiserInfo visualiserInfo = pr.getGwPropertyInfo();
			boolean fieldVisible = PermissionsManager.get()
					.checkEffectivePropertyPermission(op, pp, domainObject,
							true)
					&& visualiserInfo != null
					&& PermissionsManager.get().isPermissible(domainObject,
							visualiserInfo.visible())
					&& ((visualiserInfo.displayInfo().displayMask() & DisplayInfo.DISPLAY_AS_TREE_NODE) != 0);
			if (!fieldVisible) {
				continue;
			}
			isChildlessPoorThing = false;
			boolean withoutContainer = (visualiserInfo.displayInfo()
					.displayMask() & DisplayInfo.DISPLAY_AS_TREE_NODE_WITHOUT_CONTAINER) != 0;
			PropertyCollectionProvider provider = new PropertyCollectionProvider(
					domainObject, pr);
			if (withoutContainer) {
				//note - should only happen for one property
				((DomainCollectionProviderNode) dn)
						.setCollectionProvider(provider);
			} else {
				CollectionProviderNode node = new CollectionProviderNode(
						provider, TextProvider.get().getLabelText(c, pr),
						images.folder());
				createdNodes.put(visualiserInfo.displayInfo().orderingHint(),
						node);
			}
		}
		ArrayList<Integer> l = new ArrayList<Integer>(createdNodes.keySet());
		Collections.sort(l);
		for (Integer integer : l) {
			dn.addItem(createdNodes.get(integer));
		}
		if (isChildlessPoorThing && dn.getChildCount() == 0) {
			childlessBindables.add(domainObject);
		}
		return dn;
	}
}
