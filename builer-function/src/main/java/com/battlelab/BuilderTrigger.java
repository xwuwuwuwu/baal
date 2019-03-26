package com.battlelab;

import com.google.gson.Gson;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.QueueTrigger;
import com.microsoft.azure.functions.annotation.StorageAccount;
import com.microsoft.azure.storage.StorageException;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.util.logging.Logger;

public class BuilderTrigger {
    private static final String AZURE_WEB_JOBS_STORAGE = "AzureWebJobsStorage";
    private static final String ABI_ARM32 = "armeabi";
    private static final String ABI_ARM64 = "arm64-v8a";
    private static final String DATA_ROOT_PATH = "/workspace";
    private static final String KEY_TASK_COUNTER_FORMAT = "counter_%s";
    private org.slf4j.Logger osLogger = LoggerFactory.getLogger(BuilderTrigger.class);

    private Logger logger;

    private void logD(String message) {
        logger.info(message);
    }

    @FunctionName(value = "builderTrigger")
    @StorageAccount(AZURE_WEB_JOBS_STORAGE)
    public void start(@QueueTrigger(name = "msg",
            dataType = "string",
            queueName = "buildtask")
                              String message,
                      final ExecutionContext context) {

        logger = context.getLogger();

        context.getLogger().info(String.format("QueueTrigger: 收到 %s", message));
        osLogger.info("QueueTrigger: 收到 {}",message);
        int errorNum = 0;
        Gson gson = new Gson();
        BuilderTaskInfo taskInfo = gson.fromJson(message, BuilderTaskInfo.class);

        String connectionString = System.getenv(AZURE_WEB_JOBS_STORAGE);

        do {
            File sourceCodeZip = downloadSourceCodeZip(connectionString, taskInfo.getSourceContainer(), taskInfo.getSourceName());
            if (sourceCodeZip == null) {
                errorNum = -1;
                break;
            }
            String dest = Paths.get(DATA_ROOT_PATH, "sourceCode").toFile().getPath();
            boolean unzip = unzipSource(context, sourceCodeZip, dest);
            if (!unzip) {
                errorNum = -2;
                break;
            }

            String outputPath = Paths.get(DATA_ROOT_PATH, "out").toFile().getPath();

            boolean b32 = build(dest, ABI_ARM32, outputPath);
            if (!b32) {
                errorNum = -3;
                break;
            }

            boolean b64 = build(dest, ABI_ARM64, outputPath);
            if (!b64) {
                errorNum = -4;
                break;
            }

            int index = getUploadIndex(taskInfo.getTaskId());

            boolean rst = uploadTargetToBlob(connectionString, taskInfo.getTargetContainer()
                    , taskInfo.getTaskId(), outputPath, index);
            if (!rst) {
                errorNum = -5;
            }

        } while (false);
        context.getLogger().info(String.format("QueueTrigger: errorNum %d", errorNum));
        osLogger.info("QueueTrigger: errorNum {}", errorNum);
    }

    private boolean uploadTargetToBlob(String connectionString, String container, String taskId,
                                       String outputPath, int index) {
        File so32 = Paths.get(outputPath, ABI_ARM32, "libbootloader.so").toFile();
        File so64 = Paths.get(outputPath, ABI_ARM64, "libbootloader.so").toFile();

        String blob32 = getBlobName(taskId, ABI_ARM32, index);
        String blob64 = getBlobName(taskId, ABI_ARM64, index);

        try {
            AzureStorageHelper.uploadFileToBlob(connectionString, container, blob32, so32.getPath());
            AzureStorageHelper.uploadFileToBlob(connectionString, container, blob64, so64.getPath());
            return true;
        } catch (StorageException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String getBlobName(String taskId, String abi, int index) {
        return String.format("%s/%d/%s/libbootloader.so", taskId, index, abi);
    }

    private int getUploadIndex(String taskId) {
        Jedis jedis = JedisHelper.buildJedis();
        try {
            String format = String.format(KEY_TASK_COUNTER_FORMAT, taskId);
            return (int) (jedis.incr(format) - 1);
        } finally {
            JedisHelper.destoryJedis(jedis);
        }
    }

    private boolean unzipSource(ExecutionContext context, File sourceCodeZip, String dest) {
        String unzipCommand = String.format("unzip -o %s -d %s", sourceCodeZip.getPath(), dest);
        context.getLogger().info(String.format("QueueTrigger: unzip cmd  %s", unzipCommand));
        return execCommand(unzipCommand);
    }

    private boolean build(String source, String abi, String outPath) {
        File path = Paths.get("/workspace", abi).toFile();
        if (path.exists()) {
            path.delete();
        }
        path.mkdirs();

        String p1 = path.getPath();
        if (!copyFolder(source, p1)) {
            return false;
        }

        String tmpPath = Paths.get("/workspace", "outTmp").toFile().getPath();

        String buildCommand = String.format("azure-build.sh %s %s %s", abi, p1, tmpPath);
        osLogger.info("buildCommand {}",buildCommand);
        logD(String.format("QueueTrigger: cmd %s", buildCommand));

        if (!execCommand(buildCommand)) {
            return false;
        }
        return copyFolder(tmpPath, outPath);
    }

    private File downloadSourceCodeZip(String connectionString, String sourceContainer, String sourceName) {
        String path = Paths.get("/workspace", "source.zip").toFile().getPath();
        try {
            File file = AzureStorageHelper.downloadBlobToFile(connectionString, sourceContainer, sourceName, path);
            return file;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean copyFolder(String source, String dest) {
        //String s = source.endsWith("/") ? source : String.format("%s/", source);
        //String cmd = String.format("cp -r %s %s", s, dest);
        try {
            FileUtils.copyDirectory(new File(source),new File(dest));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        //osLogger.info("CMD {}",cmd);
        return false;
    }

    private boolean execCommand(String command) {
        try {
            Process exec = Runtime.getRuntime().exec("/bin/sh");
            OutputStream out = exec.getOutputStream();
            String c = command.endsWith("\n") ? command : String.format("%s\n", command);
            out.write(c.getBytes());
            out.write("exit\n".getBytes());
            out.flush();
            int i = exec.waitFor();
            if (i != 0) {
                return false;
            }
            out.close();
            exec.destroy();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

}
