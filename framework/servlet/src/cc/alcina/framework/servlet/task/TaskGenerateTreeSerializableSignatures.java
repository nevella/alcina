package cc.alcina.framework.servlet.task;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.Modifier;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.SystemProperty;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.SingleTableSearchDefinition;
import cc.alcina.framework.common.client.serializer.flat.FlatTreeSerializer;
import cc.alcina.framework.common.client.serializer.flat.FlatTreeSerializer.SerializerOptions;
import cc.alcina.framework.common.client.serializer.flat.TreeSerializable;
import cc.alcina.framework.common.client.serializer.flat.TypeSerialization;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.EncryptionUtils;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.projection.GraphTraversal;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

public class TaskGenerateTreeSerializableSignatures
		extends AbstractTaskPerformer {
	public transient TreeSerializableSignatures signatures = new TreeSerializableSignatures();

	private transient List<Field> missingPropertyDescriptors = new ArrayList<>();

	private transient List<String> serializationIssues = new ArrayList<>();

	private transient String signature;

	@AlcinaTransient
	public String getSignature() {
		return this.signature;
	}

	private void checkAllFieldsAreProperties(TreeSerializable serializable) {
		Field[] fields = serializable.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (Modifier.isTransient(field.getModifiers())
					|| Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			PropertyDescriptor propertyDescriptor = SEUtilities
					.getPropertyDescriptorByName(serializable.getClass(),
							field.getName());
			if (propertyDescriptor == null) {
				missingPropertyDescriptors.add(field);
			}
		}
	}

	private void checkSerializationIssues(TreeSerializable serializable) {
		try {
			FlatTreeSerializer.serialize(serializable,
					new SerializerOptions().withDefaults(false)
							.withShortPaths(true).withTestSerialized(true));
		} catch (Exception e) {
			String message = Ax.format("%s - %s",
					serializable.getClass().getSimpleName(),
					CommonUtils.toSimpleExceptionMessage(e));
			Ax.err(message);
			serializationIssues.add(message);
		}
	}

	private void generateSignature(TreeSerializable serializable) {
		String serializedDefaults = null;
		try {
			new GraphTraversal().traverse(serializable, o -> {
				if (o instanceof Date) {
					/*
					 * one of those times it's useful that Date is mutable...
					 */
					((Date) o).setTime(0);
				}
			});
			serializedDefaults = FlatTreeSerializer.serialize(serializable,
					new SerializerOptions().withDefaults(false)
							.withShortPaths(false).withTestSerialized(true));
		} catch (RuntimeException e) {
			Ax.simpleExceptionOut(e);
			serializedDefaults = FlatTreeSerializer.serialize(serializable,
					new SerializerOptions().withDefaults(false)
							.withShortPaths(false).withTestSerialized(false));
			Ax.out(serializedDefaults);
			throw e;
		}
		signatures.getClassNameDefaultSerializedForms()
				.put(serializable.getClass().getName(), serializedDefaults);
	}

	@Override
	protected void run0() throws Exception {
		Registry.impls(TreeSerializable.class).stream().filter(this::filter)
				.forEach(this::checkSerializationIssues);
		Preconditions.checkState(serializationIssues.isEmpty());
		Registry.impls(TreeSerializable.class).stream().filter(this::filter)
				.forEach(this::checkAllFieldsAreProperties);
		if (!missingPropertyDescriptors.isEmpty()) {
			missingPropertyDescriptors.stream().collect(
					AlcinaCollectors.toKeyMultimap(Field::getDeclaringClass))
					.entrySet()
					.forEach(e -> Ax.out("%s :: %s", e.getKey().getSimpleName(),
							e.getValue().stream().map(Field::getName)
									.collect(Collectors.toList())));
		}
		Preconditions.checkState(missingPropertyDescriptors.isEmpty(),
				"Missing property descriptors");
		Registry.impls(TreeSerializable.class).stream().filter(this::filter)
				.forEach(this::generateSignature);
		TreeSerializableSignatures stableCheck = signatures;
		signatures = new TreeSerializableSignatures();
		Registry.impls(TreeSerializable.class).stream().filter(this::filter)
				.forEach(this::generateSignature);
		Preconditions
				.checkState(
						signatures.getClassNameDefaultSerializedForms()
								.equals(stableCheck
										.getClassNameDefaultSerializedForms()),
						"Signature stable check not equal");
		String signaturesBytes = new JacksonJsonObjectSerializer()
				.withBase64Encoding().serialize(signatures);
		String sha1 = EncryptionUtils.get().SHA1(signaturesBytes);
		this.signature = sha1;
		String key = Ax.format("%s::%s",
				TreeSerializableSignatures.class.getName(), sha1);
		SystemProperty.ensure(key).domain().ensurePopulated()
				.setValue(signaturesBytes);
		Transaction.commit();
		logger.info("TreeSerializable serializedDefaults signature: ({}) : {} ",
				signatures.classNameDefaultSerializedForms.size(), sha1);
	}

	boolean filter(TreeSerializable treeSerializable) {
		if (treeSerializable instanceof SingleTableSearchDefinition) {
			return false;
		}
		if (Ax.isTest()) {
			try {
				Class<?> devConsoleRunnableClass = Class.forName(
						"cc.alcina.extras.dev.console.DevConsoleRunnable");
				if (devConsoleRunnableClass
						.isAssignableFrom(treeSerializable.getClass())) {
					return false;
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		TypeSerialization typeSerialization = treeSerializable.getClass()
				.getAnnotation(TypeSerialization.class);
		if (typeSerialization != null && typeSerialization.notSerializable()) {
			return false;
		}
		return true;
	}

	public static class TreeSerializableSignatures {
		private Map<String, String> classNameDefaultSerializedForms = new TreeMap<>();

		public Map<String, String> getClassNameDefaultSerializedForms() {
			return this.classNameDefaultSerializedForms;
		}

		public void setClassNameDefaultSerializedForms(
				Map<String, String> classNameDefaultSerializedForms) {
			this.classNameDefaultSerializedForms = classNameDefaultSerializedForms;
		}
	}
}
