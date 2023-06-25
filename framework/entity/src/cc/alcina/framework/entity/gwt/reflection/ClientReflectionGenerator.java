package cc.alcina.framework.entity.gwt.reflection;

import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.CachedGeneratorResult;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JRawType;
import com.google.gwt.core.ext.typeinfo.JRealClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.typemodel.JavacTypeBounds;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.DomainCollections;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.reachability.ReflectionModule;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.AnnotationProvider;
import cc.alcina.framework.common.client.reflection.AsyncSerializableTypes;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.ClientReflections;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypeBounds;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.common.client.util.ToStringComparator;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppImplRegistrations;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppReflectableTypes;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.TypeHierarchy;
import cc.alcina.framework.entity.gwt.reflection.reflector.AnnotationReflection;
import cc.alcina.framework.entity.gwt.reflection.reflector.ClassReflection;
import cc.alcina.framework.entity.gwt.reflection.reflector.ClassReflection.ProvidesTypeBounds;
import cc.alcina.framework.entity.gwt.reflection.reflector.PropertyReflection;
import cc.alcina.framework.entity.gwt.reflection.reflector.PropertyReflection.PropertyAccessor;
import cc.alcina.framework.entity.gwt.reflection.reflector.ReflectionVisibility;

/*
 * Documentation notes - note module assignment (unknown, not_reached, excluded)
 * and how that relates to production compilation - explain caching (and
 * interaction with gwt watch service) -- can probably do caching *better* - at
 * the moment any change to files should cause initial full recalc - document
 * the evils of generics (in serializable types) when pruning reachability -
 * document why this (alignment of reflection with async modules) is needed -
 * note reachability wrinkles caused by exclusion of equal priority classes from
 * registration (see listImplementationRegistrations)
 *
 *
 */
public class ClientReflectionGenerator extends IncrementalGenerator {
	/*
	 * Force consistent ordering across generators
	 */
	static Comparator<Registration> REGISTRY_LOCATION_COMPARATOR = new Comparator<Registration>() {
		Map<Registration, String> comparables = new LinkedHashMap<>();

		@Override
		public int compare(Registration o1, Registration o2) {
			String c1 = comparables.computeIfAbsent(o1, this::toComparable);
			String c2 = comparables.computeIfAbsent(o2, this::toComparable);
			return c1.compareTo(c2);
		}

		String toComparable(Registration r) {
			return Arrays.toString(r.value());
		}
	};

	private static final long GENERATOR_VERSION_ID = 1L;

	private static final String CACHED_TYPE_INFORMATION = "cached-type-info";

	static final String REF_IMPL = "__refImpl";

	static final String ANN_IMPL = "__annImpl";

	static final String DATA_FOLDER_CONFIGURATION_KEY = "ClientReflectionGenerator.ReachabilityData.folder";

	static final String FILTER_PEER_CONFIGURATION_KEY = "ClientReflectionGenerator.FilterPeer.className";

	static final String LINKER_PEER_CONFIGURATION_KEY = "ClientReflectionGenerator.LinkerPeer.className";

	static void configureRegistry() {
		if (Registry.optional(DomainCollections.class).isEmpty()) {
			Registry.register().singleton(DomainCollections.class,
					new DomainCollections());
		}
	}

	static String implementationName(JClassType type,
			boolean annotationImplementation) {
		List<JClassType> enclosedTypes = new ArrayList<>();
		while (type != null) {
			enclosedTypes.add(type);
			type = type.getEnclosingType();
		}
		FormatBuilder builder = new FormatBuilder().separator("_");
		for (int idx = enclosedTypes.size() - 1; idx >= 0; idx--) {
			builder.append(enclosedTypes.get(idx).getSimpleSourceName());
		}
		builder.append(annotationImplementation ? ANN_IMPL : REF_IMPL);
		return builder.toString();
	}

	static boolean isReflectableJavaCollectionClass(JClassType jClassType) {
		return CommonUtils.COLLECTION_CLASS_NAMES
				.contains(jClassType.getQualifiedSourceName());
	}

	static boolean isReflectableJavaCoreClass(JClassType jClassType) {
		return CommonUtils.CORE_CLASS_NAMES
				.contains(jClassType.getQualifiedSourceName())
				|| CommonUtils.PRIMITIVE_CLASS_NAMES
						.contains(jClassType.getQualifiedSourceName())
				|| CommonUtils.PRIMITIVE_WRAPPER_CLASS_NAMES
						.contains(jClassType.getQualifiedSourceName());
	}

	long start;

	TreeLogger logger;

	GeneratorContext context;

	ModuleReflectionGenerator moduleGenerator;

	JClassType generatingType;

	ReflectionModule module;

	String moduleName;

	String typeName;

	String implementationName;

	String packageName;

	Map<Class, String> annotationImplFqn = new LinkedHashMap<>();

	Set<Class<? extends Annotation>> visibleAnnotationTypes = new LinkedHashSet<>();

	JClassType classReflectorType;

	ClientReflectionFilter filter;

	AnnotationLocationTypeInfo.Resolver annotationResolver = new AnnotationLocationTypeInfo.Resolver();

	boolean reflectUnknownInInitialModule;

	VisibleAnnotationFilterClient visibleAnnotationFilter;

	ClassReflection.ProvidesTypeBounds providesJavacTypeBounds = new ProvidesJavacTypeBoundsImplementation();

	public JType voidJType;

	public Object[] firePropertyChangeMethodJTypes;

