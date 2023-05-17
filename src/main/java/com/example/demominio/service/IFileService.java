package com.example.demominio.service;

import com.example.demominio.model.ImageFile;
import io.minio.errors.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface IFileService {
    boolean isImageFile(MultipartFile file);
    List<ImageFile> findImageByFolderName(String folderName);
    List<ImageFile> uploadFiles(List<MultipartFile> multipartFiles) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;
    String accepted() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;
    String rejected();
}
