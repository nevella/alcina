package cc.alcina.framework.entity.control;

import java.util.List;

public class ClusterState {
    private String clusterId;

    private String currentWriterHost;

    private List<String> allHosts;

    private String preferredWriterHost;

    private String httpProxyBalancerUrl;

    private String httpsProxyBalancerUrl;

    private String httpsProxyBalancerUrl2;

    private String proxyToHttpPort;

    private String proxyToHttpsPort;

    private String testUrl;

    private String zkHostPortUrl;

    private String hostTunnels;

    public List<String> getAllHosts() {
        return this.allHosts;
    }

    public String getClusterId() {
        return this.clusterId;
    }

    public String getCurrentWriterHost() {
        return this.currentWriterHost;
    }

    public String getHostTunnels() {
        return this.hostTunnels;
    }

    public String getHttpProxyBalancerUrl() {
        return this.httpProxyBalancerUrl;
    }

    public String getHttpsProxyBalancerUrl() {
        return this.httpsProxyBalancerUrl;
    }

    public String getHttpsProxyBalancerUrl2() {
        return this.httpsProxyBalancerUrl2;
    }

    public String getPreferredWriterHost() {
        return this.preferredWriterHost;
    }

    public String getProxyToHttpPort() {
        return this.proxyToHttpPort;
    }

    public String getProxyToHttpsPort() {
        return this.proxyToHttpsPort;
    }

    public String getTestUrl() {
        return this.testUrl;
    }

    public String getZkHostPortUrl() {
        return this.zkHostPortUrl;
    }

    public void setAllHosts(List<String> allHosts) {
        this.allHosts = allHosts;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public void setCurrentWriterHost(String currentWriterHost) {
        this.currentWriterHost = currentWriterHost;
    }

    public void setHostTunnels(String hostTunnels) {
        this.hostTunnels = hostTunnels;
    }

    public void setHttpProxyBalancerUrl(String httpProxyBalancerUrl) {
        this.httpProxyBalancerUrl = httpProxyBalancerUrl;
    }

    public void setHttpsProxyBalancerUrl(String httpsProxyBalancerUrl) {
        this.httpsProxyBalancerUrl = httpsProxyBalancerUrl;
    }

    public void setHttpsProxyBalancerUrl2(String httpsProxyBalancerUrl2) {
        this.httpsProxyBalancerUrl2 = httpsProxyBalancerUrl2;
    }

    public void setPreferredWriterHost(String preferredWriterHost) {
        this.preferredWriterHost = preferredWriterHost;
    }

    public void setProxyToHttpPort(String proxyToHttpPort) {
        this.proxyToHttpPort = proxyToHttpPort;
    }

    public void setProxyToHttpsPort(String proxyToHttpsPort) {
        this.proxyToHttpsPort = proxyToHttpsPort;
    }

    public void setTestUrl(String testUrl) {
        this.testUrl = testUrl;
    }

    public void setZkHostPortUrl(String zkHostPortUrl) {
        this.zkHostPortUrl = zkHostPortUrl;
    }
}
