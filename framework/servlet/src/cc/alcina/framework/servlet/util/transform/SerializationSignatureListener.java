package cc.alcina.framework.servlet.util.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation.EntityCollation;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.Configuration;
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

	private boolean ensureFailed = false;

	Logger logger = LoggerFactory.getLogger(getClass());

	public synchronized String ensureSignature() {
		if (signature == null) {
			if (!Configuration.is("enabled")) {
				return null;
			}
			CountDownLatch latch = new CountDownLatch(1);
			TaskGenerateTreeSerializableSignatures task = new TaskGenerateTreeSerializableSignatures();
			AlcinaChildContextRunner
					.runInTransactionNewThread("Ensure signatures", () -> {
						try {
							/*
							 * in particular, lose
							 * TransformCommit.CONTEXT_COMMITTING
							 */
							LooseContext.getContext().clearProperties();
							/*
							 * NOT perform()
							 */
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
				ensureFailed = true;
				signature = Ax.format("(Exception generating signature) %s",
						System.currentTimeMillis());
			}
		}
		return signature;
	}

	public boolean isEnsureFailed() {
		return this.ensureFailed;
	}

	@Override
	public void onDomainTransformRequestPersistence(
			DomainTransformPersistenceEvent event) {
		if (!Configuration.is("enabled")) {
			return;
		}
		switch (event.getPersistenceEventType()) {
		case PREPARE_COMMIT:
			TransformPersistenceToken token = event
					.getTransformPersistenceToken();
			token.getTransformCollation().ensureCurrent();
			List<SetInstruction> setInstructions = new ArrayList<>();
			for (DomainTransformEvent transform : token.getRequest()
					.allTransforms()) {
				if (transform
						.getTransformType() == TransformType.CHANGE_PROPERTY_SIMPLE_VALUE) {
					if (transform.getPropertyName().matches("(.+)Serialized")) {
						checkApplySignature(token, transform, setInstructions);
					}
				}
			}
			if (setInstructions.size() > 0) {
				String signature = ensureSignature();
				token.getTransformCollation().ensureCurrent();
				setInstructions.forEach(i -> i.serializedSignatureProperty
						.set(i.entity, signature));
				token.getTransformCollation().ensureCurrent();
			}
		}
	}

	class SetInstruction {
		Property serializedSignatureProperty;

		Entity entity;

		public SetInstruction(Property serializedSignatureProperty,
				Entity entity) {
			this.serializedSignatureProperty = serializedSignatureProperty;
			this.entity = entity;
		}
	}

	private void checkApplySignature(TransformPersistenceToken token,
			DomainTransformEvent transform,
			List<SetInstruction> setInstructions) {
		Class entityClass = transform.getObjectClass();
		String propertyName = transform.getPropertyName();
		Property property = Reflections.at(transform.getObjectClass())
				.property(transform.getPropertyName());
		String sourcePropertyname = propertyName.replaceFirst("(.+)Serialized",
				"$1");
		Property toSerializeProperty = Reflections.at(entityClass)
				.property(sourcePropertyname);
		boolean serializedPropertyChange = toSerializeProperty != null
				&& toSerializeProperty.annotation(DomainProperty.class) != null
				&& toSerializeProperty.annotation(DomainProperty.class)
						.serialize();
		if (serializedPropertyChange) {
			String signaturePropertyName = sourcePropertyname + "Signature";
			if (Reflections.at(entityClass)
					.hasProperty(signaturePropertyName)) {
				Property serializedSignatureProperty = Reflections
						.at(entityClass).property(signaturePropertyName);
				AdjunctTransformCollation collation = token
						.getTransformCollation();
				EntityCollation entityCollation = collation
						.forLocator(transform.toObjectLocator());
				if (entityCollation == null) {
					logger.warn("Null collation for serialized transform : {}",
							transform.toObjectLocator());
					// FIXME - devex
				} else {
					if (!entityCollation.isDeleted()) {
						setInstructions.add(
								new SetInstruction(serializedSignatureProperty,
										entityCollation.getEntity()));
					}
				}
			}
		}
	}
}
