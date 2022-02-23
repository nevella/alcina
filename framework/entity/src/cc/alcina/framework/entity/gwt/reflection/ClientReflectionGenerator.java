package cc.alcina.framework.entity.gwt.reflection;

import java.io.PrintWriter;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.totsp.gwittir.client.beans.annotations.Omit;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.DomainCollections;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.NonClientRegistryPointType;
import cc.alcina.framework.common.client.logic.reflection.ReflectionModule;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registrations;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.AnnotationProvider;
import cc.alcina.framework.common.client.reflection.AsyncSerializableTypes;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.ClientReflections;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.common.client.util.ToStringComparator;
import cc.alcina.framework.entity.gwt.reflection.ClientReflectionGenerator.ClassReflectorGenerator.PropertyGenerator;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppImplRegistrations;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppReflectableTypes;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.TypeHierarchy;

public class ClientReflectionGenerator extends Generator {
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

	static final Predicate<Registration> CLIENT_VISIBLE_ANNOTATION_FILTER = new Predicate<Registration>() {
		@Override
		public boolean test(Registration o) {
			return o.value()[0]
					.getAnnotation(NonClientRegistryPointType.class) == null;
		}
	};

	static final String REF_IMPL = "__refImpl";

	static final String ANN_IMPL = "__annImpl";

	private static boolean alcinaCollectionsConfigured;

	static final String DATA_FOLDER_CONFIGURATION_KEY = "ClientReflectionGenerator.ReachabilityData.folder";

	static final String FILTER_PEER_CONFIGURATION_KEY = "ClientReflectionGenerator.FilterPeer.className";

	static final String LINKER_PEER_CONFIGURATION_KEY = "ClientReflectionGenerator.LinkerPeer.className";

	static JClassType erase(JClassType t) {
		if (t.isParameterized() != null) {
			return t.isParameterized().getBaseType().getErasedType();
		} else {
			return t.getErasedType();
		}
	}

