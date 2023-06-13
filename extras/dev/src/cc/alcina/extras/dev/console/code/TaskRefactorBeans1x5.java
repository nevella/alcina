package cc.alcina.extras.dev.console.code;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.BlockStmt;

import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapperVisitor;
import cc.alcina.extras.dev.console.code.CompilationUnits.TypeFlag;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.ReflectiveSerializable;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedNameProvider;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.util.FsObjectCache;
import cc.alcina.framework.entity.util.PersistentObjectCache.SingletonCache;
import cc.alcina.framework.servlet.schedule.PerformerTask;

/*

 * @formatter:off
 *

Plan (impl):
Property:
- Add field support
  - can serialize this task?
- @Bean resolver (since we want tree serializable)


Tool:
- Find beans (@bean annotation)
- Find tree/reflect serializable
- Find beanlike (has property methods and corresponding field)
- Note - use jvm, not javaparser model (quicker)
- Fix signature generation tasks


 *
 *@formatter:on
 *
 */
@Bean(PropertySource.FIELDS)
// (tmp)
@TypeSerialization(reflectiveSerializable = false, flatSerializable = false)
public class TaskRefactorBeans1x5 extends PerformerTask {
	transient CompilationUnits compUnits;

	public boolean overwriteOriginals;

	public String classPathList;

	public boolean refresh;

	public Action action;

	public boolean test;

	public Class onlyAssignableFrom;

	public String classNameFilter;

	@Override
	public void run() throws Exception {
		StringMap classPaths = StringMap.fromStringList(classPathList);
		SingletonCache<CompilationUnits> cache = FsObjectCache
				.singletonCache(CompilationUnits.class, getClass())
				.asSingletonCache();
		compUnits = CompilationUnits.load(cache, classPaths.keySet(),
				DeclarationVisitor::new, refresh);
		compUnits.declarations.values().stream().filter(dec -> dec.hasFlags())
				.filter(this::filter).forEach(type -> {
					switch (action) {
					case LIST_INTERESTING: {
						Ax.out("%s - %s", type.clazz().getSimpleName(),
								type.typeFlags);
						break;
					}
					case ACCESS_TO_PACKAGE: {
						accessToPackage(type);
						break;
					}
					case MANIFEST: {
						accessToPackage(type);
						removeDefaultPropertyMethods(type);
						break;
					}
					}
				});
		compUnits.writeDirty(test);
	}

	void accessToPackage(UnitType type) {
		// TODO:
		// verify that class has no public fields
		ClassOrInterfaceDeclaration declaration = type.getDeclaration();
		List<FieldDeclaration> targetFields = declaration.getFields().stream()
				.filter(f -> !f.isStatic()).filter(f -> !f.isTransient())
				.collect(Collectors.toList());
		List<FieldDeclaration> publicOrProtectedFields = targetFields.stream()
				.filter(f -> f.getAccessSpecifier() == AccessSpecifier.PUBLIC
						|| f.getAccessSpecifier() == AccessSpecifier.PROTECTED)
				.collect(Collectors.toList());
		if (publicOrProtectedFields.size() > 0) {
			List<String> names = publicOrProtectedFields.stream()
					.map(f -> f.getVariables().get(0).getNameAsString())
					.collect(Collectors.toList());
			Ax.err("Fields with public/protected access: %s - %s ",
					type.simpleName(), names.toString());
			return;
		}
		targetFields.stream().filter(f -> f.isPrivate()).forEach(f -> {
			type.dirty();
			f.setPrivate(false);
		});
		if (declaration.isPublic()) {
			type.dirty();
			declaration.setPublic(false);
		}
	}

	boolean filter(UnitType type) {
		if (onlyAssignableFrom != null) {
			return onlyAssignableFrom.isAssignableFrom(type.clazz());
		} else if (classNameFilter != null) {
			String name = type.clazz().getName();
			return name.matches(classNameFilter);
		} else {
			return true;
		}
	}

