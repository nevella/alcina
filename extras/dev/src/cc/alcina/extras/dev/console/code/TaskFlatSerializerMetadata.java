package cc.alcina.extras.dev.console.code;

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
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
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

import cc.alcina.extras.dev.console.code.CompilationUnits.ClassOrInterfaceDeclarationWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapperVisitor;
import cc.alcina.extras.dev.console.code.CompilationUnits.TypeFlag;
import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.search.DomainCriterionHandler;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.search.EnumMultipleCriterion;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.serializer.flat.PropertySerialization;
import cc.alcina.framework.common.client.serializer.flat.TypeSerialization;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.util.FsObjectCache;
import cc.alcina.framework.entity.util.PersistentObjectCache.SingletonCache;
import cc.alcina.framework.entity.util.SerializationStrategy.SerializationStrategy_WrappedObject;
import cc.alcina.framework.gwt.client.entity.search.BindableSearchDefinition;
import cc.alcina.framework.gwt.client.entity.search.EntitySearchDefinition;
import cc.alcina.framework.servlet.schedule.ServerTask;

public class TaskFlatSerializerMetadata
		extends ServerTask<TaskFlatSerializerMetadata> {
	private boolean overwriteOriginals;

	private String classPathList;

	private transient CompilationUnits compUnits;

	private List<Class<? extends SearchDefinition>> searchDefinitions = new ArrayList<>();

	private boolean refresh;

	private Action action;

	private boolean test;

	private transient SingletonCache<FlatSerializationConfigurations> flatSerializationConfigurations;

	private String criterionNameRemovalRegex;

	public Action getAction() {
		return this.action;
	}

	public String getClassPathList() {
		return this.classPathList;
	}

	public String getCriterionNameRemovalRegex() {
		return this.criterionNameRemovalRegex;
	}

	@PropertySerialization(leafType = Class.class)
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

	private void enumerateSearchDefinitions() {
		compUnits.declarations.values().stream().filter(
				dec -> SearchDefinition.class.isAssignableFrom(dec.clazz()))
				.forEach(dec -> {
					Ax.out(dec.qualifiedSourceName);
				});
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
				SearchDefinition def = Reflections.classLookup()
						.newInstance(defClass);
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

	protected void ensureSearchCriterionAnnotations(
			List<DomainCriterionHandler> criterionHandlers) {
		Multimap<Class, List<DomainCriterionHandler<?>>> perClassHandlers = (Multimap) criterionHandlers
				.stream().collect(AlcinaCollectors.toKeyMultimap(
						DomainCriterionHandler::handlesSearchCriterion));
		criterionHandlers.stream().forEach(handler -> {
			Class<? extends SearchCriterion> searchCriterionClass = handler
					.handlesSearchCriterion();
			ClassOrInterfaceDeclarationWrapper declarationWrapper = compUnits
					.declarationWrapperForClass(searchCriterionClass);
			if (declarationWrapper == null) {
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
				declarationWrapper.dirty();
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
						.ensureNormalTypeSerializationAnnotation(
								declarationWrapper);
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
				valueSerialization.addPair("leafType",
						enumClass.getSimpleName() + ".class");
				propertiesInitializerExpr.getValues().add(valueSerialization);
				declarationWrapper.dirty(initialSource,
						typeSerialization.toString());
			} else {
				SingleMemberAnnotationExpr typeSerialization = SourceMods
						.ensureSingleMemberTypeSerializationAnnotation(
								declarationWrapper);
				Expression memberValue = typeSerialization.getMemberValue();
				typeSerialization.setMemberValue(pathExpr);
				if (memberValue != null
						&& memberValue.toString().equals(pathExpr.toString())) {
				} else {
					declarationWrapper.dirty();
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
			ClassOrInterfaceDeclarationWrapper declarationWrapper = compUnits.declarations
					.get(clazz.getName());
			SourceMods.ensureNoGetCriteriaGroupsMethod(declarationWrapper);
			NormalAnnotationExpr typeSerializationAnnotation = SourceMods
					.ensureNormalTypeSerializationAnnotation(
							declarationWrapper);
			String initialSource = typeSerializationAnnotation.toString();
			NormalAnnotationExpr criteriaGroupsSerialization = null;
			ArrayInitializerExpr propertiesInitializerExpr = SourceMods
					.ensureValue(typeSerializationAnnotation, "properties",
							new ArrayInitializerExpr());
			propertiesInitializerExpr.getValues().stream()
					.collect(Collectors.toList()).forEach(Expression::remove);
			for (Expression e : propertiesInitializerExpr.getValues()) {
				NormalAnnotationExpr ann = (NormalAnnotationExpr) e;
				boolean criteriaGroupsAnnotation = ann.getPairs().stream()
						.anyMatch(pair -> pair.getName().toString()
								.equals("propertyName")
								&& pair.getValue().toString()
										.equals("criteriaGroups"));
				if (criteriaGroupsAnnotation) {
					criteriaGroupsSerialization = ann;
				}
			}
			if (criteriaGroupsSerialization == null) {
				criteriaGroupsSerialization = new NormalAnnotationExpr();
				criteriaGroupsSerialization
						.setName(PropertySerialization.class.getSimpleName());
				criteriaGroupsSerialization.addPair("name",
						new StringLiteralExpr("criteriaGroups"));
				criteriaGroupsSerialization.addPair("childTypes",
						"EntityCriteriaGroup.class");
				criteriaGroupsSerialization.addPair("grandchildTypes", "{}");
				propertiesInitializerExpr.getValues()
						.add(criteriaGroupsSerialization);
			}
			MemberValuePair grandchildTypesPair = criteriaGroupsSerialization
					.getPairs().stream().filter(mv -> mv.getName().asString()
							.equals("grandchildTypes"))
					.findFirst().get();
			Expression initialValue = grandchildTypesPair.getValue();
			ArrayInitializerExpr initializerExpr = new ArrayInitializerExpr();
			grandchildTypesPair.setValue(initializerExpr);
			List<ClassExpr> expressions = criterionHandlers.stream()
					.filter(dch -> dch.handlesSearchDefinition() == clazz)
					.map(dch -> {
						Class<? extends SearchCriterion> searchCriterion = dch
								.handlesSearchCriterion();
						declarationWrapper.ensureImport(searchCriterion);
						ClassOrInterfaceType type = StaticJavaParser
								.parseClassOrInterfaceType(
										searchCriterion.getSimpleName());
						return new ClassExpr(type);
					}).collect(Collectors.toList());
			initializerExpr.setValues(new NodeList(expressions));
			declarationWrapper.dirty(initialSource,
					typeSerializationAnnotation.toString());
			compUnits.declarations.values().stream()
					.filter(dec -> DomainCriterionHandler.class
							.isAssignableFrom(dec.clazz())
							&& !Modifier.isAbstract(dec.clazz().getModifiers()))
					.map(dec -> dec.<DomainCriterionHandler> typedInstance())
					.collect(Collectors.toList());
		});
	}

	@Override
	protected void performAction0(TaskFlatSerializerMetadata task)
			throws Exception {
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
		compUnits = CompilationUnits.load(cache, classPaths,
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

	<T> Class<? extends T> getHighestImplementor(Class<? extends T> clazz,
			Class<T> implementing) {
		Class<? extends T> cursor = clazz;
		while (cursor.getSuperclass() != null
				&& implementing.isAssignableFrom(cursor.getSuperclass())) {
			cursor = (Class<? extends T>) cursor.getSuperclass();
		}
		return cursor;
	}

	public enum Action {
		LIST_INTERESTING, ENUMERATE_SEARCH_DEFINITIONS, ENSURE_ANNOTATIONS,
		CREATE_TASK_HIERARCHY;
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
				declaration.setDeclaration(node);
				unit.declarations.add(declaration);
				if (declaration
						.isAssignableFrom(DomainCriterionHandler.class)) {
					declaration.setFlag(Type.DomainCriterionHandler);
				}
				if (declaration.isAssignableFrom(SearchCriterion.class)) {
					declaration.setFlag(Type.SearchCriterion);
				}
				if (declaration.isAssignableFrom(CriteriaGroup.class)) {
					declaration.setFlag(Type.CriteriaGroup);
				}
				if (declaration
						.isAssignableFrom(BindableSearchDefinition.class)) {
					declaration.setFlag(Type.BindableSearchDefinition);
				}
				if (declaration.isAssignableFrom(Task.class)) {
					declaration.setFlag(Type.Task);
				}
			}
			super.visit(node, arg);
		}
	}

	static class SourceMods {
		static Logger logger = LoggerFactory
				.getLogger(TaskFlatSerializerMetadata.class);

		public static void ensureNoGetCriteriaGroupsMethod(
				ClassOrInterfaceDeclarationWrapper declarationWrapper) {
			ClassOrInterfaceDeclaration declaration = declarationWrapper
					.getDeclaration();
			List<MethodDeclaration> methods = declaration
					.getMethodsByName("getCriteriaGroups");
			declarationWrapper.ensureImport(CriteriaGroup.class);
			declarationWrapper.ensureImport(Set.class);
			if (methods.size() > 0) {
				MethodDeclaration methodDeclaration = methods.get(0);
				if (methodDeclaration.toString().contains("")) {
					methodDeclaration.remove();
					logger.info("Removed getCriteria() for {}",
							declaration.getName());
					declarationWrapper.dirty();
				}
			}
		}

		public static NormalAnnotationExpr
				ensureNormalTypeSerializationAnnotation(
						ClassOrInterfaceDeclarationWrapper declarationWrapper) {
			Optional<AnnotationExpr> o_typeSerialization = declarationWrapper
					.getDeclaration()
					.getAnnotationByClass(TypeSerialization.class);
			if (o_typeSerialization.isPresent() && o_typeSerialization
					.get() instanceof SingleMemberAnnotationExpr) {
				o_typeSerialization.get().remove();
				o_typeSerialization = declarationWrapper.getDeclaration()
						.getAnnotationByClass(TypeSerialization.class);
			}
			if (!o_typeSerialization.isPresent()) {
				NormalAnnotationExpr annotationExpr = declarationWrapper
						.getDeclaration()
						.addAndGetAnnotation(TypeSerialization.class);
				declarationWrapper.ensureImport(TypeSerialization.class);
				declarationWrapper.ensureImport(PropertySerialization.class);
				annotationExpr.addPair("properties",
						new ArrayInitializerExpr());
				o_typeSerialization = declarationWrapper.getDeclaration()
						.getAnnotationByClass(TypeSerialization.class);
			}
			NormalAnnotationExpr typeSerialization = (NormalAnnotationExpr) o_typeSerialization
					.get();
			return typeSerialization;
		}

		public static SingleMemberAnnotationExpr
				ensureSingleMemberTypeSerializationAnnotation(
						ClassOrInterfaceDeclarationWrapper declarationWrapper) {
			Optional<AnnotationExpr> o_typeSerialization = declarationWrapper
					.getDeclaration()
					.getAnnotationByClass(TypeSerialization.class);
			if (!o_typeSerialization.isPresent()) {
				declarationWrapper.getDeclaration()
						.addAnnotation(new SingleMemberAnnotationExpr(
								new Name("TypeSerialization"),
								new StringLiteralExpr(
										declarationWrapper.simpleName())));
				declarationWrapper.ensureImport(TypeSerialization.class);
				o_typeSerialization = declarationWrapper.getDeclaration()
						.getAnnotationByClass(TypeSerialization.class);
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
	}

	enum Type implements TypeFlag {
		DomainCriterionHandler, SearchCriterion, CriteriaGroup,
		BindableSearchDefinition, Task
	}
}
