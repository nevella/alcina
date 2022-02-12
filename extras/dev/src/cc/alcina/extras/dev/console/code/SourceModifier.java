package cc.alcina.extras.dev.console.code;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;

import cc.alcina.extras.dev.console.code.CompilationUnits.ClassOrInterfaceDeclarationWrapper;

abstract class SourceModifier {
	protected ClassOrInterfaceDeclarationWrapper declarationWrapper;

	protected ClassOrInterfaceDeclaration declaration;

	private String initialSource;

	public SourceModifier(
			ClassOrInterfaceDeclarationWrapper declarationWrapper) {
		this.declarationWrapper = declarationWrapper;
		this.declaration = this.declarationWrapper.getDeclaration();
	}

	protected void clear(ArrayInitializerExpr expr) {
		expr.getValues().stream().collect(Collectors.toList())
				.forEach(Expression::remove);
	}

	protected abstract void ensureImports();

	protected NormalAnnotationExpr ensureNormalAnnotation(
			NodeWithAnnotations node,
			Class<? extends Annotation> annotationClass) {
		Optional<AnnotationExpr> annotationExpr = node
				.getAnnotationByClass(annotationClass);
		if (annotationExpr.isPresent()
				&& annotationExpr.get() instanceof SingleMemberAnnotationExpr) {
			annotationExpr.get().remove();
			annotationExpr = node.getAnnotationByClass(annotationClass);
		}
		if (!annotationExpr.isPresent()) {
			annotationExpr = Optional
					.of(node.addAndGetAnnotation(annotationClass));
		}
		return (NormalAnnotationExpr) annotationExpr.get();
	}

	protected <E extends Expression> E ensureValue(
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

	protected void modify() {
		initialSource = declaration.toString();
		ensureImports();
		modify0();
		declarationWrapper.dirty(initialSource, declaration.toString());
	}

	protected abstract void modify0();
}