	void removeDefaultPropertyMethods(UnitType type) {
		Ax.out("Removing methods: %s", NestedNameProvider.get(type.clazz()));
		ClassOrInterfaceDeclaration decl = type.getDeclaration();
		List<Property> properties = Reflections.at(type.clazz()).properties();
		List<String> warns = new ArrayList<>();
		for (Property property : properties) {
			String propertyName = property.getName();
			FieldDeclaration field = decl.getFieldByName(propertyName)
					.orElse(null);
			if (field == null) {
				// warns.add(Ax.format("%s - no field", propertyName));
				continue;
			}
			VariableDeclarator fieldVariable = field.getVariables().get(0);
			boolean isBoolean = fieldVariable.getType().asString()
					.equals("boolean");
			String fieldMethodNamePart = propertyName.substring(0, 1)
					.toUpperCase() + propertyName.substring(1);
			{
				String getMethodName = (isBoolean ? "is" : "get")
						+ fieldMethodNamePart;
				MethodDeclaration getterDeclaration = decl
						.getMethodsByName(getMethodName).stream()
						.filter(m -> m.getParameters().size() == 0).findFirst()
						.orElse(null);
				if (getterDeclaration != null) {
					BlockStmt blockStmt = getterDeclaration.getBody().get();
					if (blockStmt.getChildNodes().size() == 1) {
						String string = blockStmt.getChildNodes().get(0)
								.toString();
						String defaultBody = Ax.format("return this.%s;",
								propertyName);
						if (Objects.equals(string, defaultBody)) {
							getterDeclaration.getAnnotations().forEach(ann -> {
								field.addAnnotation(ann);
							});
							getterDeclaration.remove();
						} else {
							warns.add(
									Ax.format("%s - getter - non default body",
											propertyName));
							continue;
						}
					}
				} else {
					warns.add(Ax.format("%s - no getter", propertyName));
				}
			}
			{
				String setMethodName = "set" + fieldMethodNamePart;
				MethodDeclaration setterDeclaration = decl
						.getMethodsByName(setMethodName).stream()
						.filter(m -> m.getParameters().size() == 1).findFirst()
						.orElse(null);
				if (setterDeclaration != null) {
					BlockStmt blockStmt = setterDeclaration.getBody().get();
					if (blockStmt.getChildNodes().size() == 1) {
						String string = blockStmt.getChildNodes().get(0)
								.toString();
						String defaultBody = Ax.format("this.%s = %s;",
								propertyName, propertyName);
						if (Objects.equals(string, defaultBody)) {
							setterDeclaration.remove();
						} else {
							warns.add(
									Ax.format("%s - setter - non default body",
											propertyName));
							continue;
						}
					}
				} else {
					// warns.add(Ax.format("%s - no setter", propertyName));
				}
			}
		}
		warns.forEach(s -> Ax.out("  %s", s));
	}

	public enum Action {
		LIST_INTERESTING, MANIFEST, ACCESS_TO_PACKAGE;
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

		boolean hasBeanAnnotation(UnitType type) {
			return Reflections.at(type.clazz()).has(Bean.class);
		}

		boolean hasPropertyMethods(UnitType type) {
			return Reflections.at(type.clazz()).properties().size() > 0;
		}

		boolean hasRegistrations(UnitType type) {
			return Reflections.at(type.clazz()).has(Registration.class);
		}

		boolean isTreeOrReflectSerializable(UnitType type) {
			Class clazz = type.clazz();
			return TreeSerializable.class.isAssignableFrom(clazz)
					|| ReflectiveSerializable.class.isAssignableFrom(clazz);
		}

		void visit0(ClassOrInterfaceDeclaration node, Void arg) {
			if (!node.isInterface()) {
				UnitType type = new UnitType(unit, node);
				String nameAsString = type.getDeclaration().getNameAsString();
				type.setDeclaration(node);
				unit.declarations.add(type);
				if (hasBeanAnnotation(type)) {
					type.setFlag(Type.HasBeanAnnotation);
				}
				if (isTreeOrReflectSerializable(type)) {
					type.setFlag(Type.TreeOrReflectSerializable);
				}
				if (hasPropertyMethods(type)) {
					type.setFlag(Type.HasPropertyMethods);
				}
				if (hasRegistrations(type)) {
					type.setFlag(Type.HasRegistrations);
				}
			}
			super.visit(node, arg);
		}
	}

	enum Type implements TypeFlag {
		HasBeanAnnotation, TreeOrReflectSerializable, HasPropertyMethods,
		HasRegistrations
	}
}
