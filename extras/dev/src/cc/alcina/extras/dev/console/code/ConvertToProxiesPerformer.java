package cc.alcina.extras.dev.console.code;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.base.Preconditions;
import cc.alcina.extras.dev.console.code.CompilationUnits.ClassOrInterfaceDeclarationWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapperVisitor;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.FsObjectCache;
import cc.alcina.framework.entity.util.PersistentObjectCache.SingletonCache;
import cc.alcina.framework.common.client.logic.reflection.Registration;

/**
 * Write wrapping versions of all classes
 *
 * Analyse source, look for entrypoint accesses from accessing code (either
 * static calls or constructor calls - initially just static calls) and rewrite
 */
@Registration({ TaskPerformer.class, TaskConvertToProxies.class })
public class ConvertToProxiesPerformer
		implements TaskPerformer<TaskConvertToProxies> {
	protected Logger logger = LoggerFactory.getLogger(getClass());

	private List<File> toInstrument;

	private TreeSet<String> importMatches;

	private Set<Class> classesToProxy;

	private TaskConvertToProxies task;

	Map<Class, ProxyGenerator> generators = new LinkedHashMap<>();

	Map<String, Class> simpleClassNames = new LinkedHashMap<>();

	private CompilationUnits compUnits;

	Set<String> parents = new LinkedHashSet<>();

	List<Class<? extends com.github.javaparser.ast.Node>> okRedirectContainers = Arrays
			.asList(ExpressionStmt.class, VariableDeclarator.class,
					AssignExpr.class, ReturnStmt.class,
					ObjectCreationExpr.class, BinaryExpr.class, UnaryExpr.class,
					IfStmt.class);

	@Override
	public void performAction(TaskConvertToProxies task) throws Exception {
		this.task = task;
		scanForClassesToInstrument();
		ensureProxies();
		instrumentClasses();
	}

	public void write(CompilationUnit unit,
			ClassOrInterfaceDeclaration declaration, String to) {
		to = task.dryRun ? Ax.format("/tmp/%s", getClass().getSimpleName())
				: to;
		String packagePath = Ax.format("%s/%s", to, unit.getPackageDeclaration()
				.get().getNameAsString().replace(".", "/"));
		File packageFolder = new File(packagePath);
		packageFolder.mkdirs();
		File outFile = SEUtilities.getChildFile(packageFolder,
				declaration.getNameAsString() + ".java");
		try {
			ResourceUtilities.writeStringToFile(unit.toString(), outFile);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void ensureProxies() throws Exception {
		Set<String> nextDelta = new LinkedHashSet<>();
		Pattern p = Pattern.compile(task.importMatcherRegex);
		importMatches.stream().forEach(nextDelta::add);
		classesToProxy = new LinkedHashSet<>();
		int pass = 0;
		do {
			Set<String> delta = new LinkedHashSet<>(nextDelta);
			nextDelta.clear();
			logger.info("Traversing reachable - pass {} - adding {} classes",
					pass++, delta.size());
			for (String className : delta) {
				Class clazz = Class.forName(className);
				if (clazz.isEnum()) {
					continue;
				}
				if (clazz.isInterface()) {
					continue;
				}
				if (Modifier.isAbstract(clazz.getModifiers())) {
					continue;
				}
				if (classesToProxy.add(clazz)) {
					Set<Class> reachable = getReachableClasses(clazz);
					reachable.stream().filter(c -> {
						String importDecl = Ax.format("import %s;",
								c.getName());
						if (importDecl.matches(task.importMatcherRegex)) {
							Preconditions.checkArgument(
									c.getName().equals(c.getCanonicalName()));
							return true;
						} else {
							return false;
						}
					}).forEach(c -> nextDelta.add(c.getName()));
				}
			}
		} while (nextDelta.size() > 0);
		classesToProxy.stream().map(ProxyGenerator::new)
				.forEach(ProxyGenerator::generate);
	}

	private Set<Class> getReachableClasses(Class clazz) {
		try {
			Set<Class> reachable = new LinkedHashSet<>();
			reachable.add(clazz);
			reachable.add(clazz.getSuperclass());
			for (Field field : clazz.getDeclaredFields()) {
				reachable.add(field.getType());
			}
			for (Method method : clazz.getDeclaredMethods()) {
				if (method.getName().equals("setLoggingLevel")) {
					int debug = 3;
				}
				reachable.add(method.getReturnType());
				Arrays.stream(method.getParameterTypes())
						.forEach(reachable::add);
			}
			for (Class interfaze : clazz.getInterfaces()) {
				reachable.add(interfaze);
			}
			reachable.remove(null);
			reachable.remove(void.class);
			return reachable;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void instrumentClasses() throws Exception {
		StringMap classPaths = StringMap.fromStringList(
				task.pathsToScan.stream().collect(Collectors.joining("\n")));
		SingletonCache<CompilationUnits> cache = FsObjectCache
				.singletonCache(CompilationUnits.class, getClass())
				.asSingletonCache();
		compUnits = CompilationUnits.load(cache, classPaths,
				DeclarationVisitor::new, task.refreshCompilationUnits);
		compUnits.units.forEach(this::instrumentClass);
		compUnits.writeDirty(task.isDryRun());
	}

	private void scanForClassesToInstrument() {
		FileFilter filter = new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return true;
			}
		};
		Pattern p = Pattern.compile(task.importMatcherRegex);
		AtomicInteger javaFileCount = new AtomicInteger();
		importMatches = new TreeSet<>();
		this.toInstrument = task.pathsToScan.stream()
				.map(path -> SEUtilities.listFilesRecursive(path, filter))
				.flatMap(Collection::stream).filter(f -> {
					if (f.getName().endsWith(".java")) {
						javaFileCount.incrementAndGet();
						boolean found = false;
						if (f.getName().contains("ApdmFileImportUtil")) {
							int debug = 3;
						}
						String source = ResourceUtilities.read(f);
						if (source
								.startsWith("package " + task.outputPackage)) {
							return false;
						}
						Matcher m = p.matcher(source);
						while (m.find()) {
							found = true;
							importMatches.add(m.group(1));
						}
						return found;
					}
					return true;
				}).collect(Collectors.toList());
		logger.info("Files to instrument: {} of {} java files}",
				toInstrument.size(), javaFileCount.get());
	}

	void instrumentClass(CompilationUnitWrapper unit) {
		for (ClassOrInterfaceDeclarationWrapper decl : unit.declarations) {
			RedirectVisitor visitor = new RedirectVisitor(unit);
			decl.getDeclaration().accept(visitor, null);
		}
	}

	private class ProxyGenerator {
		private Class clazz;

		private ClassOrInterfaceDeclaration declaration;

		private CompilationUnit unit;

		private boolean isFinal;

		private boolean isStatic;

		public ProxyGenerator(Class clazz) {
			this.clazz = clazz;
			generators.put(clazz, this);
			Preconditions.checkState(
					!simpleClassNames.containsKey(clazz.getSimpleName()));
			simpleClassNames.put(clazz.getSimpleName(), clazz);
		}

		public Class getProxyClass() {
			try {
				return Class.forName(proxyFqn());
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		private void addConstructor() {
			Set<Constructor> constructors = Arrays
					.stream(clazz.getDeclaredConstructors())
					.collect(Collectors.toSet());
			if (constructors.isEmpty()) {
				constructors = Arrays.stream(clazz.getConstructors())
						.collect(Collectors.toSet());
			}
			if (constructors.size() == 0) {
				logger.info("No public constructors: {}",
						clazz.getSimpleName());
			}
			Constructor first = Ax.first(constructors);
			for (Constructor constructor : constructors) {
				/*
				 * Add exactly one (unused) private constructors
				 */
				{
					if (constructor == first) {
						ConstructorDeclaration constructorDeclaration = declaration
								.addConstructor(Keyword.PRIVATE);
						String typeIndicators = Arrays
								.stream(constructor.getParameters()).map(p -> {
									if (p.getType().isPrimitive()) {
										Object defaultValue = Array.get(Array
												.newInstance(p.getType(), 1),
												0);
										return defaultValue.toString();
									} else {
										return Ax.format("(%s)null",
												p.getType().getCanonicalName());
									}
								}).collect(Collectors.joining(", "));
						String proxyCall = Ax.format("super(%s);",
								typeIndicators);
						constructorDeclaration.getBody()
								.addStatement(proxyCall);
						Arrays.stream(constructor.getExceptionTypes()).forEach(
								constructorDeclaration::addThrownException);
					}
				}
				/*
				 * Add proxying __newInstance call
				 */
				if (Modifier.isPublic(constructor.getModifiers())) {
					MethodDeclaration methodDeclaration = declaration
							.addMethod("__newInstance", Keyword.PUBLIC);
					methodDeclaration.addModifier(Keyword.STATIC);
					for (Parameter parameter : constructor.getParameters()) {
						methodDeclaration.addParameter(parameter.getType(),
								parameter.getName());
					}
					methodDeclaration.setType(clazz);
					String methodTypesClause = Arrays
							.stream(constructor.getParameters())
							.map(Parameter::getType)
							.map(Class::getCanonicalName).map(s -> s + ".class")
							.collect(Collectors.joining(", "));
					String methodArgsClause = Arrays
							.stream(constructor.getParameters())
							.map(Parameter::getName)
							.collect(Collectors.joining(", "));
					String proxyCall = Ax.format(
							"ProxyResult result = %s.handle(%s.class, null, null, "
									+ "Arrays.asList(%s), Arrays.asList(%s)); ",
							task.classProxyImpl.getSimpleName(),
							clazz.getName(), methodTypesClause,
							methodArgsClause);
					methodDeclaration.getBody().get().addStatement(proxyCall);
					Arrays.stream(constructor.getExceptionTypes())
							.forEach(et -> {
								String checkThrow = Ax.format(
										"if(result.throwable instanceof %s){\n\t"
												+ "throw (%s)result.throwable;\n}",
										et.getCanonicalName(),
										et.getCanonicalName());
								methodDeclaration.getBody().get()
										.addStatement(checkThrow);
							});
					String returnStatement = Ax.format(
							"return (%s) result.returnValue;",
							clazz.getCanonicalName());
					methodDeclaration.getBody().get()
							.addStatement(returnStatement);
					Arrays.stream(constructor.getExceptionTypes())
							.forEach(c -> methodDeclaration
									.addThrownException((Class) c));
				}
			}
		}

		private void addMethods() {
			Set<Method> declaredMethods = Arrays
					.stream(clazz.getDeclaredMethods())
					.collect(Collectors.toSet());
			for (Method method : clazz.getMethods()) {
				if (method.getDeclaringClass() == Object.class) {
					continue;
				}
				if (method.getName().equals("clone")
						&& method.getParameterCount() == 0
						&& method.getReturnType() == Object.class) {
					continue;
				}
				String wrappingMethodName = method.getName();
				isStatic = Modifier.isStatic(method.getModifiers());
				isFinal = Modifier.isFinal(method.getModifiers());
				if (isFinal) {
					wrappingMethodName += "_final";
				}
				MethodDeclaration methodDeclaration = declaration
						.addMethod(wrappingMethodName, Keyword.PUBLIC);
				if (isStatic) {
					methodDeclaration.addModifier(Keyword.STATIC);
				}
				for (Parameter parameter : method.getParameters()) {
					methodDeclaration.addParameter(parameter.getType(),
							parameter.getName());
				}
				methodDeclaration.setType(method.getReturnType());
				String proxyString = isStatic ? "null" : "this";
				String methodTypesClause = Arrays.stream(method.getParameters())
						.map(Parameter::getType).map(Class::getCanonicalName)
						.map(s -> s + ".class")
						.collect(Collectors.joining(", "));
				String methodArgsClause = Arrays.stream(method.getParameters())
						.map(Parameter::getName)
						.collect(Collectors.joining(", "));
				String proxyCall = Ax.format(
						"ProxyResult result = %s.handle(%s.class, %s, \"%s\", "
								+ "Arrays.asList(%s), Arrays.asList(%s)); ",
						task.classProxyImpl.getSimpleName(), clazz.getName(),
						proxyString, method.getName(), methodTypesClause,
						methodArgsClause);
				methodDeclaration.getBody().get().addStatement(proxyCall);
				Arrays.stream(method.getExceptionTypes()).forEach(et -> {
					String checkThrow = Ax.format(
							"if(result.throwable instanceof %s){\n\t"
									+ "throw (%s)result.throwable;\n}",
							et.getCanonicalName(), et.getCanonicalName());
					methodDeclaration.getBody().get().addStatement(checkThrow);
				});
				if (method.getReturnType() != void.class) {
					String returnStatement = Ax.format(
							"return (%s) result.returnValue;",
							method.getReturnType().getCanonicalName());
					methodDeclaration.getBody().get()
							.addStatement(returnStatement);
				}
				Arrays.stream(method.getExceptionTypes()).forEach(
						c -> methodDeclaration.addThrownException((Class) c));
			}
		}

		private String fullOutputPackage() {
			return task.outputPackage + "." + clazz.getPackage().getName();
		}

		private String proxyClassName() {
			return clazz.getSimpleName() + "_";
		}

		private String proxyFqn() {
			return Ax.format("%s.%s", fullOutputPackage(), proxyClassName());
		}

		void generate() {
			String fullOutputPackage = fullOutputPackage();
			String java = Ax.format(
					"package %s;\n\npublic class %s extends %s{}",
					fullOutputPackage, proxyClassName(),
					clazz.getCanonicalName());
			unit = StaticJavaParser.parse(java);
			String proxyFqn = Ax.format("%s.%s", fullOutputPackage,
					proxyClassName());
			unit.addImport(task.classProxyInterfacePackage + ".ClassProxy");
			unit.addImport(task.classProxyInterfacePackage
					+ ".ClassProxy.ProxyResult");
			unit.addImport(task.classProxyImpl);
			unit.addImport(List.class);
			unit.addImport(clazz);
			unit.addImport(ArrayList.class);
			unit.addImport(Arrays.class);
			unit.addImport(Registry.class);
			declaration = unit.getClassByName(proxyClassName()).get();
			declaration.addImplementedType("ClassProxy");
			addMethods();
			addConstructor();
			write(unit, declaration, task.outputPackagePath);
		}
	}

	private class RedirectVisitor extends VoidVisitorAdapter {
		private final CompilationUnitWrapper unit;

		Pattern simpleStaticPattern = Pattern.compile("^(.+?)\\..+");

		private RedirectVisitor(CompilationUnitWrapper unit) {
			this.unit = unit;
		}

		@Override
		public void visit(com.github.javaparser.ast.expr.MethodCallExpr expr,
				Object arg) {
			Matcher matcher = simpleStaticPattern.matcher(expr.toString());
			if (expr.getChildNodes().get(0) instanceof NameExpr
					&& matcher.matches()) {
				String firstPart = matcher.group(1);
				if (simpleClassNames.containsKey(firstPart)) {
					// testOkWrapping(expr);
					Class clazz = simpleClassNames.get(firstPart);
					Class proxyClass = generators.get(clazz).getProxyClass();
					unit.ensureImport(proxyClass);
					NameExpr nameExpr = (NameExpr) expr.getChildNodes().get(0);
					nameExpr.setName(proxyClass.getSimpleName());
					unit.dirty = true;
				}
			}
			super.visit(expr, arg);
		}

		@Override
		public void visit(
				com.github.javaparser.ast.expr.ObjectCreationExpr expr,
				Object arg) {
			Matcher matcher = Pattern.compile("new ([a-zA-Z.0-9]+)(\\(.*?\\))")
					.matcher(expr.toString());
			if (matcher.matches()) {
				String firstPart = matcher.group(1);
				if (simpleClassNames.containsKey(firstPart)) {
					// testOkWrapping(expr);
					Class clazz = simpleClassNames.get(firstPart);
					Class proxyClass = generators.get(clazz).getProxyClass();
					unit.ensureImport(proxyClass);
					String expression = Ax.format("%s.__newInstance%s",
							proxyClass.getSimpleName(), matcher.group(2));
					Expression replExpr = StaticJavaParser
							.parseExpression(expression);
					Optional<Node> parent = expr.getParentNode();
					parent.get().replace(expr, replExpr);
					unit.dirty = true;
				}
			}
			super.visit(expr, arg);
		}
	}

	static class DeclarationVisitor extends CompilationUnitWrapperVisitor {
		public DeclarationVisitor(CompilationUnits units,
				CompilationUnitWrapper compUnit) {
			super(units, compUnit);
		}

		@Override
		public void visit(ClassOrInterfaceDeclaration node, Void arg) {
			try {
				visit0(node, arg);
			} catch (VerifyError ve) {
				Ax.out("Verify error: %s", node.getName());
			}
		}

		private void visit0(ClassOrInterfaceDeclaration node, Void arg) {
			if (!node.isInterface()) {
				CompilationUnits.ClassOrInterfaceDeclarationWrapper declaration = new CompilationUnits.ClassOrInterfaceDeclarationWrapper(
						unit, node);
				if (declaration.invalid) {
					Ax.err("Invalid decl: %s", unit.getFile().getName());
					return;
				}
				declaration.setDeclaration(node);
				unit.declarations.add(declaration);
			}
			super.visit(node, arg);
		}
	}
}
