package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.ThrowingRunnable;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.AlcinaParallel;
import cc.alcina.framework.entity.util.DataFolderProvider;
import cc.alcina.framework.entity.util.FsObjectCache;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.bytecode.ClassFile;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

/*
 * TODO - rewriting
 * 
 * 
 */
class ClassTransformer {
	static {
		SourceFinder.sourceFinders.add(new SourceFinderFs());
	}

	private Mvcc mvcc;

	private Map<Class, ClassTransform> classTransforms;

	private FsObjectCache<ClassTransform> cache;

	private CombinedTypeSolver typeSolver;

	ClassPool classPool;

	private Field referenceTypeClazzAccessor;

	private JavaParserFacade solver;

	private boolean addObjectResolutionChecks;

	private List<Runnable> compilationRunnables = new ArrayList<>();

	public ClassTransformer(Mvcc mvcc) {
		this.mvcc = mvcc;
		classPool = new ClassPool();
		classPool.insertClassPath(new ClassClassPath(this.getClass()));
		typeSolver = new CombinedTypeSolver();
		typeSolver.add(new ClassLoaderTypeSolver(getClass().getClassLoader()));
		this.solver = JavaParserFacade.get(typeSolver);
		try {
			referenceTypeClazzAccessor = ReflectionClassDeclaration.class
					.getDeclaredField("clazz");
			referenceTypeClazzAccessor.setAccessible(true);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public <T extends HasIdAndLocalId> T create(Class<T> clazz) {
		try {
			return (T) classTransforms.get(clazz).transformedClass
					.newInstance();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void generateTransformedClasses() {
		File cacheFolder = DataFolderProvider.get()
				.getChildFile(getClass().getName());
		cacheFolder.mkdirs();
		cache = new FsObjectCache(cacheFolder, ClassTransform.class,
				path -> null);
		classTransforms = (Map) mvcc.domainDescriptor.perClass.keySet().stream()
				.collect(Collectors.toMap(k -> k, ClassTransform::new));
		for (ClassTransform ct : classTransforms.values()) {
			ct.setTransformer(this);
			ct.init();
			ct.checkFieldAndMethodAccess();
			ct.generateMvccClass();
			ct.persist();
		}
		if (classTransforms.values().stream().anyMatch(ct -> ct.invalid)) {
			throw new IllegalStateException();
		}
		AlcinaParallel.builder().withRunnables(compilationRunnables)
				.withThreadCount(8).withCancelOnException(true).withSerial(true)
				.withThreadName("ClassTransformer-compilation").run()
				.throwOnException();
	}

	public boolean isAddObjectResolutionChecks() {
		return this.addObjectResolutionChecks;
	}

	public void
			setAddObjectResolutionChecks(boolean addObjectResolutionChecks) {
		this.addObjectResolutionChecks = addObjectResolutionChecks;
	}

	<H extends HasIdAndLocalId> Class<? extends H>
			getTransformedClass(Class<H> originalClass) {
		return classTransforms.get(originalClass).transformedClass;
	}

	private static class SourceFinderFs implements SourceFinder {
		@Override
		public String findSource(Class clazz) {
			try {
				CodeSource codeSource = clazz.getProtectionDomain()
						.getCodeSource();
				URL classFileLocation = codeSource.getLocation();
				URL sourceFileLocation = new URL(
						Ax.format("%s%s.java", classFileLocation.toString(),
								clazz.getName().replace(".", "/")));
				if (new File(toPath(sourceFileLocation)).exists()) {
					return ResourceUtilities
							.readUrlAsString(sourceFileLocation.toString());
				}
				sourceFileLocation = new URL(
						sourceFileLocation.toString().replace("/bin/", "/src/")
								.replace("/build/classes/", "/src/"));
				if (new File(toPath(sourceFileLocation)).exists()) {
					return ResourceUtilities
							.readUrlAsString(sourceFileLocation.toString());
				}
				return null;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		private String toPath(URL sourceFileLocation) {
			return sourceFileLocation.toString().replace("file://", "");
		}
	}

	static class ClassTransform<H extends HasIdAndLocalId> {
		private static final transient int VERSION = 7;

		private int version;

		private Class<H> originalClass;

		private transient Class<? extends H> transformedClass;

		private transient ClassTransformer transformer;

		private transient ClassTransform<H> lastRun;

		private transient CompilationUnit compilationUnit;

		private transient boolean invalid;

		private transient ClassOrInterfaceDeclaration classDeclaration;

		private String source;

		private Set<String> methodsWithProblematicFieldAccess = new LinkedHashSet<>();

		private Set<String> methodsWithProblematicAccess = new LinkedHashSet<>();

		private Set<String> fieldsWithProblematicAccess = new LinkedHashSet<>();

		public ClassTransform() {
		}

		public ClassTransform(Class<H> clazz) {
			this.originalClass = clazz;
		}

		public void setTransformer(ClassTransformer transformer) {
			this.transformer = transformer;
		}

		private void checkFieldModifiers() {
			SEUtilities.allFields(originalClass).stream()
					.filter(f -> (f.getModifiers() & Modifier.PRIVATE) == 0)
					.filter(f -> (f.getModifiers() & Modifier.STATIC) == 0)
					.filter(f -> (f.getModifiers() & Modifier.TRANSIENT) == 0)
					.filter(f -> !f.getName().matches(
							"id|localId|creationUser|creationDate|versionNumber|lastModificationUser|lastModificationDate|"
									+ "propertyValue"))
					.forEach(f -> fieldsWithProblematicAccess.add(f.getName()));
		}

		private void checkMethodModifiers() {
			// Old, old...methods we love. Fields terrify us
			//
			// SEUtilities.allClassMethods(originalClass).stream()
			// .filter(f -> (f.getModifiers() & Modifier.PUBLIC) == 0)
			// .filter(f -> (f.getModifiers() & Modifier.STATIC) == 0)
			// .filter(f -> (f.getModifiers() & Modifier.TRANSIENT) == 0)
			// .filter(f -> !f.getName().matches(
			// "propertyChangeSupport|comparisonString|_compareTo"))
			// .forEach(
			// f -> methodsWithProblematicAccess.add(f.getName()));
		}

		private String findSource() throws Exception {
			for (SourceFinder finder : SourceFinder.sourceFinders) {
				String source = finder.findSource(originalClass);
				if (source != null) {
					return source;
				}
			}
			return null;
		}

		private boolean isSameSourceAsLastRun() {
			return lastRun != null && Objects.equals(lastRun.source, source)
					&& source != null;
		}

		void checkFieldAndMethodAccess() {
			if (isSameSourceAsLastRun()) {
				methodsWithProblematicFieldAccess = lastRun.methodsWithProblematicFieldAccess;
				fieldsWithProblematicAccess = lastRun.fieldsWithProblematicAccess;
			} else {
				checkFieldModifiers();
				checkMethodModifiers();
				if (source != null) {
					compilationUnit = StaticJavaParser.parse(source);
					classDeclaration = compilationUnit
							.getClassByName(originalClass.getSimpleName())
							.get();
					classDeclaration.accept(new CheckFieldAccessVisitor(),
							null);
				}
			}
			if (methodsWithProblematicFieldAccess.size() > 0) {
				Ax.err("\n======================\nClass: %s\nMethodsWithProblematicFieldAccess:\n======================",
						originalClass.getName());
				methodsWithProblematicFieldAccess.forEach(Ax::err);
				Ax.out("\n");
				// should never happen
				throw new UnsupportedOperationException();
			}
			if (methodsWithProblematicAccess.size() > 0) {
				Ax.err("\n======================\nClass: %s\nMethodsWithProblematicAccess:\n======================",
						originalClass.getName());
				methodsWithProblematicAccess.forEach(Ax::err);
				Ax.out("\n");
				// should never happen
				throw new UnsupportedOperationException();
			}
			if (fieldsWithProblematicAccess.size() > 0) {
				Ax.err("\n======================\nClass: %s\nFieldsWithProblematicAccess:\n======================",
						originalClass.getName());
				fieldsWithProblematicAccess.forEach(Ax::err);
				Ax.out("\n");
				invalid = true;
			}
		}

		void generateMvccClass() {
			new ClassWriter().generateMvccClassTask();
		}

		void init() {
			try {
				this.source = findSource();
				this.lastRun = transformer.cache.get(originalClass.getName());
				if (this.lastRun != null && this.lastRun.version != VERSION) {
					this.lastRun = null;
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		void persist() {
			try {
				this.version = VERSION;
				transformer.cache.persist(this, originalClass.getName());
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		private class CheckFieldAccessExpressionVisitor
				extends VoidVisitorAdapter<Void> {
			private MethodDeclaration methodDeclaration;

			private boolean getterOrSetter;

			public CheckFieldAccessExpressionVisitor(
					MethodDeclaration methodDeclaration) {
				this.methodDeclaration = methodDeclaration;
				this.getterOrSetter = methodDeclaration.getName().asString()
						.matches("(?:set|get)[A-Z].*");
			}

			@Override
			public void visit(FieldAccessExpr expr, Void arg) {
				if (isPartOfAnnotationExpression(expr)) {
					// constant expression
					return;
				}
				/*
				 * 
				 * Transactional access::
				 * 
				 * Field references in transactionally ok objects may be stale
				 * (i.e. for [A::B], A denoting object, B denoting tx id,
				 * 
				 * o1::tx5.group = o2::tx3
				 * 
				 * So all access to that field must either both (a) in a public
				 * method (which is itself wrapped to ensure o2::tx5 is returned
				 * in the example above) and be a this. access, or be a
				 * non-domain type (since we're assuming 'this' is
				 * transactionally correct, by induction)
				 * 
				 * So...problematic iff::
				 * 
				 * field access is outside getter/setter or not 'this' in
				 * getter/setter
				 * 
				 * field type is hili
				 * 
				 * field scope is hili
				 * 
				 * 
				 * 
				 */
				try {
					ResolvedType fieldType = transformer.solver.getType(expr);
					ResolvedType scopeType = transformer.solver
							.getType(expr.getScope());
					boolean scopeTypeIsDomain = false;
					boolean fieldTypeIsDomain = false;
					if (fieldType.isReferenceType()) {
						ResolvedReferenceTypeDeclaration fieldTypeDeclaration = fieldType
								.asReferenceType().getTypeDeclaration();
						if (fieldTypeDeclaration instanceof ReflectionClassDeclaration) {
							Class clazz = (Class) transformer.referenceTypeClazzAccessor
									.get(fieldTypeDeclaration);
							fieldTypeIsDomain = HasIdAndLocalId.class
									.isAssignableFrom(clazz);
						}
					}
					if (scopeType.isReferenceType()) {
						ResolvedReferenceTypeDeclaration scopeTypeDeclaration = scopeType
								.asReferenceType().getTypeDeclaration();
						if (scopeTypeDeclaration instanceof ReflectionClassDeclaration) {
							Class clazz = (Class) transformer.referenceTypeClazzAccessor
									.get(scopeTypeDeclaration);
							scopeTypeIsDomain = HasIdAndLocalId.class
									.isAssignableFrom(clazz);
						} else if (scopeTypeDeclaration instanceof JavaParserClassDeclaration) {
							scopeTypeIsDomain = true;
						}
					}
					if (scopeTypeIsDomain && fieldTypeIsDomain) {
						if (expr.getScope() instanceof ThisExpr
								&& this.getterOrSetter) {
							// OK
							return;
						} else {
							// actually, it's still OK -
							//
							// the *only* access issue is non-private fields
							//
							// methodsWithProblematicFieldAccess.add(
							// methodDeclaration.getDeclarationAsString());
						}
					}
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}

			private boolean isPartOfAnnotationExpression(FieldAccessExpr expr) {
				Optional<Node> cursor = Optional.<Node> ofNullable(expr);
				while (cursor.isPresent()) {
					Node node = cursor.get();
					if (node instanceof NormalAnnotationExpr) {
						return true;
					}
					cursor = node.getParentNode();
				}
				return false;
			}
		}

		private class CheckFieldAccessVisitor extends VoidVisitorAdapter<Void> {
			@Override
			public void visit(MethodDeclaration methodDeclaration, Void arg) {
				if (methodDeclaration
						.getAnnotationByClass(DirectFieldAccessOk.class)
						.isPresent()) {
					return;
				}
				methodDeclaration.accept(new CheckFieldAccessExpressionVisitor(
						methodDeclaration), null);
			}
		}

		/**
		 * <h2>What is written?</h2>
		 * <ul>
		 * <li>clazz__ extends clazz implements MvccObject
		 * <li>implementation of MvccObject
		 * <li>getters (of domain objects and domain object sets) are rewritten
		 * to return resolved instances and resolving sets
		 * <li>setters are rewritten to ensure write is written to
		 * transaction-writable version
		 * <li>(optional) rewrite all non-private methods to check transactional
		 * validity of object
		 * <li>...more thinking, intercept *all* external calls to resolve -
		 * handles cross-tx and writable/non-writable versions
		 * </ul>
		 * 
		 * @author nick@alcina.cc
		 *
		 */
		class ClassWriter {
			public void generateMvccClassTask() {
				try {
					ClassFile cf = new ClassFile(false,
							originalClass.getName() + "__",
							originalClass.getName());
					cf.setInterfaces(
							new String[] { MvccObject.class.getName() });
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(baos);
					cf.write(dos);
					DataInputStream dis = new DataInputStream(
							new ByteArrayInputStream(baos.toByteArray()));
					CtClass ctClass = transformer.classPool.makeClass(dis);
					CtClass originalCtClass = transformer.classPool
							.getCtClass(originalClass.getName());
					ctClass.setModifiers(Modifier.PUBLIC);
					List<ThrowingRunnable> tasks = new ArrayList<>();
					CtClass mvccObjectVersionsCtClass = transformer.classPool
							.getCtClass(MvccObjectVersions.class.getName());
					CtClass voidCtClass = transformer.classPool
							.getCtClass(void.class.getName());
					CtClass classCtClass = transformer.classPool
							.getCtClass(Class.class.getName());
					/*
					 * implement mvccobject
					 */
					CtField f = new CtField(mvccObjectVersionsCtClass,
							"__mvccObjectVersions__", ctClass);
					f.setModifiers(Modifier.PRIVATE | Modifier.TRANSIENT);
					ctClass.addField(f);
					tasks.add(() -> {
						String body = "{\n\treturn __mvccObjectVersions__;}";
						CtMethod newMethod = CtNewMethod.make(Modifier.PUBLIC,
								mvccObjectVersionsCtClass,
								"__getMvccVersions__", new CtClass[0],
								new CtClass[0], body, ctClass);
						ctClass.addMethod(newMethod);
					});
					tasks.add(() -> {
						String body = "{\n\tthis.__mvccObjectVersions__=$1;}";
						CtMethod newMethod = CtNewMethod.make(Modifier.PUBLIC,
								voidCtClass, "__setMvccVersions__",
								new CtClass[] { mvccObjectVersionsCtClass },
								new CtClass[0], body, ctClass);
						ctClass.addMethod(newMethod);
					});
					/*
					 * override HasIdAndLocalId
					 */
					tasks.add(() -> {
						String body = Ax.format("{\n\treturn %s.class;}",
								originalCtClass.getName());
						CtMethod newMethod = CtNewMethod.make(Modifier.PUBLIC,
								classCtClass, "provideEntityClass",
								new CtClass[0], new CtClass[0], body, ctClass);
						ctClass.addMethod(newMethod);
					});
					/*
					 * add default constructor
					 */
					tasks.add(() -> {
						CtConstructor defaultConstructor = CtNewConstructor
								.defaultConstructor(ctClass);
						defaultConstructor.setBody("{\n\tsuper();\n}");
						ctClass.addConstructor(defaultConstructor);
					});
					/*
					 * Wrap non-private methods to ensure dispatch to
					 * transactionally correct object
					 */
					List<Method> allClassMethods = SEUtilities
							.allClassMethods(originalClass);
					for (CtMethod method : ctClass.getMethods()) {
						/*
						 * Interface methods don't (can't) refer to fields, so
						 * have no state (except 'this'). So don't need to
						 * retarget the call on these methods, just those (class
						 * methods) which have field access
						 */
						if (method.getDeclaringClass().isInterface()) {
							continue;
						}
						/*
						 * Private methods can (and should) refer to their own
						 * fields without resolution
						 */
						if (!isNonPrivateInstanceNonInterfaceMethod(
								allClassMethods, method)) {
							continue;
						}
						/*
						 * transitional - 'writeable' will go away (at the
						 * moment causes bytecode issues)
						 * 
						 * FIXME - get rid of it
						 */
						if (method.getName().matches("writeable")) {
							continue;
						}
						FormatBuilder bodyBuilder = new FormatBuilder();
						// FIXME - deprecated. It'd be nice (performance) to not
						// have to check every external access, but I think we
						// have to
						//
						// FIXME - 2 - just get rid of it
						if (transformer.addObjectResolutionChecks) {
							bodyBuilder.line(
									"cc.alcina.framework.entity.entityaccess.cache.mvcc.Transactions.checkResolved(this);");
						}
						// TODO - We assume only setters modify fields - which
						// is true of how anything that respects property
						// changes must work
						boolean getter = method.getName().matches("get[A-Z].*")
								&& method.getParameterTypes().length == 0;
						boolean setter = method.getName().matches("set[A-Z].*")
								&& method.getParameterTypes().length == 1;
						boolean writeResolve = setter;
						String declaringTypeName = getClassName(ctClass);
						String returnPhrase = method
								.getReturnType() == CtClass.voidType ? ""
										: "return";
						String returnVoidPhrase = method
								.getReturnType() == CtClass.voidType
										? "\n\treturn;"
										: "";
						List<String> argumentNames = getArgumentNames(method);
						String parameterList = argumentNames.stream()
								.collect(Collectors.joining(","));
						if (!writeResolve) {
							bodyBuilder.line(
									"if (__mvccObjectVersions__ == null){\n\t%s super.%s(%s);\n%s}\n",
									returnPhrase, method.getName(),
									parameterList, returnVoidPhrase);
						}
						/*
						 * This may result in two calls to resolve() - but
						 * there's no other way to do it with javassist
						 * 
						 * If we were to make a new method, (a) possibly not
						 * inlined and (b) need an instance variable of the new
						 * type (so we can call the method) which javassist
						 * don't like
						 */
						bodyBuilder.line(
								"%s __instance__ = (%s) cc.alcina.framework.entity.entityaccess.cache.mvcc.Transactions.resolve(this,%s);",
								originalClass.getName(),
								originalClass.getName(), writeResolve);
						bodyBuilder.line(
								"if (__instance__ != this){\n\t%s __instance__.%s(%s);\n}\n",
								returnPhrase, method.getName(), parameterList);
						bodyBuilder.line("else {\n\t%s super.%s(%s);\n}\n",
								returnPhrase, method.getName(), parameterList);
						String body = bodyBuilder.toString();
						if (body.length() > 0) {
							String f_body = Ax.format("{\n%s}",
									CommonUtils.padLinesLeft(body, "\t"));
							tasks.add(() -> {
								CtMethod newMethod = CtNewMethod.make(
										method.getModifiers(),
										method.getReturnType(),
										method.getName(),
										method.getParameterTypes(),
										method.getExceptionTypes(), f_body,
										ctClass);
								ctClass.addMethod(newMethod);
							});
						}
					}
					tasks.add(() -> {
						transformedClass = (Class<? extends H>) ctClass
								.toClass();
					});
					transformer.compilationRunnables
							.add(() -> ThrowingRunnable.runAll(tasks));
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}

			// https://stackoverflow.com/questions/20316965/get-a-name-of-a-method-parameter-using-javassist
			private List<String> getArgumentNames(CtMethod method)
					throws Exception {
				List<String> result = new ArrayList<>();
				MethodInfo methodInfo = method.getMethodInfo();
				int argumentLength = method.getParameterTypes().length;
				LocalVariableAttribute table = (LocalVariableAttribute) methodInfo
						.getCodeAttribute().getAttribute(
								javassist.bytecode.LocalVariableAttribute.tag);
				for (int idx = 1; idx < argumentLength + 1; idx++) {
					// int frameWithNameAtConstantPool = table.nameIndex(idx);
					// String variableName = methodInfo.getConstPool()
					// .getUtf8Info(frameWithNameAtConstantPool);
					// nope, try javassist ref-ids
					// result.add(variableName);
					result.add("$" + idx);
				}
				return result;
			}

			private String getClassName(CtClass type) {
				return type.getName();
			}

			private boolean isNonPrivateInstanceNonInterfaceMethod(
					List<Method> allClassMethods, CtMethod ctMethod)
					throws Exception {
				for (Method method : allClassMethods) {
					if (method.getName().equals(ctMethod.getName())) {
						boolean typesMatch = true;
						if (ctMethod.getParameterTypes().length == method
								.getParameterTypes().length) {
							for (int idx = 0; idx < ctMethod
									.getParameterTypes().length; idx++) {
								if (!matches(ctMethod.getParameterTypes()[idx],
										method.getParameterTypes()[idx])) {
									typesMatch = false;
									break;
								}
							}
						} else {
							typesMatch = false;
						}
						if (!matches(ctMethod.getReturnType(),
								method.getReturnType())) {
							typesMatch = false;
						}
						if (typesMatch) {
							if ((method.getModifiers()
									& Modifier.STATIC) != 0) {
								continue;
							}
							if ((method.getModifiers()
									& Modifier.PUBLIC) == 0) {
								continue;
							}
							// FIXME-apdm - we actually want the below - but
							// need to look at casting
							// if ((method.getModifiers()
							// & Modifier.PRIVATE) != 0) {
							// continue;
							// }
							return true;
						}
					}
				}
				return false;
			}

			private boolean matches(CtClass ctClass, Class<?> clazz) {
				return getClassName(ctClass).equals(clazz.getName());
			}
		}
	}
}
