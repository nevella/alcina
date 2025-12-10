package cc.alcina.framework.servlet.task;

import java.io.File;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.rpc.GwtTransient;
import com.totsp.gwittir.client.beans.annotations.Omit;

import cc.alcina.framework.common.client.logic.domain.UserProperty;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.EncryptionUtils;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.registry.CachingScanner;
import cc.alcina.framework.entity.registry.ClassMetadata;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.util.ClasspathScanner.ServletClasspathScanner;
import cc.alcina.framework.entity.util.DataFolderProvider;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.servlet.schedule.PerformerTask;
import cc.alcina.framework.servlet.task.TaskGenerateReflectiveSerializerSignatures.Scanner.ReflectiveScannerMetadata;

/*
 * FIXME - dirndl 1x1f - reflection - do a full inheritance scan to ensure all
 * implementors in the graph are serializable (ReflectiveSerializer.Checks is
 * the current workaround, but allows invalid subtypes)
 *
 * This class ensures that - excepting exclusions - all classes that are
 * ReflectiveSerializable have a valid schema:
 *
 * - non-tranient fields have getter/setters
 *
 * - non-transient properties have serializable types
 * 
 * In production environments, an exception when running this task will cancel
 * startup (since there are potential serialization issues)
 * 
 * Omit checks for a type with either via the Configuration package filter
 * string or by adding ReflectiveSerializer.Checks(ignore=true) to the type (or
 * an ancestor package)
 */
public class TaskGenerateReflectiveSerializerSignatures extends PerformerTask {
	public transient ReflectiveSerializableSignatures signatures = new ReflectiveSerializableSignatures();

	private transient List<Property> incorrectProperty = new ArrayList<>();

	private transient List<String> serializationIssues = new ArrayList<>();

	private transient String signature;

	private transient Set<Class<?>> serializables;

	private void addPropertyIssue(Property property) {
		incorrectProperty.add(property);
	}

	void checkAllTransientFieldsWithPropertiesAreTransient(Class<?> clazz) {
		if (clazz.isEnum()) {
			return;
		}
		if (omitDueToChecksAnnotation(clazz)) {
			return;
		}
		ClassReflector reflector = Reflections.at(clazz);
		if (reflector.isAbstract()) {
			return;
		}
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			if (Modifier.isTransient(field.getModifiers())
					|| field.isAnnotationPresent(GwtTransient.class)) {
				Property property = reflector.property(field.getName());
				if (property != null && !property.has(AlcinaTransient.class)
						&& !property.has(Omit.class)) {
					addPropertyIssue(property);
				}
			}
		}
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

	private void checkSerializationIssues(Class<?> clazz) {
		try {
			checkSerializationIssues0(clazz);
		} catch (Exception e) {
			e.printStackTrace();
			serializationIssues.add(Ax.format("%s - %s", clazz.getSimpleName(),
					CommonUtils.toSimpleExceptionMessage(e)));
		}
	}

	private void checkSerializationIssues0(Class<?> clazz) {
		if (omitDueToChecksAnnotation(clazz)) {
			return;
		}
		if (clazz.isAnnotationPresent(TypeSerialization.class)) {
			if (!clazz.getAnnotation(TypeSerialization.class)
					.reflectiveSerializable()) {
				return;
			}
		}
		ClassReflector<?> reflector = Reflections.at(clazz);
		reflector.properties().stream()
				.filter(Property::provideReadWriteNonTransient)
				.filter(property -> !property.has(AlcinaTransient.class)
						&& !property.has(Omit.class))
				.filter(property -> !(property.has(PropertySerialization.class)
						&& property.annotation(PropertySerialization.class)
								.notTestable()))
				.forEach(property -> {
					Class<?> type = property.getType();
					boolean result = ReflectiveSerializer.hasSerializer(type);
					if (!result) {
						result = ReflectiveSerializer.hasSerializer(
								PersistentImpl.getImplementationOrSelf(type));
					}
					if (omitDueToChecksAnnotation(type)) {
						result = true;
					}
					if (type.isAnnotationPresent(
							ReflectiveSerializer.Checks.class)) {
						result |= type
								.getAnnotation(
										ReflectiveSerializer.Checks.class)
								.hasReflectedSubtypes();
					}
					if (property.has(ReflectiveSerializer.Checks.class)) {
						result |= property
								.annotation(ReflectiveSerializer.Checks.class)
								.hasReflectedSubtypes()
								|| property.annotation(
										ReflectiveSerializer.Checks.class)
										.ignore();
					}
					result |= type.isAnnotationPresent(TypeSerialization.class)
							&& !type.getAnnotation(TypeSerialization.class)
									.reflectiveSerializable();
					if (!result) {
						addPropertyIssue(property);
					}
				});
	}

	boolean filter(Class clazz) {
		if (!clazz.isInterface()
				&& !Modifier.isAbstract(clazz.getModifiers())) {
			return false;
		}
		return true;
	}

	private void generateSignature(Class clazz) {
		CRC32 crc = new CRC32();
		crc.update(clazz.getName().getBytes());
		ClassReflector<?> reflector = Reflections.at(clazz);
		reflector.properties().stream()
				.filter(Property::provideReadWriteNonTransient)
				.map(Property::getName).sorted()
				.forEach(name -> crc.update(name.getBytes()));
		signatures.getClassNameDefaultSerializedForms().put(clazz.getName(),
				String.valueOf(crc.getValue()));
	}

