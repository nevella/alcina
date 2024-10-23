package cc.alcina.framework.entity.gwt.reflection.mvcc;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import cc.alcina.framework.common.client.domain.mvcc.MvccEntity;
import cc.alcina.framework.common.client.logic.domain.Entity;

/**
 * <p>
 * This class generates a source file for a given input {@link Entity} class.
 * It's the non-bytecode version of
 * {@link cc.alcina.framework.entity.persistence.mvcc.ClassTransformer}
 */
public class MvccSourceGenerator {
	public static String generate(Class<? extends Entity> entityClass) {
		WriterImpl writer = new WriterImpl();
		new MvccEntity(entityClass).write(writer);
		return writer.writer.toString();
	}

	static class WriterImpl implements MvccEntity.EntityWriter {
		StringWriter writer = new StringWriter();

		SourceWriter sourceWriter;

		Class<? extends Entity> entityClass;

		WriterImpl() {
		}

		@Override
		public void enter(Class<? extends Entity> entityClass) {
			this.entityClass = entityClass;
			ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
					entityClass.getPackageName(), getTransformedClassName());
			sourceWriter = factory.createSourceWriter(new PrintWriter(writer));
		}

		@Override
		public Class<? extends Entity> getEntityClass() {
			return entityClass;
		}
	}
}
