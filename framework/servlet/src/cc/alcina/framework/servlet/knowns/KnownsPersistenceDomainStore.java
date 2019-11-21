package cc.alcina.framework.servlet.knowns;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.KnownRenderableNode;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.entityaccess.KnownNodePersistentDomainStore;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.servlet.knowns.Knowns.ValueType;
import cc.alcina.framework.servlet.servlet.ServletLayerTransforms;

public class KnownsPersistenceDomainStore implements KnownsPersistence {

	@Override
	public void toPersistent(KnownNode node) {
        Stack<KnownNode> nodes = new Stack<KnownNode>();
        nodes.push(node);
        GraphProjection graphProjection = new GraphProjection();
        Class<? extends KnownNodePersistentDomainStore> persistentClass = Registry.get()
                .lookupSingle(KnownNodePersistentDomainStore.class, void.class);
        CachingMap<KnownNodePersistentDomainStore, KnownNodePersistentDomainStore> writeable = new CachingMap<>(
                kn -> kn.writeable());
        List<KnownNode> replaceWithPersistent = new ArrayList<>();
        try {
            while (!nodes.isEmpty()) {
                node = nodes.pop();
                
                KnownNodePersistentDomainStore persistent=(KnownNodePersistentDomainStore) node.persistent;
                if (persistent == null) {
                    persistent = Domain.create(persistentClass);
                    persistent.setName(node.name);
                    if (node.parent != null) {
                        persistent.setParent((KnownNodePersistentDomainStore) node.parent.persistent);
                        writeable.get((KnownNodePersistentDomainStore) node.parent.persistent).domain()
                                .addToProperty(persistent, "children");
                    }
                    node.persistent = persistent;
                    replaceWithPersistent.add(node);
                } else {
                    persistent = writeable.get(persistent);
                }
                StringMap properties = new StringMap();
                List<Field> fields = graphProjection
                        .getFieldsForClass(node.getClass());
                for (Field field : fields) {
                    if (Modifier.isTransient(field.getModifiers())) {
                        continue;
                    }
                    Object value = field.get(node);
                    Type type = field.getGenericType();
                    ValueType valueType = Knowns.getValueType(type);
                    switch (valueType) {
                    case DATA_TYPE:
                    case KRYO_PERSISTABLE: {
                        properties.put(field.getName(),
                        		Knowns.toStringValue(value, field, valueType));
                        break;
                    }
                    case KNOWN_NODE: {
                        if (value == null) {
                            throw Ax.runtimeException(
                                    "Field %s.%s is null - must be instantiated",
                                    node.path(), field.getName());
                        }
                        nodes.push((KnownNode) value);
                        break;
                    }
                    default:
                        throw new UnsupportedOperationException();
                    }
                }
                persistent.setProperties(properties.toPropertyString());
            }
            DomainTransformLayerWrapper wrapper = ServletLayerTransforms
                    .pushTransforms(null, true, true);
            replaceWithPersistent.stream().forEach(n -> {
                HiliLocator hiliLocator = wrapper.locatorMap
                        .getForLocalId(((KnownNodePersistentDomainStore)n.persistent).getLocalId());
                n.persistent = Domain.find(hiliLocator);
            });
            if (wrapper.persistentEvents.size() > 0) {
                synchronized (Knowns.reachableKnownsModificationNotifier) {
                	Knowns.lastModified = System.currentTimeMillis();
                	Knowns.reachableKnownsModificationNotifier.notifyAll();
                }
            }
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }		
	}

	@Override
	public KnownRenderableNode fromPersistent(KnownNode node) {
		 Class<? extends KnownNodePersistentDomainStore> persistentClass = Registry.get()
	                .lookupSingle(KnownNodePersistentDomainStore.class, void.class);
	        if (node.parent == null) {
	            node.persistent = Domain.byProperty(persistentClass, "parent",
	                    null);
	        }
	        CachingMap<KnownNode, KnownRenderableNode> renderableNodes = new CachingMap<>();
	        renderableNodes.setFunction(kn -> {
	            KnownRenderableNode renderableNode = new KnownRenderableNode();
	            if (kn != null) {
	                renderableNode.name = kn.name;
	                renderableNode.parent = renderableNodes.get(kn.parent);
	                renderableNode.parent.children.add(renderableNode);
	            }
	            return renderableNode;
	        });
	        Stack<KnownNode> nodes = new Stack<KnownNode>();
	        nodes.push(node);
	        GraphProjection graphProjection = new GraphProjection();
	        try {
	            while (!nodes.isEmpty()) {
	                node = nodes.pop();
	                KnownNodePersistentDomainStore persistent = (KnownNodePersistentDomainStore) node.persistent;
	                if (persistent == null) {
	                    continue;
	                }
	                persistent = persistent.domain().domainVersion();
	                KnownRenderableNode renderableNode = renderableNodes.get(node);
	                StringMap properties = StringMap
	                        .fromPropertyString(persistent.getProperties());
	                List<Field> fields = graphProjection
	                        .getFieldsForClass(node.getClass());
	                for (Field field : fields) {
	                    Type type = field.getGenericType();
	                    ValueType valueType = Knowns.getValueType(type);
	                    switch (valueType) {
	                    case DATA_TYPE:
	                    case KRYO_PERSISTABLE: {
	                        String value = properties.get(field.getName());
	                        Object typedValue = Knowns.fromStringValue(renderableNode.path(),value, field,
	                                valueType);
	                        field.set(node, typedValue);
	                        Knowns.mapToRenderablePropertyNode(renderableNode, value,
	                                typedValue, field.getName(),field,null);
	                        break;
	                    }
	                    case KNOWN_NODE: {
	                        Optional<KnownNodePersistentDomainStore> persistentChild = persistent
	                                .getChildren().stream().filter(n -> n.getName()
	                                        .equals(field.getName()))
	                                .findFirst();
	                        if (persistentChild.isPresent()) {
	                            Object object = field.get(node);
	                            KnownNode child = (KnownNode) object;
	                            child.persistent = persistentChild.get();
	                            KnownRenderableNode childRenderableNode = renderableNodes
	                                    .get(child);
	                            childRenderableNode.field = field;
	                            childRenderableNode.typedValue = child;
	                            nodes.push(child);
	                        }
	                        break;
	                    }
	                    default:
	                        throw new UnsupportedOperationException();
	                    }
	                }
	            }
	        } catch (Exception e) {
	            throw new WrappedRuntimeException(e);
	        }
	        KnownRenderableNode renderableRoot = renderableNodes.get(Knowns.root);
	        renderableRoot.allNodes().stream()
	                .filter(krn -> !Knowns.root.exportRenderable(krn))
	                .forEach(KnownRenderableNode::removeFromParent);
	        renderableRoot.allNodes().forEach(Knowns::handleStatusRule);
	        renderableRoot.allNodes().forEach(KnownRenderableNode::calculateStatus);
	        return renderableRoot;
	}
}
