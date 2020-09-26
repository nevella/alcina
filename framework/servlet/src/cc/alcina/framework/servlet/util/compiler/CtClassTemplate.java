package cc.alcina.framework.servlet.util.compiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;

public class CtClassTemplate {
	public static volatile String CLASS_DEBUG_PATH = null;

	private final String name;

	private String extendsClass;

	private final List<String> implementsInterfaces = new ArrayList<>();

	private final List<CtFieldTemplate> fields = new ArrayList<>();

	private final List<CtMethodTemplate> methods = new ArrayList<>();

	public CtClassTemplate(final String name) {
		this.name = name;
	}

	public CtFieldTemplate addField(final String code) {
		final CtFieldTemplate result = new CtFieldTemplate(code);
		fields.add(result);
		return result;
	}

	public void addImplements(final String ifcName) {
		implementsInterfaces.add(ifcName);
	}

	public CtMethodTemplate addMethod(final String code) {
		final CtMethodTemplate result = new CtMethodTemplate(code);
		methods.add(result);
		return result;
	}

	public <T> Class<T> createClass(final ClassPool pool)
			throws CannotCompileException {
		return createClass(pool, CtClassTemplate.class.getClassLoader());
	}

	public <T> Class<T> createClass(final ClassPool pool, final ClassLoader cl)
			throws CannotCompileException {
		try {
			final CtClass ctClass = pool.makeClass(name);
			if (extendsClass != null && !extendsClass.isEmpty()) {
				ctClass.setSuperclass(pool.get(extendsClass));
			}
			for (String ifc : implementsInterfaces) {
				ctClass.addInterface(pool.get(ifc));
			}
			for (CtFieldTemplate field : fields) {
				final CtField ctField = CtField.make(field.getCode(), ctClass);
				ctClass.addField(ctField);
			}
			for (CtMethodTemplate method : methods) {
				final CtMethod ctMethod = CtMethod.make(method.getCode(),
						ctClass);
				ctClass.addMethod(ctMethod);
			}
			final String cdp = CLASS_DEBUG_PATH;
			if (cdp != null) {
				ctClass.writeFile(cdp);
			}
			return ctClass.toClass(cl, null);
		} catch (NotFoundException | IOException e) {
			throw new CannotCompileException(e);
		}
	}

	public void setExtends(final String className) {
		this.extendsClass = className;
	}

	public class CtFieldTemplate {
		private String code;

		private CtFieldTemplate(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
	}

	public class CtMethodTemplate {
		private String code;

		private CtMethodTemplate(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
	}
}