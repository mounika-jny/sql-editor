package org.mouni.ds.editor.autoconfig;

import org.mouni.ds.editor.controller.SqlController;
import org.mouni.ds.editor.service.SqlExecutorService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
@EnableConfigurationProperties(DsEditorProperties.class)
@ConditionalOnClass({JdbcTemplate.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "ds.editor", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({SqlExecutorService.class, SqlController.class})
public class DsEditorAutoConfiguration {
}
