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

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.totsp.gwittir.client.beans.annotations.Omit;
import com.totsp.gwittir.rebind.beans.IntrospectorFilter;
import com.totsp.gwittir.rebind.beans.IntrospectorFilterHelper;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.NonClientRegistryPointType;
import cc.alcina.framework.common.client.logic.reflection.ReflectionModule;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registrations;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.AnnotationResolver;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.ClientReflections;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.ToStringComparator;

public class ClientReflectionGenerator extends Generator {
	static Comparator<RegistryLocation> REGISTRY_LOCATION_COMPARATOR = new Comparator<RegistryLocation>() {
		@Override
		public int compare(RegistryLocation o1, RegistryLocation o2) {
			int i = o1.registryPoint().getName()
					.compareTo(o2.registryPoint().getName());
			if (i != 0) {
				return i;
			}
			i = o1.targetClass().getName()
					.compareTo(o2.targetClass().getName());
			if (i != 0) {
				return i;
			}
			return CommonUtils.compareInts(o1.priority(), o2.priority());
		}
	};

	static final Predicate<RegistryLocation> CLIENT_VISIBLE_ANNOTATION_FILTER = new Predicate<RegistryLocation>() {
		@Override
		public boolean test(RegistryLocation o) {
			return o.registryPoint()
					.getAnnotation(NonClientRegistryPointType.class) == null;
		}
	};

	static final String REF_IMPL = "__refImpl";

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

	static String implementationName(JClassType type) {
		List<JClassType> enclosedTypes = new ArrayList<>();
		while (type != null) {
			enclosedTypes.add(type);
			type = type.getEnclosingType();
		}
		FormatBuilder builder = new FormatBuilder().separator("_");
		for (int idx = enclosedTypes.size() - 1; idx >= 0; idx--) {
			builder.append(enclosedTypes.get(idx).getSimpleSourceName());
		}
		builder.append(REF_IMPL);
		return builder.toString();
	}

	IntrospectorFilter filter;

	long start;

	String moduleName;

	TreeLogger logger;

	GeneratorContext context;

	ModuleReflectionGenerator moduleGenerator;

	JClassType generatingType;

	int maxReflectors = Integer.MAX_VALUE;

	ReflectionModule module;

	String typeName;

	String implementationName;

	String packageName;

	Map<Class, String> annotationImplFqn = new LinkedHashMap<>();

	Set<Class<? extends Annotation>> visibleAnnotationTypes = new LinkedHashSet<>();

	JClassType classReflectorType;

