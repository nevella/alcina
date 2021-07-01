package cc.alcina.framework.entity.proxy;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.SelfPerformer;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;

public class TaskConvertToProxies
		implements SelfPerformer<TaskConvertToProxies> {
	protected transient Logger logger = LoggerFactory.getLogger(getClass());

	private String importMatcherRegex;

	private List<Class> classesToProxy;

	private String outputPackage;

	private String outputPackagePath;

	private List<String> pathsToScan;

	private Class<?> classProxyImpl;

	private String classProxyInterfacePackage;

	private boolean dryRun;

	private String inputPackagePrefix;

	public List<Class> getClassesToProxy() {
		return this.classesToProxy;
	}

	public Class<?> getClassProxyImpl() {
		return this.classProxyImpl;
	}

	public String getClassProxyInterfacePackage() {
		return this.classProxyInterfacePackage;
	}

	public String getImportMatcherRegex() {
		return this.importMatcherRegex;
	}

	public String getInputPackagePrefix() {
		return this.inputPackagePrefix;
	}

	public String getOutputPackage() {
		return this.outputPackage;
	}

	public String getOutputPackagePath() {
		return this.outputPackagePath;
	}

	public List<String> getPathsToScan() {
		return this.pathsToScan;
	}

	public boolean isDryRun() {
		return this.dryRun;
	}

	@Override
	public void performAction(TaskConvertToProxies task) throws Exception {
		ensureProxies();
		scanForClassesToInstrument();
		instrumentClasses();
	}

	public void setClassesToProxy(List<Class> classesToProxy) {
		this.classesToProxy = classesToProxy;
	}

	public void setClassProxyImpl(Class<?> classProxyImpl) {
		this.classProxyImpl = classProxyImpl;
	}

	public void
			setClassProxyInterfacePackage(String classProxyInterfacePackage) {
		this.classProxyInterfacePackage = classProxyInterfacePackage;
	}

	public void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
	}

	public void setImportMatcherRegex(String importMatcherRegex) {
		this.importMatcherRegex = importMatcherRegex;
	}

	public void setInputPackagePrefix(String inputPackagePrefix) {
		this.inputPackagePrefix = inputPackagePrefix;
	}

	public void setOutputPackage(String outputPackage) {
		this.outputPackage = outputPackage;
	}

	public void setOutputPackagePath(String outputPackagePath) {
		this.outputPackagePath = outputPackagePath;
	}

	public void setPathsToScan(List<String> pathsToScan) {
		this.pathsToScan = pathsToScan;
	}

	public void write(CompilationUnit unit,
			ClassOrInterfaceDeclaration declaration, String to) {
		to = dryRun ? Ax.format("/tmp/%s", getClass().getSimpleName()) : to;
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

	private void ensureProxies() {
		classesToProxy.stream().map(ProxyGenerator::new)
				.forEach(ProxyGenerator::generate);
	}

	private void instrumentClasses() {
		// TODO Auto-generated method stub
	}

	private void scanForClassesToInstrument() {
		// TODO Auto-generated method stub
	}

	private class ProxyGenerator {
		private Class clazz;

		private ClassOrInterfaceDeclaration declaration;

		private CompilationUnit unit;

		public ProxyGenerator(Class clazz) {
			this.clazz = clazz;
		}

		private void addMethods() {
			Set<Method> declaredMethods = Arrays
					.stream(clazz.getDeclaredMethods())
					.collect(Collectors.toSet());
			for (Method method : clazz.getMethods()) {
				if (!declaredMethods.contains(method)) {
					continue;
				}
				MethodDeclaration methodDeclaration = declaration
						.addMethod(method.getName(), Keyword.PUBLIC);
				boolean isStatic = Modifier.isStatic(method.getModifiers());
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
						returnString, classProxyImpl.getSimpleName(),
						clazz.getName(), proxyString, method.getName(),
						methodTypesClause, methodArgsClause);
				methodDeclaration.getBody().get().addStatement(proxyCall);
			}
		}

		private String proxyClassName() {
			return clazz.getSimpleName() + "_";
		}

		void generate() {
			String fullOutputPackage = outputPackage + clazz.getPackage()
					.getName().substring(inputPackagePrefix.length());
			String java = Ax.format("package %s;\n\npublic class %s{}",
					outputPackage, proxyClassName());
			unit = StaticJavaParser.parse(java);
			String proxyFqn = Ax.format("%s.%s", fullOutputPackage,
					proxyClassName());
			unit.addImport(classProxyInterfacePackage + ".ClassProxy");
			unit.addImport(classProxyImpl);
			unit.addImport(List.class);
			unit.addImport(clazz);
			unit.addImport(ArrayList.class);
			unit.addImport(Arrays.class);
			unit.addImport(Registry.class);
			declaration = unit.getClassByName(proxyClassName()).get();
			declaration.addImplementedType("ClassProxy");
			addMethods();
			write(unit, declaration, outputPackagePath);
		}
	}
}
