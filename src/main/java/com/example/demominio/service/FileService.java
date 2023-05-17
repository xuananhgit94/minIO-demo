package com.example.demominio.service;
import com.example.demominio.model.ImageFile;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class FileService implements IFileService{
    private final MinioClient minioClient;
    @Value("${minio.bucket}")
    private String bucket;
    @Value("${bucket.new}")
    private String newFolder;
    @Value("${bucket.line}")
    private String lineFolder;
    @Value("${bucket.old}")
    private String oldFolder;

    public FileService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public boolean isImageFile(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return ImageIO.read(inputStream) != null;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<ImageFile> uploadFiles(List<MultipartFile> multipartFiles)
            throws ServerException, InsufficientDataException, ErrorResponseException,
            IOException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException {

        deleteImagesInFolder(newFolder);
        return multipartFiles.stream().map(this::uploadFile).collect(Collectors.toList());
    }

    @Override
    public String accepted() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (!isExistInFolder(newFolder)) {
            throw new FileNotFoundException("new folder is empty");
        }
        if (isExistInFolder(oldFolder)) {
            deleteImagesInFolder(oldFolder);
        }
        if (isExistInFolder(lineFolder)) {
            moveToFolder(lineFolder, oldFolder);
        }
        moveToFolder(newFolder, lineFolder);
        return "accepted";
    }

    private void moveToFolder(String sourceFolder, String targetFolder) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .prefix(sourceFolder)
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();
            String sourceObjectName = item.objectName();
            String destinationObjectName = sourceObjectName.replace(sourceFolder, targetFolder);

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .source(CopySource.builder().bucket(bucket).object(sourceObjectName).build())
                            .bucket(bucket)
                            .object(destinationObjectName)
                            .build()
            );
        }
        deleteImagesInFolder(sourceFolder);
    }

    @Override
    public String rejected() {
        try {
            deleteImagesInFolder(newFolder);
            return "rejected";
        } catch (Exception e) {
            return "cannot reject";
        }
    }

    private ImageFile uploadFile(MultipartFile file) {
        try {
            String filename = newFolder + UUID.randomUUID() + file.getOriginalFilename();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(filename)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            String url = getUrlImage(filename);
            return ImageFile
                    .builder()
                    .fileName(filename)
                    .url(url)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    private String getUrlImage(String fileName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(fileName)
                .build();
        return minioClient.getPresignedObjectUrl(args);
    }

    public void deleteImagesInFolder(String folderName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .prefix(folderName)
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(item.objectName())
                            .build()
            );
        }
    }

    @Override
    public List<ImageFile> findImageByFolderName(String folderName) {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .prefix(folderName + "/")
                        .recursive(true)
                        .build()
        );
        return StreamSupport.stream(results.spliterator(), false).map(this::getImageFile).collect(Collectors.toList());
    }

    public boolean isExistInFolder(String folderName) {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .prefix(folderName + "/")
                        .recursive(true)
                        .build()
        );
        return results.iterator().hasNext();
    }

    private ImageFile getImageFile(@NonNull Result<Item> result) {
        Item item;
        ImageFile imageFile = null;
        try {
            item = result.get();
            String fileName = item.objectName();
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(fileName)
                            .build()
            );
            imageFile = ImageFile
                    .builder()
                    .fileName(fileName)
                    .url(url)
                    .build();
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
            e.printStackTrace();
        }
        return imageFile;
    }

    public Resource downloadFile(String fileId) {
        try {
            byte[] data = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(fileId)
                    .build()).readAllBytes();
            return new ByteArrayResource(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file", e);
        }
    }

}
