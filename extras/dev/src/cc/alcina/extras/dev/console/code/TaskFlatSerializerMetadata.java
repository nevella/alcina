package cc.alcina.extras.dev.console.code;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import cc.alcina.extras.dev.console.code.CompilationUnits.ClassOrInterfaceDeclarationWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapperVisitor;
import cc.alcina.extras.dev.console.code.CompilationUnits.TypeFlag;
import cc.alcina.framework.common.client.domain.search.DomainCriterionHandler;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.serializer.flat.PropertySerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.util.FsObjectCache;
import cc.alcina.framework.entity.util.PersistentObjectCache.SingletonCache;
import cc.alcina.framework.gwt.client.entity.search.BindableSearchDefinition;
import cc.alcina.framework.servlet.schedule.ServerTask;

public class TaskFlatSerializerMetadata
		extends ServerTask<TaskFlatSerializerMetadata> {
	static Logger logger = LoggerFactory
			.getLogger(TaskFlatSerializerMetadata.class);

	private boolean overwriteOriginals;

	private String classPathList;

	private CompilationUnits compUnits;

	private List<Class<? extends SearchDefinition>> searchDefinitions = new ArrayList<>();

	private boolean refresh;

	private Action action;

	private boolean test;

	public Action getAction() {
		return this.action;
	}

	public String getClassPathList() {
		return this.classPathList;
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

	public void setAction(Action action) {
		this.action = action;
	}

	public void setClassPathList(String classPathList) {
		this.classPathList = classPathList;
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

	private void enumerateSearchDefinitions() {
		compUnits.declarations.values().stream().filter(
				dec -> SearchDefinition.class.isAssignableFrom(dec.clazz()))
				.forEach(dec -> {
					Ax.out(dec.qualifiedSourceName);
				});
	}

	private void updateAnnotations() {
		List<DomainCriterionHandler> criterionHandlers = compUnits.declarations
				.values().stream()
				.filter(dec -> DomainCriterionHandler.class
						.isAssignableFrom(dec.clazz())
						&& !Modifier.isAbstract(dec.clazz().getModifiers()))
				.map(dec -> dec.<DomainCriterionHandler> typedInstance())
				.collect(Collectors.toList());
		getSearchDefinitions().forEach(clazz -> {
			ClassOrInterfaceDeclarationWrapper declarationWrapper = compUnits.declarations
					.get(clazz.getName());
			/*
			 * ensure getCriteriaGroups method
			 */
			MethodDeclaration methodDeclaration = SourceMods
					.ensureGetCriteriaGroupsMethod(declarationWrapper);
			NormalAnnotationExpr propertySerialization = (NormalAnnotationExpr) methodDeclaration
					.getAnnotationByClass(PropertySerialization.class).get();
			MemberValuePair grandchildTypesPair = propertySerialization
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
			if (initialValue.toString().equals(initializerExpr.toString())) {
			} else {
				declarationWrapper.dirty();
			}
		});
		compUnits.writeDirty(isTest());
	}

	@Override
	protected void performAction0(TaskFlatSerializerMetadata task)
			throws Exception {
		StringMap classPaths = StringMap.fromStringList(classPathList);
		SingletonCache<CompilationUnits> cache = FsObjectCache
				.singletonCache(CompilationUnits.class, getClass())
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
		case UPDATE_ANNOTATIONS:
			updateAnnotations();
			break;
		}
	}

	public enum Action {
		LIST_INTERESTING, ENUMERATE_SEARCH_DEFINITIONS, UPDATE_ANNOTATIONS;
	}

	static class DeclarationVisitor extends CompilationUnitWrapperVisitor {
		public DeclarationVisitor(CompilationUnits units,
				CompilationUnitWrapper compUnit) {
			super(units, compUnit);
		}

		@Override
		public void visit(ClassOrInterfaceDeclaration node, Void arg) {
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
			}
			super.visit(node, arg);
		}
	}

	static class SourceMods {
		public static MethodDeclaration ensureGetCriteriaGroupsMethod(
				ClassOrInterfaceDeclarationWrapper declarationWrapper) {
			ClassOrInterfaceDeclaration declaration = declarationWrapper
					.getDeclaration();
			List<MethodDeclaration> methods = declaration
					.getMethodsByName("getCriteriaGroups");
			if (methods.size() > 0) {
				return methods.get(0);
			}
			MethodDeclaration methodDeclaration = declaration
					.addMethod("getCriteriaGroups", Keyword.PUBLIC);
			methodDeclaration.setType("Set<CriteriaGroup>");
			NormalAnnotationExpr annotationExpr = methodDeclaration
					.addAndGetAnnotation(PropertySerialization.class);
			declarationWrapper.ensureImport(PropertySerialization.class);
			annotationExpr.addPair("defaultProperty", "true");
			annotationExpr.addPair("childTypes", "EntityCriteriaGroup.class");
			annotationExpr.addPair("grandchildTypes", "{}");
			BlockStmt body = new BlockStmt();
			body.addAndGetStatement("return super.getCriteriaGroups();");
			methodDeclaration.setBody(body);
			logger.info("Created getCriteria() for {}", declaration.getName());
			declarationWrapper.dirty();
			return methodDeclaration;
		}
	}

	enum Type implements TypeFlag {
		DomainCriterionHandler, SearchCriterion, CriteriaGroup,
		BindableSearchDefinition
	}
}
