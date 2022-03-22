package cc.alcina.framework.entity.gwt.reflection;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.SyntheticArtifact;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JRawType;
import com.google.gwt.core.ext.typeinfo.JRealClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.JWildcardType;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.ReflectionModule;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;

class ReachabilityData {
	static String dataFolder;

	static Set<String> typeParametersLogged = new LinkedHashSet<>();

	static Class<? extends ClientReflectionFilterPeer> filterPeerClass;

	static Class<? extends ReachabilityLinkerPeer> linkerPeerClass;

	

	private static Set<JClassType> computeImplementations(
			JTypeParameter typeParameter,
			Multiset<JClassType, Set<JClassType>> subtypes) {
		Set<JClassType> result = null;
		JClassType firstBound = typeParameter.getFirstBound();
		if (typeParameter.getBounds().length > 1) {
			result = subtypes.get(firstBound).stream()
					.collect(AlcinaCollectors.toLinkedHashSet());
			for (int idx = 1; idx < typeParameter.getBounds().length; idx++) {
				JClassType bound = typeParameter.getBounds()[idx];
				result.retainAll(subtypes.get(bound));
			}
		} else {
			result = Stream.of(firstBound).collect(Collectors.toSet());
		}
		if (result.size() > 0
				&& typeParametersLogged.add(typeParameter.toString())) {
			Ax.out(" -- %s", typeParameter);
		}
		return result;
	}

	@SuppressWarnings("deprecation")
	private static byte[] toJsonBytes(Object object) {
		String json = new JacksonJsonObjectSerializer().withDefaults(false)
				.withPrettyPrint().withIdRefs().withAllowUnknownProperties()
				.serialize(object);
		return json.getBytes(StandardCharsets.UTF_8);
	}

	static <T> T deserialize(Class<T> clazz, File file) {
		String json = ResourceUtilities.read(file);
		return new JacksonJsonObjectSerializer().withIdRefs().deserialize(json,
				clazz);
	}

	// java.xx class reachability is configured in code
	static boolean excludeJavaType(JClassType type) {
		return !type.getPackage().getName().startsWith("java");
	}

	static File getReachabilityFile(String fileName) {
		new File(dataFolder).mkdirs();
		return new File(Ax.format("%s/%s", dataFolder, fileName));
	}

