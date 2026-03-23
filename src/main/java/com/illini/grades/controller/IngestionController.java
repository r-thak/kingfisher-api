package com.illini.grades.controller;

import com.illini.grades.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/v1/admin")
@Tag(name = "Admin")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    @Operation(summary = "Ingest CSV data")
    public Map<String, Object> ingest(@RequestParam("file") MultipartFile file) {
        try {
            ingestionService.ingest(file.getInputStream());
            return Map.of("message", "Ingested successfully", "errors", new String[]{});
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}
