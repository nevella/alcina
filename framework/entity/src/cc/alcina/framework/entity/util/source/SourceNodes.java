package cc.alcina.framework.entity.util.source;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration.Singleton
public class SourceNodes {
	public static SourceNodes get() {
		return Registry.impl(SourceNodes.class);
	}

	public static Optional<JavadocComment> getTypeJavadoc(Class<?> clazz) {
		return get().getTypeJavadoc0(clazz);
	}

	Map<Class, CompilationUnit> units = new LinkedHashMap<>();

	synchronized CompilationUnit ensureUnit(Class<?> clazz) {
		return units.computeIfAbsent(clazz, c -> {
			try {
				String source = SourceFinder.findSource(c);
				CompilationUnit compilationUnit = StaticJavaParser
						.parse(source);
				return compilationUnit;
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		});
	}

	Optional<ClassOrInterfaceDeclaration> getDeclaration(Class<?> clazz) {
		CompilationUnit compilationUnit = ensureUnit(clazz);
		List<ClassOrInterfaceDeclaration> childNodesByType = compilationUnit
				.findAll(ClassOrInterfaceDeclaration.class);
		Optional<ClassOrInterfaceDeclaration> declaration = childNodesByType
				.stream()
				.filter(n -> n.getNameAsString().equals(clazz.getSimpleName()))
				.findFirst();
		return declaration;
	}

	Optional<JavadocComment> getTypeJavadoc0(Class<?> clazz) {
		Optional<ClassOrInterfaceDeclaration> declaration = getDeclaration(
				clazz);
		if (declaration.isPresent()) {
			ClassOrInterfaceDeclaration decl = declaration.get();
			Optional<JavadocComment> javadocComment = decl.getJavadocComment();
			/*
			 * the comment may (?) be rendered as a node *following* the type
			 * declaration, if there's also a non-javadoc comment (yup,
			 * limitation in the JavaParser model - should support multiple
			 * comments I guess)
			 */
			if (javadocComment.isEmpty()) {
				Node parent = decl.getParentNode().get();
				List<Node> children = parent.getChildNodes();
				int idx = children.indexOf(decl);
				if (idx + 1 < children.size()) {
					Node test = children.get(idx + 1);
					if (test instanceof JavadocComment) {
						javadocComment = Optional.of((JavadocComment) test);
					}
				}
			}
			return javadocComment;
		} else {
			return Optional.empty();
		}
	}

	public static String getMethodBody(Class<?> clazz, String methodName) {
		return get().getMethodBody0(clazz, methodName);
	}

	String getMethodBody0(Class<?> clazz, String methodName) {
		MethodDeclaration methodDeclaration = getDeclaration(clazz).get()
				.getMethodsByName(methodName).get(0);
		BlockComment blockComment = (BlockComment) methodDeclaration
				.getChildNodes().stream().filter(n -> n instanceof BlockComment)
				.findFirst().get();
		String boundaryPattern = "(?s)/\\*-\\{(.+)\\}-\\*/";
		String jni = blockComment.toString();
		Matcher matcher = Pattern.compile(boundaryPattern).matcher(jni);
		matcher.matches();
		return matcher.group(1);
	}
}