	static void initConfiguration(PropertyOracle propertyOracle) {
		try {
			dataFolder = Ax.first(propertyOracle.getConfigurationProperty(
					ClientReflectionGenerator.DATA_FOLDER_CONFIGURATION_KEY)
					.getValues());
			filterPeerClass = (Class<? extends ClientReflectionFilterPeer>) Class
					.forName(Ax.first(propertyOracle.getConfigurationProperty(
							ClientReflectionGenerator.FILTER_PEER_CONFIGURATION_KEY)
							.getValues()));
			linkerPeerClass = (Class<? extends ReachabilityLinkerPeer>) Class
					.forName(Ax.first(propertyOracle.getConfigurationProperty(
							ClientReflectionGenerator.LINKER_PEER_CONFIGURATION_KEY)
							.getValues()));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	static boolean isObjectType(JClassType type) {
		return type.getQualifiedSourceName()
				.equals(Object.class.getCanonicalName());
	}

	static <T> void serializeReachabilityFile(TreeLogger logger,
			Object contents, File file) {
		String existing = file.exists() ? ResourceUtilities.read(file) : null;
		String json = new String(toJsonBytes(contents));
		if (!Objects.equals(existing, json)) {
			if (Boolean.getBoolean("reachability.production")) {
				logger.log(TreeLogger.Type.WARN,
						"Not committing reachability changes (production build system)");
			} else {
				ResourceUtilities.write(json, file);
			}
		}
	}

	static Stream<JClassType> toReachableConcreteTypes(JType type,
			Multiset<JClassType, Set<JClassType>> subtypes) {
		Set<JClassType> resolved = new LinkedHashSet<>();
		Set<JType> visited = new LinkedHashSet<>();
		Stack<JType> unresolved = new Stack<>();
		unresolved.add(type);
		while (unresolved.size() > 0) {
			JType pop = unresolved.pop();
			if (!visited.add(pop)) {
				continue;
			}
			if (pop instanceof JPrimitiveType) {
				// ignore, only interested in class types
			} else {
				JClassType cursor = (JClassType) pop;
				if (cursor instanceof JParameterizedType) {
					JParameterizedType parameterizedType = (JParameterizedType) cursor;
					unresolved.add(parameterizedType.getBaseType());
					Arrays.stream(parameterizedType.getTypeArgs())
							.forEach(unresolved::add);
				} else if (cursor instanceof JGenericType) {
					JGenericType genericType = (JGenericType) cursor;
					resolved.add(cursor.getErasedType());
					Arrays.stream(genericType.getTypeParameters())
							.forEach(unresolved::add);
					// nope, must be abstract
				} else if (cursor instanceof JRealClassType) {
					resolved.add(cursor);
				} else if (cursor instanceof JRawType) {
					JRawType rawType = (JRawType) cursor;
					resolved.add(cursor.getErasedType());
					unresolved.add(rawType.getBaseType());
					// ignore
				} else if (cursor instanceof JWildcardType) {
					JWildcardType wildcardType = (JWildcardType) cursor;
					switch (wildcardType.getBoundType()) {
					case EXTENDS:
						unresolved.add(wildcardType.getBaseType());
						break;
					case UNBOUND:
					case SUPER:
						// throw new UnsupportedOperationException(
						// "Illegal bound type");
						// ignore
						break;
					}
					// ignore
				} else if (cursor instanceof JTypeParameter) {
					JTypeParameter jTypeParameter = (JTypeParameter) cursor;
					Set<JClassType> implementations = computeImplementations(
							jTypeParameter, subtypes);
					unresolved.addAll(implementations);
					// ignore
				} else if (cursor instanceof JArrayType) {
					// ignore - arrays are illegal for serialization
				} else {
					throw new UnsupportedOperationException();
				}
			}
		}
		// hard-coded - TODO - revisit reachability
		resolved.removeIf(ReachabilityData::isObjectType);
		resolved.removeIf(
				t -> t.getQualifiedSourceName().equals(Entity.class.getName()));
		resolved.removeIf(
				t -> t.getQualifiedSourceName().equals(Enum.class.getName()));
		return resolved.stream();
	}

	static <T> T typedArtifact(Class<T> clazz,
			Class<? extends SyntheticArtifact> artifactClass,
			ArtifactSet artifacts) {
		SyntheticArtifact artifact = artifacts.find(artifactClass).iterator()
				.next();
		try {
			InputStream contents = artifact.getContents(null);
			return new JacksonJsonObjectSerializer().withIdRefs()
					.deserialize(new InputStreamReader(contents), clazz);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	static class AppImplRegistrations {
		static AppImplRegistrations fromArtifact(ArtifactSet artifacts) {
			return typedArtifact(AppImplRegistrations.class, Artifact.class,
					artifacts);
		}

		List<Entry> entries = new ArrayList<>();

		void add(JClassType t, List<Registration> registrations) {
			registrations.stream()
					.map(registration -> new Entry(t, registration))
					.forEach(entries::add);
		}

		Artifact serialize() {
			return new Artifact("registrations", this);
		}

		static class Artifact extends BaseArtifact {
			public Artifact(String partialName, Object object) {
				super(partialName, object);
			}
		}

		static class Entry {
			Type registered;

			List<Type> keys;

			private transient Registration registration;

			Entry() {
			}

			Entry(JClassType t, Registration registration) {
				this.registration = registration;
				registered = Type.get(t);
				keys = Arrays.stream(registration.value()).map(Type::get)
						.collect(Collectors.toList());
			}

			public boolean retainRegistrations(Set<Registration> retain) {
				return retain.contains(registration);
			}

			@Override
			public String toString() {
				return String.format("%-50s <-- %s", registered, keys);
			}

			boolean isVisible(Set<Type> types) {
				return types.containsAll(keys);
			}
		}
	}

	static class AppReflectableTypes {
		static AppReflectableTypes fromArtifact(ArtifactSet artifacts) {
			return typedArtifact(AppReflectableTypes.class, Artifact.class,
					artifacts);
		}

		List<TypeHierarchy> typeHierarchies = new ArrayList<>();

		transient Map<Type, TypeHierarchy> byType;

		void addType(TypeHierarchy t) {
			typeHierarchies.add(t);
		}

		void buildLookup() {
			byType = typeHierarchies.stream()
					.collect(AlcinaCollectors.toKeyMap(th -> th.type));
		}

		boolean contains(Type t) {
			return byType.containsKey(t);
		}

		Artifact serialize() {
			return new Artifact("reflectable-types", this);
		}

		TypeHierarchy typeHierarchy(Type t) {
			return byType.get(t);
		}

		static class Artifact extends BaseArtifact {
			public Artifact(String partialName, Object object) {
				super(partialName, object);
			}
		}
	}

	abstract static class BaseArtifact extends SyntheticArtifact {
		BaseArtifact(String partialName, Object object) {
			super(ReflectionReachabilityLinker.class,
					"reflection/" + partialName + ".json", toJsonBytes(object));
			setVisibility(Visibility.Private);
		}
	}

	static class LegacyModuleAssignments {
		static LegacyModuleAssignments fromArtifact(ArtifactSet artifacts) {
			return typedArtifact(LegacyModuleAssignments.class, Artifact.class,
					artifacts);
		}

		Map<String, List<Type>> byModule = new LinkedHashMap<>();

		public boolean hasAssignments() {
			return byModule.size() > 0;
		}

		void addType(JClassType t, String moduleName) {
			byModule.computeIfAbsent(moduleName, name -> new ArrayList<>())
					.add(Type.get(t));
		}

		boolean isAssignedToModule(Type t, String moduleName) {
			if (byModule.size() > 0) {
				switch (moduleName) {
				case ReflectionModule.INITIAL:
				case ReflectionModule.LEFTOVER:
					return byModule.containsKey(moduleName)
							? byModule.get(moduleName).contains(t)
							: true;
				default:
					return false;
				}
			} else {
				return false;
			}
		}

		Artifact serialize() {
			return new Artifact("legacy-assignments", this);
		}

		static class Artifact extends BaseArtifact {
			public Artifact(String partialName, Object object) {
				super(partialName, object);
			}
		}
	}

	static class ModuleTypes {
		List<TypeList> moduleLists = new ArrayList<>();

		transient Map<String, Type> sourceNameType = new LinkedHashMap<>();

		transient Map<Type, String> typeModule = new LinkedHashMap<>();

		public boolean permit(JClassType type, String moduleName) {
			Type t = typeFor(type.getQualifiedSourceName());
			return t != null && typeModule.get(t).equals(moduleName);
		}

		public Set<Type> typesFor(List<String> moduleNames) {
			return moduleLists.stream()
					.filter(ml -> moduleNames.contains(ml.moduleName))
					.flatMap(tl -> tl.types.stream()).sorted()
					.collect(AlcinaCollectors.toLinkedHashSet());
		}

		public boolean unknownToNotReached() {
			TypeList unknown = ensureTypeList(ReflectionModule.UNKNOWN);
			TypeList notReached = ensureTypeList(ReflectionModule.NOT_REACHED);
			List<Type> preAssignTypes = notReached.types;
			Set<Type> notReachedComputed = Stream
					.concat(preAssignTypes.stream(), unknown.types.stream())
					.collect(AlcinaCollectors.toLinkedHashSet());
			unknown.types.clear();
			moduleLists.stream()
					.filter(tl -> ReflectionModule.Modules
							.provideIsFragment(tl.moduleName))
					.forEach(
							tl -> tl.types.forEach(notReachedComputed::remove));
			notReached.types = notReachedComputed.stream()
					.collect(Collectors.toList());
			return !(notReached.types.equals(preAssignTypes));
		}

		boolean doesNotContain(JClassType type) {
			return typeFor(type.getQualifiedSourceName()) == null;
		}

		TypeList ensureTypeList(String moduleName) {
			Optional<TypeList> optional = moduleLists.stream()
					.filter(tl -> Objects.equals(tl.moduleName, moduleName))
					.findFirst();
			if (optional.isEmpty()) {
				TypeList typeList = new TypeList();
				typeList.moduleName = moduleName;
				moduleLists.add(typeList);
				return typeList;
			} else {
				return optional.get();
			}
		}

		void generateLookup() {
			moduleLists.forEach(ml -> {
				ml.types.forEach(t -> {
					sourceNameType.put(t.qualifiedSourceName, t);
					typeModule.put(t, ml.moduleName);
				});
			});
		}

		String moduleFor(String qualifiedSourceName) {
			Type type = typeFor(qualifiedSourceName);
			return typeModule.get(type);
		}

		Type typeFor(String qualifiedSourceName) {
			return sourceNameType.get(qualifiedSourceName);
		}

		static class TypeList {
			String moduleName;

			List<Type> types = new ArrayList<>();

			void add(JClassType jClassType) {
				types.add(Type.get(jClassType));
			}
		}
	}

	static class ProcessHistory {
		List<Entry> entries = new ArrayList<>();

		static class Entry {
			Type type;

			String moduleName;

			int pass;

			String log;
		}
	}

	@JsonSerialize(using = ReasonSerializer.class)
	@JsonDeserialize(using = ReasonDeserializer.class)
	static class Reason implements Comparable<Reason> {
		String reason;

		Category category;

		enum Category {
			CODE, REGISTRY, RPC, HIERARCHY
		}

		@Override
		public String toString() {
			return reason;
		}

		Reason() {
		}

		Reason(int fragmentNumber, String fragmentName, Category category) {
			this(fragmentNumber, fragmentName, category, null);
		}

		Reason(int fragmentNumber, String fragmentName, Category category,
				String reason) {
			this.category = category;
			FormatBuilder fb = new FormatBuilder().separator("-");
			fb.appendIfNotBlank(fragmentNumber, fragmentName,
					Ax.friendly(category), reason);
			this.reason = fb.toString();
		}

		Reason(String reason) {
			this.reason = reason;
			String[] splits = reason.split(" - ");
			if (splits.length > 2) {
				category = CommonUtils.getEnumValueOrNull(Category.class,
						splits[2], true, null);
			}
		}

		@Override
		public boolean equals(Object obj) {
			return Objects.equals(reason, ((Reason) obj).reason);
		}

		@Override
		public int hashCode() {
			return reason.hashCode();
		}

		@Override
		public int compareTo(Reason o) {
			return reason.compareTo(o.reason);
		}
	}

	static class ReasonDeserializer extends StdDeserializer<Reason> {
		ReasonDeserializer() {
			super(Reason.class);
		}

		@Override
		public Reason deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			return new Reason(p.getText());
		}
	}

	static class ReasonSerializer extends StdSerializer<Reason> {
		ReasonSerializer() {
			super(Reason.class);
		}

		@Override
		public void serialize(Reason value, JsonGenerator gen,
				SerializerProvider provider) throws IOException {
			gen.writeString(value.reason);
		}
	}

	@JsonSerialize(using = TypeQnameSerializer.class)
	@JsonDeserialize(using = TypeQnameDeserializer.class)
	static class Type implements Comparable<Type> {
		private transient static Map<String, Type> forName = new LinkedHashMap<>();

		static Type get(Class clazz) {
			return get(clazz.getCanonicalName());
		}

		static Type get(JClassType t) {
			return get(t.getQualifiedSourceName());
		}

		static Type get(String qualifiedSourceName) {
			return forName.computeIfAbsent(qualifiedSourceName, n -> {
				Type type = new Type();
				type.qualifiedSourceName = n;
				return type;
			});
		}

		String qualifiedSourceName;

		transient Class clazz;

		Type() {
		}

		@Override
		public int compareTo(Type o) {
			return qualifiedSourceName.compareTo(o.qualifiedSourceName);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Type && ((Type) obj).qualifiedSourceName
					.equals(qualifiedSourceName);
		}

		@Override
		public int hashCode() {
			return qualifiedSourceName.hashCode();
		}

		@Override
		public String toString() {
			return qualifiedSourceName;
		}

		public Class<?> getType() {
			if (clazz == null) {
				try {
					clazz = Class.forName(qualifiedSourceName);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
			return clazz;
		}

		public boolean matchesClass(Class clazz) {
			return qualifiedSourceName.equals(clazz.getCanonicalName());
		}
	}

	static class TypeHierarchy {
		Type type;

		List<Type> typeAndSuperTypes;

		Stream<Type> typeAndSuperTypes() {
			return typeAndSuperTypes.stream();
		}

		List<Type> subtypes;

		public Stream<Type> subtypes() {
			return this.subtypes.stream();
		}

		/*
		 * Types which are either arguments or parameterized type arguments of
		 * property set methods - e.g. setRecords(List<Record>) would have
		 * settableTypes List and Record
		 */
		List<Type> settableTypes;

		/*
		 * Types which are parameterized type arguments of ReflectiveRpc method
		 * arguments - so login(LoginRequest request,
		 * AsyncCallback<LoginResponse> callback) has rpcSerializableTypes and
		 * LoginResponse LoginResponse
		 */
		List<Type> rpcSerializableTypes;

		String packageName;

		TypeHierarchy() {
		}

		TypeHierarchy(JClassType classType,
				Multiset<JClassType, Set<JClassType>> subtypes,
				Multiset<JClassType, Set<JClassType>> rpcSerializableTypes,
				Multiset<JClassType, Set<JClassType>> settableTypes) {
			type = Type.get(classType);
			this.packageName = classType.getPackage().getName();
			this.typeAndSuperTypes = classType.getFlattenedSupertypeHierarchy()
					.stream().map(Type::get).collect(Collectors.toList());
			this.subtypes = asList(classType, subtypes);
			this.settableTypes = asList(classType, settableTypes);
			this.rpcSerializableTypes = asList(classType, rpcSerializableTypes);
		}

		private List<Type> asList(JClassType classType,
				Multiset<JClassType, Set<JClassType>> associated) {
			return associated.containsKey(classType) ? associated.get(classType)
					.stream().map(Type::get).collect(Collectors.toList())
					: new ArrayList<>();
		}
	}

	static class TypeQnameDeserializer extends StdDeserializer<Type> {
		TypeQnameDeserializer() {
			super(Type.class);
		}

		@Override
		public Type deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			return Type.get(p.getText());
		}
	}

	static class TypeQnameSerializer extends StdSerializer<Type> {
		TypeQnameSerializer() {
			super(Type.class);
		}

		@Override
		public void serialize(Type value, JsonGenerator gen,
				SerializerProvider provider) throws IOException {
			gen.writeString(value.qualifiedSourceName);
		}
	}

	static class TypesReason implements Comparable<TypesReason> {
		Reason reason;

		List<Type> types = new ArrayList<>();

		@Override
		public int compareTo(TypesReason o) {
			return reason.compareTo(o.reason);
		}

		@Override
		public String toString() {
			return Ax.format("%s - %s types", reason, types.size());
		}
	}

	static class TypesReasons {
		List<TypesReason> typesReasons = new ArrayList<>();;

		transient Map<Reason, TypesReason> byReason = new LinkedHashMap<>();

		void add(Reason reason, Type type) {
			TypesReason typesReason = byReason.get(reason);
			if (typesReason == null) {
				typesReason = new TypesReason();
				typesReason.reason = reason;
				typesReasons.add(typesReason);
				byReason.put(reason, typesReason);
			}
			typesReason.types.add(type);
		}

		void generateLookup() {
			typesReasons.forEach(tr -> byReason.put(tr.reason, tr));
		}

		public void sort() {
			Collections.sort(typesReasons);
			typesReasons.forEach(tr->Collections.sort(tr.types));
		}
	}

	public static <T> T newInstance(Class<? extends T> clazz) {
		try {
			return clazz.getConstructor().newInstance();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
