package org.mouni.ds.sqlrunner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "runner")
public class RunnerProperties {

    /**
     * Logical node id, used for distributed lock diagnostics.
     * Default is 'local-node', but in k8s you can map to HOSTNAME.
     */
    private String nodeId = "local-node";

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
}
