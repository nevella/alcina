package cc.alcina.extras.dev.console.code;

import java.util.Optional;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.google.common.base.Preconditions;

import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapperVisitor;
import cc.alcina.extras.dev.console.code.CompilationUnits.TypeFlag;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedNameProvider;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.util.FsObjectCache;
import cc.alcina.framework.entity.util.PersistentObjectCache.SingletonCache;
import cc.alcina.framework.servlet.schedule.PerformerTask;

/**
 * <p>
 * Refactor Directed.receives - remove (as unneeded), except in the case of
 * receives/reemits (in which case merge the two)
 */
@Bean(PropertySource.FIELDS)
// (tmp)
@TypeSerialization(reflectiveSerializable = false, flatSerializable = false)
public class TaskRefactorDirectedReceives extends PerformerTask {
	transient CompilationUnits compUnits;

	public boolean overwriteOriginals;

	public String classPathList;

	public boolean refresh;

	public Action action;

	public boolean test;

	@Override
	public void run() throws Exception {
		StringMap classPaths = StringMap.fromStringList(classPathList);
		SingletonCache<CompilationUnits> cache = FsObjectCache
				.singletonCache(CompilationUnits.class, getClass())
				.asSingletonCache();
		compUnits = CompilationUnits.load(cache, classPaths.keySet(),
				DeclarationVisitor::new, refresh);
		compUnits.declarations.values().stream().filter(dec -> dec.hasFlags())
				.forEach(type -> {
					switch (action) {
					case LIST_INTERESTING: {
						Ax.out("%s - %s", type.clazz().getSimpleName(),
								type.typeFlags);
						break;
					}
					case MANIFEST: {
						remove(type);
						break;
					}
					}
				});
		compUnits.writeDirty(test);
	}

	void remove(UnitType type) {
		Ax.out("Removing receives: %s", NestedNameProvider.get(type.clazz()));
		type.dirty();
		MemberValuePair receviesPair = null;
		type.getDeclaration().stream()
				.filter(n -> n instanceof NormalAnnotationExpr)
				.map(n -> (NormalAnnotationExpr) n)
				.filter(n -> UnitType.findContainingClassOrInterfaceDeclaration(
						n) == type.getDeclaration())
				.filter(n -> n.getNameAsString().equals("Directed"))
				.forEach(this::removeFromAnnotation);
	}

	void removeFromAnnotation(NormalAnnotationExpr expr) {
		MemberValuePair receviesPair;
		Optional<MemberValuePair> o_receivesPair = expr.getPairs().stream()
				.filter(mv -> mv.getNameAsString().equals("receives"))
				.findFirst();
		if (!o_receivesPair.isPresent()) {
			return;
		}
		receviesPair = o_receivesPair.get();
		Optional<MemberValuePair> o_reemitsPair = expr.getPairs().stream()
				.filter(mv -> mv.getNameAsString().equals("reemits"))
				.findFirst();
		if (o_reemitsPair.isPresent()) {
			Expression receives = receviesPair.getValue();
			MemberValuePair reemitsPair = o_reemitsPair.get();
			Expression reemits = reemitsPair.getValue();
			NodeList<Expression> values = new NodeList<>();
			if (receives instanceof ClassExpr) {
				Preconditions.checkState(reemits instanceof ClassExpr);
				values.add(receives);
				values.add(reemits);
			} else {
				ArrayInitializerExpr receivesExpr = (ArrayInitializerExpr) receives;
				ArrayInitializerExpr reemitsExpr = (ArrayInitializerExpr) reemits;
				Preconditions.checkState(receivesExpr.getChildNodes()
						.size() == reemitsExpr.getChildNodes().size());
				for (int idx = 0; idx < receivesExpr.getChildNodes()
						.size(); idx++) {
					values.add(
							(Expression) receivesExpr.getChildNodes().get(idx));
					values.add(
							(Expression) reemitsExpr.getChildNodes().get(idx));
				}
			}
			ArrayInitializerExpr out = new ArrayInitializerExpr();
			out.setValues(values);
			reemitsPair.setValue(out);
		}
		receviesPair.remove();
		if (expr.getPairs().isEmpty()) {
			MarkerAnnotationExpr markerAnnotationExpr = new MarkerAnnotationExpr(
					"Directed");
			expr.replace(markerAnnotationExpr);
		}
	}

	public enum Action {
		LIST_INTERESTING, MANIFEST;
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

		@Override
		public void visit(MarkerAnnotationExpr n, Void arg) {
			visitAnnotationExpr(n);
		}

		@Override
		public void visit(NormalAnnotationExpr n, Void arg) {
			visitAnnotationExpr(n);
		}

		@Override
		public void visit(SingleMemberAnnotationExpr n, Void arg) {
			visitAnnotationExpr(n);
		}

		boolean hasDirectedReceives(AnnotationExpr expr) {
			if (expr.getNameAsString().equals("Directed")) {
				if (expr instanceof NormalAnnotationExpr) {
					NormalAnnotationExpr expr2 = (NormalAnnotationExpr) expr;
					Optional<MemberValuePair> receivesPair = expr2
							.getPairs().stream().filter(mv -> mv
									.getNameAsString().equals("receives"))
							.findFirst();
					if (receivesPair.isPresent()) {
						return true;
					}
				}
			}
			return false;
		}

		void visit0(ClassOrInterfaceDeclaration node, Void arg) {
			UnitType type = new UnitType(unit, node);
			type.setDeclaration(node);
			unit.declarations.add(type);
			super.visit(node, arg);
		};

		void visitAnnotationExpr(AnnotationExpr n) {
			if (hasDirectedReceives(n)) {
				ClassOrInterfaceDeclaration declaration = UnitType
						.findContainingClassOrInterfaceDeclaration(n);
				UnitType type = unit.typeFor(declaration);
				type.setFlag(Type.HasBeanAnnotation);
			}
		}
	}

	enum Type implements TypeFlag {
		HasBeanAnnotation
	}
}
