package com.example.demominio.controller;
import com.example.demominio.service.FileService;
import io.minio.errors.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/files")
public class FileController {
    private final FileService fileService;
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    public ResponseEntity<?> uploadFiles(MultipartHttpServletRequest request) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        List<MultipartFile> multipartFiles = request.getFiles("file").stream().filter(fileService::isImageFile).collect(Collectors.toList());
        return ResponseEntity.ok(fileService.uploadFiles(multipartFiles));
    }

    @GetMapping("/folder/{folderName}")
    public ResponseEntity<?> getAllNewFiles(@PathVariable String folderName) {
        return ResponseEntity.ok(fileService.findImageByFolderName(folderName));
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateImages(@RequestParam("status") Boolean status) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        return ResponseEntity.ok(status ? fileService.accepted() : fileService.rejected());
    }
}
