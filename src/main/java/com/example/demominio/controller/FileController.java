package com.example.demominio.controller;

import com.example.demominio.service.FileService;
import io.minio.errors.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RestController
@RequestMapping("/file")
public class FileController {
    private final FileService fileService;
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileId = fileService.uploadFile(file);
        return ResponseEntity.ok(fileId);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        Resource file = fileService.downloadFile(fileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileId)
                .body(file);
    }

    @GetMapping
    public ResponseEntity<List<String>> getAllFiles() {
        List<String> files = fileService.getAllFiles();
        return ResponseEntity.ok(files);
    }

    @GetMapping("/path/{fileId}")
    public String getPathFile(@PathVariable String fileId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        return fileService.getPathFile(fileId);
    }
}
