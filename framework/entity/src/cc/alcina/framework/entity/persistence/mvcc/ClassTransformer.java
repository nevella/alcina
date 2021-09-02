package cc.alcina.framework.entity.persistence.mvcc;

import java.beans.PropertyDescriptor;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.GraphProjectionTransient;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.ThrowingRunnable;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListenerReference;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.mvcc.MvccAccess.MvccAccessType;
import cc.alcina.framework.entity.persistence.mvcc.MvccCorrectnessIssue.MvccCorrectnessIssueType;
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
 * 
 * VERSIONS:
 * 
 * (just historical interest)
 * 17 - model READ_INVALID resolvedVersionState
 * 16 - disallow covariant return types
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

	private List<Runnable> compilationRunnables = new ArrayList<>();

	Logger logger = LoggerFactory.getLogger(getClass());

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

	public <T extends Entity> T create(Class<T> clazz) {
		try {
			ClassTransform classTransform = classTransforms.get(clazz);
			return classTransform == null ? null
					: (T) classTransform.transformedClass.newInstance();
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
				.sorted(Comparator.comparing(Class::getSimpleName))
				.collect(AlcinaCollectors.toValueMap(ClassTransform::new));
		MvccCorrectnessToken token = new MvccCorrectnessToken();
		Stream<ClassTransform> stream = ResourceUtilities
				.is("checkClassCorrectnessParallel")
						? classTransforms.values().parallelStream()
						: classTransforms.values().stream();
		stream.forEach(ct -> {
			ct.setTransformer(this);
			ct.init(false);
			if (ResourceUtilities.is(ClassTransformer.class,
					"checkClassCorrectness")) {
				if (!ResourceUtilities.is(ClassTransformer.class,
						"checkClassCorrectnessForceOk")) {
					ct.checkFieldAndMethodAccess(true, false, token);
				}
			}
			ct.generateMvccClass();
		});
		if (ResourceUtilities.is(ClassTransformer.class,
				"cancelStartupIfInvalid")) {
			if (classTransforms.values().stream().anyMatch(ct -> ct.invalid)) {
				throw new IllegalStateException();
			}
		}
		AlcinaParallel.builder().withRunnables(compilationRunnables)
				.withThreadCount(8).withCancelOnException(true).withSerial(true)
				.withThreadName("ClassTransformer-compilation").run()
				.throwOnException();
		logger.info("Generated {} new mvcc classes, loaded {} existing",
				compilationRunnables.size(),
				classTransforms.size() - compilationRunnables.size());
		if (ResourceUtilities.is(ClassTransformer.class,
				"checkClassCorrectness")) {
			for (ClassTransform ct : classTransforms.values()) {
				if (!ct.invalid) {
					ct.persist();
				}
			}
		}
	}

	<H extends Entity> Class<? extends H>
			getTransformedClass(Class<H> originalClass) {
		ClassTransform classTransform = classTransforms.get(originalClass);
		return classTransform == null ? null : classTransform.transformedClass;
	}

	boolean testClassTransform(Class clazz, MvccCorrectnessToken token) {
		ClassTransform<? extends Entity> transform = classTransforms.get(clazz);
		TopicListenerReference ref = transform.correctnessIssueTopic
				.add((k, issue) -> {
					Ax.err("Correctness issue: %s %s", issue.type,
							issue.message);
				});
		try {
			transform.checkFieldAndMethodAccess(true, true, token);
			return !transform.invalid;
		} finally {
			ref.remove();
		}
	}

	String testClassTransform(Class<? extends Entity> clazz,
			MvccCorrectnessIssueType issueType, MvccCorrectnessToken token) {
		ClassTransform<? extends Entity> ct = new ClassTransform<>(clazz);
		StringBuilder logBuilder = new StringBuilder();
		ct.correctnessIssueTopic.add((k, issue) -> {
			if (issue.type == issueType) {
				logBuilder.append(issue.message);
			}
			if (issue.type.isUnknown()) {
				Ax.err("Unknown type: %s %s", issue.type, issue.message);
			}
		});
		ct.setTransformer(this);
		ct.init(true);
		ct.checkFieldAndMethodAccess(false, true, token);
		ct.generateMvccClass();
		return logBuilder.toString();
	}

	public static interface SourceFinderFsHelper {
		public URL transformPath(URL path);
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
				if (new File(toPath(sourceFileLocation)).exists()
						&& !sourceFileLocation.toString().contains("/build/")) {
					return ResourceUtilities
							.readUrlAsString(sourceFileLocation.toString());
				}
				sourceFileLocation = new URL(sourceFileLocation.toString()
						.replace("/alcina/bin/",
								"/alcina/framework/entity/src/")
						.replace("/bin/", "/src/")
						.replace("/build/classes/", "/src/"));
				if (sourceFileLocation.toString().contains("/build/")
						&& !sourceFileLocation.toString().contains("/src/")) {
					sourceFileLocation = new URL(sourceFileLocation.toString()
							.replace("/build/", "/src/"));
				}
				if (new File(toPath(sourceFileLocation)).exists()) {
					return ResourceUtilities
							.readUrlAsString(sourceFileLocation.toString());
				}
				sourceFileLocation = new URL(sourceFileLocation.toString()
						.replace("/alcina/framework/entity/src/",
								"/alcina/framework/common/src/"));
				if (new File(toPath(sourceFileLocation)).exists()) {
					return ResourceUtilities
							.readUrlAsString(sourceFileLocation.toString());
				}
				Optional<SourceFinderFsHelper> helper = Registry
						.optional(SourceFinderFsHelper.class);
				if (helper.isPresent()) {
					sourceFileLocation = new URL(sourceFileLocation.toString()
							.replace("/alcina/framework/entity/src/",
									"/alcina/framework/common/src/"));
					if (new File(toPath(sourceFileLocation)).exists()) {
						return ResourceUtilities
								.readUrlAsString(sourceFileLocation.toString());
					}
				}
				return null;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		private String toPath(URL sourceFileLocation) {
			return sourceFileLocation.toString().replaceFirst("^file:/*/", "/");
		}
	}

	static class ClassTransform<H extends Entity> {
		private static final transient int VERSION = 17;

		transient Topic<MvccCorrectnessIssue> correctnessIssueTopic = Topic
				.local();

		private int version;

		private Class<H> originalClass;

		private transient Class<? extends H> transformedClass;

		private transient ClassTransformer transformer;

		private transient ClassTransform<H> lastRun;

		private transient CompilationUnit compilationUnit;

		private transient boolean invalid;

		private List<String> classSources;

		private byte[] transformedClassBytes;

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

		private void checkDuplicateFieldNames() {
			Multimap<String, List<Field>> byName = SEUtilities
					.allFields(originalClass).stream()
					.collect(AlcinaCollectors.toKeyMultimap(Field::getName));
			byName.entrySet().stream().filter(e -> e.getValue().size() > 1)
					.filter(e -> !(e.getKey().equals("id") && ResourceUtilities
							.is(ClassTransformer.class, "allowShadowIdField")))
					.forEach(e -> {
						fieldsWithProblematicAccess.add(e.getKey());
						correctnessIssueTopic.publish(new MvccCorrectnessIssue(
								MvccCorrectnessIssueType.Duplicate_field_name,
								Ax.format(
										"Duplicate field name: field '%s' - fields: \n\t%s",
										e.getKey(),
										CommonUtils.joinWithNewlineTab(
												e.getValue()))));
					});
		}

		private void checkFieldModifiers() {
			SEUtilities.allFields(originalClass).stream()
					.filter(f -> (f.getModifiers() & Modifier.PRIVATE) == 0)
					.filter(f -> (f.getModifiers() & Modifier.STATIC) == 0)
					.filter(f -> (f.getModifiers() & Modifier.TRANSIENT) == 0)
					.filter(f -> !f.getName().matches(
							"id|localId|creationDate|versionNumber|lastModificationDate|"
									+ "propertyValue"))
					.forEach(f -> {
						fieldsWithProblematicAccess.add(f.getName());
						correctnessIssueTopic.publish(new MvccCorrectnessIssue(
								MvccCorrectnessIssueType.Invalid_field_access,
								Ax.format("Incorrect access: field '%s'",
										f.getName())));
					});
		}

		/*
		 * Only getters are required (for graph projection - that will project
		 * using field.set() on a non-mvcc object)
		 */
		private void checkFieldsHaveGetters() {
			SEUtilities.allFields(originalClass).stream()
					.filter(f -> (f.getModifiers() & Modifier.PRIVATE) != 0)
					.filter(f -> (f.getModifiers() & Modifier.STATIC) == 0)
					.filter(f -> (f.getModifiers() & Modifier.TRANSIENT) == 0)
					.filter(f -> f.getAnnotation(
							GraphProjectionTransient.class) == null)
					.forEach(f -> {
						PropertyDescriptor propertyDescriptor = SEUtilities
								.getPropertyDescriptorByName(originalClass,
										f.getName());
						if (propertyDescriptor != null
								&& propertyDescriptor.getReadMethod() != null) {
						} else {
							fieldsWithProblematicAccess.add(f.getName());
							correctnessIssueTopic
									.publish(new MvccCorrectnessIssue(
											MvccCorrectnessIssueType.Invalid_field_access,
											Ax.format(
													"Missing getter/setter: field '%s'",
													f.getName())));
						}
					});
		}

		private String findSource(Class clazz) throws Exception {
			for (SourceFinder finder : SourceFinder.sourceFinders) {
				String source = finder.findSource(clazz);
				if (source != null) {
					return source;
				}
			}
			Ax.err("Warn - cannot find source:\n\t%s", clazz.getName());
			return null;
		}

		private boolean isSameSourceAsLastRun() {
			boolean sameSource = lastRun != null
					&& Objects.equals(lastRun.classSources, classSources)
					&& classSources.size() > 0;
			// if (!sameSource && lastRun != null && lastRun.classSources !=
			// null
			// && lastRun.classSources.size() > 0) {
			// ThreeWaySetResult<String> split = CommonUtils
			// .threeWaySplit(lastRun.classSources, classSources);
			// }
			return sameSource;
		}

		void checkFieldAndMethodAccess(boolean logWarnings,
				boolean ignoreLastRun, MvccCorrectnessToken token) {
			if (isSameSourceAsLastRun() && !ignoreLastRun) {
				methodsWithProblematicAccess = lastRun.methodsWithProblematicAccess;
				fieldsWithProblematicAccess = lastRun.fieldsWithProblematicAccess;
			} else {
				Ax.out("checking unit : %s", originalClass.getSimpleName());
				checkFieldModifiers();
				checkDuplicateFieldNames();
				checkFieldsHaveGetters();
				for (String source : classSources) {
					if (token.checkedSources.add(source)) {
						compilationUnit = StaticJavaParser.parse(source);
						compilationUnit.findAll(TypeDeclaration.class)
								.forEach(classDeclaration -> {
									String typeName = classDeclaration.getName()
											.asString();
									if (logWarnings) {
										Ax.out("checking correctness: %s",
												typeName);
									}
									CheckAccessVisitor visitor = new CheckAccessVisitor();
									classDeclaration.accept(visitor, null);
								});
					}
				}
			}
			if (logWarnings) {
				if (fieldsWithProblematicAccess.size() > 0) {
					Ax.err("\n======================\nClass: %s\nFieldsWithProblematicAccess:\n======================",
							originalClass.getName());
					fieldsWithProblematicAccess.forEach(Ax::err);
					Ax.out("\n");
					invalid = true;
				}
				if (methodsWithProblematicAccess.size() > 0) {
					Ax.err("\n======================\nClass: %s\nMethodsWithProblematicAccess:\n======================",
							originalClass.getName());
					methodsWithProblematicAccess.forEach(Ax::err);
					Ax.out("\n");
					invalid = true;
				}
			}
		}

		synchronized void generateMvccClass() {
			if (isSameSourceAsLastRun()
					&& lastRun.transformedClassBytes != null) {
				try {
					CtClass ctClass = transformer.classPool
							.makeClass(new ByteArrayInputStream(
									lastRun.transformedClassBytes));
					transformedClass = (Class<? extends H>) ctClass.toClass();
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			} else {
				new ClassWriter().generateMvccClassTask();
			}
		}

		void init(boolean ignoreLastRun) {
			try {
				this.classSources = new ArrayList<>();
				Class clazz = originalClass;
				String superClassFilter = ResourceUtilities.get(
						ClassTransformer.class,
						"checkClassCorrectness.superClassFilter");
				while (clazz != Object.class) {
					if (Ax.notBlank(superClassFilter)
							&& clazz.getName().matches(superClassFilter)) {
						break;
					}
					classSources.add(findSource(clazz));
					clazz = clazz.getSuperclass();
				}
				classSources.removeIf(Ax::isNull);
				this.lastRun = transformer.cache.get(originalClass.getName());
				if (ignoreLastRun) {
					this.lastRun = null;
				}
				if (this.lastRun != null && this.lastRun.version != VERSION) {
					this.lastRun = null;
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		synchronized void persist() {
			if (isSameSourceAsLastRun()
					&& lastRun.transformedClassBytes != null) {
				return;
			}
			try {
				this.version = VERSION;
				transformer.cache.persist(originalClass.getName(), this);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		private class CheckAccessVisitor extends VoidVisitorAdapter<Void> {
			private MethodDeclaration methodDeclaration;

			private TypeDeclaration typeDeclaration;

			private boolean expressionIsInNestedType;

			private boolean expressionIsInNestedAnonymousType;

			private String containingClassName;

			@SuppressWarnings("unused")
			// for debugging
			private MethodCallExpr visiting;

			private ClassOrInterfaceDeclaration classOrInterfaceDeclaration;

			public CheckAccessVisitor() {
			}

			@Override
			public void visit(
					ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
					Void arg) {
				this.classOrInterfaceDeclaration = classOrInterfaceDeclaration;
				super.visit(classOrInterfaceDeclaration, arg);
			}

			@Override
			// checkd as a subtype of 'NameExpr' checks (since field access can
			// be without a this.xxx form)
			//
			/*
			 * 
			 * 
			 * Transactional access::
			 * 
			 * Two main issues: field access and object identity
			 * 
			 * Identity: for objects created or registered in the domain
			 * store transaction manager, there is only *one instance* of
			 * the object used for equality. This applies to all objects
			 * objects created in tx phase 'TO_DOMAIN_COMMITTING' - i.e.
			 * with a non-zero id field - and also objects
			 * objects created with an initial non-zero localid in tx phase 
			 * TO_DB_PREPARING. All references between
			 * graph objects use that 'domain identity' object.
			 * 
			 * The effect of that constraint on the class rewriter is quite
			 * profound - we can't allow any reference to 'this' from
			 * transactional object versions to escape from code - and that
			 * includes inner classes. Consequent constraints are:
			 * 
			 * @formatter:off
			 * - inner class creation must be in dedicated public/protected methods in the entity class which 
			 * *only* return the inner class (and are hooked through 'domainIdentity().xxx' by the programmer) 
			 * - entity field access can only occur in the main class. no inner class can access entity class fields
			 * - 'this' can't be assigned or returned
			 * 
			 * @formatter:on
			 * 
			 * TODO: rewrite rewriter to go getXX->resolveTx.__super__getXX (which calls super.getXX)
			 * 
			 * 
			 * 
			 */
			public void visit(FieldAccessExpr expr, Void arg) {
				super.visit(expr, arg);
				if (isDefinedOk(expr)) {
					// annotation etc
					return;
				}
				try {
					SymbolReference<? extends ResolvedValueDeclaration> ref = transformer.solver
							.solve(expr);
					if (ref.getCorrespondingDeclaration().isField()) {
						ResolvedFieldDeclaration field = ref
								.getCorrespondingDeclaration().asField();
						if (expressionIsInNestedType
								&& !field.declaringType().getQualifiedName()
										.equals(containingClassName)
								&& field.accessSpecifier() != AccessSpecifier.PUBLIC) {
							addProblematicAccess(
									MvccCorrectnessIssueType.InnerClassOuterFieldAccess);
						}
						if (expressionIsInNestedAnonymousType && field
								.accessSpecifier() != AccessSpecifier.PUBLIC) {
							addProblematicAccess(
									MvccCorrectnessIssueType.InnerAnonymousClassOuterFieldAccess);
						}
					}
				} catch (RuntimeException e) {
					logSolverException(expr, e);
				}
			}

			@Override
			public void visit(MethodCallExpr expr, Void arg) {
				this.visiting = expr;
				if (isDefinedOk(expr)) {
					return;
				}
				super.visit(expr, arg);
				try {
					SymbolReference<ResolvedMethodDeclaration> ref = transformer.solver
							.solve(expr);
					if (!ref.isSolved()) {
						// Ax.out("Not solved: %s", expr);
						return;
					}
					ResolvedMethodDeclaration decl = ref
							.getCorrespondingDeclaration();
					boolean privateOrPackageMethod = decl
							.accessSpecifier() == AccessSpecifier.PRIVATE
							|| decl.accessSpecifier() == AccessSpecifier.PACKAGE_PRIVATE;
					boolean staticMethod = decl.isStatic();
					boolean entityMethod = decl.declaringType()
							.getQualifiedName().equals(originalClass.getName());
					if (privateOrPackageMethod && entityMethod && !staticMethod
							&& (expressionIsInNestedType
									|| expressionIsInNestedAnonymousType)) {
						addProblematicAccess(
								MvccCorrectnessIssueType.InnerClassOuterPrivateMethodAccess);
					}
				} catch (RuntimeException e) {
					logSolverException(expr, e);
				}
			}

			@Override
			public void visit(MethodReferenceExpr expr, Void arg) {
				super.visit(expr, arg);
				if (isDefinedOk(expr)) {
					return;
				}
				// by wriggling, we could access private methods here...but have
				// to try reaally hard
				//
				// MvccTestEntity.this::disallowedInnerAccessMethod
				// since solver doesn't work nicely for these, just forbid
				if (expressionIsInNestedType) {
					if (expr.toString().matches(".+\\.this::.+")) {
						addProblematicAccess(
								MvccCorrectnessIssueType.InnerClassOuterPrivateMethodRef);
					}
				}
			}

			@Override
			public void visit(NameExpr expr, Void arg) {
				super.visit(expr, arg);
				if (isDefinedOk(expr)) {
					// constant expression
					return;
				}
				try {
					SymbolReference<? extends ResolvedValueDeclaration> ref = transformer.solver
							.solve(expr);
					if (!ref.isSolved()) {
						// Ax.out("Not solved: %s", expr);
						return;
					}
					if (ref.getCorrespondingDeclaration().isField()) {
						ResolvedFieldDeclaration field = ref
								.getCorrespondingDeclaration().asField();
						AccessSpecifier accessSpecifier = field
								.accessSpecifier();
						if (accessSpecifier != AccessSpecifier.PUBLIC) {
							if (expressionIsInNestedType
									&& !field.declaringType().getQualifiedName()
											.equals(containingClassName)) {
								addProblematicAccess(
										MvccCorrectnessIssueType.InnerClassOuterFieldAccessByName);
							}
							if (expressionIsInNestedAnonymousType) {
								addProblematicAccess(
										MvccCorrectnessIssueType.InnerAnonymousClassOuterFieldAccess);
							}
						}
					}
				} catch (RuntimeException e) {
					logSolverException(expr, e);
				}
			}

			@Override
			public void visit(ObjectCreationExpr expr, Void arg) {
				super.visit(expr, arg);
				if (isDefinedOk(expr)) {
					// constant expression
					return;
				}
				ClassOrInterfaceType type = expr.getType();
				ResolvedType creationType = transformer.solver.getType(expr);
				ResolvedReferenceTypeDeclaration creationTypeDeclaration = creationType
						.asReferenceType().getTypeDeclaration().get();
				if (creationTypeDeclaration instanceof JavaParserClassDeclaration) {
					try {
						Class clazz = getJvmClassFromTypeDeclaration(
								creationTypeDeclaration);
						boolean innerNonStatic = isInnerNonStatic(clazz);
						if (innerNonStatic) {
							// not allowed, must be annotated with
							// @MvccAccessCorrect - see
							// cc.alcina.framework.entity.persistence.mvcc.MvccTestEntity.valid_InnerConstructor()
							addProblematicAccess(
									MvccCorrectnessIssueType.InnerClassConstructor);
						}
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					} catch (VerifyError vr) {
						Logger logger = LoggerFactory.getLogger(getClass());
						logger.warn("Verify error in visitor: {} - {}",
								CommonUtils.toSimpleExceptionMessage(vr),
								classOrInterfaceDeclaration.getName()
										.toString());
						logger.warn("Verify error", vr);
					}
				}
			}

			@Override
			public void visit(SuperExpr expr, Void arg) {
				super.visit(expr, arg);
				// super.xxx calls are actually fine - we can't leak 'this'
				// (since those methods are also checked) and we're already
				// resolved (for field
				// value correctness)
				//
				//
				// if (isDefinedOk(expr)) {
				// // constant expression
				// return;
				// }
				// if (isInStaticNonEntityInnerClass(expr)) {
				// return;
				// }
				// addProblematicAccess(MvccCorrectnessIssueType.Super_usage);
				// Node parent = expr.getParentNode().get();
				// if (parent instanceof ReturnStmt) {
				// }
			}

			@Override
			public void visit(ThisExpr expr, Void arg) {
				super.visit(expr, arg);
				if (isDefinedOk(expr)) {
					return;
				}
				if (expressionIsInNestedType) {
					// OK, 'this' is nested in domainIdentity()
					return;
				}
				Node parent = expr.getParentNode().get();
				// OK -- e.g. this.id
				if (parent instanceof FieldAccessExpr) {
					return;
				}
				if (parent instanceof MethodCallExpr) {
					MethodCallExpr methodCallExpr = (MethodCallExpr) parent;
					if (methodCallExpr.getScope().isPresent()
							&& methodCallExpr.getScope().get() == expr) {
						// OK -- e.g. this.getId()
						return;
					} else {
						// foo(this);
						addProblematicAccess(
								MvccCorrectnessIssueType.This_assignment_unknown);
						return;
					}
				}
				// OK -- e.g. MvccTestEntity.this::someMethod (we check access
				// elsewhere)
				if (parent instanceof MethodReferenceExpr) {
					return;
				}
				if (parent instanceof InstanceOfExpr) {
					// OK - this instanceof Foo
					return;
				}
				if (isInStaticNonEntityInnerClass(expr)) {
					return;
				}
				if (parent instanceof ReturnStmt) {
					addProblematicAccess(
							MvccCorrectnessIssueType.This_ReturnStmt);
				} else if (parent instanceof VariableDeclarator) {
					addProblematicAccess(
							MvccCorrectnessIssueType.This_VariableDeclarator);
				} else if (parent instanceof AssignExpr) {
					addProblematicAccess(
							MvccCorrectnessIssueType.This_AssignExpr);
				} else if (parent instanceof BinaryExpr) {
					addProblematicAccess(
							MvccCorrectnessIssueType.This_BinaryExpr);
				} else if (parent instanceof ObjectCreationExpr) {
					addProblematicAccess(
							MvccCorrectnessIssueType.This_assignment_unknown);
				} else if (parent instanceof CastExpr) {
					addProblematicAccess(
							MvccCorrectnessIssueType.This_assignment_unknown);
				} else if (parent instanceof SwitchStmt) {
					addProblematicAccess(
							MvccCorrectnessIssueType.This_assignment_unknown);
				} else if (parent instanceof ConditionalExpr) {
					addProblematicAccess(
							MvccCorrectnessIssueType.This_assignment_unknown);
				} else if (parent instanceof SynchronizedStmt) {
					addProblematicAccess(
							MvccCorrectnessIssueType.This_assignment_unknown);
				} else if (parent instanceof EnclosedExpr) {
					throw new UnsupportedOperationException(Ax
							.format("Remove enclosed expression: %s", parent));
				} else {
					addProblematicAccess(
							MvccCorrectnessIssueType.This_assignment_unknown);
					Ax.sysLogHigh("really should check this: %s",
							parent.getClass().getSimpleName());
					// unknown
					// throw new UnsupportedOperationException();
				}
			}

			private TypeDeclaration getContainingDeclaration(Node node) {
				while (node != null) {
					if (node instanceof TypeDeclaration) {
						return (TypeDeclaration) node;
					}
					node = node.getParentNode().get();
				}
				return null;
			}

			private Class getJvmClassFromTypeDeclaration(
					ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration) {
				try {
					String qualifiedName = resolvedReferenceTypeDeclaration
							.getQualifiedName();
					FormatBuilder fb = new FormatBuilder().separator(".");
					for (String part : qualifiedName.split("\\.")) {
						fb.append(part);
						if (part.matches("[A-Z].+")) {
							fb.separator("$");
						}
					}
					Class clazz = Class.forName(fb.toString());
					return clazz;
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}

			private boolean isDefinedOk(Expression expr) {
				boolean isInAnnotationExpression = false;
				boolean mvccAccessCorrectAnnotationPresent = false;
				Optional<Node> cursor = Optional.<Node> ofNullable(expr);
				boolean debug = false;
				expressionIsInNestedAnonymousType = false;
				while (cursor.isPresent()) {
					Node node = cursor.get();
					if (debug) {
						Ax.out(node.getClass().getSimpleName());
					}
					if (node instanceof MethodDeclaration) {
						methodDeclaration = (MethodDeclaration) node;
						mvccAccessCorrectAnnotationPresent = methodDeclaration
								.getAnnotationByClass(MvccAccess.class)
								.isPresent();
					}
					if (node instanceof ObjectCreationExpr
							&& ((ObjectCreationExpr) node)
									.getAnonymousClassBody().isPresent()) {
						expressionIsInNestedAnonymousType = true;
					}
					if (node instanceof TypeDeclaration) {
						typeDeclaration = (TypeDeclaration) node;
						containingClassName = transformer.solver
								.getTypeDeclaration(typeDeclaration)
								.asReferenceType().getQualifiedName();
						expressionIsInNestedType = typeDeclaration
								.isNestedType();
						break;
					}
					if (node instanceof NormalAnnotationExpr) {
						isInAnnotationExpression = true;
					}
					cursor = node.getParentNode();
				}
				return isInAnnotationExpression
						|| mvccAccessCorrectAnnotationPresent;
			}

			private boolean isInnerNonStatic(Class clazz) {
				boolean innerNonStatic = false;
				while (clazz.getEnclosingClass() != null) {
					if ((clazz.getModifiers() & Modifier.STATIC) != 0) {
						break;
					}
					clazz = clazz.getEnclosingClass();
					if (clazz.getEnclosingClass() == null) {
						innerNonStatic = true;
						break;
					}
				}
				return innerNonStatic;
			}

			private boolean isInStaticNonEntityInnerClass(Expression expr) {
				TypeDeclaration containingDeclaration = getContainingDeclaration(
						expr);
				ResolvedReferenceTypeDeclaration creationTypeDeclaration = transformer.solver
						.getTypeDeclaration(containingDeclaration);
				Class clazz = getJvmClassFromTypeDeclaration(
						creationTypeDeclaration);
				boolean innerNonStatic = isInnerNonStatic(clazz);
				return !innerNonStatic && !Entity.class.isAssignableFrom(clazz);
			}

			private void logSolverException(Expression expr,
					RuntimeException e) {
				Ax.simpleExceptionOut(e);
				Ax.sysLogHigh("%s:%s\nNot solved: %s", containingClassName,
						(methodDeclaration == null ? null
								: methodDeclaration.getName().toString()),
						expr);
				int debug = 3;
			}

			protected void addProblematicAccess(MvccCorrectnessIssueType type) {
				Ax.out("Incorrect access: method '%s'", decorateLocation());
				methodsWithProblematicAccess
						.add(methodDeclaration == null ? "(constructor)"
								: methodDeclaration.getDeclarationAsString());
				correctnessIssueTopic.publish(new MvccCorrectnessIssue(type,
						Ax.format("Incorrect access: method '%s'",
								decorateLocation())));
			}

			String decorateLocation() {
				return methodDeclaration == null ? "(no method)"
						: methodDeclaration.getDeclarationAsString();
			}

			@SuppressWarnings("unused")
			class T1 {
				void yum(int a) {
				}

				class T2 {
					void test() {
						IntStream.of(1, 2).forEach(T1.this::yum);
					}
				}
			}
		}

		/**
		 * <h2>What is written?</h2>
		 * <ul>
		 * <li>clazz__ extends clazz implements MvccObject
		 * <li>implementation of MvccObject
		 * <li>non-private methods are rewritten to apply call to transactional
		 * version of object
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
					CtClass entityCtClass = transformer.classPool
							.getCtClass(Entity.class.getName());
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
					 * override Entity
					 * 
					 */
					tasks.add(() -> {
						String body = Ax.format("{\n\treturn %s.class;}",
								originalCtClass.getName());
						CtMethod newMethod = CtNewMethod.make(Modifier.PUBLIC,
								classCtClass, "entityClass", new CtClass[0],
								new CtClass[0], body, ctClass);
						ctClass.addMethod(newMethod);
					});
					tasks.add(() -> {
						FormatBuilder bodyBuilder = new FormatBuilder();
						bodyBuilder.line(
								"{\n\t%s versions = __mvccObjectVersions__;",
								mvccObjectVersionsCtClass.getName());
						bodyBuilder.line(
								"\tif (versions == null){\n\t\treturn this;\n\t} else {\n\t\t"
										+ "return (%s) versions.getDomainIdentity();\t\n}\n}",
								ctClass.getName());
						String body = bodyBuilder.toString();
						CtMethod newMethod = CtNewMethod.make(Modifier.PUBLIC,
								entityCtClass, "domainIdentity", new CtClass[0],
								new CtClass[0], body, ctClass);
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
					 * transactionally correct object.
					 */
					List<Method> allClassMethods = SEUtilities
							.allClassMethods(originalClass);
					checkCovariantReturnTypeMethods(allClassMethods);
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
						if (method.getName().matches("entityClass")) {
							continue;
						}
						if (method.getName().matches("domainIdentity")) {
							continue;
						}
						/*
						 * equals() and hashcode() should not be overridden from
						 * the Entity definitions - and those are safe across
						 * versions (i.e. different versions of the same object
						 * will have the same hashcode and will be equal())
						 * 
						 * so it doesn't matter which version we test against,
						 * hence no need to reroute
						 */
						if (method.getName().matches("equals")) {
							continue;
						}
						if (method.getName().matches("hashCode")) {
							continue;
						}
						/*
						 * id is only set on the domain identity - ditto localId
						 */
						if (method.getName()
								.matches("getId|setId|getLocalId|setLocalId")) {
							continue;
						}
						/*
						 * both final
						 */
						if (method.getName()
								.matches("toStringEntity|toLocator")) {
							continue;
						}
						FormatBuilder bodyBuilder = new FormatBuilder();
						// TODO - We assume only setters modify fields - which
						// is true of how anything that respects property
						// changes must work
						boolean setter = method.getName().matches("set[A-Z].*")
								&& method.getParameterTypes().length == 1;
						boolean propertyChangeListenersMutator = method
								.getName().matches(
										"(?:(?:add|remove).*PropertyChangeListener)|propertyChangeSupport");
						// Tristaate resolvedversions
						//
						// propertyChangeListeners are problematic because of,
						// among other things, circular calls to TLTM.listenTo
						// when removing if we have them be 'writeresolve' (and
						// creation of unnecessaary writeable versions)
						//
						// if the object version is already in write state (i.e.
						// called from a setter), all good.
						//
						// if not, listener will be added to
						// data-field-immutable prior-tx
						// object (since it'll be read-resolved) which is
						// inelegant but harmless. Or is it? It is, because
						// generation of writeable version doesn't copy
						// transients. Cleanup is more a question of
						// throw-or-warn-if object is not in writeable state (if
						// object is an mvcc object)
						//
						// The solution would be tristate resolve
						// (read,write,read_invalid) which logs or throws
						//
						ResolvedVersionState state = ResolvedVersionState.READ;
						if (setter) {
							state = ResolvedVersionState.WRITE;
						} else if (propertyChangeListenersMutator) {
							state = ResolvedVersionState.READ_INVALID;
						}
						MvccAccessType accessType = null;
						if (method.hasAnnotation(MvccAccess.class)) {
							MvccAccess annotation = (MvccAccess) method
									.getAnnotation(MvccAccess.class);
							accessType = annotation.type();
						}
						if (accessType == MvccAccessType.TRANSACTIONAL_ACCESS_NOT_SUPPORTED) {
							bodyBuilder.line(
									"throw new UnsupportedOperationException();");
						} else {
							String declaringTypeName = getClassName(ctClass);
							String returnPhrase = method
									.getReturnType() == CtClass.voidType ? ""
											: "return";
							String returnVoidPhrase = method
									.getReturnType() == CtClass.voidType
											? "\n\treturn;"
											: "";
							List<String> argumentNames = getArgumentNames(
									method);
							String parameterList = argumentNames.stream()
									.collect(Collectors.joining(","));
							if (state == ResolvedVersionState.READ) {
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
							 * inlined and (b) need an instance variable of the
							 * new type (so we can call the method) which
							 * javassist don't like
							 * 
							 * We could write another super_call method - but
							 * remember, 99.9% of the time we don't reach this
							 * point...
							 */
							ResolvedVersionState emitState = accessType == MvccAccessType.RESOLVE_TO_DOMAIN_IDENTITY
									? ResolvedVersionState.READ
									: state;
							bodyBuilder.line(
									"%s __instance__ = (%s) cc.alcina.framework.entity.persistence.mvcc.Transactions.resolve(this, "
											+ "cc.alcina.framework.entity.persistence.mvcc.ResolvedVersionState.%s, %s);",
									cf.getName(), cf.getName(), emitState,
									accessType == MvccAccessType.RESOLVE_TO_DOMAIN_IDENTITY);
							bodyBuilder.line(
									"if (__instance__ != this){\n\t%s __instance__.%s(%s);\n}\n",
									returnPhrase, method.getName(),
									parameterList);
							bodyBuilder.line("else {\n\t%s super.%s(%s);\n}\n",
									returnPhrase, method.getName(),
									parameterList);
						}
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
						transformedClassBytes = ctClass.toBytecode();
						transformedClass = (Class<? extends H>) ctClass
								.toClass();
					});
					transformer.compilationRunnables
							.add(() -> ThrowingRunnable.runAll(tasks));
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}

			private void checkCovariantReturnTypeMethods(
					List<Method> allClassMethods) {
				Multimap<MethodNameArgTypes, List<Method>> covariantBeanMethods = allClassMethods
						.stream()
						.filter(m -> m.getName().matches("(get|set|is)[A-Z].*"))
						.sorted(Comparator.comparing(Method::getName))
						.collect(AlcinaCollectors
								.toKeyMultimap(MethodNameArgTypes::new));
				covariantBeanMethods.entrySet()
						.removeIf(e -> e.getValue().stream()
								.map(Method::getReturnType).distinct()
								.count() == 1);
				if (covariantBeanMethods.size() > 0) {
					Ax.err("Covariant methods (disallowed for mvcc)");
					covariantBeanMethods.entrySet().stream().forEach(e -> {
						Ax.out(e.getValue());
						Ax.out("");
					});
					throw new IllegalStateException();
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
									& Modifier.PRIVATE) != 0) {
								continue;
							}
							return true;
						}
					}
				}
				return false;
			}

			private boolean matches(CtClass ctClass, Class<?> clazz) {
				return getClassName(ctClass).equals(clazz.getName());
			}

			private class MethodNameArgTypes {
				private Method method;

				MethodNameArgTypes(Method method) {
					this.method = method;
				}

				@Override
				public boolean equals(Object obj) {
					MethodNameArgTypes o = (MethodNameArgTypes) obj;
					try {
						return o.method.getName().equals(method.getName())
								&& Arrays.equals(o.method.getParameterTypes(),
										method.getParameterTypes());
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
				}

				@Override
				public int hashCode() {
					return method.getName().hashCode();
				}
			}
		}
	}
}
