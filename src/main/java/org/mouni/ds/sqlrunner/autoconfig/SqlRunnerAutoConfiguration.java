package org.mouni.ds.sqlrunner.autoconfig;

import org.mouni.ds.sqlrunner.config.RunnerProperties;
import org.mouni.ds.sqlrunner.config.ScriptProperties;
import org.mouni.ds.sqlrunner.repo.LockRepository;
import org.mouni.ds.sqlrunner.repo.ScriptHistoryRepository;
import org.mouni.ds.sqlrunner.service.ScriptExecutionService;
import org.mouni.ds.sqlrunner.web.ScriptController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@AutoConfiguration
@EnableConfigurationProperties({ScriptProperties.class, RunnerProperties.class})
@ConditionalOnClass({DataSource.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "sqlrunner", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SqlRunnerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ScriptExecutionService scriptExecutionService(
            ScriptProperties scriptProperties,
            ScriptHistoryRepository historyRepository,
            LockRepository lockRepository,
            RunnerProperties runnerProperties,
            DataSource dataSource) {
        return new ScriptExecutionService(scriptProperties, historyRepository, lockRepository, runnerProperties, dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public ScriptController scriptController(ScriptExecutionService scriptExecutionService) {
        return new ScriptController(scriptExecutionService);
    }
}
