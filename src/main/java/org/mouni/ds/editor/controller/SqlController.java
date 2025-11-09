package org.mouni.ds.editor.controller;

import jakarta.validation.Valid;
import org.mouni.ds.editor.model.ApiResponse;
import org.mouni.ds.editor.model.SqlRequestDto;
import org.mouni.ds.editor.service.SqlExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sql")
public class SqlController {

    private static final Logger log = LoggerFactory.getLogger(SqlController.class);

    private final SqlExecutorService sqlExecutorService;

    public SqlController(SqlExecutorService sqlExecutorService) {
        this.sqlExecutorService = sqlExecutorService;
    }

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<?>> run(@Valid @RequestBody SqlRequestDto request) {
        log.info("Received SQL execution request");
        log.debug("SQL query: {}", request.getQuery());
        
        Object payload = sqlExecutorService.execute(request);
        
        log.info("SQL execution completed successfully");
        return ResponseEntity.ok(ApiResponse.ok(payload));
    }

    @PostMapping("/run/batch")
    public ResponseEntity<ApiResponse<?>> runBatch(@Valid @RequestBody List<SqlRequestDto> requests) {
        log.info("Received batch SQL execution request with {} queries", requests == null ? 0 : requests.size());
        
        if (requests == null || requests.isEmpty()) {
            log.warn("Batch request rejected: empty request list");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Request list must not be empty"));
        }
        
        List<Object> results = new ArrayList<>(requests.size());
        int successCount = 0;
        int errorCount = 0;
        
        for (int i = 0; i < requests.size(); i++) {
            SqlRequestDto req = requests.get(i);
            try {
                log.debug("Executing batch query {} of {}", i + 1, requests.size());
                Object res = sqlExecutorService.execute(req);
                results.add(res);
                successCount++;
            } catch (Exception ex) {
                log.error("Error executing batch query {} of {}: {}", i + 1, requests.size(), ex.getMessage());
                Map<String, Object> error = new HashMap<>();
                error.put("error", ex.getMessage());
                error.put("index", i);
                results.add(error);
                errorCount++;
            }
        }
        
        log.info("Batch execution completed: {} successful, {} failed", successCount, errorCount);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneric(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, ex.getMessage()));
    }
}