	@AlcinaTransient
	public String getSignature() {
		return this.signature;
	}

	private void performAction1() throws Exception {
		try {
			Registry.optional(AppPersistenceBase.InitRegistrySupport.class)
					.ifPresent(r -> r.muteClassloaderLogging(true));
			ClassMetadataCache<ClassMetadata> classes = new ServletClasspathScanner(
					"*", true, true, null, Registry.MARKER_RESOURCE,
					Arrays.asList(new String[] {})).getClasses();
			Scanner scanner = new Scanner();
			File file = DataFolderProvider.get().getClassDataFile(this);
			file.getParentFile().mkdirs();
			scanner.scan(classes, file.getPath());
			serializables = scanner.getOutgoingCache().classData.values()
					.stream().filter(m -> m.isReflective)
					.map(m -> Reflections.forName(m.className))
					.collect(AlcinaCollectors.toLinkedHashSet());
		} finally {
			Registry.optional(AppPersistenceBase.InitRegistrySupport.class)
					.ifPresent(r -> r.muteClassloaderLogging(false));
		}
		serializables.forEach(this::checkSerializationIssues);
		Preconditions.checkState(serializationIssues.isEmpty());
		serializables.forEach(
				this::checkAllTransientFieldsWithPropertiesAreTransient);
		if (!incorrectProperty.isEmpty()) {
			incorrectProperty.stream()
					.collect(AlcinaCollectors
							.toKeyMultimap(Property::getDeclaringType))
					.entrySet()
					.forEach(e -> Ax.out("%s :: %s :: %s",
							e.getKey().getSimpleName(),
							e.getValue().stream().map(Property::getName)
									.collect(Collectors.toList()),
							e.getKey().getName()));
		}
		if (incorrectProperty.size() > 0) {
			Ax.sysLogHigh(
					"TaskGenerateReflectiveSerializerSignatures :: problematic properties\n\n\t%s\n\n ",
					CommonUtils.joinWithNewlineTab(incorrectProperty));
		}
		Preconditions.checkState(incorrectProperty.isEmpty(),
				"Incorrect property serialization");
		serializables.forEach(this::generateSignature);
		ReflectiveSerializableSignatures stableCheck = signatures;
		signatures = new ReflectiveSerializableSignatures();
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
				ReflectiveSerializableSignatures.class.getName(), sha1);
		if (DomainStore.stores().hasInitialisedDatabaseStore()) {
			MethodContext.instance().withRootPermissions(true).run(() -> {
				UserProperty<?> property = UserProperty.ensure(key);
				property.domain().ensurePopulated();
				property.setValue(signaturesBytes);
				Transaction.commit();
			});
		}
		logger.info(
				"ReflectiveSerializableSignatures serializedDefaults signature: ({}) : {} ",
				signatures.classNameDefaultSerializedForms.size(), sha1);
	}

	@Override
	public void run() throws Exception {
		MethodContext.instance()
				.withMetricKey("TaskGenerateReflectiveSerializerSignatures")
				.run(this::performAction1);
	}

	public static class ReflectiveSerializableSignatures {
		private Map<String, String> classNameDefaultSerializedForms = new TreeMap<>();

		public Map<String, String> getClassNameDefaultSerializedForms() {
			return this.classNameDefaultSerializedForms;
		}

		public void setClassNameDefaultSerializedForms(
				Map<String, String> classNameDefaultSerializedForms) {
			this.classNameDefaultSerializedForms = classNameDefaultSerializedForms;
		}
	}

	static class Scanner extends CachingScanner<ReflectiveScannerMetadata> {
		@Override
		protected ReflectiveScannerMetadata createMetadata(String className,
				ClassMetadata found) {
			return new ReflectiveScannerMetadata(className).fromUrl(found);
		}

		ClassMetadataCache<ReflectiveScannerMetadata> getOutgoingCache() {
			return outgoingCache;
		}

		@Override
		protected ReflectiveScannerMetadata process(Class clazz,
				String className, ClassMetadata found) {
			ReflectiveScannerMetadata out = createMetadata(className, found);
			if ((!Modifier.isPublic(clazz.getModifiers()))
					|| (Modifier.isAbstract(clazz.getModifiers())
							&& !clazz.isEnum())
					|| clazz.isInterface()) {
			} else {
				/*
				 */
				boolean bi = new AnnotationLocation(clazz, null)
						.hasAnnotation(Bean.class);
				boolean refl = new AnnotationLocation(clazz, null)
						.hasAnnotation(Reflected.class);
				if (bi
				// || refl - no, not reflected (don't check props of @reflected
				// types)
				) {
					try {
						clazz.getDeclaredFields();
						out.isReflective = true;
					} catch (Error e) {
						// ignore, close link to something Dommish
						// e.printStackTrace();
					}
				} else {
				}
			}
			return out;
		}

		public static class ReflectiveScannerMetadata
				extends ClassMetadata<ReflectiveScannerMetadata> {
			public boolean isReflective;

			public ReflectiveScannerMetadata() {
			}

			public ReflectiveScannerMetadata(String className) {
				super(className);
			}
		}
	}
}