	ClientReflectionFilter reflectionFilter;

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
				System.out.format(
						"Client reflection generation  [%s] -  %s types - %s ms\n",
						filter.getModuleName(),
						moduleGenerator.classReflectors.size(),
						System.currentTimeMillis() - start);
				filter.generationComplete();
			}
			return moduleGenerator.fqn();
		} catch (Exception e) {
			e.printStackTrace();
			throw new WrappedRuntimeException(e);
		}
	}

	private void setupFilter() throws Exception {
		String filterClassName = context.getPropertyOracle()
				.getConfigurationProperty(
						ClientReflectionFilter.class.getName())
				.getValues().get(0);
		reflectionFilter = (ClientReflectionFilter) Class
				.forName(filterClassName).getDeclaredConstructor()
				.newInstance();
		reflectionFilter.init(context, moduleName);
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

	<A extends Annotation> boolean has(JClassType t, Class<A> annotationClass) {
		return t.getAnnotation(annotationClass) != null;
	}

	void setupEnvironment() throws NotFoundException {
		filter = IntrospectorFilterHelper.getFilter(context);
		start = System.currentTimeMillis();
		String superClassName = null;
		generatingType = context.getTypeOracle().getType(typeName);
		classReflectorType = context.getTypeOracle()
				.getType(ClassReflector.class.getCanonicalName());
		module = generatingType.getAnnotation(ReflectionModule.class);
		filter.setModuleName(module.value());
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
					"resolver.annotations.put(%s.class,annotation);",
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
			super(annotationType, annotationType);
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
			annotationImplFqn.put(annotationClass, fqn());
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

		List<RegistryLocation> registrations = new ArrayList<>();

		public ClassReflectorGenerator(JClassType type) {
			super(type, classReflectorType);
			this.type = type;
		}

		@Override
		public int compareTo(ClassReflectorGenerator o) {
			return type.getQualifiedSourceName()
					.compareTo(o.type.getQualifiedSourceName());
		}

		@Override
		protected void prepare() {
			isAbstract = type.isAbstract();
			hasCallableNoArgsConstructor = !isAbstract
					&& !type.getQualifiedSourceName().equals("java.lang.Class")
					&& Arrays.stream(type.getConstructors())
							.filter(c -> c.getParameters().length == 0)
							.findFirst().isPresent();
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
			composerFactory.setSuperclass(
					superClassOrInterfaceType.getQualifiedSourceName());
			composerFactory.addImport(LinkedHashMap.class.getName());
			composerFactory.addImport(Map.class.getName());
			composerFactory.addImport(Supplier.class.getName());
			composerFactory.addImport(Registry.class.getName());
			composerFactory.addImport(Predicate.class.getName());
			composerFactory.addImport(List.class.getName());
			composerFactory.addImport(ArrayList.class.getName());
			composerFactory.addImport(AnnotationResolver.class.getName());
			composerFactory.addImport(Annotation.class.getCanonicalName());
			composerFactory.addImport(
					AnnotationResolver.LookupResolver.class.getCanonicalName());
			composerFactory.addImport(ClientReflections.class.getName());
			composerFactory.addImport(Property.class.getName());
			composerFactory.addImport(
					cc.alcina.framework.common.client.reflection.Method.class
							.getCanonicalName());
			composerFactory.addImport(Registration.class.getName());
			sourceWriter = createWriter(composerFactory, printWriter);
			sourceWriter.println("protected void init0(){");
			sourceWriter.indent();
			sourceWriter.println("Class clazz = %s.class;", fqn());
			sourceWriter
					.println("List<Property> properties = new ArrayList<>();");
			propertyGenerators.values().forEach(PropertyGenerator::write);
			sourceWriter.println("List<Class> interfaces = new ArrayList<>();");
			sourceWriter.println(
					"AnnotationResolver.LookupResolver resolver = new AnnotationResolver.LookupResolver();");
			annotationExpressionWriters.forEach(
					expressionWriter -> expressionWriter.write(sourceWriter));
			sourceWriter.println(
					"Map<String, Property> byName = new LinkedHashMap<>();");
			sourceWriter.println(
					"properties" + ".forEach(p->byName.put(p.getName(),p));");
			if (hasCallableNoArgsConstructor) {
				sourceWriter.println("Supplier supplier = %s::new;", fqn());
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
			sourceWriter.println("init(clazz, properties, byName, resolver,"
					+ " supplier, assignableTo, interfaces, reflective, isAbstract);");
			sourceWriter.outdent();
			sourceWriter.println("}");
			closeClassBody();
		}

		void prepareProperties() {
			boolean hasReflectableProperties = has(type, Bean.class);
			if (hasReflectableProperties) {
				Arrays.stream(type.getMethods()).map(this::toPropertyMethod)
						.filter(Objects::nonNull).forEach(m -> {
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
			Multimap<JClassType, List<Annotation>> superclassAnnotations = new Multimap<>();
			JClassType cursor = type;
			while (cursor.getSuperclass() != null) {
				superclassAnnotations.addCollection(cursor,
						Arrays.asList(cursor.getAnnotations()));
				cursor = cursor.getSuperclass();
			}
			Registry.filterForRegistryPointUniqueness(superclassAnnotations)
					.stream().filter(CLIENT_VISIBLE_ANNOTATION_FILTER)
					.forEach(registrations::add);
		}

		PropertyMethod toPropertyMethod(JMethod method) {
			if (method.getAnnotation(Omit.class) != null) {
				return null;
			}
			if (method.getName().equals("getClass")) {
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
			sourceWriter.println(
					"map.put(\"%s\",new Supplier(){public Object get(){return %s.class;}});",
					type.getQualifiedBinaryName(), fqn());
		}

		void writeRegisterReflectorSupplier(SourceWriter sourceWriter) {
			sourceWriter.println("map.put(\"%s\",%s::new);", fqn(), fqn());
		}

		void writeRegisterRegistrations(SourceWriter sourceWriter) {
			registrations.stream().sorted(REGISTRY_LOCATION_COMPARATOR)
					.forEach(l -> {
						sourceWriter.print("Registry.get().register(%s.class,",
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
						"AnnotationResolver.LookupResolver resolver = new AnnotationResolver.LookupResolver();");
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
						"Property property = new Property(name, getter, setter, propertyType, definingType, resolver);");
				sourceWriter.println("properties.add(property);");
				sourceWriter.outdent();
				sourceWriter.println("}");
			}

			void printMethodFunction(PropertyMethod method) {
				if (method == null) {
					sourceWriter.print("null");
					return;
				}
				sourceWriter.print("new Method(");
				String toString = Ax.format("[Method: %s]",
						method.method.getName());
				sourceWriter.print(stringLiteral(toString));
				sourceWriter.print(", ");
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

		List<ClassReflectorGenerator> classReflectors = new ArrayList<>();

		boolean alreadyWritten = false;

		protected ModuleReflectionGenerator(String implementationName,
				JClassType superClassOrInterfaceType) {
			super(superClassOrInterfaceType.getPackage().getName(),
					implementationName, superClassOrInterfaceType);
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
			List<JClassType> types = determineReachableTypes();
			types.stream().limit(maxReflectors)
					.map(ClassReflectorGenerator::new)
					.forEach(classReflectors::add);
			classReflectors.forEach(ClassReflectorGenerator::prepare);
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
			composerFactory.addImport(Supplier.class.getName());
			composerFactory.setSuperclass(
					superClassOrInterfaceType.getQualifiedSourceName());
			sourceWriter = createWriter(composerFactory, printWriter);
			writeRegisterReflectorSuppliers();
			writeRegisterForNames();
			writeRegisterRegistrations();
			closeClassBody();
		}

		List<JClassType> determineReachableTypes() {
			return Arrays.stream(context.getTypeOracle().getTypes())
					.filter(t -> (has(t, ClientInstantiable.class)
							|| has(t, Bean.class)
							|| filter.isReflectableJavaCoreClass(t)
							|| filter.isReflectableJavaCollectionClass(t)))
					.filter(reflectionFilter::permit)
					.map(JClassType::getFlattenedSupertypeHierarchy)
					.flatMap(Collection::stream)
					.map(ClientReflectionGenerator::erase).distinct()
					.filter(t -> t.isPublic()).collect(Collectors.toList());
		}

		void prepareAnnotationImplementationGenerators() {
			Arrays.stream(context.getTypeOracle().getTypes())
					.filter(t -> t.isAnnotationPresent(ClientVisible.class))
					.map(JClassType::isAnnotation)
					.map(AnnotationImplementationGenerator::new).sorted()
					.forEach(annotationImplementations::add);
			annotationImplementations.stream()
					.filter(AnnotationImplementationGenerator::isPending)
					.forEach(AnnotationImplementationGenerator::prepare);
		}

		void writeClassReflectors() {
			classReflectors.forEach(ClassReflectorGenerator::write);
		}

		void writeForClassReflectors(String methodName, String mapSignature,
				Consumer<ClassReflectorGenerator> perReflector) {
			String mapName = Ax.notBlank(mapSignature) ? "map" : "";
			String methodArguments = Ax.notBlank(mapSignature)
					? Ax.format("%s map", mapSignature)
					: "";
			if (classReflectors.isEmpty()) {
				writeMethodDefinition(methodName, methodArguments, "public",
						"");
			}
			for (int idx = 0; idx < classReflectors.size(); idx++) {
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
				perReflector.accept(classReflectors.get(idx));
			}
			sourceWriter.outdent();
			sourceWriter.println("}");
			sourceWriter.println();
		}

		void writeRegisterForNames() {
			writeForClassReflectors("registerForNames",
					"Map<String, Supplier<Class>>",
					crg -> crg.writeRegisterForName(sourceWriter));
		}

		void writeRegisterReflectorSuppliers() {
			writeForClassReflectors("registerReflectorSuppliers",
					"Map<String, Supplier<ClassReflector>>",
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

		protected UnitGenerator(JClassType reflectionInfoForType,
				JClassType superClassOrInterfaceType) {
			this(reflectionInfoForType.getPackage().getName(),
					implementationName(reflectionInfoForType),
					superClassOrInterfaceType);
		}

		protected UnitGenerator(String packageName, String implementationName,
				JClassType superClassOrInterfaceType) {
			super();
			if (packageName.startsWith("java")) {
				packageName = getClass().getPackageName() + "."
						+ packageName.replace(".", "_");
			}
			this.packageName = packageName;
			this.implementationName = implementationName;
			this.superClassOrInterfaceType = superClassOrInterfaceType;
			composerFactory = new ClassSourceFileComposerFactory(packageName,
					implementationName);
			printWriter = context.tryCreate(logger, packageName,
					implementationName);
			pending = printWriter != null;
		}

		public boolean isPending() {
			return this.pending;
		}

		protected void closeClassBody() {
			sourceWriter.outdent();
			sourceWriter.println("}");
			context.commit(logger, printWriter);
		}

		protected void createSourceWriter(String packageName,
				String className) {
		}

		protected String fqn() {
			return packageName + "." + implementationName;
		}
	}
}
