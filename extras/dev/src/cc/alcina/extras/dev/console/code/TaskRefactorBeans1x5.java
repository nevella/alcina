package cc.alcina.extras.dev.console.code;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.TypeParameter;
import com.google.common.base.Preconditions;

import cc.alcina.extras.dev.console.code.CompilationUnits.ClassOrInterfaceDeclarationWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapperVisitor;
import cc.alcina.extras.dev.console.code.CompilationUnits.TypeFlag;
import cc.alcina.framework.common.client.domain.search.DomainCriterionHandler;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.util.FsObjectCache;
import cc.alcina.framework.entity.util.PersistentObjectCache.SingletonCache;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;
import cc.alcina.framework.servlet.schedule.PerformerTask;

/*

 * @formatter:off
 *

Plan (impl):
Property:
- Add field support
  - can serialize this task?
- @Bean resolver (since we want tree serializable) 
- 


Tool:
- Find beans (@bean annotation)
- Find beanlike (getter/setter but no @bean)(exclude tasks)
- Fix signature generation tasks


 *
 *@formatter:on
 *
 */
@Bean.Fields
public class TaskRefactorBeans1x5 extends PerformerTask {
	transient CompilationUnits compUnits;

	public boolean overwriteOriginals;

	public String classPathList;

	public boolean refresh;

	public Action action;

	public boolean test;

	public Class onlyAssignableFrom;

	public String classNameFilter;

	public void removeFilter0FilterMethod(
			ClassOrInterfaceDeclarationWrapper declarationWrapper,
			Type... types) {
		ClassOrInterfaceDeclaration decl = declarationWrapper.getDeclaration();
		List<MethodDeclaration> methods = decl.getMethods();
		for (MethodDeclaration method : methods) {
			for (Type type : types) {
				switch (type) {
				case DomainCriterionHandler:
					if (method.getNameAsString().equals("getFilter")
							&& method.getType().toString()
									.contains("DomainFilter")
							&& method.toString()
									.contains("return getFilter0(sc);")) {
						declarationWrapper.dirty();
						method.remove();
					}
					break;
				default:
					throw new UnsupportedOperationException();
				}
			}
		}
	}

	public void removeHandlesMethod(
			ClassOrInterfaceDeclarationWrapper declarationWrapper,
			Type... types) {
		ClassOrInterfaceDeclaration decl = declarationWrapper.getDeclaration();
		List<MethodDeclaration> methods = decl.getMethods();
		for (MethodDeclaration method : methods) {
			for (Type type : types) {
				switch (type) {
				case DomainCriterionHandler:
					if (method.getNameAsString()
							.equals("handlesSearchCriterion")
							&& !method.isFinal() && !decl.getNameAsString()
									.equals("DomainCriterionHandler")) {
						declarationWrapper.dirty();
						method.remove();
					}
					break;
				case BasePlaceTokenizer:
					if (method.getNameAsString().equals("getTokenizedClass")
							&& !decl.getNameAsString().matches(
									"(BasePlaceTokenizer|BindablePlaceTokenizer)")) {
						declarationWrapper.dirty();
						method.remove();
					}
					break;
				default:
					throw new UnsupportedOperationException();
				}
			}
		}
	}

	@Override
	public void run() throws Exception {
		StringMap classPaths = StringMap.fromStringList(classPathList);
		SingletonCache<CompilationUnits> cache = FsObjectCache
				.singletonCache(CompilationUnits.class, getClass())
				.asSingletonCache();
		compUnits = CompilationUnits.load(cache, classPaths.keySet(),
				DeclarationVisitor::new, refresh);
		switch (action) {
		case LIST_INTERESTING: {
			compUnits.declarations.values().stream()
					.filter(dec -> dec.hasFlags()).filter(this::assignableFrom)
					.forEach(dec -> Ax.out("%s - %s",
							dec.clazz().getSimpleName(), dec.typeFlags));
			break;
		}
		case LIST_TWO_KEY_ROOTS: {
			Stream<Stream<Class<?>>> map = compUnits.declarations.values()
					.stream()
					.filter(dec -> dec.hasFlag(Type.TwoKeyRegistration))
					.map(ClassOrInterfaceDeclarationWrapper::rootTypes);
			map.flatMap(Function.identity()).distinct().map(Class::getName)
					.sorted().forEach(Ax::out);
			break;
		}
		case CLEAN_HANDLERS: {
			compUnits.declarations.values().stream()
					.filter(dec -> dec.hasFlag(Type.DomainCriterionHandler))
					.forEach(dec -> this.removeHandlesMethod(dec,
							Type.DomainCriterionHandler));
			// rearrange class hierarchy so logic interfaces can override
			// filter()
			compUnits.declarations.values().stream()
					.filter(dec -> dec.hasFlag(Type.DomainCriterionHandler))
					.forEach(dec -> this.removeFilter0FilterMethod(dec,
							Type.DomainCriterionHandler));
			break;
		}
		case CLEAN_TOKENIZERS: {
			compUnits.declarations.values().stream()
					.filter(dec -> dec.hasFlag(Type.BasePlaceTokenizer))
					.forEach(dec -> this.removeHandlesMethod(dec,
							Type.BasePlaceTokenizer));
			break;
		}
		case UPDATE_TWO_KEY_ANNOTATIONS: {
			Preconditions.checkArgument(onlyAssignableFrom != null);
			updateTwoKeyAnnotations();
			break;
		}
		}
		compUnits.writeDirty(test);
	}

