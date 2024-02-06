package cc.alcina.extras.dev.console.code;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;

import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapperVisitor;
import cc.alcina.extras.dev.console.code.CompilationUnits.TypeFlag;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.util.FsObjectCache;
import cc.alcina.framework.entity.util.PersistentObjectCache.SingletonCache;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskRefactorPropertySerialization extends PerformerTask {
	private boolean overwriteOriginals;

	private String classPathList;

	private transient CompilationUnits compUnits;

	private boolean refresh;

	private Action action;

	private boolean test;

	private void ensureAnnotations() {
		compUnits.declarations.values().stream().filter(
				dec -> dec.hasFlag(Type.PropertySerializationAnnotation))
				.forEach(dec -> SourceMods
						.removeRedundantPropertySerializationAnnotations(dec));
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
			compUnits.declarations.values().stream().filter(
					dec -> dec.hasFlag(Type.PropertySerializationAnnotation))
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

		boolean hasAnnotation(NodeWithAnnotations decl) {
			return decl.isAnnotationPresent(PropertySerialization.class);
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
				boolean hasAnnotation = node.getMethods().stream()
						.anyMatch(this::hasAnnotation);
				if (hasAnnotation) {
					type.setFlag(Type.PropertySerializationAnnotation);
				}
			}
			super.visit(node, arg);
		}
	}

	static class SourceMods {
		static Logger logger = LoggerFactory
				.getLogger(TaskFlatSerializerMetadata.class);

		private static void cleanIfRedundant(
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
			NodeList<MemberValuePair> pairs = normalExpr.getPairs();
			if (pairs.size() != 1) {
				// not a simple 'serialize collection like this' annotation
				return;
			}
			Optional<MemberValuePair> namePair = pairs.stream()
					.filter(p -> p.getName().toString().equals("types"))
					.findFirst();
			if (!namePair.isPresent()) {
				return;
			}
			MemberValuePair pair = namePair.get();
			Expression valueExpr = pair.getValue();
			ClassExpr classExpr = null;
			if (valueExpr instanceof ClassExpr) {
				classExpr = (ClassExpr) valueExpr;
			} else {
				ArrayInitializerExpr arrayInitializerExpr = (ArrayInitializerExpr) valueExpr;
				if (arrayInitializerExpr.getChildNodes().size() == 1) {
					classExpr = (ClassExpr) arrayInitializerExpr.getChildNodes()
							.get(0);
				}
			}
			if (classExpr == null) {
				// multiple types
				return;
			}
			type.dirty();
			expr.remove();
		}

		public static void removeRedundantPropertySerializationAnnotations(
				UnitType type) {
			ClassOrInterfaceDeclaration declaration = type
					.getDeclaration();
			declaration.getMethods().forEach(m -> {
				Optional<AnnotationExpr> annotation = m
						.getAnnotationByClass(PropertySerialization.class);
				cleanIfRedundant(type, annotation, m);
			});
		}
	}

	enum Type implements TypeFlag {
		PropertySerializationAnnotation
	}
}
