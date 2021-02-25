package cc.alcina.extras.dev.console.code;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapperVisitor;
import cc.alcina.extras.dev.console.code.CompilationUnits.TypeFlag;
import cc.alcina.framework.common.client.domain.search.DomainCriterionHandler;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.util.FsObjectCache;
import cc.alcina.framework.entity.util.PersistentObjectCache.SingletonCache;
import cc.alcina.framework.gwt.client.entity.search.BindableSearchDefinition;
import cc.alcina.framework.servlet.schedule.ServerTask;

public class TaskFlatSerializerMetadata extends ServerTask {
	private boolean overwriteOriginals;

	private String classPathList;

	private CompilationUnits compUnits;

	public String getClassPathList() {
		return this.classPathList;
	}

	public boolean isOverwriteOriginals() {
		return this.overwriteOriginals;
	}

	public void setClassPathList(String classPathList) {
		this.classPathList = classPathList;
	}

	public void setOverwriteOriginals(boolean overwriteOriginals) {
		this.overwriteOriginals = overwriteOriginals;
	}

	@Override
	protected void performAction0(Task task) throws Exception {
		StringMap classPaths = StringMap.fromStringList(classPathList);
		SingletonCache<CompilationUnits> cache = FsObjectCache
				.singletonCache(CompilationUnits.class, getClass())
				.asSingletonCache();
		compUnits = CompilationUnits.load(cache, classPaths,
				DeclarationVisitor::new, true);
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
				declaration.declaration = node;
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

	enum Type implements TypeFlag {
		DomainCriterionHandler, SearchCriterion, CriteriaGroup,
		BindableSearchDefinition
	}
}
