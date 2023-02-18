package cc.alcina.extras.dev.console.code;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;

import cc.alcina.extras.dev.console.code.CompilationUnits.ClassOrInterfaceDeclarationWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapperVisitor;
import cc.alcina.extras.dev.console.code.CompilationUnits.TypeFlag;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.Implementation;
import cc.alcina.framework.common.client.logic.reflection.Registration.Priority;
import cc.alcina.framework.common.client.logic.reflection.Registrations;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.FsObjectCache;
import cc.alcina.framework.entity.util.PersistentObjectCache.SingletonCache;
import cc.alcina.framework.servlet.schedule.ServerTask;

public class TaskRefactorRegistrations
		extends ServerTask {
	private boolean overwriteOriginals;

	private String classPathList;

	private transient CompilationUnits compUnits;

	private boolean refresh;

	private Action action;

	private boolean test;

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

	private void ensureRegistrations() {
		compUnits.declarations.values().stream()
				.filter(this::hasRegistryLocationAnnotations)
				.forEach(declarationWrapper -> new RegistrationsModifier(
						declarationWrapper).modify());
		compUnits.writeDirty(isTest());
	}

	@Override
	public void run()
			throws Exception {
		ClassOrInterfaceDeclarationWrapper.evaluateSuperclassFqn = false;
		StringMap classPaths = StringMap.fromStringList(classPathList);
		SingletonCache<CompilationUnits> cache = FsObjectCache
				.singletonCache(CompilationUnits.class, getClass())
				.asSingletonCache();
		compUnits = CompilationUnits.load(cache, classPaths,
				DeclarationVisitor::new, isRefresh());
		switch (getAction()) {
		case TRANSLATE_LOCATIONS: {
			ensureRegistrations();
			break;
		}
		default:
			throw new UnsupportedOperationException();
		}
	}

	boolean hasRegistryLocationAnnotations(
			ClassOrInterfaceDeclarationWrapper wrapper) {
		// boolean hasOld = wrapper.getDeclaration()
		// .getAnnotationByClass(RegistryLocation.class).isPresent()
		// || wrapper.getDeclaration()
		// .getAnnotationByClass(RegistryLocations.class)
		// .isPresent();
		// boolean hasNew = wrapper.getDeclaration()
		// .getAnnotationByClass(Registration.class).isPresent()
		// || wrapper.getDeclaration()
		// .getAnnotationByClass(Registrations.class).isPresent()
		// || wrapper.getDeclaration()
		// .getAnnotationByClass(Registration.Singleton.class)
		// .isPresent();
		// return hasOld && !hasNew;
		throw new UnsupportedOperationException();
	}

	public enum Action {
		TRANSLATE_LOCATIONS, REMOVE_LOCATIONS
	}

	private class RegistrationsModifier extends SourceModifier {
		public RegistrationsModifier(
				ClassOrInterfaceDeclarationWrapper declarationWrapper) {
			super(declarationWrapper);
		}

		private Expression createEnumConstantExpression(Enum value) {
			SimpleName simpleName = new SimpleName(value.toString());
			NameExpr scope = getNestedName(value.getClass());
			return new FieldAccessExpr(scope, simpleName.toString());
		}

		private List<Expression> getCoordinates(NormalAnnotationExpr in) {
			List<Expression> list = new ArrayList<>();
			in.getPairs().stream()
					.filter(p -> p.getNameAsString().equals("registryPoint"))
					.findFirst().map(MemberValuePair::getValue)
					.ifPresent(list::add);
			in.getPairs().stream()
					.filter(p -> p.getNameAsString().equals("targetClass"))
					.findFirst().map(MemberValuePair::getValue)
					.ifPresent(list::add);
			list.removeIf(e -> e.toString().equals("void.class"));
			return list;
		}

		private Implementation getImplementationType(NormalAnnotationExpr in) {
			Optional<MemberValuePair> implementationPair = in.getPairs()
					.stream().filter(p -> p.getNameAsString()
							.equals("implementationType"))
					.findFirst();
			if (implementationPair.isPresent()) {
				String implementationValue = implementationPair.get().getValue()
						.toString();
				switch (implementationValue) {
				case "ImplementationType.MULTIPLE":
				case "ImplementationType.INSTANCE":
					return Registration.Implementation.INSTANCE;
				case "ImplementationType.FACTORY":
					return Implementation.FACTORY;
				case "ImplementationType.SINGLETON":
					return Implementation.SINGLETON;
				case "ImplementationType.NONE":
					// Fix by hand (Priority.REMOVE, probably)
					throw new UnsupportedOperationException();
				default:
					throw new UnsupportedOperationException();
				}
			}
			return Registration.Implementation.INSTANCE;
		}

		private NameExpr getNestedName(Class clazz) {
			return new NameExpr(SEUtilities.getNestedSimpleName(clazz));
		}

		private Priority getPriority(NormalAnnotationExpr in) {
			Optional<MemberValuePair> priorityPair = in.getPairs().stream()
					.filter(p -> p.getNameAsString().equals("priority"))
					.findFirst();
			if (priorityPair.isPresent()) {
				String priorityValue = priorityPair.get().getValue().toString();
				switch (priorityValue) {
				case "RegistryLocation.DEFAULT_PRIORITY":
					return Registration.Priority._DEFAULT;
				case "RegistryLocation.INTERMEDIATE_LIBRARY_PRIORITY":
					return Priority.INTERMEDIATE_LIBRARY;
				case "RegistryLocation.PREFERRED_LIBRARY_PRIORITY":
					return Priority.PREFERRED_LIBRARY;
				case "RegistryLocation.MANUAL_PRIORITY":
					return Priority.APP;
				case "RegistryLocation.IGNORE_PRIORITY":
					return Priority.REMOVE;
				case "15":
					return Priority.INTERMEDIATE_LIBRARY;
				case "20":
					return Priority.PREFERRED_LIBRARY;
				default:
					throw new UnsupportedOperationException();
				}
			}
			return Registration.Priority._DEFAULT;
		}

		@Override
		protected void ensureImports() {
		}

		@Override
		protected void modify0() {
			// declaration.getAnnotationByClass(Registration.class)
			// .ifPresent(AnnotationExpr::remove);
			// declaration.getAnnotationByClass(Registration.Singleton.class)
			// .ifPresent(AnnotationExpr::remove);
			// declaration.getAnnotationByClass(Registrations.class)
			// .ifPresent(AnnotationExpr::remove);
			// declaration.getAnnotationByClass(RegistryLocation.class)
			// .map(this::translateLocation)
			// .ifPresent(declarationWrapper::addAnnotation);
			// declaration.getAnnotationByClass(RegistryLocations.class)
			// .map(this::translateLocations)
			// .ifPresent(declarationWrapper::addAnnotation);
			// declarationWrapper.ensureImport(Registration.class);
			throw new UnsupportedOperationException();
		}

		AnnotationExpr translateLocation(AnnotationExpr location) {
			NormalAnnotationExpr in = (NormalAnnotationExpr) location;
			Registration.Priority priority = getPriority(in);
			Registration.Implementation implementation = getImplementationType(
					in);
			List<Expression> value = getCoordinates(in);
			Optional<MemberValuePair> outPriority = priority == Priority._DEFAULT
					? Optional.empty()
					: Optional.of(new MemberValuePair("priority",
							createEnumConstantExpression(priority)));
			Optional<MemberValuePair> outImplementation = implementation == Implementation.INSTANCE
					? Optional.empty()
					: Optional.of(new MemberValuePair("implementation",
							createEnumConstantExpression(implementation)));
			NodeList valueList = new NodeList();
			value.forEach(v -> valueList.add(v.clone()));
			Expression outValue = null;
			if (valueList.size() > 1) {
				outValue = new ArrayInitializerExpr(valueList);
			} else {
				outValue = value.get(0).clone();
			}
			Name registration = new Name(Registration.class.getSimpleName());
			Name singleton = new Name(registration,
					Registration.Singleton.class.getSimpleName());
			boolean allowSingleton = location.getParentNode()
					.get() instanceof ClassOrInterfaceDeclaration;
			if (outPriority.isEmpty() && outImplementation.isEmpty()) {
				return new SingleMemberAnnotationExpr(registration, outValue);
			} else if (implementation == Implementation.SINGLETON
					&& allowSingleton) {
				if (outPriority.isEmpty()) {
					ClassOrInterfaceDeclaration typeDeclaration = (ClassOrInterfaceDeclaration) location
							.getParentNode().get();
					if (outValue instanceof ClassExpr && ((ClassExpr) outValue)
							.getType().toString()
							.equals(typeDeclaration.getName().toString())) {
						return new MarkerAnnotationExpr(singleton);
					} else {
						return new SingleMemberAnnotationExpr(singleton,
								outValue);
					}
				} else {
					NormalAnnotationExpr expr = new NormalAnnotationExpr();
					expr.setName(singleton);
					expr.addPair("value", outValue);
					outPriority.ifPresent(p -> expr.addPair(p.getNameAsString(),
							p.getValue()));
					return expr;
				}
			} else {
				NormalAnnotationExpr expr = new NormalAnnotationExpr();
				expr.setName(registration);
				expr.addPair("value", outValue);
				outPriority.ifPresent(
						p -> expr.addPair(p.getNameAsString(), p.getValue()));
				outImplementation.ifPresent(
						p -> expr.addPair(p.getNameAsString(), p.getValue()));
				return expr;
			}
		}

		@SuppressWarnings("unused")
		AnnotationExpr translateLocations(AnnotationExpr location) {
			declarationWrapper.ensureImport(Registrations.class);
			ArrayInitializerExpr inArray = null;
			if (location instanceof SingleMemberAnnotationExpr) {
				inArray = (ArrayInitializerExpr) ((SingleMemberAnnotationExpr) location)
						.getMemberValue();
			} else {
				inArray = (ArrayInitializerExpr) ((NormalAnnotationExpr) location)
						.getPairs().iterator().next().getValue();
			}
			NodeList<Expression> values = inArray.getValues();
			ArrayInitializerExpr outValue = new ArrayInitializerExpr();
			SingleMemberAnnotationExpr out = new SingleMemberAnnotationExpr(
					new Name(Registrations.class.getSimpleName()), outValue);
			if (values.size() == 1) {
				return translateLocation((AnnotationExpr) values.get(0));
			} else {
				values.forEach(v -> {
					AnnotationExpr translated = translateLocation(
							(AnnotationExpr) v);
					outValue.getValues().add(translated);
				});
				return out;
			}
		}
	}

	class DeclarationVisitor extends CompilationUnitWrapperVisitor {
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
				if (hasRegistryLocationAnnotations(declaration)) {
					declaration.setFlag(Type.RegistryLocation);
				}
			}
			super.visit(node, arg);
		}
	}

	enum Type implements TypeFlag {
		RegistryLocation, Registration
	}
}
