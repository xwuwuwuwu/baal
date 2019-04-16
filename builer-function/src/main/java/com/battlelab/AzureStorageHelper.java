package com.battlelab;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

public class AzureStorageHelper {

    public static InputStream openBlockBlobInputStream(String connectionString, String container, String blobname) throws URISyntaxException, InvalidKeyException, StorageException {
        CloudBlobContainer containerReference = getCloudBlobContainer(connectionString, container);
        CloudBlockBlob blockBlobReference = containerReference.getBlockBlobReference(blobname);

        return blockBlobReference.openInputStream();
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