	@Override
	public RebindResult generateIncrementally(TreeLogger logger,
			GeneratorContext context, String typeName)
			throws UnableToCompleteException {
		try {
			checkSinglePermutationBuild(logger, context);
			this.logger = logger;
			this.context = context;
			this.typeName = typeName;
			setupEnvironment();
			setupFilter();
			moduleGenerator = new ModuleReflectionGenerator(implementationName,
					generatingType);
			moduleGenerator.prepare();
			if (useCachedResult(logger, context)) {
				logger.log(Type.INFO, String.format(
						"Reflection [%s] - using cached reflection metadata",
						moduleName));
				return new RebindResult(RebindMode.USE_ALL_CACHED, typeName);
			}
			boolean updated = moduleGenerator.write();
			// should only be called once from code
			if (!updated) {
				RebindResult result = new RebindResult(RebindMode.USE_EXISTING,
						moduleGenerator.implementationFqn());
				return result;
			}
			ReachabilityData.AppImplRegistrations registrations = moduleGenerator
					.listImplementationRegistrations();
			ReachabilityData.AppReflectableTypes reflectableTypes = moduleGenerator
					.listReflectableTypes();
			String emitMessage = String.format(
					"Reflection [%s] -  %s/%s/%s reflected types - %s ms\n",
					moduleName, moduleGenerator.writeReflectors.size(),
					moduleGenerator.classReflectors.size(),
					context.getTypeOracle().getTypes().length,
					System.currentTimeMillis() - start);
			filter.onGenerationComplete(registrations, reflectableTypes,
					Arrays.stream(context.getTypeOracle().getTypes()),
					emitMessage);
			RebindResult result = new RebindResult(RebindMode.USE_ALL_NEW,
					moduleGenerator.implementationFqn());
			result.putClientData(CACHED_TYPE_INFORMATION,
					new IncrementalSupport().prepareCacheInfo(this));
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public long getVersionId() {
		return GENERATOR_VERSION_ID;
	}

	private void checkSinglePermutationBuild(TreeLogger logger,
			GeneratorContext context) throws BadPropertyValueException {
		// TODO - jjs can access permutationlist (in precompile phase)
		Preconditions.checkArgument(
				context.getPropertyOracle()
						.getSelectionProperty(logger, "user.agent")
						.getCurrentValue().equals("safari"),
				"Only configured for single-permutation (safari) builds");
	}

	private void setupFilter() throws Exception {
		ModuleReflectionFilter modulefilter = new ModuleReflectionFilter();
		modulefilter.init(logger, context, module.value(),
				reflectUnknownInInitialModule);
		filter = modulefilter;
	}

	protected boolean useCachedResult(TreeLogger logger,
			GeneratorContext generatorContext) {
		/*
		 * Do a series of checks to see if we can use a previously cached
		 * result, and if so, we can skip further execution and return
		 * immediately.
		 */
		boolean useCache = false;
		CachedGeneratorResult lastRebindResult = generatorContext
				.getCachedGeneratorResult();
		if (lastRebindResult != null
				&& generatorContext.isGeneratorResultCachingEnabled()) {
			IncrementalSupport incrementalSupport = (IncrementalSupport) lastRebindResult
					.getClientData(CACHED_TYPE_INFORMATION);
			if (incrementalSupport != null && incrementalSupport
					.checkSourcesUnmodified(logger, this)) {
				useCache = true;
			}
		}
		return useCache;
	}

	void addImport(ClassSourceFileComposerFactory factory, Class<?> type) {
		if (!type.isPrimitive()) {
			factory.addImport(type.getCanonicalName().replace("[]", ""));
		}
	}

	SourceWriter createWriter(ClassSourceFileComposerFactory factory,
			PrintWriter contextWriter) {
		PrintWriter writer = contextWriter;
		return factory.createSourceWriter(writer);
	}

	String escapeClassName(JClassType type) {
		return type.getQualifiedSourceName().replace(".", "_");
	}

	String getQualifiedSourceName(JType jType) {
		if (jType.isTypeParameter() != null) {
			return jType.isTypeParameter().getBaseType()
					.getQualifiedSourceName();
		} else {
			return jType.getQualifiedSourceName();
		}
	}

	JClassType getType(Class clazz) {
		return getType(clazz.getCanonicalName());
	}

	JClassType getType(String typeName) {
		try {
			return context.getTypeOracle().getType(typeName);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	<A extends Annotation> boolean has(JClassType t, Class<A> annotationClass) {
		if (annotationClass == Reflected.class
				|| annotationClass == Registration.class) {
			return new AnnotationLocationTypeInfo(t, annotationResolver)
					.hasAnnotation(annotationClass);
		}
		return t.getAnnotation(annotationClass) != null;
	}

	void setupEnvironment() {
		start = System.currentTimeMillis();
		configureRegistry();
		String superClassName = null;
		generatingType = getType(typeName);
		classReflectorType = getType(ClassReflector.class);
		module = generatingType.getAnnotation(ReflectionModule.class);
		implementationName = String.format("ModuleReflector_%s_Impl",
				module.value());
		moduleName = module.value();
		/*
		 * In dev mode -or- production compilation mode, add any unknown
		 * reflectable typeinfo to the initial module. That ensures the program
		 * is correct, but non-optimal -- optimisation occurs during
		 * reachability linking
		 */
		reflectUnknownInInitialModule = !context.isProdMode()
				|| Boolean.getBoolean("reachability.production");
		voidJType = JPrimitiveType.VOID;
		firePropertyChangeMethodJTypes = new JType[] { getType(String.class),
				getType(Object.class), getType(Object.class) };
	}

	String stringLiteral(String value, boolean quoted) {
		String escaped = value.replace("\\", "\\\\").replace("\n", "\\n");
		return quoted ? escaped : String.format("\"%s\"", escaped);
	}

	void writeAnnotationValue(SourceWriter sourceWriter, Object value,
			Class declaredType) {
		writeAnnotationValue(sourceWriter, value, declaredType, false);
	}

	void writeAnnotationValue(SourceWriter sourceWriter, Object value,
			Class declaredType, boolean quoted) {
		if (value == null) {
			// annotation has no default value
			sourceWriter.print("null");
			return;
		}
		Class<? extends Object> clazz = value.getClass();
		if (clazz.isArray()) {
			Class componentType = declaredType.getComponentType();
			String componentImplementationName = annotationImplFqn.containsKey(
					componentType) ? annotationImplFqn.get(componentType)
							: componentType.getCanonicalName();
			if (quoted) {
				sourceWriter.print("{");
			} else {
				sourceWriter.print("new %s[]{", componentImplementationName);
			}
			int length = Array.getLength(value);
			for (int i = 0; i < length; i++) {
				if (i != 0) {
					sourceWriter.print(", ");
				}
				Object element = Array.get(value, i);
				writeAnnotationValue(sourceWriter, element, componentType,
						quoted);
			}
			sourceWriter.print("}");
		} else if (declaredType.isAnnotation()) {
			new AnnotationExpressionWriter(
					new AnnotationReflection((Annotation) value))
							.writeExpression(sourceWriter);
		} else if (clazz.equals(Class.class)) {
			if (quoted) {
				sourceWriter.print(((Class) value).getSimpleName() + ".class");
			} else {
				sourceWriter
						.print(((Class) value).getCanonicalName() + ".class");
			}
		} else if (clazz.equals(String.class)) {
			sourceWriter.print(stringLiteral(value.toString(), quoted));
		} else if (Enum.class.isAssignableFrom(clazz)) {
			sourceWriter.print("%s.%s", clazz.getCanonicalName(),
					value.toString());
		} else {
			sourceWriter.print(value.toString());
		}
	}

	private final class ProvidesJavacTypeBoundsImplementation
			implements ProvidesTypeBounds {
		JavacTypeBounds.Computed computedTypeBounds = new JavacTypeBounds.Computed();

		@Override
		public List<? extends JClassType> provideTypeBounds(JClassType type) {
			return computedTypeBounds.get(
					(com.google.gwt.dev.javac.typemodel.JClassType) type,
					context.getTypeOracle().getJavaLangObject()).bounds;
		}
	}

	class AnnotationExpressionWriter {
		AnnotationReflection reflection;

		public AnnotationExpressionWriter(
				AnnotationReflection annotationReflection) {
			this.reflection = annotationReflection;
		}

		protected void write(SourceWriter sourceWriter) {
			sourceWriter.println("{");
			sourceWriter.indent();
			sourceWriter.print("Annotation annotation = ");
			writeExpression(sourceWriter);
			sourceWriter.println(";");
			sourceWriter.println(
					"provider.annotations.put(%s.class,annotation);",
					reflection.getAnnotation().annotationType()
							.getCanonicalName());
			sourceWriter.outdent();
			sourceWriter.println("}");
		}

		void writeExpression(SourceWriter sourceWriter) {
			Annotation annotation = reflection.getAnnotation();
			Class<? extends Annotation> annotationType = annotation
					.annotationType();
			List<Method> declaredMethods = new ArrayList<Method>(
					Arrays.asList(annotationType.getDeclaredMethods()));
			Collections.sort(declaredMethods, ToStringComparator.INSTANCE);
			String implementationName = annotationImplFqn.get(annotationType);
			sourceWriter.print("new %s()", implementationName);
			try {
				for (Method method : declaredMethods) {
					if (method.getName().matches(
							"hashCode|toString|equals|annotationType")) {
						continue;
					}
					Object annotationValue = method.invoke(annotation,
							CommonUtils.EMPTY_OBJECT_ARRAY);
					Object defaultValue = annotation.annotationType()
							.getDeclaredMethod(method.getName(), new Class[0])
							.getDefaultValue();
					if (!Objects.equals(annotationValue, defaultValue)) {
						sourceWriter.print("._set%s(", method.getName());
						writeAnnotationValue(sourceWriter, annotationValue,
								method.getReturnType());
						sourceWriter.print(")");
					}
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	class AnnotationImplementationGenerator extends UnitGenerator
			implements Comparable<AnnotationImplementationGenerator> {
		Class<? extends Annotation> annotationClass;

		public AnnotationImplementationGenerator(JClassType annotationType) {
			super(annotationType, annotationType, true);
			try {
				annotationClass = (Class<? extends Annotation>) Class
						.forName(annotationType.getQualifiedBinaryName());
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		@Override
		public int compareTo(AnnotationImplementationGenerator o) {
			return annotationClass.getCanonicalName()
					.compareTo(o.annotationClass.getCanonicalName());
		}

		@Override
		protected void prepare() {
			annotationImplFqn.put(annotationClass, implementationFqn());
			visibleAnnotationTypes.add(annotationClass);
			Arrays.stream(annotationClass.getDeclaredMethods())
					.map(Method::getReturnType)
					.forEach(t -> addImport(composerFactory, t));
		}

		@Override
		protected boolean write() {
			if (!createPrintWriter()) {
				return false;
			}
			composerFactory.addImport(Annotation.class.getCanonicalName());
			composerFactory.addImplementedInterface(
					annotationClass.getCanonicalName());
			sourceWriter = createWriter(composerFactory, printWriter);
			List<Method> declaredMethods = Arrays
					.stream(annotationClass.getDeclaredMethods())
					.sorted(Comparator.comparing(Method::getName))
					.collect(Collectors.toList());
			declaredMethods.stream().map(Method::getReturnType)
					.forEach(t -> addImport(composerFactory, t));
			for (Method method : declaredMethods) {
				Class returnType = method.getReturnType();
				String returnTypeName = returnType.getCanonicalName();
				String methodName = method.getName();
				sourceWriter.print("%s %s = ", returnTypeName, methodName);
				writeAnnotationValue(sourceWriter, method.getDefaultValue(),
						returnType);
				sourceWriter.println(";");
				sourceWriter.println("public %s %s(){return %s;}",
						returnTypeName, methodName, methodName);
				sourceWriter.println(
						"public %s _set%s(%s %s){this.%s = %s;return this;}",
						implementationName, methodName, returnTypeName,
						methodName, methodName, methodName);
				sourceWriter.println();
			}
			sourceWriter.println();
			sourceWriter.println(
					"public Class<? extends Annotation> annotationType() {");
			sourceWriter.indentln("return %s.class;",
					annotationClass.getCanonicalName());
			sourceWriter.println("}");
			sourceWriter.println();
			// o will be non-null
			sourceWriter.println("private String __stringValue(Object o) {");
			sourceWriter.indent();
			sourceWriter.println(
					"if(o instanceof Class){return ((Class)o).getSimpleName()+\".class\";}");
			sourceWriter.println(
					"if(o.getClass().isArray()){return \"[\"+java.util.Arrays.stream"
							+ "((Object[])o).map(this::__stringValue).collect(java.util.stream.Collectors.joining(\",\"))+\"]\";}");
			sourceWriter.println("return o.toString();");
			sourceWriter.outdent();
			sourceWriter.println("}");
			sourceWriter.println("public String toString() {");
			sourceWriter.indent();
			sourceWriter.println(
					"StringBuilder stringBuilder = new StringBuilder();");
			int methodIndex = 0;
			for (Method method : declaredMethods) {
				if (methodIndex++ > 0) {
					sourceWriter.print("stringBuilder.append(\", \");");
				}
				String methodName = method.getName();
				Class returnType = method.getReturnType();
				sourceWriter.println("stringBuilder.append(\"%s\"); ",
						methodName);
				sourceWriter.println("stringBuilder.append(\"=\"); ");
				sourceWriter.print("stringBuilder.append(__stringValue(%s));",
						method.getName());
				sourceWriter.println();
			}
			sourceWriter.println("return stringBuilder.toString();");
			sourceWriter.outdent();
			sourceWriter.println("}");
			closeClassBody();
			return true;
		}
	}

	class ClassReflectorGenerator extends UnitGenerator
			implements Comparable<ClassReflectorGenerator> {
		private ClassReflection reflection;

		public ClassReflectorGenerator(JClassType type) {
			super(type, classReflectorType, false);
			this.reflection = new ClassReflection(type,
					sourcesPropertyChangeEvents(), visibleAnnotationFilter,
					providesJavacTypeBounds);
		}

		@Override
		public int compareTo(ClassReflectorGenerator o) {
			return reflection.type.getQualifiedSourceName()
					.compareTo(o.reflection.type.getQualifiedSourceName());
		}

		@Override
		public String toString() {
			return "reflector-generator: " + reflectedTypeFqBinaryName();
		}

		@Override
		protected void prepare() {
			reflection.prepare();
		}

		@Override
		protected boolean write() {
			if (!createPrintWriter()) {
				// FIXME - reflection - not correctly segregated modules
				return false;
			}
			composerFactory.setSuperclass(
					superClassOrInterfaceType.getQualifiedSourceName());
			composerFactory.addImport(LinkedHashMap.class.getName());
			composerFactory.addImport(Map.class.getName());
			composerFactory.addImport(Supplier.class.getName());
			composerFactory.addImport(Registry.class.getName());
			composerFactory.addImport(Predicate.class.getName());
			composerFactory.addImport(List.class.getName());
			composerFactory.addImport(ArrayList.class.getName());
			composerFactory.addImport(AnnotationProvider.class.getName());
			composerFactory.addImport(Annotation.class.getCanonicalName());
			composerFactory.addImport(
					AnnotationProvider.LookupProvider.class.getCanonicalName());
			composerFactory.addImport(ClientReflections.class.getName());
			composerFactory.addImport(Property.class.getName());
			composerFactory.addImport(TypeBounds.class.getName());
			composerFactory.addImport(
					cc.alcina.framework.common.client.reflection.Method.class
							.getCanonicalName());
			composerFactory.addImport(Registration.class.getName());
			composerFactory.addImport(Override.class.getName());
			boolean hasCallableNoArgsConstructor = reflection
					.isHasCallableNoArgsConstructor();
			if (hasCallableNoArgsConstructor) {
				composerFactory
						.addImplementedInterface(Supplier.class.getName());
			}
			sourceWriter = createWriter(composerFactory, printWriter);
			sourceWriter.println("public static final Class clazz = %s.class;",
					reflectedTypeFqn());
			/*
			 * Write no-args constructor
			 */
			if (hasCallableNoArgsConstructor) {
				sourceWriter.println("public  %s get(){", reflectedTypeFqn());
				sourceWriter.indent();
				sourceWriter.println("return new %s();", reflectedTypeFqn());
				sourceWriter.outdent();
				sourceWriter.println("}");
				sourceWriter.println();
			}
			/*
			 * Write init0 (which builds ClassReflector fields then calls
			 * ClassReflector.init() )
			 */
			sourceWriter.println("protected void init0(){");
			sourceWriter.indent();
			sourceWriter
					.println("List<Property> properties = new ArrayList<>();");
			reflection.getSortedPropertyReflections().stream()
					.filter(propertyReflector -> filter.emitProperty(
							reflection.type, propertyReflector.name))
					.map(PropertyGenerator::new)
					.forEach(PropertyGenerator::write);
			sourceWriter.println("List<Class> interfaces = new ArrayList<>();");
			sourceWriter.println("List<Class> bounds = new ArrayList<>();");
			sourceWriter.println(
					"AnnotationProvider.LookupProvider provider = new AnnotationProvider.LookupProvider();");
			reflection.getAnnotationReflections().stream()
					.filter(annRefl -> filter.emitAnnotation(reflection.type,
							annRefl.getAnnotation().annotationType()))
					.map(AnnotationExpressionWriter::new)
					.forEach(expressionWriter -> expressionWriter
							.write(sourceWriter));
			sourceWriter.println(
					"Map<String, Property> byName = new LinkedHashMap<>();");
			sourceWriter.println(
					"properties" + ".forEach(p->byName.put(p.getName(),p));");
			if (hasCallableNoArgsConstructor) {
				sourceWriter.println("Supplier supplier = this;",
						implementationFqn());
			} else {
				sourceWriter.println("Supplier supplier = null;");
			}
			sourceWriter.println(
					"Predicate<Class> assignableTo = c -> ClientReflections.isAssignableFrom(c,clazz);");
			Arrays.stream(reflection.type.getImplementedInterfaces())
					.forEach(i -> {
						sourceWriter.println("interfaces.add(%s.class);",
								i.getQualifiedSourceName());
					});
			List<JClassType> bounds = reflection
					.computeTypeBounds(providesJavacTypeBounds);
			bounds.forEach(t -> {
				sourceWriter.println("bounds.add(%s.class);",
						t.getQualifiedSourceName());
			});
			sourceWriter
					.println("TypeBounds typeBounds = new TypeBounds(bounds);");
			sourceWriter.println("boolean isAbstract = %s;",
					reflection.isHasAbstractModifier());
			sourceWriter.println("boolean isFinal = %s;",
					reflection.isHasFinalModifier());
			sourceWriter.println("init(clazz, properties, byName, provider,"
					+ " supplier, assignableTo, interfaces, typeBounds, isAbstract, isFinal);");
			sourceWriter.outdent();
			sourceWriter.println("}");
			closeClassBody();
			return true;
		}

		PropertyMethodGenerator
				propertyMethodGenerator(PropertyAccessor accessor) {
			if (accessor instanceof PropertyAccessor.Field) {
				return new FieldSourced((PropertyAccessor.Field) accessor);
			} else {
				return new MethodSourced((PropertyAccessor.Method) accessor);
			}
		}

		boolean sourcesPropertyChangeEvents() {
			if (reflectedType.getQualifiedSourceName().endsWith("Thaithala")) {
				JMethod jMethod = Arrays
						.stream(reflectedType.getInheritableMethods())
						.filter(m -> m.getName()
								.equals("propertyChangeListeners"))
						.findFirst().get();
				int debug = 3;
			}
			return Arrays.stream(reflectedType.getInheritableMethods())
					.filter(m -> m.getName().equals("firePropertyChange"))
					.filter(m -> m.getReturnType() == voidJType)
					.anyMatch(m -> Arrays.equals(m.getParameterTypes(),
							firePropertyChangeMethodJTypes));
		}

		void writeForNameCase(SourceWriter sourceWriter) {
			sourceWriter.println("case \"%s\":",
					reflection.type.getQualifiedBinaryName());
			sourceWriter.indent();
			sourceWriter.println("return %s.clazz;", implementationFqn());
			sourceWriter.outdent();
		}

		/*
		 * Using a switch avoids an extra type per reflected class. Ditto
		 * generated ClassReflector being its own instance supplier
		 */
		void writeReflectorCase(SourceWriter sourceWriter) {
			sourceWriter.println("case \"%s\":", reflectedTypeFqBinaryName());
			sourceWriter.indent();
			sourceWriter.println("return new %s();", implementationFqn());
			sourceWriter.outdent();
		}

		void writeRegisterRegistrations(SourceWriter sourceWriter) {
			reflection.getRegistrations().stream()
					.sorted(REGISTRY_LOCATION_COMPARATOR)
					.forEach(registryLocation -> {
						if (reflection.type.isPublic()) {
							sourceWriter.print(
									"Registry.register().add(%s.class,",
									reflection.type.getQualifiedSourceName());
						} else {
							sourceWriter.print(
									"Registry.register().add(%s___refImpl.clazz,",
									reflection.type.getQualifiedSourceName());
						}
						AnnotationExpressionWriter instanceGenerator = new AnnotationExpressionWriter(
								new AnnotationReflection(registryLocation));
						instanceGenerator.writeExpression(sourceWriter);
						sourceWriter.println(");");
					});
		}

		class FieldSourced extends PropertyMethodGenerator {
			FieldSourced(PropertyAccessor.Field accessor) {
				super(accessor);
			}

			@Override
			protected void printHoist() {
				if (accessor.getter) {
					sourceWriter.println("@Override");
					sourceWriter.println("public Object get(Object bean){");
					sourceWriter.indent();
					sourceWriter.print("return ((%s)bean).%s;",
							accessor.getEnclosingType()
									.getQualifiedSourceName(),
							accessor.getName());
					sourceWriter.outdent();
					sourceWriter.println("}");
				} else {
					sourceWriter.println("@Override");
					sourceWriter.println(
							"public void set(Object bean,Object value){");
					sourceWriter.indent();
					if (reflection.sourcesPropertyChanges) {
						/*
						 * See BaseSourcesPropertyChangeEvents.set for similar code - basically, prep the propertyChange
						 * @formatter:off
						 * output is:
						 @Override
					      public void set(Object bean,Object value){
					      		<beantypefqn> typed = (<dbeantypefqn>)bean;
					      		Object oldValue = typed.<fieldName>;
					            typed.<fieldName>=((valuetypefqn)value);
					            typed.firePropertyChange("overwriteOriginals", oldValue, value);
					            }
					      }
						 * @formatter: on
						 */
						sourceWriter.println("%s typed = (%s) bean;",
								accessor.getEnclosingType()
										.getQualifiedSourceName(),
										accessor.getEnclosingType()
										.getQualifiedSourceName()
								);
						sourceWriter.println("Object oldValue = typed.%s;",
								accessor.getName());
						sourceWriter.println("typed.%s=(%s)value;",
								accessor.getName(), accessor.getPropertyType()
										.getQualifiedSourceName());
						sourceWriter.println("typed.firePropertyChange(\"%s\", oldValue, value);",accessor.getName());
					} else {
						/*
						 * @formatter:off
						 * output is:
						 @Override
					      public void set(Object bean,Object value){
					            ((<beantypefqn>)bean).<fieldName>=((valuetypefqn)value);}
					      }
						 * @formatter: on
						 */
						sourceWriter.println("((%s)bean).%s=(%s)value;",
								accessor.getEnclosingType()
										.getQualifiedSourceName(),
								accessor.getName(), accessor.getPropertyType()
										.getQualifiedSourceName());
					}
					sourceWriter.outdent();
					sourceWriter.println("}");
				}
			}
		}

		class MethodSourced extends PropertyMethodGenerator {
			MethodSourced(PropertyAccessor.Method accessor) {
				super(accessor);
			}

			// FIXME - beans1x5 - add 'serialization setter' which doesn't
			// emit
			// a property change
			@Override
			public void printHoist() {
				if (accessor.getter) {
					sourceWriter.println("@Override");
					sourceWriter.println("public Object get(Object bean){");
					sourceWriter.indent();
					sourceWriter.print("return  ((%s)bean).%s();",
							accessor.getEnclosingType()
									.getQualifiedSourceName(),
							accessor.getName());
					sourceWriter.outdent();
					sourceWriter.println("}");
				} else {
					sourceWriter.println("@Override");
					sourceWriter.println(
							"public void set(Object bean,Object value){");
					sourceWriter.indent();
					sourceWriter.print("  ((%s)bean).%s((%s)value);",
							accessor.getEnclosingType()
									.getQualifiedSourceName(),
							accessor.getName(), accessor.getPropertyType()
									.getQualifiedSourceName());
					sourceWriter.outdent();
					sourceWriter.println("}");
				}
			}
		}

		class PropertyGenerator extends ReflectorGenerator {
			private PropertyReflection reflection;

			public PropertyGenerator(PropertyReflection reflection) {
				this.reflection = reflection;
			}

			@Override
			protected void prepare() {
				// noop, all done by PropertyReflection
				throw new UnsupportedOperationException();
			}

			@Override
			protected boolean write() {
				sourceWriter.println("{");
				sourceWriter.indent();
				sourceWriter.println(
						"AnnotationProvider.LookupProvider provider = new AnnotationProvider.LookupProvider();");
				reflection.getAnnotationReflections().stream()
						.map(AnnotationExpressionWriter::new)
						.forEach(expressionWriter -> expressionWriter
								.write(sourceWriter));
				sourceWriter.println("String name = %s;",
						stringLiteral(reflection.name, false));
				sourceWriter.print("Method getter = ");
				printMethodRef(reflection.getter);
				sourceWriter.print("Method setter = ");
				printMethodRef(reflection.setter);
				sourceWriter.println("Class propertyType = %s.class;",
						reflection.propertyType.getQualifiedSourceName());
				sourceWriter.println("Class owningType = %s.class;",
						reflection.classReflection.type
								.getQualifiedSourceName());
				sourceWriter.println("Class declaringType = %s.class;",
						reflection.declaringType.getQualifiedSourceName());
				sourceWriter.println("List<Class> bounds = new ArrayList<>();");
				List<? extends JClassType> bounds = reflection.jtypeBounds;
				bounds.forEach(t -> {
					sourceWriter.println("bounds.add(%s.class);",
							t.getQualifiedSourceName());
				});
				sourceWriter.println(
						"TypeBounds typeBounds = new TypeBounds(bounds);");
				sourceWriter.print(
						"Property property = new Property(name, getter, setter, propertyType, owningType, declaringType, typeBounds, provider)");
				sourceWriter.println("{");
				sourceWriter.indent();
				printMethodHoist(reflection.getter);
				printMethodHoist(reflection.setter);
				sourceWriter.outdent();
				sourceWriter.println("};");
				sourceWriter.println("");
				sourceWriter.println("properties.add(property);");
				sourceWriter.outdent();
				sourceWriter.println("}");
				sourceWriter.println("");
				return true;
			}

			void printMethodHoist(PropertyAccessor accessor) {
				if (accessor == null) {
					return;
				}
				PropertyMethodGenerator methodGenerator = propertyMethodGenerator(
						accessor);
				methodGenerator.printHoist();
			}

			void printMethodRef(PropertyAccessor method) {
				if (method == null) {
					sourceWriter.print("null;");
					return;
				} else {
					sourceWriter.print("Method.EXISTS_REF;");
					return;
				}
			}
		}

		abstract class PropertyMethodGenerator {
			PropertyAccessor accessor;

			PropertyMethodGenerator(PropertyAccessor propertyMethod) {
				this.accessor = propertyMethod;
			}

			protected abstract void printHoist();
		}
	}

	//
	//
	// FIXME - speed - gwt - write only changed classreflectors (although this
	// gets us
	// 90% of possible speedup)
	static class IncrementalSupport implements Serializable {
		Map<String, Long> writeableTimes;

		public IncrementalSupport() {
		}

		boolean checkSourcesUnmodified(TreeLogger logger,
				ClientReflectionGenerator reflectionGenerator) {
			boolean unmodified = writeableTimes(reflectionGenerator)
					.equals(writeableTimes);
			return unmodified;
		}

		IncrementalSupport prepareCacheInfo(
				ClientReflectionGenerator reflectionGenerator) {
			this.writeableTimes = writeableTimes(reflectionGenerator);
			return this;
		}

		Map<String, Long>
				writeableTimes(ClientReflectionGenerator reflectionGenerator) {
			Map<String, Long> writeableTimes = new LinkedHashMap<>();
			TypeOracle typeOracle = reflectionGenerator.context.getTypeOracle();
			writeableTypes(reflectionGenerator).forEach(t -> writeableTimes
					.put(t.getQualifiedSourceName(), t.getLastModifiedTime()));
			return writeableTimes;
		}

		Stream<JRealClassType>
				writeableTypes(ClientReflectionGenerator reflectionGenerator) {
			Stream<JRealClassType> writeReflectorTypes = reflectionGenerator.moduleGenerator.writeReflectors
					.stream().map(r -> r.realType())
					.map(t -> (JRealClassType) t);
			Stream<JRealClassType> annotationTypes = reflectionGenerator.moduleGenerator.annotationImplementations
					.stream().map(r -> r.realType())
					.map(t -> (JRealClassType) t);
			return Stream.concat(writeReflectorTypes, annotationTypes);
		}
	}

	class ModuleReflectionGenerator extends UnitGenerator {
		List<AnnotationImplementationGenerator> annotationImplementations = new ArrayList<>();

		Map<JClassType, ClassReflectorGenerator> classReflectors = new LinkedHashMap<>();

		List<ClassReflectorGenerator> writeReflectors;

		boolean alreadyWritten = false;

		private Multiset<JClassType, Set<JClassType>> subtypes;

		protected ModuleReflectionGenerator(String implementationName,
				JClassType superClassOrInterfaceType) {
			super(null, superClassOrInterfaceType.getPackage().getName(),
					implementationName, superClassOrInterfaceType);
			visibleAnnotationFilter = new VisibleAnnotationFilterClient();
		}

		/*
		 * Not all registrations! Only those for which Registry.impl() or
		 * Registry.Query.forEnum() could return the registration.
		 *
		 * Note that where multiple classes have equal priority in the registry,
		 * registration data for those classes will *not* be added to the
		 * registry unless the class impelements Registration.EnumDiscriminator
		 * or Registration.Ensure
		 */
		public AppImplRegistrations listImplementationRegistrations() {
			AppImplRegistrations implRegistrations = new AppImplRegistrations();
			List<Registration> allRegistrations = new ArrayList<>();
			computeRegistryTypes().stream().forEach(t -> {
				List<Registration> typeRegistrations = new AnnotationLocationTypeInfo(
						t, annotationResolver)
								.getAnnotations(Registration.class);
				allRegistrations.addAll(typeRegistrations);
				implRegistrations.add(t, typeRegistrations);
			});
			Predicate<Registration> permitEqualPriorityTest = r -> Registration.EnumDiscriminator.class
					.isAssignableFrom(r.value()[0])
					|| Registration.Ensure.class.isAssignableFrom(r.value()[0]);
			Set<Registration> implementationRegistrations = Registry.Internals
					.removeNonImplmentationRegistrations(allRegistrations,
							permitEqualPriorityTest)
					.stream().collect(Collectors.toSet());
			implRegistrations.entries.removeIf(
					e -> !e.retainRegistrations(implementationRegistrations));
			return implRegistrations;
		}

		public AppReflectableTypes listReflectableTypes() {
			AppReflectableTypes reflectableTypes = new AppReflectableTypes();
			computeReflectableTypes().forEach(reflectableTypes::addType);
			return reflectableTypes;
		}

		private Set<JClassType>
				computeAsyncSerializableArguments(JClassType type) {
			JClassType asyncCallbackType = getType(
					AsyncCallback.class.getCanonicalName());
			return Arrays.stream(type.getMethods())
					.flatMap(m -> Arrays.stream(m.getParameterTypes())
							.filter(t -> t instanceof JParameterizedType
									&& ((JParameterizedType) t)
											.getBaseType() == asyncCallbackType)
							.flatMap(t -> ReachabilityData
									.toReachableConcreteTypes(t, subtypes)))
					.filter(t -> t != asyncCallbackType.getErasedType())
					.filter(ReachabilityData::excludeJavaType)
					.collect(AlcinaCollectors.toLinkedHashSet());
		}

		private Multiset<JClassType, Set<JClassType>>
				computeAsyncSerializableTypes(
						Multiset<JClassType, Set<JClassType>> subtypes) {
			Multiset<JClassType, Set<JClassType>> result = new Multiset<JClassType, Set<JClassType>>();
			subtypes.get(
					getType(AsyncSerializableTypes.class.getCanonicalName()))
					.forEach(t -> result.put(t,
							computeAsyncSerializableArguments(t)));
			return result;
		}

		private Multiset<JClassType, Set<JClassType>> computeSettableTypes() {
			Multiset<JClassType, Set<JClassType>> result = new Multiset<JClassType, Set<JClassType>>();
			Arrays.stream(context.getTypeOracle().getTypes())
					.filter(t -> t.isAnnotationPresent(Bean.class))
					.forEach(t -> result.put(t.getErasedType(),
							computeSetterArguments(t)));
			return result;
		}

		private Set<JClassType> computeSetterArguments(JClassType type) {
			ClassReflectorGenerator reflectorGenerator = classReflectors
					.get(type.getErasedType());
			if (reflectorGenerator == null) {
				return Collections.emptySet();
			}
			Set<JClassType> computed = reflectorGenerator.reflection
					.getSortedPropertyReflections().stream()
					.filter(PropertyReflection::isSerializable)
					.map(PropertyReflection::getPropertyType)
					.flatMap(t -> ReachabilityData.toReachableConcreteTypes(t,
							subtypes))
					.filter(ReachabilityData::excludeJavaType)
					.collect(AlcinaCollectors.toLinkedHashSet());
			return computed;
		}

		private Multiset<JClassType, Set<JClassType>> computeSubtypes() {
			Multiset<JClassType, Set<JClassType>> result = new Multiset<>();
			Arrays.stream(context.getTypeOracle().getTypes())
					.map(JClassType::getErasedType).distinct().forEach(t -> {
						Set<? extends JClassType> supertypeHierarchy = t
								.getFlattenedSupertypeHierarchy();
						supertypeHierarchy.stream()
								.forEach(st -> result.add(st, t));
					});
			return result;
		}

		private boolean isReflectable(JClassType t) {
			return filter.isWhitelistReflectable(t) || has(t, Reflected.class)
					|| has(t, Bean.class) || has(t, Registration.class)
					// the annotations themselves
					|| t.isAnnotationPresent(ClientVisible.class)
					|| isReflectableJavaCoreClass(t)
					|| isReflectableJavaCollectionClass(t);
		}

		private void writeMethodDefinition(String accessModifier,
				String returnType, String methodName, String methodArguments,
				String methodIndex) {
			sourceWriter.println("%s %s %s%s(%s){", accessModifier, returnType,
					methodName, methodIndex, methodArguments);
			sourceWriter.indent();
		}

		@Override
		protected void prepare() {
			prepareAnnotationImplementationGenerators();
			List<JClassType> types = computeReachableTypes();
			filter.updateReachableTypes(types);
			types.stream().map(ClassReflectorGenerator::new).forEach(
					crg -> classReflectors.put(crg.reflection.type, crg));
			classReflectors.values().forEach(ClassReflectorGenerator::prepare);
			writeReflectors = classReflectors.values().stream()
					.filter(r -> filter.emitType(r.reflection.type))
					.collect(Collectors.toList());
		}

		@Override
		protected boolean write() {
			if (!createPrintWriter()) {
				return false;
			}
			annotationImplementations.stream()
					.forEach(AnnotationImplementationGenerator::write);
			writeClassReflectors();
			composerFactory.addImport(LinkedHashMap.class.getName());
			composerFactory.addImport(Map.class.getName());
			composerFactory.addImport(Registry.class.getName());
			composerFactory.addImport(ClientReflections.class.getName());
			composerFactory.addImport(ClassReflector.class.getName());
			composerFactory.addImport(Supplier.class.getName());
			composerFactory.setSuperclass(
					superClassOrInterfaceType.getQualifiedSourceName());
			sourceWriter = createWriter(composerFactory, printWriter);
			writeReflectorCases();
			writeForNameCases();
			writeRegisterRegistrations();
			closeClassBody();
			return true;
		}

		List<JClassType> computeReachableTypes() {
			return Arrays.stream(context.getTypeOracle().getTypes())
					.filter(this::isReflectable)
					.map(JClassType::getFlattenedSupertypeHierarchy)
					.flatMap(Collection::stream).map(ClassReflection::erase)
					.distinct().filter(this::isProtectionVisible)
					.collect(Collectors.toList());
		}
		Stream<TypeHierarchy> computeReflectableTypes() {
			subtypes = computeSubtypes();
			Multiset<JClassType, Set<JClassType>> asyncSerializableTypes = computeAsyncSerializableTypes(
					subtypes);
			Multiset<JClassType, Set<JClassType>> settableTypes = computeSettableTypes();
			return Arrays.stream(context.getTypeOracle().getTypes())
					.filter(t -> isReflectable(t))
					.map(JClassType::getFlattenedSupertypeHierarchy)
					.flatMap(Collection::stream).map(ClassReflection::erase)
					.distinct()
					.filter(t -> !t.getQualifiedSourceName()
							.equals(Object.class.getCanonicalName()))
					.filter(t -> t.isPublic()).map(t -> new TypeHierarchy(t,
							subtypes, asyncSerializableTypes, settableTypes));
		}

		List<JClassType> computeRegistryTypes() {
			return Arrays.stream(context.getTypeOracle().getTypes())
					.filter(t -> has(t, Registration.class))
					.map(ClassReflection::erase).distinct()
					// only interested in instantiable types
					.filter(t -> t.isPublic() && !t.isAbstract())
					.collect(Collectors.toList());
		}

		boolean isProtectionVisible(JClassType type){
			if(type.isPrivate()){
			return false;
			}
			if(type.isPublic()){
				return true;
			}
			return !type.getQualifiedSourceName().startsWith("java");
		}

		void prepareAnnotationImplementationGenerators() {
			Arrays.stream(context.getTypeOracle().getTypes())
					.filter(t -> t.isAnnotationPresent(ClientVisible.class)
							|| t.getQualifiedSourceName().equals(
									Registration.class.getCanonicalName()))
					.map(JClassType::isAnnotation)
					.map(AnnotationImplementationGenerator::new).sorted()
					.forEach(annotationImplementations::add);
			annotationImplementations.stream()
					.forEach(AnnotationImplementationGenerator::prepare);
		}

		void writeClassReflectors() {
			writeReflectors.forEach(ClassReflectorGenerator::write);
		}

		void writeForClassReflectors(String methodName, String methodArguments,
				String returnType,
				Consumer<ClassReflectorGenerator> perReflector) {
			boolean voidMethod = returnType.equals("void");
			String methodArgumentName = methodArguments.isEmpty() ? ""
					: methodArguments.replaceFirst(".+ (.+)", "$1");
			if (writeReflectors.isEmpty()) {
				String accessModifier = "public";
				writeMethodDefinition(accessModifier, returnType, methodName,
						methodArguments, "");
			}
			for (int idx = 0; idx < writeReflectors.size(); idx++) {
				boolean writePreamble = idx % 100 == 0;
				boolean initial = idx == 0;
				if (writePreamble) {
					String accessModifier = initial ? "public" : "private";
					String methodIndex = initial ? "" : "_" + idx / 100;
					// first terminate current container method
					if (initial) {
					} else {
						// continue registration with next method
						if (voidMethod) {
							sourceWriter.println("%s%s(%s);", methodName,
									methodIndex, methodArgumentName);
						} else {
							sourceWriter.println("default:");
							sourceWriter.indent();
							sourceWriter.println("return %s%s(%s);", methodName,
									methodIndex, methodArgumentName);
							sourceWriter.outdent();
							sourceWriter.outdent();
							sourceWriter.println("}");
						}
						sourceWriter.outdent();
						sourceWriter.println("}");
						sourceWriter.println();
					}
					// write method preamble
					writeMethodDefinition(accessModifier, returnType,
							methodName, methodArguments, methodIndex);
					if (!voidMethod) {
						sourceWriter.println("switch (%s){",
								methodArgumentName);
						sourceWriter.indent();
					}
				}
				perReflector.accept(writeReflectors.get(idx));
			}
			// terminate last method
			if (!voidMethod) {
				if (writeReflectors.isEmpty()) {
					sourceWriter.println("return null;");
				} else {
					sourceWriter.println("default:");
					sourceWriter.indent();
					sourceWriter.println("return null;");
					sourceWriter.outdent();
					sourceWriter.outdent();
					sourceWriter.println("}");
				}
			}
			sourceWriter.outdent();
			sourceWriter.println("}");
			sourceWriter.println();
		}

		void writeForNameCases() {
			writeForClassReflectors("forName", "String className", "Class",
					crg -> crg.writeForNameCase(sourceWriter));
		}

		void writeReflectorCases() {
			writeForClassReflectors("getClassReflector_", "String className",
					"ClassReflector",
					crg -> crg.writeReflectorCase(sourceWriter));
		}

		void writeRegisterRegistrations() {
			writeForClassReflectors("registerRegistrations", "", "void",
					crg -> crg.writeRegisterRegistrations(sourceWriter));
		}
	}

	abstract class ReflectorGenerator {
		protected abstract void prepare();

		/**
		 * @return true if code was written
		 */
		protected abstract boolean write();
	}

	abstract class UnitGenerator extends ReflectorGenerator {
		protected SourceWriter sourceWriter;

		protected ClassSourceFileComposerFactory composerFactory;

		protected String packageName;

		protected String implementationName;

		protected PrintWriter printWriter;

		JClassType superClassOrInterfaceType;

		protected JClassType reflectedType;

		protected UnitGenerator(JClassType reflectionInfoForType,
				JClassType superClassOrInterfaceType,
				boolean annotationImplementation) {
			this(reflectionInfoForType,
					reflectionInfoForType.getPackage().getName(),
					implementationName(reflectionInfoForType,
							annotationImplementation),
					superClassOrInterfaceType);
		}

		protected UnitGenerator(JClassType reflectedType, String packageName,
				String implementationName,
				JClassType superClassOrInterfaceType) {
			super();
			this.reflectedType = reflectedType;
			if (packageName.startsWith("java")) {
				packageName = getClass().getPackageName() + "."
						+ packageName.replace(".", "_");
			}
			this.packageName = packageName;
			this.implementationName = implementationName;
			this.superClassOrInterfaceType = superClassOrInterfaceType;
			composerFactory = new ClassSourceFileComposerFactory(packageName,
					implementationName);
		}

		protected void closeClassBody() {
			sourceWriter.outdent();
			sourceWriter.println("}");
			context.commit(logger, printWriter);
		}

		protected boolean createPrintWriter() {
			printWriter = context.tryCreate(logger, packageName,
					implementationName);
			return printWriter != null;
		}

		protected void createSourceWriter(String packageName,
				String className) {
		}

		protected String implementationFqn() {
			return packageName + "." + implementationName;
		}

		protected JRealClassType realType() {
			if (reflectedType instanceof JRealClassType) {
				return (JRealClassType) reflectedType;
			} else if (reflectedType instanceof JParameterizedType) {
				return ((JParameterizedType) reflectedType).getBaseType();
			} else if (reflectedType instanceof JRawType) {
				return ((JRawType) reflectedType).getBaseType();
			} else {
				throw new UnsupportedOperationException();
			}
		}

		protected String reflectedTypeFqBinaryName() {
			return reflectedType.getQualifiedBinaryName();
		}

		protected String reflectedTypeFqn() {
			return reflectedType.getQualifiedSourceName();
		}
	}

	class VisibleAnnotationFilterClient implements ReflectionVisibility {
		@Override
		public boolean isVisibleAnnotation(
				Class<? extends Annotation> annotationType) {
			return !Registration.Support
					.isRegistrationAnnotation(annotationType)
					&& visibleAnnotationTypes.contains(annotationType);
		}

		@Override
		public boolean isVisibleType(JType type) {
			return ClassReflection.has((JClassType) type, Bean.class);
		}
	}
}
