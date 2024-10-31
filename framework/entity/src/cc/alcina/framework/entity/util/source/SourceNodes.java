package cc.alcina.framework.entity.util.source;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.SEUtilities;

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

	public static SourceMethod getMethod(Class<?> clazz, String methodName,
			List<Class> argumentTypes) {
		return get().getMethod0(clazz, methodName, argumentTypes);
	}

	SourceMethod getMethod0(Class<?> clazz, String methodName,
			List<Class> argumentTypes) {
		return new SourceMethod(clazz, methodName, argumentTypes);
	}

	public class SourceMethod {
		MethodDeclaration methodDeclaration;

		public List<String> argumentNames;

		List<Class> argumentTypes;

		SourceMethod(Class<?> clazz, String methodName,
				List<Class> argumentTypes) {
			if (argumentTypes.isEmpty()) {
				// can either indicate 'no-args' or 'sole method with that name'
				List<MethodDeclaration> namedMethods = getDeclaration(clazz)
						.get().getMethodsByName(methodName);
				if (namedMethods.size() == 1) {
					methodDeclaration = namedMethods.get(0);
					Method method = SEUtilities.allClassMethods(clazz).stream()
							.filter(m -> m.getName().equals(methodName))
							.findFirst().get();
					argumentTypes = List.of(method.getParameterTypes());
				}
			}
			this.argumentTypes = argumentTypes;
			String[] argumentTypeNames = this.argumentTypes.stream()
					.map(Class::getSimpleName).toArray(len -> new String[len]);
			List<String> argumentTypeNameList = Arrays
					.asList(argumentTypeNames);
			if (methodDeclaration == null) {
				methodDeclaration = getDeclaration(clazz).get()
						.getMethodsBySignature(methodName, argumentTypeNames)
						.get(0);
			}
			argumentNames = methodDeclaration.getParameters().stream()
					.map(p -> p.getName().toString())
					.collect(Collectors.toList());
		}

		public String getBody() {
			BlockComment blockComment = (BlockComment) methodDeclaration
					.getChildNodes().stream()
					.filter(n -> n instanceof BlockComment).findFirst().get();
			String boundaryPattern = "(?s)/\\*-\\{(.+)\\}-\\*/\\s*";
			String jni = blockComment.toString();
			Matcher matcher = Pattern.compile(boundaryPattern).matcher(jni);
			matcher.matches();
			String js = matcher.group(1);
			// very quick+dirty+incorrect check there are no JSNI/Java refs
			Preconditions.checkArgument(!js.contains("@"));
			return js;
		}

		public List<Class> getArgumentTypes() {
			return argumentTypes;
		}
	}
}
