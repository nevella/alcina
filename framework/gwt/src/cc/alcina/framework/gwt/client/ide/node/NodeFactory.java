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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.TreeItem;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.SortedMultimap;
import cc.alcina.framework.gwt.client.ide.provider.LazyCollectionProvider;
import cc.alcina.framework.gwt.client.ide.provider.PropertyCollectionProvider;
import cc.alcina.framework.gwt.client.ide.provider.UmbrellaCollectionProviderMultiplexer.UmbrellaCollectionProvider;
import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImages;

/**
 * @author Nick Reddel
 */
@Registration.Singleton
public class NodeFactory {
	protected static final StandardDataImages images = GWT
			.create(StandardDataImages.class);

	public static NodeFactory get() {
		return Registry.impl(NodeFactory.class);
	}

	private Set<Class> childlessBindables = new HashSet<>();

	private Class lastDomainObjectClass;

	private NodeCreator nodeCreator;

	private Multimap<Class, List<Property>> subCollectionFolders = new Multimap<Class, List<Property>>();

	protected NodeFactory() {
		super();
	}

	public DomainNode
			getNodeForDomainObject(SourcesPropertyChangeEvents domainObject) {
		DomainNode dn = createDomainNode(domainObject);
		Class<? extends SourcesPropertyChangeEvents> bindableClass = domainObject
				.getClass();
		if (childlessBindables.contains(bindableClass)) {
			return dn;
		}
		ClassReflector<? extends SourcesPropertyChangeEvents> classReflector = Reflections
				.at(bindableClass);
		ObjectPermissions op = classReflector
				.annotation(ObjectPermissions.class);
		Bean beanInfo = classReflector.annotation(Bean.class);
		SortedMultimap<Integer, List<TreeItem>> createdNodes = new SortedMultimap<Integer, List<TreeItem>>();
		List<Property> visibleProperties = subCollectionFolders.computeIfAbsent(
				bindableClass,
				c -> classReflector.properties().stream().filter(property -> {
					PropertyPermissions pp = property
							.annotation(PropertyPermissions.class);
					Display display = property.annotation(Display.class);
					boolean fieldVisible = display != null
							&& ((display.displayMask()
									& Display.DISPLAY_AS_TREE_NODE) != 0)
							&& PermissionsManager.get()
									.checkEffectivePropertyPermission(op, pp,
											domainObject, true)
							&& PermissionsManager.get().isPermitted(
									domainObject, display.visible());
					return fieldVisible;
				}).collect(Collectors.toList()));
		visibleProperties.forEach(property -> {
			Display display = property.annotation(Display.class);
			boolean withoutContainer = (display.displayMask()
					& Display.DISPLAY_AS_TREE_NODE_WITHOUT_CONTAINER) != 0;
			boolean lazyCollectionNode = (display.displayMask()
					& Display.DISPLAY_LAZY_COLLECTION_NODE) != 0;
			// this (lazyCollectionNode) is not implemented - it'd be sort of
			// hard (but possible)
			// main thing is, we'd need a parallel (tree) structure of
			// collections...note not sure about this doc, if ya care, check
			// it...
			PropertyCollectionProvider provider = new PropertyCollectionProvider(
					domainObject, property);
			if (withoutContainer) {
				// note - should only happen for one property
				((DomainCollectionProviderNode) dn)
						.setCollectionProvider(provider);
			} else {
				ContainerNode node = new CollectionProviderNode(provider,
						TextProvider.get().getLabelText(bindableClass,
								property),
						images.folder(), false, this);
				createdNodes.add(display.orderingHint(), node);
			}
		});
		for (TreeItem item : createdNodes.allItems()) {
			dn.addItem(item);
		}
		if (visibleProperties.isEmpty() && dn.getChildCount() == 0) {
			childlessBindables.add(bindableClass);
		}
		return dn;
	}

	public TreeItem getNodeForObject(Object object) {
		if (object instanceof SourcesPropertyChangeEvents) {
			return getNodeForDomainObject((SourcesPropertyChangeEvents) object);
		}
		if (object instanceof UmbrellaCollectionProvider) {
			return getNodeForUmbrella((LazyCollectionProvider) object);
		}
		return null;
	}

	private UmbrellaProviderNode
			getNodeForUmbrella(LazyCollectionProvider providerChild) {
		return new UmbrellaProviderNode(providerChild, null, null, this);
	}

	protected DomainNode
			createDomainNode(SourcesPropertyChangeEvents domainObject) {
		Class clazz = domainObject.getClass();
		if (lastDomainObjectClass != clazz) {
			nodeCreator = Registry.query(NodeCreator.class).addKeys(clazz)
					.impl();
		}
		return nodeCreator.createDomainNode(domainObject, this);
	}

	@RegistryLocation(registryPoint = NodeCreator.class)
	@ClientInstantiable
	@Registration(NodeCreator.class)
	public static class DefaultNodeCreator implements NodeCreator {
		@Override
		public DomainNode createDomainNode(
				SourcesPropertyChangeEvents domainObject, NodeFactory factory) {
			return new DomainNode(domainObject, factory);
		}
	}

	public static interface NodeCreator {
		public DomainNode createDomainNode(
				SourcesPropertyChangeEvents domainObject, NodeFactory factory);
	}
}
