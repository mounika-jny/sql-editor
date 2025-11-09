package org.mouni.ds.editor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DsEditorApplication {

    private static final Logger log = LoggerFactory.getLogger(DsEditorApplication.class);

    public static void main(String[] args) {
        log.info("Starting DS Editor Application");
        SpringApplication.run(DsEditorApplication.class, args);
        log.info("DS Editor Application started successfully");
    }

}
