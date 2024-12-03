package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.remote.ReflectiveCommonRemoteServiceAsync;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.util.Async;

/**
 * <p>
 * A node which models a reference to an entity, is decorated by user activity
 * (such as a # tag populated from a dropdown)
 *
 * <p>
 * It's better to use entitylocator rather than entity here - since (for various
 * reasons) the client may not have the entity itself loaded into the graph when
 * unmarshalling the node
 *
 * <p>
 * Note that the validation behaviour is slightly wrinkly wrt entities -
 * consider the following:
 * <ol>
 * <li>User creates a new entity via selecting some sort of 'create new'
 * decorator option
 * <li>Next time the editable is edited, the 'local to persistent' call (which
 * should convert the local entitylocator to its persistent equivalent) doesn't
 * return before editing is complete
 * <li>Any linked references that use the entity will be removed (since the
 * client cannot determine the entity corresponding to the locator)
 * </ol>
 *
 * @param <E>
 *            the type this decorator references
 */
public abstract class EntityNode<E extends Entity>
		extends DecoratorNode<E, EntityLocator> {
	protected Class<E> entityClass() {
		return Reflections.at(this).firstGenericBound();
	}

	void handleGetPersistentLocators(Map<EntityLocator, EntityLocator> map) {
		EntityLocator result = map.values().iterator().next();
		if (result != null) {
			setStringRepresentable(result);
		}
	}

	public void putEntity(Entity entity) {
		setStringRepresentable(entity.toLocator());
		String text = getDescriptor().triggerSequence()
				+ ((Function) getDescriptor().itemRenderer()).apply(entity);
		setContent(text);
	}

	/**
	 * This is only called when a chooser is *not* showing (post close or on
	 * contenteditable init), so isValid is logically correct
	 */
	public void validate() {
		if (!isValid()) {
			DomNode firstNode = domNode().children.firstNode();
			domNode().strip();
			if (firstNode.isText()) {
				// undo split
				firstNode.text().mergeWithAdjacentTexts();
			}
		}
		validateLocator();
	}

	void validateLocator() {
		EntityLocator entityLocator = stringRepresentable;
		if (entityLocator != null && entityLocator.getId() == 0 && entityLocator
				.getClientInstanceId() != ClientInstance.self().getId()) {
			Set<EntityLocator> locators = new LinkedHashSet<>();
			locators.add(entityLocator);
			AsyncCallback<Map<EntityLocator, EntityLocator>> callback = Async
					.<Map<EntityLocator, EntityLocator>> callbackBuilder()
					.success(this::handleGetPersistentLocators).build();
			ReflectiveCommonRemoteServiceAsync.get()
					.getPersistentLocators(locators, callback);
		}
	}

	public static class ContextLocatorTransform
			implements Binding.Bidi<EntityLocator> {
		@Override
		public Function<EntityLocator, String> leftToRight() {
			return new ContextLocatorTransformLeft();
		}

		@Override
		public Function<String, EntityLocator> rightToLeft() {
			return new ContextLocatorTransformRight();
		}
	}

	public static class ContextLocatorTransformLeft
			extends Binding.AbstractContextSensitiveTransform<EntityLocator> {
		@Override
		public String apply(EntityLocator t) {
			return t == null ? null : t.toRecoverableNumericString();
		}
	}

	public static class ContextLocatorTransformRight extends
			Binding.AbstractContextSensitiveReverseTransform<EntityLocator> {
		@Override
		public EntityLocator apply(String t) {
			EntityNode contextNode = node.getModel();
			return t == null ? null
					: EntityLocator.parse(contextNode.entityClass(), t);
		}
	}

	public static abstract class Descriptor<WT extends Entity>
			extends DecoratorNode.Descriptor<WT, EntityNode> {
	}
}