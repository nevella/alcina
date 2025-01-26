package cc.alcina.extras.dev.console.code;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.common.base.Preconditions;

import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapperVisitor;
import cc.alcina.extras.dev.console.code.CompilationUnits.TypeFlag;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.search.BindableSearchDefinition;
import cc.alcina.framework.common.client.domain.search.DomainCriterionHandler;
import cc.alcina.framework.common.client.domain.search.EntitySearchDefinition;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.search.EnumMultipleCriterion;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.FsObjectCache;
import cc.alcina.framework.entity.util.JaxbUtils;
import cc.alcina.framework.entity.util.PersistentObjectCache.SingletonCache;
import cc.alcina.framework.entity.util.SerializationStrategy;
import cc.alcina.framework.servlet.schedule.PerformerTask;

/*
 * Rewrites flatserializer/wrappedobject definitions as
 * flatserializer-serialized
 */
public class TaskFlatSerializerMetadata extends PerformerTask {
	private boolean overwriteOriginals;

	private String classPathList;

	private transient CompilationUnits compUnits;

	private List<Class<? extends SearchDefinition>> searchDefinitions = new ArrayList<>();

	private boolean refresh;

	private Action action;

	private boolean test;

	private transient SingletonCache<FlatSerializationConfigurations> flatSerializationConfigurations;

	private String criterionNameRemovalRegex;

	private void createTaskHierarchy() {
		List<Class<? extends Task>> taskClasses = (List) compUnits.declarations
				.values().stream()
				.filter(dec -> Task.class.isAssignableFrom(dec.clazz())
						&& !Modifier.isAbstract(dec.clazz().getModifiers())
						&& !dec.clazz().isInterface())
				.map(dec -> (Class) dec.clazz()).collect(Collectors.toList());
		Multimap<Class<? extends Task>, List<Class<? extends Task>>> byHighestTaskImplementor = taskClasses
				.stream().collect(AlcinaCollectors.toKeyMultimap(
						c -> getHighestImplementor(c, Task.class)));
		byHighestTaskImplementor.forEach(
				(k, v) -> Ax.out("%s :: %s", k.getSimpleName(), v.size()));
	}

	private void ensureAnnotations() {
		List<DomainCriterionHandler> criterionHandlers = compUnits.declarations
				.values().stream()
				.filter(dec -> DomainCriterionHandler.class
						.isAssignableFrom(dec.clazz())
						&& !Modifier.isAbstract(dec.clazz().getModifiers()))
				.map(dec -> dec.<DomainCriterionHandler> typedInstance())
				.collect(Collectors.toList());
		ensureSearchDefinitionAnnotations(criterionHandlers);
		ensureSearchCriterionAnnotations(criterionHandlers);
		compUnits.writeDirty(isTest());
	}