	static JType erase(JType t) {
		if (t.isClass() != null) {
			return erase((JClassType) t);
		} else {
			return t;
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

	long start;

	TreeLogger logger;

	GeneratorContext context;

	ModuleReflectionGenerator moduleGenerator;

	JClassType generatingType;

	ReflectionModule module;

	String typeName;

	String implementationName;

	String packageName;

	Map<Class, String> annotationImplFqn = new LinkedHashMap<>();

	Set<Class<? extends Annotation>> visibleAnnotationTypes = new LinkedHashSet<>();

	JClassType classReflectorType;

	ClientReflectionFilter filter;

	AnnotationLocationTypeInfo.Resolver annotationResolver = new AnnotationLocationTypeInfo.Resolver();

	@Override
	public String generate(TreeLogger logger, GeneratorContext context,
			String typeName) throws UnableToCompleteException {
		try {
			this.logger = logger;
			this.context = context;
			this.typeName = typeName;
			setupEnvironment();
			setupFilter();
			moduleGenerator = new ModuleReflectionGenerator(implementationName,
					generatingType);
			if (moduleGenerator.isPending()) {
				moduleGenerator.prepare();
				moduleGenerator.write();
				ReachabilityData.AppImplRegistrations registrations = moduleGenerator
						.listImplementationRegistrations();
				ReachabilityData.AppReflectableTypes reflectableTypes = moduleGenerator
						.listReflectableTypes();
				filter.onGenerationComplete(registrations, reflectableTypes,
						Arrays.stream(context.getTypeOracle().getTypes()));
				System.out.format(
						"Client reflection generation  [%s] -  %s/%s/%s reflected types - %s ms\n",
						module.value(), moduleGenerator.writeReflectors.size(),
						moduleGenerator.classReflectors.size(),
						context.getTypeOracle().getTypes().length,
						System.currentTimeMillis() - start);
			}
			return moduleGenerator.implementationFqn();
		} catch (Exception e) {
			e.printStackTrace();
			throw new WrappedRuntimeException(e);
		}
	}

	private void setupFilter() throws Exception {
		filter = new ClientReflectionFilter();
		filter.init(logger, context, module.value());
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

	JClassType getType(String typeName) {
		try {
			return context.getTypeOracle().getType(typeName);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	<A extends Annotation> boolean has(JClassType t, Class<A> annotationClass) {
		return t.getAnnotation(annotationClass) != null;
	}

	void setupEnvironment() {
		if (!alcinaCollectionsConfigured) {
			Registry.register().singleton(DomainCollections.class,
					new DomainCollections());
			alcinaCollectionsConfigured = true;
		}
		start = System.currentTimeMillis();
		String superClassName = null;
		generatingType = getType(typeName);
		classReflectorType = getType(ClassReflector.class.getCanonicalName());
		module = generatingType.getAnnotation(ReflectionModule.class);
		implementationName = String.format("ModuleReflector_%s_Impl",
				module.value());
	}

	String stringLiteral(String value) {
		return String.format("\"%s\"",
				value.replace("\\", "\\\\").replace("\n", "\\n"));
	}

	void writeAnnotationValue(SourceWriter sourceWriter, Object value,
			Class declaredType) {
		// FIXME
		if (value == null) {
			sourceWriter.print("null");
			return;
		}
		Class<? extends Object> clazz = value.getClass();
		if (clazz.isArray()) {
			Class componentType = declaredType.getComponentType();
			String componentImplementationName = annotationImplFqn.containsKey(
					componentType) ? annotationImplFqn.get(componentType)
							: componentType.getCanonicalName();
			sourceWriter.print("new %s[]{", componentImplementationName);
			int length = Array.getLength(value);
			for (int i = 0; i < length; i++) {
				if (i != 0) {
					sourceWriter.print(", ");
				}
				Object element = Array.get(value, i);
				writeAnnotationValue(sourceWriter, element, componentType);
			}
			sourceWriter.print("}");
		} else if (declaredType.isAnnotation()) {
			new AnnotationExpressionWriter((Annotation) value)
					.writeExpression(sourceWriter);
		} else if (clazz.equals(Class.class)) {
			sourceWriter.print(((Class) value).getCanonicalName() + ".class");
		} else if (clazz.equals(String.class)) {
			sourceWriter.print(stringLiteral(value.toString()));
		} else if (Enum.class.isAssignableFrom(clazz)) {
			sourceWriter.print("%s.%s", clazz.getCanonicalName(),
					value.toString());
		} else {
			sourceWriter.print(value.toString());
		}
	}

	class AnnotationExpressionWriter
			implements Comparable<AnnotationExpressionWriter> {
		Annotation annotation;

		public AnnotationExpressionWriter(Annotation annotation) {
			this.annotation = annotation;
		}

		@Override
		public int compareTo(AnnotationExpressionWriter o) {
			return annotation.annotationType().getCanonicalName().compareTo(
					o.annotation.annotationType().getCanonicalName());
		}

		protected void write(SourceWriter sourceWriter) {
			sourceWriter.println("{");
			sourceWriter.indent();
			sourceWriter.print("Annotation annotation = ");
			writeExpression(sourceWriter);
			sourceWriter.println(";");
			sourceWriter.println(
					"provider.annotations.put(%s.class,annotation);",
					annotation.annotationType().getCanonicalName());
			sourceWriter.outdent();
			sourceWriter.println("}");
		}

		void writeExpression(SourceWriter sourceWriter) {
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
					if (!CommonUtils.equalsWithNullEmptyEquality(
							annotationValue, defaultValue)) {
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
		protected void write() {
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
			closeClassBody();
		}
	}

	class ClassReflectorGenerator extends UnitGenerator
			implements Comparable<ClassReflectorGenerator> {
		Map<String, PropertyGenerator> propertyGenerators = new LinkedHashMap<>();

		List<AnnotationExpressionWriter> annotationExpressionWriters = new ArrayList<>();

		JClassType type;

		boolean hasCallableNoArgsConstructor;

		boolean isAbstract;

		Pattern getterPattern = Pattern.compile("(?:is|get)([A-Z].*)");

		Pattern setterPattern = Pattern.compile("(?:set)([A-Z].*)");

		List<Registration> registrations = new ArrayList<>();

		public ClassReflectorGenerator(JClassType type) {
			super(type, classReflectorType, false);
			this.type = type;
		}

		@Override
		public int compareTo(ClassReflectorGenerator o) {
			return type.getQualifiedSourceName()
					.compareTo(o.type.getQualifiedSourceName());
		}

		@Override
		protected void createPrintWriter(boolean inConstructor) {
			if (!inConstructor) {
				super.createPrintWriter(inConstructor);
			}
		}

		@Override
		protected void prepare() {
			isAbstract = type.isAbstract();
			hasCallableNoArgsConstructor = !isAbstract
					&& !type.getQualifiedSourceName().equals("java.lang.Class")
					&& Arrays.stream(type.getConstructors())
							.filter(c -> c.getParameters().length == 0)
							.findFirst().filter(c -> c.isPublic()).isPresent();
			Arrays.stream(type.getAnnotations())
					.filter(a -> a.annotationType() != Registration.class && a
							.annotationType() != Registration.Singleton.class
							&& a.annotationType() != Registrations.class
							&& visibleAnnotationTypes
									.contains(a.annotationType()))
					.map(AnnotationExpressionWriter::new).sorted()
					.forEach(annotationExpressionWriters::add);
			if (!isAbstract) {
				prepareProperties();
				prepareRegistrations();
			}
		}

		@Override
		protected void write() {
			createPrintWriter(false);
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
			composerFactory.addImport(
					cc.alcina.framework.common.client.reflection.Method.class
							.getCanonicalName());
			composerFactory.addImport(Registration.class.getName());
			sourceWriter = createWriter(composerFactory, printWriter);
			if (hasCallableNoArgsConstructor) {
				sourceWriter.println("public static %s __new(){",
						reflectedTypeFqn());
				sourceWriter.indent();
				sourceWriter.println("return new %s();", reflectedTypeFqn());
				sourceWriter.outdent();
				sourceWriter.println("}");
				sourceWriter.println();
			}
			sourceWriter.println("protected void init0(){");
			sourceWriter.indent();
			sourceWriter.println("Class clazz = %s.class;", reflectedTypeFqn());
			sourceWriter
					.println("List<Property> properties = new ArrayList<>();");
			propertyGenerators.values().stream()
					.filter(propertyGenerator -> filter.emitProperty(type,
							propertyGenerator.name))
					.forEach(PropertyGenerator::write);
			sourceWriter.println("List<Class> interfaces = new ArrayList<>();");
			sourceWriter.println(
					"AnnotationProvider.LookupProvider provider = new AnnotationProvider.LookupProvider();");
			annotationExpressionWriters.stream()
					.filter(aew -> filter.emitAnnotation(type,
							aew.annotation.annotationType()))
					.forEach(expressionWriter -> expressionWriter
							.write(sourceWriter));
			sourceWriter.println(
					"Map<String, Property> byName = new LinkedHashMap<>();");
			sourceWriter.println(
					"properties" + ".forEach(p->byName.put(p.getName(),p));");
			if (hasCallableNoArgsConstructor) {
				sourceWriter.println("Supplier supplier = %s::__new;",
						implementationFqn());
			} else {
				sourceWriter.println("Supplier supplier = null;");
			}
			sourceWriter.println(
					"Predicate<Class> assignableTo = c -> ClientReflections.isAssignableFrom(c,clazz);");
			Arrays.stream(type.getImplementedInterfaces()).forEach(i -> {
				sourceWriter.println("interfaces.add(%s.class);",
						i.getQualifiedSourceName());
			});
			// will probably need to adjust
			sourceWriter.println("boolean reflective = true;");
			sourceWriter.println("boolean isAbstract = %s;", isAbstract);
			sourceWriter.println("init(clazz, properties, byName, provider,"
					+ " supplier, assignableTo, interfaces, reflective, isAbstract);");
			sourceWriter.outdent();
			sourceWriter.println("}");
			closeClassBody();
		}

		void prepareProperties() {
			boolean hasReflectableProperties = has(type, Bean.class);
			if (hasReflectableProperties) {
				Arrays.stream(type.getInheritableMethods())
						.map(this::toPropertyMethod).filter(Objects::nonNull)
						.forEach(m -> {
							PropertyGenerator propertyGenerator = propertyGenerators
									.computeIfAbsent(m.propertyName,
											PropertyGenerator::new);
							propertyGenerator.addMethod(m);
						});
			}
			propertyGenerators.values().stream().sorted()
					.forEach(PropertyGenerator::prepare);
		}

		void prepareRegistrations() {
			List<Registration> annotations = new AnnotationLocationTypeInfo(
					type, annotationResolver)
							.getAnnotations(Registration.class);
			annotations.stream().filter(CLIENT_VISIBLE_ANNOTATION_FILTER)
					.forEach(registrations::add);
		}

		PropertyMethod toPropertyMethod(JMethod method) {
			if (method.getAnnotation(Omit.class) != null) {
				return null;
			}
			if (method.getName().equals("getClass")) {
				return null;
			}
			if (!method.isPublic()) {
				return null;
			}
			// getter
			if (method.getParameters().length == 0) {
				Matcher m = getterPattern.matcher(method.getName());
				if (m.matches()) {
					return new PropertyMethod(CommonUtils.lcFirst(m.group(1)),
							true, method);
				}
			}
			if (method.getParameters().length == 1
					&& method.getReturnType() == JPrimitiveType.VOID) {
				Matcher m = setterPattern.matcher(method.getName());
				if (m.matches()) {
					return new PropertyMethod(CommonUtils.lcFirst(m.group(1)),
							false, method);
				}
			}
			return null;
		}

		void writeRegisterForName(SourceWriter sourceWriter) {
			// sourceWriter.println("map.put(\"%s\",() -> %s.class);",
			// type.getQualifiedBinaryName(), fqn());
			/*
			 * gwt compiler can't handle the class literal in the lambda - so
			 * use an anonymous class
			 */
			// sourceWriter.println(
			// "map.put(\"%s\",new Supplier(){public Object get(){return
			// %s.class;}});",
			// type.getQualifiedBinaryName(), fqn());
			// back to direct, not suppliers
			sourceWriter.println("map.put(\"%s\", %s.class);",
					type.getQualifiedBinaryName(), reflectedTypeFqn());
		}

		void writeRegisterReflectorSupplier(SourceWriter sourceWriter) {
			sourceWriter.println("map.put(%s.class,%s::new);",
					reflectedTypeFqn(), implementationFqn());
		}

		void writeRegisterRegistrations(SourceWriter sourceWriter) {
			registrations.stream().sorted(REGISTRY_LOCATION_COMPARATOR)
					.forEach(l -> {
						sourceWriter.print("Registry.register().add(%s.class,",
								type.getQualifiedSourceName());
						AnnotationExpressionWriter instanceGenerator = new AnnotationExpressionWriter(
								l);
						instanceGenerator.writeExpression(sourceWriter);
						sourceWriter.println(");");
					});
		}

		class PropertyGenerator extends ReflectorGenerator
				implements Comparable<PropertyGenerator> {
			List<AnnotationExpressionWriter> annotationExpressionWriters;

			PropertyMethod getter;

			PropertyMethod setter;

			String name;

			JType propertyType;

			public PropertyGenerator(String name) {
				this.name = name;
			}

			public void addMethod(PropertyMethod method) {
				if (method.getter) {
					getter = method;
					propertyType = method.method.getReturnType();
				} else {
					setter = method;
					propertyType = method.method.getParameters()[0].getType();
				}
				propertyType = erase(propertyType);
			}

			@Override
			public int compareTo(PropertyGenerator o) {
				return name.compareTo(o.name);
			}

			@Override
			protected void prepare() {
				annotationExpressionWriters = getter == null ? new ArrayList<>()
						: Arrays.stream(getter.method.getAnnotations())
								.filter(a -> visibleAnnotationTypes
										.contains(a.annotationType()))
								.map(AnnotationExpressionWriter::new).sorted()
								.collect(Collectors.toList());
			}

			@Override
			protected void write() {
				sourceWriter.println("{");
				sourceWriter.indent();
				sourceWriter.println(
						"AnnotationProvider.LookupProvider provider = new AnnotationProvider.LookupProvider();");
				annotationExpressionWriters
						.forEach(expressionWriter -> expressionWriter
								.write(sourceWriter));
				sourceWriter.println("String name = %s;", stringLiteral(name));
				sourceWriter.print("Method getter = ");
				printMethodFunction(getter);
				sourceWriter.println(";");
				sourceWriter.print("Method setter = ");
				printMethodFunction(setter);
				sourceWriter.println(";");
				sourceWriter.println("Class propertyType = %s.class;",
						propertyType.getQualifiedSourceName());
				sourceWriter.println("Class definingType = %s.class;",
						ClassReflectorGenerator.this.type
								.getQualifiedSourceName());
				sourceWriter.print(
						"Property property = new Property(name, getter, setter, propertyType, definingType, provider);");
				sourceWriter.println("properties.add(property);");
				sourceWriter.outdent();
				sourceWriter.println("}");
			}

			boolean isSerializable() {
				return getter != null && setter != null && !getter.method
						.isAnnotationPresent(AlcinaTransient.class);
			}

			void printMethodFunction(PropertyMethod method) {
				if (method == null) {
					sourceWriter.print("null");
					return;
				}
				sourceWriter.print("new Method(");
				// doesn't intern - so fairly bulky for final artifact
				// String toString = Ax.format("[Method: %s]",
				// method.method.getName());
				// sourceWriter.print(stringLiteral(toString));
				sourceWriter.print("null , ");
				method.printInvoker();
				sourceWriter.print(", ");
				sourceWriter.print("%s.class",
						erase(method.method.getReturnType())
								.getQualifiedSourceName());
				sourceWriter.print(")");
			}
		}

		class PropertyMethod {
			String propertyName;

			boolean getter;

			JMethod method;

			PropertyMethod(String propertyName, boolean getter,
					JMethod method) {
				this.propertyName = propertyName;
				this.getter = getter;
				this.method = method;
			}

			public void printInvoker() {
				if (getter) {
					sourceWriter.print("(target,args) -> ((%s)target).%s()",
							method.getEnclosingType().getQualifiedSourceName(),
							method.getName());
				} else {
					sourceWriter.print("(target,args) -> {((%s)target).%s(",
							method.getEnclosingType().getQualifiedSourceName(),
							method.getName());
					sourceWriter.print("(%s)((Object[])args)[0]",
							method.getParameters()[0].getType()
									.getQualifiedSourceName());
					sourceWriter.print("); return null;}");
				}
			}
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
		}

		/*
		 * Not all registrations! Only those for which Registry.impl() or
		 * Registry.Query.forEnum() could return the registration
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
			if (type.getName().contains("DomainTranche")) {
				int debug = 3;
			}
			ClassReflectorGenerator reflectorGenerator = classReflectors
					.get(type.getErasedType());
			if (reflectorGenerator == null) {
				return Collections.emptySet();
			}
			Set<JClassType> computed = reflectorGenerator.propertyGenerators
					.values().stream().filter(PropertyGenerator::isSerializable)
					.map(generator -> generator.setter.method)
					.flatMap(m -> Arrays.stream(m.getParameterTypes())
							.flatMap(t -> ReachabilityData
									.toReachableConcreteTypes(t, subtypes)))
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

		private boolean hasRegistrations(JClassType t) {
			return new AnnotationLocationTypeInfo(t, annotationResolver)
					.hasAnnotation(Registration.class);
		}

		private void writeMethodDefinition(String methodName,
				String methodArguments, String accessModifier,
				String methodIndex) {
			sourceWriter.println("%s void %s%s(%s){", accessModifier,
					methodName, methodIndex, methodArguments);
			sourceWriter.indent();
		}

		@Override
		protected void prepare() {
			prepareAnnotationImplementationGenerators();
			List<JClassType> types = computeReachableTypes();
			types.stream().map(ClassReflectorGenerator::new)
					.forEach(crg -> classReflectors.put(crg.type, crg));
			classReflectors.values().forEach(ClassReflectorGenerator::prepare);
			writeReflectors = classReflectors.values().stream()
					.filter(r -> filter.emitType(r.type))
					.collect(Collectors.toList());
		}

		@Override
		protected void write() {
			annotationImplementations.stream()
					.filter(AnnotationImplementationGenerator::isPending)
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
			writeRegisterReflectorSuppliers();
			writeRegisterForNames();
			writeRegisterRegistrations();
			closeClassBody();
		}

		List<JClassType> computeReachableTypes() {
			return Arrays.stream(context.getTypeOracle().getTypes())
					.filter(t -> (has(t, ClientInstantiable.class)
							|| has(t, Bean.class) || hasRegistrations(t)
							// the annotations themselves
							|| t.isAnnotationPresent(ClientVisible.class)
							|| filter.isReflectableJavaCoreClass(t)
							|| filter.isReflectableJavaCollectionClass(t)))
					.map(JClassType::getFlattenedSupertypeHierarchy)
					.flatMap(Collection::stream)
					.map(ClientReflectionGenerator::erase).distinct()
					.filter(t -> t.isPublic()).collect(Collectors.toList());
		}

		Stream<TypeHierarchy> computeReflectableTypes() {
			subtypes = computeSubtypes();
			Multiset<JClassType, Set<JClassType>> asyncSerializableTypes = computeAsyncSerializableTypes(
					subtypes);
			Multiset<JClassType, Set<JClassType>> settableTypes = computeSettableTypes();
			return Arrays.stream(context.getTypeOracle().getTypes())
					.filter(t -> (has(t, ClientInstantiable.class)
							|| has(t, Bean.class) || hasRegistrations(t)
							// the annotations themselves
							|| t.isAnnotationPresent(ClientVisible.class)
							|| filter.isReflectableJavaCoreClass(t)
							|| filter.isReflectableJavaCollectionClass(t)))
					.map(JClassType::getFlattenedSupertypeHierarchy)
					.flatMap(Collection::stream)
					.map(ClientReflectionGenerator::erase).distinct()
					.filter(t -> !t.getQualifiedSourceName()
							.equals(Object.class.getCanonicalName()))
					.filter(t -> t.isPublic()).map(t -> new TypeHierarchy(t,
							subtypes, asyncSerializableTypes, settableTypes));
		}

		List<JClassType> computeRegistryTypes() {
			return Arrays.stream(context.getTypeOracle().getTypes())
					.filter(t -> hasRegistrations(t))
					.map(ClientReflectionGenerator::erase).distinct()
					// only interested in instantiable types
					.filter(t -> t.isPublic() && !t.isAbstract())
					.collect(Collectors.toList());
		}

		void prepareAnnotationImplementationGenerators() {
			Arrays.stream(context.getTypeOracle().getTypes()).filter(t -> t
					.isAnnotationPresent(ClientVisible.class)
					|| t.getQualifiedSourceName().equals(
							"cc.alcina.framework.common.client.logic.reflection.Registration"))
					.map(JClassType::isAnnotation)
					.map(AnnotationImplementationGenerator::new).sorted()
					.forEach(annotationImplementations::add);
			annotationImplementations.stream()
					.filter(AnnotationImplementationGenerator::isPending)
					.forEach(AnnotationImplementationGenerator::prepare);
		}

		void writeClassReflectors() {
			writeReflectors.forEach(ClassReflectorGenerator::write);
		}

		void writeForClassReflectors(String methodName, String mapSignature,
				Consumer<ClassReflectorGenerator> perReflector) {
			String mapName = Ax.notBlank(mapSignature) ? "map" : "";
			String methodArguments = Ax.notBlank(mapSignature)
					? Ax.format("%s map", mapSignature)
					: "";
			if (writeReflectors.isEmpty()) {
				writeMethodDefinition(methodName, methodArguments, "public",
						"");
			}
			for (int idx = 0; idx < writeReflectors.size(); idx++) {
				boolean writePreamble = idx % 100 == 0;
				boolean initial = idx == 0;
				if (writePreamble) {
					String accessModifier = initial ? "public" : "private";
					String methodIndex = initial ? "" : "_" + idx / 100;
					if (initial) {
					} else {
						// continue registration with next method
						sourceWriter.println("%s%s(%s);", methodName,
								methodIndex, mapName);
						sourceWriter.outdent();
						sourceWriter.println("}");
						sourceWriter.println();
					}
					writeMethodDefinition(methodName, methodArguments,
							accessModifier, methodIndex);
				}
				perReflector.accept(writeReflectors.get(idx));
			}
			sourceWriter.outdent();
			sourceWriter.println("}");
			sourceWriter.println();
		}

		void writeRegisterForNames() {
			writeForClassReflectors("registerForNames", "Map<String, Class>",
					crg -> crg.writeRegisterForName(sourceWriter));
		}

		void writeRegisterReflectorSuppliers() {
			writeForClassReflectors("registerReflectorSuppliers",
					"Map<Class, Supplier<ClassReflector>>",
					crg -> crg.writeRegisterReflectorSupplier(sourceWriter));
		}

		void writeRegisterRegistrations() {
			writeForClassReflectors("registerRegistrations", "",
					crg -> crg.writeRegisterRegistrations(sourceWriter));
		}
	}

	abstract class ReflectorGenerator {
		protected abstract void prepare();

		protected abstract void write();
	}

	abstract class UnitGenerator extends ReflectorGenerator {
		protected SourceWriter sourceWriter;

		protected ClassSourceFileComposerFactory composerFactory;

		protected String packageName;

		protected String implementationName;

		protected PrintWriter printWriter;

		boolean pending;

		JClassType superClassOrInterfaceType;

		private JClassType reflectedType;

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
			createPrintWriter(true);
		}

		public boolean isPending() {
			return this.pending;
		}

		protected void closeClassBody() {
			sourceWriter.outdent();
			sourceWriter.println("}");
			context.commit(logger, printWriter);
		}

		protected void createPrintWriter(boolean inConstructor) {
			printWriter = context.tryCreate(logger, packageName,
					implementationName);
			pending = printWriter != null;
		}

		protected void createSourceWriter(String packageName,
				String className) {
		}

		protected String implementationFqn() {
			return packageName + "." + implementationName;
		}

		protected String reflectedTypeFqn() {
			return reflectedType.getQualifiedSourceName();
		}
	}
}
