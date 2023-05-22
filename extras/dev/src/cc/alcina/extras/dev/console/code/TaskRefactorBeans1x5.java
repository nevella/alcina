package cc.alcina.extras.dev.console.code;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapperVisitor;
import cc.alcina.extras.dev.console.code.CompilationUnits.TypeFlag;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.ReflectiveSerializable;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
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
				.filter(this::filter).forEach(dec -> {
					switch (action) {
					case LIST_INTERESTING: {
						Ax.out("%s - %s", dec.clazz().getSimpleName(),
								dec.typeFlags);
						break;
					}
					case ACCESS_TO_PACKAGE: {
						break;
					}
					case MANIFEST: {
						break;
					}
					}
				});
		compUnits.writeDirty(test);
	}

	void accessToPackage(UnitType declaration) {
		// TODO:
		// verify that class has no public fields
		// change class to package
		// change fields to package
	}

	boolean filter(UnitType declarationWrapper) {
		if (onlyAssignableFrom != null) {
			return onlyAssignableFrom
					.isAssignableFrom(declarationWrapper.clazz());
		} else if (classNameFilter != null) {
			return declarationWrapper.clazz().getName()
					.matches(classNameFilter);
		} else {
			return true;
		}
	}

	void removeDefaultPropertyMethods(UnitType declaration) {
		// TODO:
		// verify that field is non-transient, protected as class
		// verify that method bodies are default
		// remove methods
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

		boolean hasBeanAnnotation(UnitType declaration) {
			return Reflections.at(declaration.clazz()).has(Bean.class);
		}

		boolean hasPropertyMethods(UnitType declaration) {
			return Reflections.at(declaration.clazz()).properties().size() > 0;
		}

		boolean hasRegistrations(UnitType declaration) {
			return Reflections.at(declaration.clazz()).has(Registration.class);
		}

		boolean isTreeOrReflectSerializable(UnitType declaration) {
			Class clazz = declaration.clazz();
			return TreeSerializable.class.isAssignableFrom(clazz)
					|| ReflectiveSerializable.class.isAssignableFrom(clazz);
		}

		void visit0(ClassOrInterfaceDeclaration node, Void arg) {
			if (!node.isInterface()) {
				UnitType declaration = new UnitType(
						unit, node);
				String nameAsString = declaration.getDeclaration()
						.getNameAsString();
				declaration.setDeclaration(node);
				unit.declarations.add(declaration);
				if (hasBeanAnnotation(declaration)) {
					declaration.setFlag(Type.HasBeanAnnotation);
				}
				if (isTreeOrReflectSerializable(declaration)) {
					declaration.setFlag(Type.TreeOrReflectSerializable);
				}
				if (hasPropertyMethods(declaration)) {
					declaration.setFlag(Type.HasPropertyMethods);
				}
				if (hasRegistrations(declaration)) {
					declaration.setFlag(Type.HasRegistrations);
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
