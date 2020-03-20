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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.TreeItem;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.SortedMultimap;
import cc.alcina.framework.gwt.client.ide.provider.LazyCollectionProvider;
import cc.alcina.framework.gwt.client.ide.provider.PropertyCollectionProvider;
import cc.alcina.framework.gwt.client.ide.provider.UmbrellaCollectionProviderMultiplexer.UmbrellaCollectionProvider;
import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImages;

/**
 * 
 * @author Nick Reddel
 */
public class NodeFactory {
    protected static final StandardDataImages images = GWT
            .create(StandardDataImages.class);

    public static NodeFactory get() {
        NodeFactory singleton = Registry.checkSingleton(NodeFactory.class);
        if (singleton == null) {
            singleton = new NodeFactory();
            Registry.registerSingleton(NodeFactory.class, singleton);
        }
        return singleton;
    }

    private Set<Class> childlessBindables = new HashSet<>();

    private Class lastDomainObjectClass;

    private NodeCreator nodeCreator;

    private Multimap<Class, List<ClientPropertyReflector>> subCollectionFolders = new Multimap<Class, List<ClientPropertyReflector>>();

    protected NodeFactory() {
        super();
    }

    public DomainNode getNodeForDomainObject(
            SourcesPropertyChangeEvents domainObject) {
        DomainNode dn = createDomainNode(domainObject);
        if (childlessBindables.contains(domainObject.getClass())) {
            return dn;
        }
        boolean isChildlessPoorThing = true;
        ClientBeanReflector bi = ClientReflector.get()
                .beanInfoForClass(domainObject.getClass());
        Collection<ClientPropertyReflector> prs = bi.getPropertyReflectors()
                .values();
        Class<? extends Object> c = domainObject.getClass();
        ObjectPermissions op = bi.getAnnotation(ObjectPermissions.class);
        Bean beanInfo = bi.getAnnotation(Bean.class);
        SortedMultimap<Integer, List<TreeItem>> createdNodes = new SortedMultimap<Integer, List<TreeItem>>();
        if (!subCollectionFolders.containsKey(c)) {
            subCollectionFolders.getAndEnsure(c);
            for (ClientPropertyReflector pr : prs) {
                PropertyPermissions pp = pr
                        .getAnnotation(PropertyPermissions.class);
                Display displayInfo = pr.getDisplayInfo();
                boolean fieldVisible = displayInfo != null
                        && ((displayInfo.displayMask()
                                & Display.DISPLAY_AS_TREE_NODE) != 0)
                        && PermissionsManager.get()
                                .checkEffectivePropertyPermission(op, pp,
                                        domainObject, true)
                        && PermissionsManager.get().isPermissible(domainObject,
                                displayInfo.visible());
                if (fieldVisible) {
                    subCollectionFolders.add(c, pr);
                }
            }
        }
        for (ClientPropertyReflector pr : subCollectionFolders.get(c)) {
            Display displayInfo = pr.getDisplayInfo();
            isChildlessPoorThing = false;
            boolean withoutContainer = (displayInfo.displayMask()
                    & Display.DISPLAY_AS_TREE_NODE_WITHOUT_CONTAINER) != 0;
            boolean lazyCollectionNode = (displayInfo.displayMask()
                    & Display.DISPLAY_LAZY_COLLECTION_NODE) != 0;
            // this (lazyCollectionNode) is not implemented - it'd be sort of
            // hard (but possible)
            // main thing is, we'd need a parallel (tree) structure of
            // collections...note not sure about this doc, if ya care, check
            // it...
            PropertyCollectionProvider provider = new PropertyCollectionProvider(
                    domainObject, pr);
            if (withoutContainer) {
                // note - should only happen for one property
                ((DomainCollectionProviderNode) dn)
                        .setCollectionProvider(provider);
            } else {
                // ContainerNode node = lazyCollectionNode ? new
                // UmbrellaProviderNode(
                // provider, TextProvider.get().getLabelText(c, pr),
                // images.folder(), false, this)
                // : new CollectionProviderNode(provider, TextProvider
                // .get().getLabelText(c, pr), images.folder(),
                // false, this);
                ContainerNode node = new CollectionProviderNode(provider,
                        TextProvider.get().getLabelText(c, pr), images.folder(),
                        false, this);
                createdNodes.add(displayInfo.orderingHint(), node);
            }
        }
        for (TreeItem item : createdNodes.allItems()) {
            dn.addItem(item);
        }
        if (isChildlessPoorThing && dn.getChildCount() == 0) {
            childlessBindables.add(domainObject.getClass());
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

    private UmbrellaProviderNode getNodeForUmbrella(
            LazyCollectionProvider providerChild) {
        return new UmbrellaProviderNode(providerChild, null, null, this);
    }

    
    protected DomainNode createDomainNode(
            SourcesPropertyChangeEvents domainObject) {
        Class clazz = domainObject.getClass();
        if (lastDomainObjectClass != clazz) {
            nodeCreator = (NodeCreator) Registry.get()
                    .instantiateSingle(NodeCreator.class, clazz);
        }
        return nodeCreator.createDomainNode(domainObject, this);
    }

    @RegistryLocation(registryPoint = NodeCreator.class)
    @ClientInstantiable
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
