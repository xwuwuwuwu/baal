package com.battlelab.downloader;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

public class AzureStorageHelper {

    public static InputStream openBlockBlobInputStream(String connectionString, String container, String blobname) throws URISyntaxException, InvalidKeyException, StorageException {
        CloudBlobContainer containerReference = getCloudBlobContainer(connectionString, container);
        CloudBlockBlob blockBlobReference = containerReference.getBlockBlobReference(blobname);

        return blockBlobReference.openInputStream();
    }

    public static byte[] downloadBlobToByteArray(String connectionString, String container, String blobname) throws URISyntaxException, StorageException, InvalidKeyException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(100 * 1024);
        try {
            CloudBlobContainer containerReference = getCloudBlobContainer(connectionString, container);
            CloudBlockBlob blockBlob = containerReference.getBlockBlobReference(blobname);
            blockBlob.download(outputStream);
            return outputStream.toByteArray();
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
            }
        }
    }

    public static byte[] downloadBlobToByteArray(String connectionString, String uri) throws URISyntaxException, InvalidKeyException, StorageException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(100 * 1024);

        System.out.println("downloadBlobToByteArray : " + uri);
        try {
            CloudStorageAccount account = CloudStorageAccount.parse(connectionString);
            CloudBlockBlob cloudBlockBlob = new CloudBlockBlob(URI.create(uri), account.getCredentials());
            cloudBlockBlob.download(outputStream);
            return outputStream.toByteArray();
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
            }
        }
    }

    public static File downloadBlobToFile(String connectionString, String container, String blobname, String fileName) throws IOException, StorageException, URISyntaxException, InvalidKeyException {
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        CloudBlobContainer containerReference = getCloudBlobContainer(connectionString, container);
        CloudBlockBlob blockBlobReference = containerReference.getBlockBlobReference(blobname);
        blockBlobReference.downloadToFile(fileName);

        return file;
    }

    private static CloudBlobContainer getCloudBlobContainer(String connectionString, String container) throws URISyntaxException, InvalidKeyException, StorageException {
        CloudStorageAccount account = CloudStorageAccount.parse(connectionString);
        CloudBlobClient cloudBlobClient = account.createCloudBlobClient();
        return cloudBlobClient.getContainerReference(container);
    }

    public static void uploadBlockBlob(String connectionString, String container, String blobname, InputStream input, long length) throws InvalidKeyException, StorageException, URISyntaxException, IOException {
        CloudBlobContainer containerReference = getCloudBlobContainer(connectionString, container);
        CloudBlockBlob blockBlobReference = containerReference.getBlockBlobReference(blobname);
        blockBlobReference.upload(input, length);
    }

    public static void uploadFileToBlob(String connectionString, String container, String blobname, String filePath) throws IOException, StorageException, URISyntaxException, InvalidKeyException {
        CloudBlobContainer containerReference = getCloudBlobContainer(connectionString, container);
        CloudBlockBlob blockBlobReference = containerReference.getBlockBlobReference(blobname);
        blockBlobReference.uploadFromFile(filePath);
    }

    public static void clearBlockBlob(String connectionString, String container, String blobname, InputStream input, long length) throws InvalidKeyException, StorageException, URISyntaxException, IOException {
        CloudBlobContainer containerReference = getCloudBlobContainer(connectionString, container);
        CloudBlockBlob blockBlobReference = containerReference.getBlockBlobReference(blobname);
        blockBlobReference.deleteIfExists();
    }
}