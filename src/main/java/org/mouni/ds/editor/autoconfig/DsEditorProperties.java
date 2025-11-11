package org.mouni.ds.editor.autoconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ds.editor")
public class DsEditorProperties {

    /**
     * Enables DS Editor auto-configuration.
     * Set to false to completely disable all beans/endpoints provided by this library.
     */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
