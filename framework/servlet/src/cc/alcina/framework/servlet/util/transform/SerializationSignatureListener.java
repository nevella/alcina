package cc.alcina.framework.servlet.util.transform;

import java.util.concurrent.CountDownLatch;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation.EntityCollation;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.transform.AdjunctTransformCollation;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceListener;
import cc.alcina.framework.entity.util.AlcinaChildRunnable.AlcinaChildContextRunner;
import cc.alcina.framework.servlet.task.TaskGenerateTreeSerializableSignatures;

/**
 * Adds the serialization signature to persisted serialization properties if the
 * signature property name (xxxSignature) exists. There won't be a deadlock
 * issue with ensureSignature() because calling run() on that task doesn't
 * result in any transforms which would trigger ensureSignature().
 * 
 */
public class SerializationSignatureListener
		implements DomainTransformPersistenceListener {
	private String signature;

	public synchronized String ensureSignature() {
		if (signature == null) {
			if (!ResourceUtilities.is("enabled")) {
				return null;
			}
			CountDownLatch latch = new CountDownLatch(1);
			TaskGenerateTreeSerializableSignatures task = new TaskGenerateTreeSerializableSignatures();
			AlcinaChildContextRunner
					.runInTransactionNewThread("Ensure signatures", () -> {
						try {
							task.run();
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							latch.countDown();
						}
					});
			try {
				latch.await();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			signature = task.getSignature();
			if (signature == null) {
				signature = Ax.format("(exception generating signature) %s",
						System.currentTimeMillis());
			}
		}
		return signature;
	}

	@Override
	public void onDomainTransformRequestPersistence(
			DomainTransformPersistenceEvent event) {
		if (!ResourceUtilities.is("enabled")) {
			return;
		}
		switch (event.getPersistenceEventType()) {
		case PRE_COMMIT:
			TransformPersistenceToken token = event
					.getTransformPersistenceToken();
			for (DomainTransformEvent transform : token.getRequest()
					.allTransforms()) {
				if (transform
						.getTransformType() == TransformType.CHANGE_PROPERTY_SIMPLE_VALUE) {
					if (transform.getPropertyName().matches("(.+)Serialized")) {
						checkApplySignature(token, transform);
					}
				}
			}
		}
	}

	private void checkApplySignature(TransformPersistenceToken token,
			DomainTransformEvent transform) {
		Class entityClass = transform.getObjectClass();
		String propertyName = transform.getPropertyName();
		PropertyReflector propertyReflector = Reflections.classLookup()
				.getPropertyReflector(transform.getObjectClass(),
						transform.getPropertyName());
		String sourcePropertyname = propertyName.replaceFirst("(.+)Serialized",
				"$1");
		PropertyReflector toSerializeReflector = Reflections.classLookup()
				.getPropertyReflector(entityClass, sourcePropertyname);
		boolean serializedPropertyChange = toSerializeReflector != null
				&& toSerializeReflector
						.getAnnotation(DomainProperty.class) != null
				&& toSerializeReflector.getAnnotation(DomainProperty.class)
						.serialize();
		if (serializedPropertyChange) {
			String signaturePropertyName = sourcePropertyname + "Signature";
			if (Reflections.classLookup().hasProperty(entityClass,
					signaturePropertyName)) {
				PropertyReflector serializedSignatureReflector = Reflections
						.classLookup().getPropertyReflector(entityClass,
								signaturePropertyName);
				AdjunctTransformCollation transformCollation = token
						.getTransformCollation();
				transformCollation.ensureApplied();
				EntityCollation entityCollation = transformCollation
						.forLocator(transform.toObjectLocator());
				if (!entityCollation.isDeleted()) {
					serializedSignatureReflector.setPropertyValue(
							entityCollation.getObject(), ensureSignature());
					token.addCascadedEvents(false);
				}
			}
		}
	}
}
