package cc.alcina.framework.entity.util;

import java.io.File;
import java.util.Arrays;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ThrowingFunction;
import cc.alcina.framework.entity.ResourceUtilities;

public class FsStringCache {
    private File root;

    private ThrowingFunction<String, String> contentMapper;

    public FsStringCache(File root,
            ThrowingFunction<String, String> contentMapper) {
        this.root = root;
        this.contentMapper = contentMapper;
    }

    public String get(String path) {
        return get(path, path);
    }

    public String get(String path, String content) {
        File cacheFile = getCacheFile(path);
        if (!cacheFile.exists()) {
            try {
                String value = contentMapper.apply(content);
                ResourceUtilities.write(value, cacheFile);
            } catch (Exception e) {
                throw new WrappedRuntimeException(e);
            }
        } else {
        }
        return ResourceUtilities.read(cacheFile);
    }

    public void invalidate(String path) {
        getCacheFile(path).delete();
    }

    public void invalidateAll() {
        Arrays.asList(getCacheFile("0").getParentFile().listFiles()).stream()
                .filter(File::isFile).forEach(File::delete);
    }

    private File getCacheFile(String path) {
        return new File(Ax.format("%s/%s.dat", root.getPath(), path));
    }
}