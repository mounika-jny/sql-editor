package org.mouni.ds.sqlrunner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scripts")
public class ScriptProperties {

    /**
     * Enable or disable the SQL runner functionality.
     * Default is true (enabled).
     */
    private boolean enabled = true;

    /**
     * Directory on file system where SQL scripts are stored.
     * Default is ./scripts but can be overridden via property or env SCRIPTS_DIR.
     */
    private String dir = "./scripts";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }
}
