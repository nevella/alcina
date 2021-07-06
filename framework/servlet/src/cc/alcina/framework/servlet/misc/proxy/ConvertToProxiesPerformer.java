package cc.alcina.framework.servlet.misc.proxy;

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
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.base.Preconditions;

import cc.alcina.extras.dev.console.code.CompilationUnits;
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

/**
 * 
 * Write wrapping versions of all classes
 * 
 * 
 * Analyse source, look for entrypoint accesses from accessing code (either
 * static calls or constructor calls - initially just static calls) and rewrite
 * 
 * 
 */
@RegistryLocation(registryPoint = TaskPerformer.class, targetClass = TaskConvertToProxies.class)
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
		importMatches.stream()
				.map(imp -> imp.replaceFirst("import (.+);", "$1"))
				.forEach(nextDelta::add);
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
						String source = ResourceUtilities.read(f);
						if (source.contains(task.outputPackage)) {
							return false;
						}
						Matcher m = p.matcher(source);
						while (m.find()) {
							found = true;
							importMatches.add(m.group());
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
			simpleClassNames.put(clazz.getSimpleName(), clazz);
		}

		private void addConstructor() {
			Set<Constructor> declaredConstructors = Arrays
					.stream(clazz.getDeclaredConstructors())
					.collect(Collectors.toSet());
			if (declaredConstructors.isEmpty()) {
				return;
			}
			ConstructorDeclaration constructorDeclaration = declaration
					.addConstructor(Keyword.PUBLIC);
			Constructor constructor = declaredConstructors.iterator().next();
			String typeIndicators = Arrays.stream(constructor.getParameters())
					.map(p -> {
						if (p.getType().isPrimitive()) {
							Object defaultValue = Array
									.get(Array.newInstance(p.getType(), 1), 0);
							return defaultValue.toString();
						} else {
							return Ax.format("(%s)null",
									p.getType().getCanonicalName());
						}
					}).collect(Collectors.joining(", "));
			String proxyCall = Ax.format("super(%s);", typeIndicators);
			constructorDeclaration.getBody().addStatement(proxyCall);
			Arrays.stream(constructor.getExceptionTypes())
					.forEach(constructorDeclaration::addThrownException);
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
				String returnString = method.getReturnType() == void.class ? ""
						: Ax.format("return (%s) ",
								method.getReturnType().getCanonicalName());
				String methodTypesClause = Arrays.stream(method.getParameters())
						.map(Parameter::getType).map(Class::getCanonicalName)
						.map(s -> s + ".class")
						.collect(Collectors.joining(", "));
				String methodArgsClause = Arrays.stream(method.getParameters())
						.map(Parameter::getName)
						.collect(Collectors.joining(", "));
				String proxyCall = Ax.format(
						"%sRegistry.impl(%s.class).handle(%s.class, %s, \"%s\", "
								+ "Arrays.asList(%s), Arrays.asList(%s)); ",
						returnString, task.classProxyImpl.getSimpleName(),
						clazz.getName(), proxyString, method.getName(),
						methodTypesClause, methodArgsClause);
				methodDeclaration.getBody().get().addStatement(proxyCall);
			}
		}

		private String proxyClassName() {
			return clazz.getSimpleName() + "_";
		}

		void generate() {
			String fullOutputPackage = task.outputPackage + "."
					+ clazz.getPackage().getName();
			String java = Ax.format(
					"package %s;\n\npublic class %s extends %s{}",
					fullOutputPackage, proxyClassName(),
					clazz.getCanonicalName());
			unit = StaticJavaParser.parse(java);
			String proxyFqn = Ax.format("%s.%s", fullOutputPackage,
					proxyClassName());
			unit.addImport(task.classProxyInterfacePackage + ".ClassProxy");
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
			if (matcher.matches()) {
				String firstPart = matcher.group(1);
				if (simpleClassNames.containsKey(firstPart)) {
					Class<? extends Node> parentClass = expr.getParentNode()
							.get().getClass();
					if (okRedirectContainers.contains(parentClass)) {
						// ensure we only redirect from the start of a method
						// call expr
						return;
					}
					String containerName = parentClass.getSimpleName();
					String containerName2 = expr.getParentNode().get()
							.getParentNode().get().getClass().getSimpleName();
					if (parents.add(containerName) || true) {
						Ax.out(this.unit.file.getName());
						Ax.out(expr);
						Ax.out(containerName);
						Ax.out(expr.getParentNode().get());
						Ax.out(containerName2);
						Ax.out(expr.getParentNode().get().getParentNode()
								.get());
						int debug = 3;
					}
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
					Ax.err("Invalid decl: %s", unit.file.getName());
					return;
				}
				declaration.setDeclaration(node);
				unit.declarations.add(declaration);
			}
			super.visit(node, arg);
		}
	}
}
