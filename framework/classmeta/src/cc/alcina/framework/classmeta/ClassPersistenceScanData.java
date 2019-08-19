package cc.alcina.framework.classmeta;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlRootElement;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasEquivalence;
import cc.alcina.framework.entity.registry.ClassMetadata;

@XmlRootElement
public class ClassPersistenceScanData
        implements HasEquivalence<ClassPersistenceScanData> {
    public Date generated;

    public ClassPersistenceScanSchema schema;

    public Set<ClassPersistenceScannedClass> classes = new LinkedHashSet<>();

    @Override
    public boolean equivalentTo(ClassPersistenceScanData other) {
        return schema.equals(other.schema)
                && HasEquivalenceHelper.equivalent(classes, other.classes);
    }

    @Override
    public String toString() {
        return Ax.format("scan - %s classpath urls - %s persistent classes",
                schema.classPathUrls.size(), classes.size());
    }

    public static class ClassPersistenceScannedClass
            extends ClassMetadata<ClassPersistenceScannedClass>
            implements HasEquivalenceHash<ClassPersistenceScannedClass> {
        public List<String> persistentAnnotationSignatures = new ArrayList<>();

        public List<ClassPersistenceScannedPersistenceGetter> persistentGetters = new ArrayList<>();

        public boolean persistent;

        transient int hash = -1;

        public ClassPersistenceScannedClass() {
        }

        public ClassPersistenceScannedClass(String className) {
            super(className);
        }

        @Override
        public int equivalenceHash() {
            if (this.hash == -1) {
                int hash = Math.abs(Objects.hash(className.hashCode(),
                        persistentAnnotationSignatures.hashCode(),
                        persistentGetters.hashCode()));
                this.hash = hash;
            }
            return hash;
        }

        @Override
        public boolean equivalentTo(ClassPersistenceScannedClass o) {
            return className.equals(o.className)
                    && persistentAnnotationSignatures
                            .equals(o.persistentAnnotationSignatures)
                    && HasEquivalenceHelper.equivalent(persistentGetters,
                            o.persistentGetters);
        }

        @Override
        public String toString() {
            return Ax.format(
                    "Persistent scanned class:\n=============\n"
                            + "class: %s\nannotations:\n\t%s\ngetters:\n\t%s",
                    className,
                    CommonUtils
                            .joinWithNewlineTab(persistentAnnotationSignatures),
                    CommonUtils.joinWithNewlineTab(persistentGetters));
        }
    }

    public static class ClassPersistenceScannedPersistenceGetter implements
            HasEquivalenceHash<ClassPersistenceScannedPersistenceGetter>,
            Comparable<ClassPersistenceScannedPersistenceGetter> {
        public String methodSignature;

        public List<String> persistentAnnotationSignatures = new ArrayList<>();

        transient int hash = -1;

        @Override
        public int compareTo(ClassPersistenceScannedPersistenceGetter o) {
            {
                int i = methodSignature.compareTo(o.methodSignature);
                if (i != 0) {
                    return i;
                }
            }
            {
                int i = persistentAnnotationSignatures.toString()
                        .compareTo(o.persistentAnnotationSignatures.toString());
                if (i != 0) {
                    return i;
                }
            }
            return 0;
        }

        @Override
        public int equivalenceHash() {
            return hashCode();
        }

        @Override
        public boolean equivalentTo(
                ClassPersistenceScannedPersistenceGetter other) {
            if (methodSignature.equals(methodSignature)
                    && persistentAnnotationSignatures
                            .equals(other.persistentAnnotationSignatures)) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            if (this.hash == -1) {
                int hash = Math.abs(Objects.hash(methodSignature,
                        persistentAnnotationSignatures.hashCode()));
                this.hash = hash;
            }
            return hash;
        }

        @Override
        public String toString() {
            return Ax.format("%s\n\t - %s", methodSignature,
                    persistentAnnotationSignatures.stream()
                            .collect(Collectors.joining("\n\t - ")));
        }
    }
}
