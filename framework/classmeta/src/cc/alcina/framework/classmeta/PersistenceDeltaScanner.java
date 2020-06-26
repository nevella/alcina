package cc.alcina.framework.classmeta;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.classmeta.ClassPersistenceScanData.ClassPersistenceScannedClass;
import cc.alcina.framework.classmeta.ClassPersistenceScanData.ClassPersistenceScannedPersistenceGetter;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.registry.CachingScanner;
import cc.alcina.framework.entity.registry.ClassMetadata;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.registry.JarHelper;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

class PersistenceDeltaScanner
		extends CachingScanner<ClassPersistenceScannedClass> {
	private ClassPersistenceScanData result;

	Set<String> jarPathsAdded = new LinkedHashSet<>();

	ClassPool pool = new ClassPool();;

	public PersistenceDeltaScanner(ClassPersistenceScanData result) {
		this.result = result;
	}

	@Override
	public InputStream getStreamForMd5(ClassMetadata classMetadata)
			throws Exception {
		return new JarHelper().openStream(classMetadata.url());
	}

	@Override
	public void scan(ClassMetadataCache<ClassMetadata> foundCache,
			String cachePath) throws Exception {
		super.scan(foundCache, cachePath);
		commit();
	}

	private void commit() {
		result.classes = outgoingCache.classData.values().stream()
				.filter(cpsc -> cpsc.persistent)
				.sorted(Comparator.comparing(cpsc -> cpsc.className))
				.collect(AlcinaCollectors.toLinkedHashSet());
	}

	private boolean isPersistent(Annotation typed) {
		String annotationTypeName = typed.annotationType().getName();
		return annotationTypeName
				.matches("(org.hibernate.annotations|javax.persistence).+");
	}

	@Override
	protected ClassPersistenceScannedClass createMetadata(String className,
			ClassMetadata found) {
		return new ClassPersistenceScannedClass(className).fromUrl(found);
	}

	@Override
	protected Class loadClass(List<ClassLoader> classLoaders, String className)
			throws ClassNotFoundException, Error {
		return null;// never want to load, different jvm
	}

	@Override
	protected ClassPersistenceScannedClass process(Class clazz,
			String className, ClassMetadata found) {
		ClassPersistenceScannedClass out = createMetadata(className, found);
		out.persistent = false;
		try {
			URL url = found.url;
			String str = url.toString();
			if (str.contains("$")) {
				// inner class, ignore by convention
			} else {
				String sourceUrlStr = str.replace(".class", ".java");
				URL sourceUrl = SEUtilities.toURL(sourceUrlStr);
				String jarUrl = url.toString().replaceFirst("jar:(.+?)!.+",
						"$1");
				String jarPath = url.toString()
						.replaceFirst("jar:file:(.+?)!.+", "$1");
				if (jarPathsAdded.add(jarPath)) {
					pool.appendClassPath(jarPath);
				}
				CtClass ctClass = pool.get(className);
				Object[] annotations = ctClass.getAnnotations();
				boolean hasPersistentAnnotations = false;
				for (Object annotation : annotations) {
					if (Annotation.class
							.isAssignableFrom(annotations[0].getClass())) {
						Annotation typed = (Annotation) annotation;
						hasPersistentAnnotations |= isPersistent(typed);
					}
				}
				if (!hasPersistentAnnotations) {
					return out;
				}
				PersistenceMapping mapping = new PersistenceMapping(ctClass,
						sourceUrl, out);
			}
		} catch (Exception e) {
			if (e instanceof FileNotFoundException) {
				System.out.println(e.getMessage());
			} else {
				throw new WrappedRuntimeException(e);
			}
		}
		return out;
	}

	class PersistenceMapping {
		ClassPersistenceScannedClass out;

		URL sourceUrl;

		private CtClass ctClass;

		public PersistenceMapping(CtClass ctClass, URL sourceUrl,
				ClassPersistenceScannedClass out) {
			this.ctClass = ctClass;
			this.out = out;
			this.sourceUrl = sourceUrl;
			try {
				process();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		private String signature(Annotation annotation) {
			return annotation.toString();
		}

		private Annotation[] toAnnotations(Object[] in) {
			Annotation[] out = new Annotation[in.length];
			for (int i = 0; i < in.length; i++) {
				Object object = in[i];
				out[i] = (Annotation) object;
			}
			return out;
		}

		void process() throws Exception {
			out.persistent = true;
			for (Annotation annotation : toAnnotations(
					ctClass.getAnnotations())) {
				if (isPersistent(annotation)) {
					out.persistentAnnotationSignatures
							.add(signature(annotation));
				}
			}
			for (CtMethod m : ctClass.getMethods()) {
				if (m.getName().startsWith("get")) {
					ClassPersistenceScannedPersistenceGetter getter = new ClassPersistenceScannedPersistenceGetter();
					getter.methodSignature = m.toString().replaceFirst(
							"javassist.CtMethod@.+?\\[(.+)\\]", "$1");
					out.persistentGetters.add(getter);
					for (Annotation annotation : toAnnotations(
							m.getAnnotations())) {
						if (isPersistent(annotation)) {
							getter.persistentAnnotationSignatures
									.add(signature(annotation));
						}
					}
				}
			}
			Collections.sort(out.persistentAnnotationSignatures);
			Collections.sort(out.persistentGetters);
			out.persistentGetters.forEach(
					g -> Collections.sort(g.persistentAnnotationSignatures));
		}
	}
}