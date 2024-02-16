package cc.alcina.extras.dev.console.code;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.google.common.base.Preconditions;

import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapperVisitor;
import cc.alcina.extras.dev.console.code.CompilationUnits.TypeFlag;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.util.FsObjectCache;
import cc.alcina.framework.entity.util.PersistentObjectCache.SingletonCache;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskRefactorDisplayName extends PerformerTask {
	private boolean overwriteOriginals;

	private String classPathList;

	private transient CompilationUnits compUnits;

	private boolean refresh;

	private Action action;

	private boolean test;

	private void ensureAnnotations() {
		compUnits.declarations.values().stream()
				.filter(dec -> dec.hasFlag(Type.DisplayAnnotations))
				.forEach(dec -> SourceMods.cleanDisplayAnnotations(dec));
		compUnits.writeDirty(isTest());
	}

	public Action getAction() {
		return this.action;
	}

	public String getClassPathList() {
		return this.classPathList;
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
		compUnits = CompilationUnits.load(cache, classPaths.keySet(),
				DeclarationVisitor::new, isRefresh());
		switch (getAction()) {
		case LIST_INTERESTING: {
			compUnits.declarations.values().stream()
					.filter(dec -> dec.hasFlag(Type.DisplayAnnotations))
					.forEach(dec -> Ax.out("%s - %s",
							dec.clazz().getSimpleName(), dec.typeFlags));
			break;
		}
		case UPDATE_ANNOTATIONS: {
			ensureAnnotations();
			break;
		}
		}
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

	public void setTest(boolean test) {
		this.test = test;
	}

	public enum Action {
		LIST_INTERESTING, UPDATE_ANNOTATIONS;
	}

	static class DeclarationVisitor extends CompilationUnitWrapperVisitor {
		public DeclarationVisitor(CompilationUnits units,
				CompilationUnitWrapper compUnit) {
			super(units, compUnit);
		}

		boolean hasDisplayAnnotation(NodeWithAnnotations decl) {
			return decl.isAnnotationPresent(Display.class);
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
				UnitType type = new UnitType(
						unit, node);
				type.setDeclaration(node);
				unit.declarations.add(type);
				boolean hasDisplayAnnotation = hasDisplayAnnotation(node)
						|| node.getMethods().stream()
								.anyMatch(this::hasDisplayAnnotation);
				if (hasDisplayAnnotation) {
					type.setFlag(Type.DisplayAnnotations);
				}
			}
			super.visit(node, arg);
		}
	}

	static class SourceMods {
		static Logger logger = LoggerFactory
				.getLogger(TaskFlatSerializerMetadata.class);

		private static void clean(
				UnitType type,
				Optional<AnnotationExpr> annotation,
				MethodDeclaration methodDeclaration) {
			if (!annotation.isPresent()) {
				return;
			}
			AnnotationExpr expr = annotation.get();
			if (!(expr instanceof NormalAnnotationExpr)) {
				return;
			}
			NormalAnnotationExpr normalExpr = (NormalAnnotationExpr) expr;
			Optional<MemberValuePair> namePair = normalExpr.getPairs().stream()
					.filter(p -> p.getName().toString().equals("name"))
					.findFirst();
			if (!namePair.isPresent()) {
				return;
			}
			MemberValuePair pair = namePair.get();
			Expression valueExpr = pair.getValue();
			if (!(valueExpr instanceof StringLiteralExpr)) {
				return;
			}
			StringLiteralExpr stringValueExpr = (StringLiteralExpr) valueExpr;
			String string = stringValueExpr.getValue();
			String methodName = methodDeclaration.getNameAsString();
			String regex = "(get|is)([A-Z])(.+)";
			Preconditions.checkArgument(methodName.matches(regex));
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(methodName);
			matcher.matches();
			String propertyName = matcher.group(2).toLowerCase()
					+ matcher.group(3);
			String defaultName = CommonUtils.deInfix(propertyName);
			if (Objects.equals(defaultName, string)) {
				type.dirty();
				normalExpr.remove(pair);
				if (normalExpr.getChildNodes().isEmpty()) {
					normalExpr.getParentNode().get().remove(normalExpr);
					methodDeclaration.addAndGetAnnotation(Display.class);
				}
			}
		}

		public static void cleanDisplayAnnotations(
				UnitType type) {
			ClassOrInterfaceDeclaration declaration = type
					.getDeclaration();
			{
				Optional<AnnotationExpr> annotation = declaration
						.getAnnotationByClass(Display.class);
				// only applicable for on-method anns
				// clean(type, annotation);
			}
			declaration.getMethods().forEach(m -> {
				Optional<AnnotationExpr> annotation = m
						.getAnnotationByClass(Display.class);
				clean(type, annotation, m);
			});
			// List<MethodDeclaration> methods = declaration
			// .getMethodsByName("getCriteriaGroups");
			// type.ensureImport(CriteriaGroup.class);
			// type.ensureImport(Set.class);
			// if (methods.size() > 0) {
			// MethodDeclaration methodDeclaration = methods.get(0);
			// if (methodDeclaration.toString().contains("")) {
			// methodDeclaration.remove();
			// logger.info("Removed getCriteria() for {}",
			// declaration.getName());
			// type.dirty();
			// }
			// }
		}
	}

	enum Type implements TypeFlag {
		DisplayAnnotations
	}
}
