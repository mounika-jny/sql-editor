package org.mouni.ds.sqlrunner.web;


import org.mouni.ds.sqlrunner.model.ScriptRecord;
import org.mouni.ds.sqlrunner.service.ScriptExecutionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@ConditionalOnProperty(prefix = "scripts", name = "enabled", havingValue = "true", matchIfMissing = true)
@RestController
@RequestMapping("/scripts")
public class ScriptController {

    private final ScriptExecutionService service;

    public ScriptController(ScriptExecutionService service) {
        this.service = service;
    }

    @GetMapping
    public List<ScriptRecord> list() {
        return service.listHistory();
    }

    @PostMapping("/run")
    public ResponseEntity<String> runAll() throws Exception {
        service.runAllScripts();
        return ResponseEntity.ok("Scripts run completed");
    }

    @PostMapping("/{scriptName}/rerun")
    public ResponseEntity<String> rerun(@PathVariable String scriptName) throws Exception {
        service.rerunScript(scriptName);
        return ResponseEntity.ok("Rerun completed for " + scriptName);
    }
}
