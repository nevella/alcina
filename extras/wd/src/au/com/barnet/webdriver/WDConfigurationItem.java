package au.com.barnet.webdriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;

@RegistryLocation(registryPoint = JaxbContextRegistration.class)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class WDConfigurationItem {
    public String uri;

    public int predelayMs;

    public boolean recurrentTest = false;

    public String name;

    public int times = 1;

    public String topLevelClassName;

    public boolean closeOnError = true;

    // if non-zero, stats.do will report as a long load time (easier to see in
    // cacti) rather than as an error
    public int reportErrorAs200OfTimeMs = 0;

    public WebDriverType driverType;

    public transient long usedCacheIfFresherThan;

    public WDDriverHandler driverHandler() {
        return WDDriverHandlerProvider.get().driverHandler(driverType);
    }

    public String getHostAndScheme() {
        try {
            URL url = new URL(uri);
            URL url2 = new URL(url.getProtocol(), url.getHost(), url.getPort(),
                    "");
            return url2.toExternalForm();
        } catch (MalformedURLException e) {
            return "";
        }
    }

    public String toHtml() {
        String tplt = "<a href='test.do?%s=%s'>%s</a><br />&nbsp;&nbsp;&nbsp;%s : %s : %s";
        return CommonUtils.formatJ(tplt, "testname", name, name, driverType,
                topLevelClassName, uri);
    }

    @RegistryLocation(registryPoint = JaxbContextRegistration.class)
    @XmlRootElement(name = "wdConfiguration")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class WDConfiguration {
        @XmlElementWrapper(name = "items")
        @XmlElement(name = "item")
        public List<WDConfigurationItem> configurations;

        public boolean runRecurrentTests;

        public int recurrentTestPeriodSeconds = 60 * 5;
    }

    @RegistryLocation(registryPoint = WDDriverHandlerProvider.class, implementationType = ImplementationType.SINGLETON)
    public static abstract class WDDriverHandlerProvider {
        public static WDConfigurationItem.WDDriverHandlerProvider get() {
            return Registry
                    .impl(WDConfigurationItem.WDDriverHandlerProvider.class);
        }

        public abstract WDDriverHandler driverHandler(WebDriverType driverType);
    }

    public enum WebDriverType {
        FIREFOX, IE, SAFARI, HTMLUNIT, IE8, CHROME, CHROME_LOCAL,
        CHROME_NO_PROFILE, FIREFOX_GWT, CHROMIUM_GWT, IE11, IE9,
        CHROMIUM_ALT_REMOTE
    }
}