	public void updateTwoKeyAnnotations() {
		compUnits.declarations.values().stream()
				.filter(dec -> dec.hasFlag(Type.TwoKeyRegistration))
				.filter(this::assignableFrom).forEach(dec -> SourceMods
						.removeRedundantRegistrationAnnotation(dec));
	}

	boolean assignableFrom(
			ClassOrInterfaceDeclarationWrapper declarationWrapper) {
		if (onlyAssignableFrom != null) {
			return onlyAssignableFrom
					.isAssignableFrom(declarationWrapper.clazz());
		} else {
			return true;
		}
	}

	public enum Action {
		LIST_INTERESTING, UPDATE_TWO_KEY_ANNOTATIONS, LIST_TWO_KEY_ROOTS,
		CLEAN_HANDLERS, CLEAN_TOKENIZERS;
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

		boolean hasTwoKeyAnnotation(NodeWithAnnotations<?> decl) {
			if (decl.isAnnotationPresent(Registration.class)) {
				AnnotationExpr annotationExpr = decl
						.getAnnotationByClass(Registration.class).get();
				Expression value = null;
				if (annotationExpr instanceof SingleMemberAnnotationExpr) {
					value = ((SingleMemberAnnotationExpr) annotationExpr)
							.getMemberValue();
				} else if (annotationExpr instanceof NormalAnnotationExpr) {
					value = ((NormalAnnotationExpr) annotationExpr).getPairs()
							.stream()
							.filter(p -> p.getNameAsString().equals("value"))
							.findFirst().map(MemberValuePair::getValue)
							.orElse(null);
				}
				if (value != null && value instanceof ArrayInitializerExpr) {
					ArrayInitializerExpr arrayInitializerExpr = (ArrayInitializerExpr) value;
					if (arrayInitializerExpr.getValues().size() == 2) {
						return true;
					}
				}
			}
			return false;
		}

		boolean isBasePlaceTokenizer(
				ClassOrInterfaceDeclarationWrapper declaration) {
			try {
				return declaration.isAssignableFrom(BasePlaceTokenizer.class);
			} catch (Exception e) {
				Ax.simpleExceptionOut(e);
				return false;
			}
		}

		boolean isDomainStoreHandler(
				ClassOrInterfaceDeclarationWrapper declaration) {
			try {
				return declaration
						.isAssignableFrom(DomainCriterionHandler.class);
			} catch (Exception e) {
				Ax.simpleExceptionOut(e);
				return false;
			}
		}

		void visit0(ClassOrInterfaceDeclaration node, Void arg) {
			if (!node.isInterface()) {
				CompilationUnits.ClassOrInterfaceDeclarationWrapper declaration = new CompilationUnits.ClassOrInterfaceDeclarationWrapper(
						unit, node);
				String nameAsString = declaration.getDeclaration()
						.getNameAsString();
				declaration.setDeclaration(node);
				unit.declarations.add(declaration);
				if (hasTwoKeyAnnotation(node)) {
					declaration.setFlag(Type.TwoKeyRegistration);
				}
				if (isDomainStoreHandler(declaration)) {
					declaration.setFlag(Type.DomainCriterionHandler);
				}
				if (isBasePlaceTokenizer(declaration)) {
					declaration.setFlag(Type.BasePlaceTokenizer);
				}
			}
			super.visit(node, arg);
		}
	}

	static class SourceMods {
		static Logger logger = LoggerFactory
				.getLogger(TaskFlatSerializerMetadata.class);

		static void removeRedundantRegistrationAnnotation(
				ClassOrInterfaceDeclarationWrapper declarationWrapper) {
			ClassOrInterfaceDeclaration declaration = declarationWrapper
					.getDeclaration();
			if (declaration.isInterface()) {
				return;
			}
			NodeList<TypeParameter> typeParameters = declaration
					.getTypeParameters();
			Optional<AnnotationExpr> annotation = declaration
					.getAnnotationByClass(Registration.class);
			// either extends a generic type, or a single generic interface
			boolean hasSingleGenericParameterParent = false;
			if (typeParameters.size() == 0) {
				if (declaration.getExtendedTypes().size() == 1) {
					ClassOrInterfaceType extendedType = declaration
							.getExtendedTypes().get(0);
					Optional<NodeList<com.github.javaparser.ast.type.Type>> typeArguments = extendedType
							.getTypeArguments();
					if (typeArguments.isPresent()
							&& typeArguments.get().size() == 1) {
						hasSingleGenericParameterParent = true;
					}
				} else {
					hasSingleGenericParameterParent = declaration
							.getImplementedTypes().stream().filter(t -> {
								Optional<NodeList<com.github.javaparser.ast.type.Type>> typeArguments = t
										.getTypeArguments();
								return typeArguments.isPresent()
										&& typeArguments.get().size() == 1;
							}).count() == 1;
				}
			}
			if (annotation.isPresent()) {
				if (hasSingleGenericParameterParent) {
					declarationWrapper.dirty();
					annotation.get().remove();
				} else {
					Ax.out("Must add generic: %s", declaration);
				}
			}
		}
	}

	enum Type implements TypeFlag {
		TwoKeyRegistration, DomainCriterionHandler, BasePlaceTokenizer
	}
}