	// TODO - refactor as per SearchDefinitionModifier
	protected void ensureSearchCriterionAnnotations(
			List<DomainCriterionHandler> criterionHandlers) {
		Multimap<Class, List<DomainCriterionHandler<?>>> perClassHandlers = (Multimap) criterionHandlers
				.stream().collect(AlcinaCollectors.toKeyMultimap(
						DomainCriterionHandler::handlesSearchCriterion));
		criterionHandlers.stream().forEach(handler -> {
			Class<? extends SearchCriterion> searchCriterionClass = handler
					.handlesSearchCriterion();
			UnitType unitType = compUnits.typeForClass(searchCriterionClass);
			if (unitType == null) {
				Ax.out("--**-- omit -- %s", searchCriterionClass);
				return;
			}
			String name = searchCriterionClass.getCanonicalName();
			Optional<FlatSerializationConfiguration> o_configuration = flatSerializationConfigurations
					.get().names.stream()
							.filter(fsc -> fsc.className.equals(name))
							.findFirst();
			// configuration.shortForm = name
			// .replaceFirst("(.+?)(?:Status)?Criterion", "$1");
			if (!o_configuration.isPresent()) {
				FlatSerializationConfiguration configuration = new FlatSerializationConfiguration();
				configuration.className = name;
				flatSerializationConfigurations.get().names.add(configuration);
				o_configuration = Optional.of(configuration);
				unitType.dirty();
			}
			FlatSerializationConfiguration configuration = o_configuration
					.get();
			if (!CommonUtils.bv(configuration.manual)) {
				configuration.shortForm = shortForm(searchCriterionClass,
						perClassHandlers.get(searchCriterionClass));
			}
			StringLiteralExpr pathExpr = new StringLiteralExpr(
					configuration.shortForm);
			if (EnumMultipleCriterion.class
					.isAssignableFrom(searchCriterionClass)) {
				NormalAnnotationExpr typeSerialization = SourceMods
						.ensureNormalTypeSerializationAnnotation(unitType);
				String initialSource = typeSerialization.toString();
				SourceMods.ensureValue(typeSerialization, "value", pathExpr);
				ArrayInitializerExpr propertiesInitializerExpr = SourceMods
						.ensureValue(typeSerialization, "properties",
								new ArrayInitializerExpr());
				propertiesInitializerExpr.getValues().stream()
						.collect(Collectors.toList())
						.forEach(Expression::remove);
				NormalAnnotationExpr valueSerialization = new NormalAnnotationExpr();
				valueSerialization
						.setName(PropertySerialization.class.getSimpleName());
				valueSerialization.addPair("name",
						new StringLiteralExpr("value"));
				valueSerialization.addPair("defaultProperty", "true");
				EnumMultipleCriterion newInstance = (EnumMultipleCriterion) Reflections
						.newInstance(searchCriterionClass);
				Class enumClass = newInstance.enumClass();
				//
				valueSerialization.addPair("types",
						enumClass.getSimpleName() + ".class");
				valueSerialization.getPairs().stream()
						.filter(p -> p.getName().toString().equals("leafType"))
						.forEach(p -> p.remove());
				valueSerialization.getPairs().stream()
						.filter(p -> p.getName().toString().equals("type"))
						.forEach(p -> p.remove());
				propertiesInitializerExpr.getValues().add(valueSerialization);
				unitType.dirty(initialSource, typeSerialization.toString());
			} else {
				SingleMemberAnnotationExpr typeSerialization = SourceMods
						.ensureSingleMemberTypeSerializationAnnotation(
								unitType);
				if (typeSerialization == null) {
					// already set - ignore
					return;
				}
				Expression memberValue = typeSerialization.getMemberValue();
				typeSerialization.setMemberValue(pathExpr);
				if (memberValue != null
						&& memberValue.toString().equals(pathExpr.toString())) {
				} else {
					unitType.dirty();
				}
			}
		});
	}

	protected void ensureSearchDefinitionAnnotations(
			List<DomainCriterionHandler> criterionHandlers) {
		getSearchDefinitions().forEach(clazz -> {
			if (!EntitySearchDefinition.class.isAssignableFrom(clazz)) {
				return;
			}
			UnitType type = compUnits.declarations.get(clazz.getName());
			new SearchDefinitionModifier(type)
					.withCriterionHandlers(criterionHandlers)
					.withSearchDefinitionClass(clazz).modify();
		});
	}

	private void enumerateSearchDefinitions() {
		compUnits.declarations.values().stream().filter(
				dec -> SearchDefinition.class.isAssignableFrom(dec.clazz()))
				.forEach(dec -> {
					Ax.out(dec.qualifiedSourceName);
				});
	}

	public Action getAction() {
		return this.action;
	}

	public String getClassPathList() {
		return this.classPathList;
	}

	public String getCriterionNameRemovalRegex() {
		return this.criterionNameRemovalRegex;
	}

	<T> Class<? extends T> getHighestImplementor(Class<? extends T> clazz,
			Class<T> implementing) {
		Class<? extends T> cursor = clazz;
		while (cursor.getSuperclass() != null
				&& implementing.isAssignableFrom(cursor.getSuperclass())) {
			cursor = (Class<? extends T>) cursor.getSuperclass();
		}
		return cursor;
	}

