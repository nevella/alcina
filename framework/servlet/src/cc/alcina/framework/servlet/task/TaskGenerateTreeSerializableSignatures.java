package cc.alcina.framework.servlet.task;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.domain.UserProperty;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer.SerializerOptions;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer.SerializerOptions.Reachables;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.entity.EncryptionUtils;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.projection.GraphTraversal;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.servlet.schedule.PerformerTask;

/**
 * <p>
 * Generates the runtime signature of the reachable serializable objects, with
 * awareness of what will be serialized (i.e. not pure-client classes which
 * aren't, although they implement say {@code Model})
 *
 *
 *
 *
 *
 *
 */
/*
 * @formatter:off
 *
- only shared (client/server) paths
- remove unneeded typeserialization
- ditto remove unneeded @Bean, @Reflected
- TaskGenerateReflectiveSerializerSignatures annotations

 * @formatter:on
 */
@Bean(PropertySource.FIELDS)
public class TaskGenerateTreeSerializableSignatures extends PerformerTask {
	public transient TreeSerializableSignatures signatures = new TreeSerializableSignatures();

	private transient List<Field> missingPropertyDescriptors = new ArrayList<>();

	private transient List<String> serializationIssues = new ArrayList<>();

	private transient String signature;

	private void checkAllFieldsAreProperties(TreeSerializable serializable) {
		Field[] fields = serializable.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (Modifier.isTransient(field.getModifiers())
					|| Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			Property property = Reflections.at(serializable)
					.property(field.getName());
			if (property == null) {
				missingPropertyDescriptors.add(field);
			}
		}
	}

	public void checkSerializationIssues(TreeSerializable serializable) {
		try {
			LooseContext.pushWithTrue(
					TreeSerializable.CONTEXT_IGNORE_CUSTOM_CHECKS);
			Reachables reachables = new SerializerOptions.Reachables();
			FlatTreeSerializer.serialize(serializable,
					new SerializerOptions().withElideDefaults(false)
							.withShortPaths(true).withTestSerialized(true)
							.withTestSerializedReachables(reachables));
			while (reachables.pending.size() > 0) {
				Iterator<Class<? extends TreeSerializable>> itr = reachables.pending
						.iterator();
				Class<? extends TreeSerializable> clazz = itr.next();
				itr.remove();
				reachables.traversed.add(clazz);
				if (Reflections.at(clazz).isAbstract()) {
					continue;
				}
				FlatTreeSerializer.serialize(Reflections.newInstance(clazz),
						new SerializerOptions().withElideDefaults(false)
								.withShortPaths(true).withTestSerialized(true)
								.withTestSerializedReachables(reachables));
			}
		} catch (Exception e) {
			String message = Ax.format("%s - %s",
					NestedName.get(serializable.getClass()),
					CommonUtils.toSimpleExceptionMessage(e));
			// e.printStackTrace();
			Ax.err(message);
			serializationIssues.add(message);
		} finally {
			LooseContext.pop();
		}
	}

	boolean filter(TreeSerializable treeSerializable) {
		Class<? extends TreeSerializable> clazz = treeSerializable.getClass();
		TypeSerialization typeSerialization = clazz
				.getAnnotation(TypeSerialization.class);
		if (typeSerialization != null
				&& !typeSerialization.flatSerializable()) {
			return false;
		}
		if (omitDueToChecksAnnotation(clazz)) {
			return false;
		}
		return true;
	}

	boolean omitDueToChecksAnnotation(Class<?> clazz) {
		if (clazz.getName().startsWith("java")) {
			return false;
		}
		AnnotatedElement element = clazz;
		while (element != null) {
			if (element
					.isAnnotationPresent(ReflectiveSerializer.Checks.class)) {
				return element.getAnnotation(ReflectiveSerializer.Checks.class)
						.ignore();
			}
			if (element instanceof Class) {
				element = ((Class) element).getPackage();
			} else {
				Package pkg = (Package) element;
				element = SEUtilities.getNearestAncestorPackage(pkg, clazz);
			}
		}
		return false;
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
					new SerializerOptions().withElideDefaults(false)
							.withShortPaths(false).withTestSerialized(true));
		} catch (RuntimeException e) {
			Ax.simpleExceptionOut(e);
			serializedDefaults = FlatTreeSerializer.serialize(serializable,
					new SerializerOptions().withElideDefaults(false)
							.withShortPaths(false).withTestSerialized(false));
			Ax.out(serializedDefaults);
			throw e;
		}
		signatures.getClassNameDefaultSerializedForms()
				.put(serializable.getClass().getName(), serializedDefaults);
	}

	@AlcinaTransient
	public String getSignature() {
		return this.signature;
	}

	@Override
	public void run() throws Exception {
		List<TreeSerializable> serializables = Registry
				.query(TreeSerializable.class).implementations()
				.sorted(Comparator.comparing(c -> c.getClass().getName()))
				.filter(this::filter).collect(Collectors.toList());
		serializables.forEach(this::checkSerializationIssues);
		Preconditions.checkState(serializationIssues.isEmpty());
		serializables.forEach(this::checkAllFieldsAreProperties);
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
		serializables.forEach(this::generateSignature);
		TreeSerializableSignatures stableCheck = signatures;
		signatures = new TreeSerializableSignatures();
		serializables.forEach(this::generateSignature);
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
		if (DomainStore.hasStores()) {
			MethodContext.instance().withRootPermissions(true).run(() -> {
				UserProperty.ensure(key).domain().ensurePopulated()
						.setValue(signaturesBytes);
				Transaction.commit();
			});
		}
		logger.info("TreeSerializable serializedDefaults signature: ({}) : {} ",
				signatures.classNameDefaultSerializedForms.size(), sha1);
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
