package com.app.FoodApp.aws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URL;

@Service
@Slf4j
@RequiredArgsConstructor
public class AwsS3ServiceImpl implements AwsS3Service {

    // AWS S3 client injected via constructor (thanks to @RequiredArgsConstructor)
    private final S3Client s3Client;

    // Reads the S3 bucket name from application.properties or application.yml
    @Value("${aws.s3.bucket}")
    private String bucketName;

    /**
     * Uploads a file to the configured S3 bucket.
     *
     * @param keyName the name (path) of the object in the S3 bucket
     * @param file the file to upload (Spring MultipartFile)
     * @return a URL to access the uploaded file
     */
    @Override
    public URL uploadFile(String keyName, MultipartFile file) {
        log.info("Uploading file");

        try {
            // Build request to put (upload) object into S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)                 // target bucket
                    .key(keyName)                       // object key (file path in S3)
                    .contentType(file.getContentType()) // MIME type of the file
                    .build();

            // Upload the file contents as bytes
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            // Generate a pre-signed URL pointing to the uploaded object
            return s3Client.utilities().getUrl(builder -> builder
                    .bucket(bucketName)
                    .key(keyName));
        }
        catch (Exception ex) {
            // Wrap and rethrow any exception as RuntimeException
            throw new RuntimeException(ex.getMessage());
        }
    }

    /**
     * Deletes a file from the configured S3 bucket.
     *
     * @param keyName the name (path) of the object in the S3 bucket
     */
    @Override
    public void deleteFile(String keyName) {
        // Build request to delete object from S3
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)  // target bucket
                .key(keyName)        // object key (file path in S3)
                .build();

        // Execute deletion
        s3Client.deleteObject(deleteObjectRequest);

        // Log deletion success
        log.info("File {} deleted from bucket {}", keyName, bucketName);
    }
}