	public List<Class<? extends SearchDefinition>> getSearchDefinitions() {
		return this.searchDefinitions;
	}

	public boolean isOverwriteOriginals() {
		return this.overwriteOriginals;
	}

	public boolean isRefresh() {
		return refresh;
	}

	public boolean isTest() {
		return this.test;
	}

	@Override
	public void run() throws Exception {
		StringMap classPaths = StringMap.fromStringList(classPathList);
		SingletonCache<CompilationUnits> cache = FsObjectCache
				.singletonCache(CompilationUnits.class, getClass())
				.asSingletonCache();
		FsObjectCache<FlatSerializationConfigurations> flatSerializationConfigurationsCache = FsObjectCache
				.singletonCache(FlatSerializationConfigurations.class,
						getClass());
		flatSerializationConfigurationsCache.setSerializationStrategy(
				new SerializationStrategy_WrappedObject());
		flatSerializationConfigurations = flatSerializationConfigurationsCache
				.asSingletonCache();
		compUnits = CompilationUnits.load(cache, classPaths.keySet(),
				DeclarationVisitor::new, isRefresh());
		switch (getAction()) {
		case LIST_INTERESTING: {
			compUnits.declarations.values().forEach(dec -> Ax.out("%s - %s",
					dec.clazz().getSimpleName(), dec.typeFlags));
			break;
		}
		case ENUMERATE_SEARCH_DEFINITIONS: {
			enumerateSearchDefinitions();
			break;
		}
		case ENSURE_ANNOTATIONS:
			ensureAnnotations();
			flatSerializationConfigurations.persist();
			break;
		case CREATE_TASK_HIERARCHY:
			createTaskHierarchy();
			break;
		}
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public void setClassPathList(String classPathList) {
		this.classPathList = classPathList;
	}

	public void setCriterionNameRemovalRegex(String criterionNameRemovalRegex) {
		this.criterionNameRemovalRegex = criterionNameRemovalRegex;
	}

	public void setOverwriteOriginals(boolean overwriteOriginals) {
		this.overwriteOriginals = overwriteOriginals;
	}

	public void setRefresh(boolean refresh) {
		this.refresh = refresh;
	}

	public void setSearchDefinitions(
			List<Class<? extends SearchDefinition>> searchDefinitions) {
		this.searchDefinitions = searchDefinitions;
	}

	public void setTest(boolean test) {
		this.test = test;
	}

	private String shortForm(
			Class<? extends SearchCriterion> searchCriterionClass,
			List<DomainCriterionHandler<?>> dchs) {
		String name = searchCriterionClass.getSimpleName();
		if (criterionNameRemovalRegex != null) {
			name = name.replaceFirst(criterionNameRemovalRegex, "$1");
		}
		name = name.replaceFirst("(.+?)(?:Multiple)?(?:Enum)?Criterion", "$1");
		if (dchs.size() == 1) {
			Class<? extends SearchDefinition> defClass = dchs.get(0)
					.handlesSearchDefinition();
			if (defClass != null) {
				SearchDefinition def = Reflections.newInstance(defClass);
				if (def instanceof BindableSearchDefinition) {
					Class<? extends Bindable> bindableClass = ((BindableSearchDefinition) def)
							.queriedBindableClass();
					String bindableSimpleName = bindableClass.getSimpleName();
					if (criterionNameRemovalRegex != null) {
						bindableSimpleName = bindableSimpleName
								.replaceFirst(criterionNameRemovalRegex, "$1");
					}
					if (name.startsWith(bindableSimpleName)) {
						name = name.substring(bindableSimpleName.length());
					}
				}
			}
		}
		// remove type/status/object if there's anything else
		name = name.replaceFirst("(.+)Object$", "$1");
		// name = name.replaceFirst("(.+)Status", "$1");
		name = name.replaceFirst("(.+)Type$", "$1");
		name = name.toLowerCase();
		Preconditions.checkArgument(name.length() > 0);
		return name;
	}

	public enum Action {
		LIST_INTERESTING, ENUMERATE_SEARCH_DEFINITIONS, ENSURE_ANNOTATIONS,
		CREATE_TASK_HIERARCHY;
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
				UnitType type = new UnitType(unit, node);
				type.setDeclaration(node);
				unit.unitTypes.add(type);
				if (type.isAssignableFrom(DomainCriterionHandler.class)) {
					type.setFlag(Type.DomainCriterionHandler);
				}
				if (type.isAssignableFrom(SearchCriterion.class)) {
					type.setFlag(Type.SearchCriterion);
				}
				if (type.isAssignableFrom(CriteriaGroup.class)) {
					type.setFlag(Type.CriteriaGroup);
				}
				if (type.isAssignableFrom(BindableSearchDefinition.class)) {
					type.setFlag(Type.BindableSearchDefinition);
				}
				if (type.isAssignableFrom(Task.class)) {
					type.setFlag(Type.Task);
				}
			}
			super.visit(node, arg);
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class FlatSerializationConfiguration {
		public String className;

		public String shortForm;

		public boolean defaultForType = false;

		public Boolean manual;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement
	public static class FlatSerializationConfigurations {
		public List<FlatSerializationConfiguration> names = new ArrayList<>();
	}

	private class SearchDefinitionModifier extends SourceModifier {
		private NormalAnnotationExpr defTypeSerializationAnnotation;

		private ArrayInitializerExpr defTypePropertiesInitializerExpr;

		private NormalAnnotationExpr criteriaGroupsPropertySerialization;

		private ClassOrInterfaceDeclaration entityCriteriaGroup;

		private String entityCriteriaGroupName;

		private NormalAnnotationExpr criteriaGroupTypeSerializationAnnotation;

		private ArrayInitializerExpr criteriaGroupTypePropertiesInitializerExpr;

		private NormalAnnotationExpr criteriaPropertySerialization;

		private List<DomainCriterionHandler> criterionHandlers;

		private Class<? extends SearchDefinition> searchDefinitionClass;

		public SearchDefinitionModifier(UnitType type) {
			super(type);
		}

		private void addModifier(TypeDeclaration declaration, Keyword keyword) {
			declaration.getModifiers()
					.add(new com.github.javaparser.ast.Modifier(keyword));
		}

		private void createCriteriaAnnotation() {
			criteriaPropertySerialization = new NormalAnnotationExpr();
			criteriaPropertySerialization
					.setName(PropertySerialization.class.getSimpleName());
			criteriaPropertySerialization.addPair("name",
					new StringLiteralExpr("criteria"));
			criteriaPropertySerialization.addPair("defaultProperty", "true");
		}

		private void createCriteriaGroupsAnnotation() {
			criteriaGroupsPropertySerialization = new NormalAnnotationExpr();
			criteriaGroupsPropertySerialization
					.setName(PropertySerialization.class.getSimpleName());
			criteriaGroupsPropertySerialization.addPair("name",
					new StringLiteralExpr("criteriaGroups"));
			criteriaGroupsPropertySerialization.addPair("types",
					Ax.format("%s.class", entityCriteriaGroupName));
			criteriaGroupsPropertySerialization.addPair("defaultProperty",
					"true");
		}

		private void ensureCriteriaGroupSubclass() {
			entityCriteriaGroupName = declaration.getNameAsString()
					.replace("SearchDefinition", "CriteriaGroup");
			List<ClassOrInterfaceDeclaration> innerClasses = declaration
					.findAll(ClassOrInterfaceDeclaration.class);
			entityCriteriaGroup = innerClasses.stream()
					.filter(decl -> decl.getNameAsString()
							.equals(entityCriteriaGroupName))
					.findFirst().orElse(null);
			if (entityCriteriaGroup == null) {
				entityCriteriaGroup = new ClassOrInterfaceDeclaration();
				entityCriteriaGroup.setName(entityCriteriaGroupName);
				addModifier(entityCriteriaGroup, Keyword.PUBLIC);
				addModifier(entityCriteriaGroup, Keyword.STATIC);
				ClassOrInterfaceType extendsType = new ClassOrInterfaceType();
				extendsType.getName().setIdentifier("EntityCriteriaGroup");
				entityCriteriaGroup.getExtendedTypes().add(extendsType);
				declaration.addMember(entityCriteriaGroup);
			}
			String fqn = Ax.format("%s.%s", searchDefinitionClass.getName(),
					entityCriteriaGroupName);
			type.ensureImport(fqn);
		}

		@Override
		protected void ensureImports() {
			type.ensureImport(CriteriaGroup.class);
			type.ensureImport(Set.class);
			type.ensureImport(TypeSerialization.class);
			type.ensureImport(PropertySerialization.class);
		}

		private void ensureNoGetCriteriaGroupsMethod() {
			declaration.getMethodsByName("getCriteriaGroups").forEach(m -> {
				m.remove();
				logger.info("Removed {}.{}", declaration.getName(),
						m.getName());
			});
		}

		@Override
		protected void modify0() {
			ensureNoGetCriteriaGroupsMethod();
			defTypeSerializationAnnotation = ensureNormalAnnotation(declaration,
					TypeSerialization.class);
			defTypePropertiesInitializerExpr = ensureValue(
					defTypeSerializationAnnotation, "properties",
					new ArrayInitializerExpr());
			clear(defTypePropertiesInitializerExpr);
			ensureCriteriaGroupSubclass();
			createCriteriaGroupsAnnotation();
			defTypePropertiesInitializerExpr.getValues()
					.add(criteriaGroupsPropertySerialization);
			criteriaGroupTypeSerializationAnnotation = ensureNormalAnnotation(
					entityCriteriaGroup, TypeSerialization.class);
			criteriaGroupTypePropertiesInitializerExpr = ensureValue(
					criteriaGroupTypeSerializationAnnotation, "properties",
					new ArrayInitializerExpr());
			clear(criteriaGroupTypePropertiesInitializerExpr);
			createCriteriaAnnotation();
			criteriaGroupTypePropertiesInitializerExpr.getValues()
					.add(criteriaPropertySerialization);
			populateReachableCriteria();
		}

		private void populateReachableCriteria() {
			ArrayInitializerExpr initializerExpr = new ArrayInitializerExpr();
			criteriaPropertySerialization.addPair("types", initializerExpr);
			List<ClassExpr> expressions = criterionHandlers.stream()
					.filter(dch -> dch
							.handlesSearchDefinition() == searchDefinitionClass)
					.map(dch -> {
						Class<? extends SearchCriterion> searchCriterion = dch
								.handlesSearchCriterion();
						type.ensureImport(searchCriterion);
						ClassOrInterfaceType type = StaticJavaParser
								.parseClassOrInterfaceType(
										searchCriterion.getSimpleName());
						return new ClassExpr(type);
					}).collect(Collectors.toList());
			initializerExpr.setValues(new NodeList(expressions));
		}

		public SearchDefinitionModifier withCriterionHandlers(
				List<DomainCriterionHandler> criterionHandlers) {
			this.criterionHandlers = criterionHandlers;
			return this;
		}

		public SearchDefinitionModifier withSearchDefinitionClass(
				Class<? extends SearchDefinition> searchDefinitionClass) {
			this.searchDefinitionClass = searchDefinitionClass;
			return this;
		}
	}

	static class SourceMods {
		static Logger logger = LoggerFactory
				.getLogger(TaskRefactorDisplayName.class);

		public static void ensureNoGetCriteriaGroupsMethod(UnitType type) {
			ClassOrInterfaceDeclaration declaration = type.getDeclaration();
			List<MethodDeclaration> methods = declaration
					.getMethodsByName("getCriteriaGroups");
			type.ensureImport(CriteriaGroup.class);
			type.ensureImport(Set.class);
			if (methods.size() > 0) {
				MethodDeclaration methodDeclaration = methods.get(0);
				if (methodDeclaration.toString().contains("")) {
					methodDeclaration.remove();
					logger.info("Removed getCriteria() for {}",
							declaration.getName());
					type.dirty();
				}
			}
		}

		public static NormalAnnotationExpr
				ensureNormalTypeSerializationAnnotation(UnitType type) {
			Optional<AnnotationExpr> o_typeSerialization = type.getDeclaration()
					.getAnnotationByClass(TypeSerialization.class);
			if (o_typeSerialization.isPresent() && o_typeSerialization
					.get() instanceof SingleMemberAnnotationExpr) {
				o_typeSerialization.get().remove();
				o_typeSerialization = type.getDeclaration()
						.getAnnotationByClass(TypeSerialization.class);
			}
			if (!o_typeSerialization.isPresent()) {
				NormalAnnotationExpr annotationExpr = type.getDeclaration()
						.addAndGetAnnotation(TypeSerialization.class);
				type.ensureImport(TypeSerialization.class);
				type.ensureImport(PropertySerialization.class);
				annotationExpr.addPair("properties",
						new ArrayInitializerExpr());
				o_typeSerialization = type.getDeclaration()
						.getAnnotationByClass(TypeSerialization.class);
			}
			NormalAnnotationExpr typeSerialization = (NormalAnnotationExpr) o_typeSerialization
					.get();
			return typeSerialization;
		}

		public static SingleMemberAnnotationExpr
				ensureSingleMemberTypeSerializationAnnotation(UnitType type) {
			Optional<AnnotationExpr> o_typeSerialization = type.getDeclaration()
					.getAnnotationByClass(TypeSerialization.class);
			if (!o_typeSerialization.isPresent()) {
				type.getDeclaration()
						.addAnnotation(new SingleMemberAnnotationExpr(
								new Name("TypeSerialization"),
								new StringLiteralExpr(type.simpleName())));
				type.ensureImport(TypeSerialization.class);
				o_typeSerialization = type.getDeclaration()
						.getAnnotationByClass(TypeSerialization.class);
			} else {
				if (o_typeSerialization.get() instanceof NormalAnnotationExpr) {
					return null;
				}
			}
			SingleMemberAnnotationExpr typeSerialization = (SingleMemberAnnotationExpr) o_typeSerialization
					.get();
			return typeSerialization;
		}

		public static <E extends Expression> E ensureValue(
				NormalAnnotationExpr annotationExpr, String name, E valueExpr) {
			Optional<MemberValuePair> match = annotationExpr.getPairs().stream()
					.filter(pair -> pair.getName().toString().equals(name))
					.findFirst();
			if (match.isPresent()) {
				return (E) match.get().getValue();
			} else {
				annotationExpr.addPair(name, valueExpr);
				return valueExpr;
			}
		}

		public static Optional<MemberValuePair>
				getPair(NormalAnnotationExpr annotation, String name) {
			return annotation.getPairs().stream()
					.filter(mv -> mv.getName().asString().equals(name))
					.findFirst();
		}

		public static void remove(NormalAnnotationExpr ann, String name) {
			getPair(ann, name).ifPresent(MemberValuePair::remove);
		}
	}

	enum Type implements TypeFlag {
		DomainCriterionHandler, SearchCriterion, CriteriaGroup,
		BindableSearchDefinition, Task
	}

	public static class SerializationStrategy_WrappedObject
			implements SerializationStrategy {
		@Override
		public <T> T deserializeFromFile(File cacheFile, Class<T> clazz) {
			return JaxbUtils.xmlDeserialize(clazz,
					Io.read().file(cacheFile).asString());
		}

		@Override
		public String getFileSuffix() {
			return "xml";
		}

		@Override
		public <T> byte[] serializeToByteArray(T t) {
			try {
				return JaxbUtils.xmlSerialize(t).getBytes("UTF-8");
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		@Override
		public <T> void serializeToFile(T t, File cacheFile) {
			try {
				Io.write().bytes(serializeToByteArray(t)).toFile(cacheFile);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}
}
