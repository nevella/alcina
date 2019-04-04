package cc.alcina.extras.webdriver;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.util.DataFolderProvider;

public abstract class WDDriverHandlerExt implements WDDriverHandler {
    static Map<Class, RemoteWebDriver> lastDrivers = new LinkedHashMap<>();

    private static PersistentDriverData persistentDriverData;

    public static void closeDrivers() {
        lastDrivers.values().forEach(driver -> {
            try {
                driver.close();
                driver.quit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static PersistentDriverData persistentDriverData() {
        if (persistentDriverData == null) {
            try {
                persistentDriverData = KryoUtils.deserializeFromFile(
                        new File(persistentDriverDataPath()),
                        PersistentDriverData.class);
            } catch (Exception e) {
                persistentDriverData = new PersistentDriverData();
            }
        }
        return persistentDriverData;
    }

    static String persistentDriverDataPath() {
        return Ax.format("%s/%s", DataFolderProvider.get().getDataFolder(),
                "persistentDriverData.dat");
    }

    protected RemoteWebDriver driver;

    @Override
    public void closeAndCleanup() {
        HttpServletRequest req = LooseContext.get(WDManager.CONTEXT_REQUEST);
        if (Boolean.valueOf(req.getParameter("reuse"))) {
            putlastDriver(driver);
            return;
        }
        try {
            driver.close();
            driver.quit();
        } catch (Exception e) {
            throw new WrappedRuntimeException(e,
                    SuggestedAction.NOTIFY_WARNING);
        }
    }

    @Override
    public WebDriver getDriver() {
        HttpServletRequest req = LooseContext.get(WDManager.CONTEXT_REQUEST);
        if (driver == null) {
            if (Boolean.valueOf(req.getParameter("reuse"))) {
                RemoteWebDriver lastDriver = lastDriver();
                if (lastDriver != null) {
                    driver = lastDriver;
                    try {
                        driver.getWindowHandle();
                        driver.getCurrentUrl();
                        return driver;
                    } catch (Exception e) {
                        try {
                            driver.close();
                            driver.quit();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        driver = null;
                        putlastDriver(null);
                        // unreachable browser
                    }
                }
            }
            try {
                createNewDriver();
                putlastDriver(driver);
            } catch (Exception e) {
                throw new WrappedRuntimeException(e,
                        SuggestedAction.NOTIFY_WARNING);
            }
        }
        return driver;
    }

    protected void closeLastDriver0(Class clazz) {
        RemoteWebDriver lastDriver = lastDriver();
        if (lastDriver != null) {
            try {
                lastDriver.close();
                lastDriver.quit();
            } catch (Exception e) {
                throw new WrappedRuntimeException(e,
                        SuggestedAction.NOTIFY_WARNING);
            }
        }
    }

    protected abstract void createNewDriver() throws Exception;

    protected RemoteWebDriver lastDriver() {
        return lastDrivers.get(getClass());
    }

    protected void putlastDriver(RemoteWebDriver driver) {
        lastDrivers.put(getClass(), driver);
        if (driver != null) {
            persistentDriverData().nodes
                    .removeIf(n -> n.driverClass == getClass());
            persistentDriverData().nodes
                    .add(new PersistentDriverDataNode(getClass(), driver));
            File file = new File(persistentDriverDataPath());
            KryoUtils.serializeToFile(persistentDriverData(), file);
            // check serz ok
            KryoUtils.deserializeFromFile(file, PersistentDriverData.class);
        }
    }

    public static class PersistentDriverData {
        public List<PersistentDriverDataNode> nodes = new ArrayList<>();
    }

    public static class PersistentDriverDataNode {
        public String currentUrl;

        public Class<? extends WDDriverHandlerExt> driverClass;

        public String sessionId;

        public Capabilities capabilities;

        Class responseCodecClass;

        Class commandCodecClass;

        public PersistentDriverDataNode() {
        }

        public PersistentDriverDataNode(
                Class<? extends WDDriverHandlerExt> clazz,
                RemoteWebDriver driver) {
            driverClass = clazz;
            sessionId = driver.getSessionId().toString();
            currentUrl = driver.getCurrentUrl();
            MutableCapabilities caps = (MutableCapabilities) driver
                    .getCapabilities();
            caps.asMap().keySet().forEach(k -> {
                Object capability = caps.getCapability(k);
                if (capability instanceof Map) {
                    caps.setCapability(k, new LinkedHashMap((Map) capability));
                }
            });
            capabilities = driver.getCapabilities();
            try {
                {
                    Field field = RemoteWebDriver.class
                            .getDeclaredField("executor");
                    field.setAccessible(true);
                    HttpCommandExecutor executor = (HttpCommandExecutor) field
                            .get(driver);
                    {
                        Field field2 = HttpCommandExecutor.class
                                .getDeclaredField("commandCodec");
                        field2.setAccessible(true);
                        commandCodecClass = field2.get(executor).getClass();
                    }
                    {
                        Field field2 = HttpCommandExecutor.class
                                .getDeclaredField("responseCodec");
                        field2.setAccessible(true);
                        responseCodecClass = field2.get(executor).getClass();
                    }
                }
            } catch (Exception e) {
                throw new WrappedRuntimeException(e);
            }
        }
    }
}
