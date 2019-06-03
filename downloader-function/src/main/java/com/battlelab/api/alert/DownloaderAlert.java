package com.battlelab.api.alert;

import com.battlelab.Constant;
import com.battlelab.TraceHelper;
import com.battlelab.api.TableEntityHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.StorageAccount;
import com.microsoft.azure.functions.annotation.TableInput;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import com.microsoft.azure.storage.StorageException;
import com.sendgrid.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class DownloaderAlert implements TableEntityHelper {

    private static Gson gson = new Gson();
    private static final int ALERT_THRESHOLD = 3;
    private static final String ALERT_RECORD_TABLE_NAME = "alertRecord";
    private static final String ALERT_RECORD_PARTITION_KEY = "alertV1";

    @FunctionName("DownloaderAlert")
    @StorageAccount("AzureWebJobsStorage")
    public void run(
        @TimerTrigger(name = "timerInfo", schedule = "0 0 */1 * * *") String timerInfo,
        @TableInput(name = "configuration",
            tableName = "deployv2",
            partitionKey = "v1",
            rowKey = "latest")
            String configuration,
        final ExecutionContext context) throws StorageException, InvalidKeyException, URISyntaxException {

        TraceHelper trace = new TraceHelper(context.getLogger());

        JsonObject jsonObject = gson.fromJson(configuration, JsonObject.class);
        int threshold = jsonObject.get(Constant.DeployTask.THRESHOLD).getAsInt();
        String deployAt = jsonObject.get(Constant.DeployTask.DEPLOY_AT).getAsString();
        String taskId = jsonObject.get(Constant.DeployTask.TASK_ID).getAsString();

        DownloaderAlertRecordEntity alertRecord = query(ALERT_RECORD_TABLE_NAME, ALERT_RECORD_PARTITION_KEY, taskId,
            DownloaderAlertRecordEntity.class);

        if (alertRecord == null) {
            alertRecord = new DownloaderAlertRecordEntity();
            alertRecord.setFrequencyAlertCount(0);
            alertRecord.setTotalAlertCount(0);
            alertRecord.setRowKey(taskId);
            alertRecord.setPartitionKey(ALERT_RECORD_PARTITION_KEY);
            insert(ALERT_RECORD_TABLE_NAME, alertRecord);
        }

        try {

            LocalDateTime dateTime = LocalDateTime.parse(deployAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            long d = (LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli()
                - dateTime.toInstant(ZoneOffset.UTC).toEpochMilli()) / 1000;
            String timespan = secondsToISO8601Timespan(d);
            trace.trace("api警报 : timespan " + timespan);
            String countResult = executeQuery(Constant.Alert.COUNT_FUNCTION_NAME, timespan);
            trace.trace("api警报 : 总数查询结果 " + countResult);
            if (countResult == null) {
                sendEmailViaSendGrid("查询下发次数 失败.");
                return;
            }

            int count = parseCountResult(countResult);

            double rate = Double.valueOf(count) / threshold;
            if (rate > Constant.Alert.RATE && alertRecord.getTotalAlertCount() < ALERT_THRESHOLD) {
                StringBuilder body = new StringBuilder("下发比例超过阈值,");
                body.append("当前下发 : ");
                body.append(count);
                body.append(",总数 : ");
                body.append(threshold);
                body.append("\r\n\r\n");
                body.append("数据如下: \r\n\r\n");
                body.append(countResult);
                sendEmailViaSendGrid(body.toString());
                alertRecord.setTotalAlertCount(alertRecord.getTotalAlertCount() + 1);
                return;
            }

            String frequencyResult = executeQuery(Constant.Alert.FREQUENCY_FUNCTION_NAME, timespan);

            trace.trace("api警报 : 总数查询结果 " + frequencyResult);
            if (frequencyResult == null) {
                sendEmailViaSendGrid("查询 随机下发频次 失败.");
                return;
            }

            boolean needAlert = testFrequency(frequencyResult);
            if (needAlert && alertRecord.getFrequencyAlertCount() < ALERT_THRESHOLD) {
                StringBuilder body = new StringBuilder("单次下发频次超过阈值,");
                body.append("数据如下: \r\n\r\n");
                body.append(frequencyResult);
                sendEmailViaSendGrid(body.toString());
                alertRecord.setFrequencyAlertCount(alertRecord.getFrequencyAlertCount() + 1);
            }
        } finally {
            update(ALERT_RECORD_TABLE_NAME, alertRecord);
        }
    }


    private static void sendEmailViaSendGrid(String mailBody) {
        Email from = new Email("ApiLarity@tigerjoys.com");
        String subject = "Api云端化警报";

        String emails = System.getenv(Constant.Alert.AlertEmails);
        String[] split = emails.split(",");
        Personalization personalization = new Personalization();
        for (int i = 0; i < split.length; i++) {
            Email to = new Email(split[i]);
            personalization.addTo(to);
        }

        Content content = new Content("text/plain", mailBody);
        Mail mail = new Mail();
        mail.setFrom(from);
        mail.setSubject(subject);
        mail.addPersonalization(personalization);
        mail.addContent(content);
        SendGrid sg = new SendGrid(System.getenv("SENDGRID_API_KEY"));
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
//            System.out.println(response.getStatusCode());
//            System.out.println(response.getBody());
//            System.out.println(response.getHeaders());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static boolean testFrequency(String frequencyResult) {
        JsonObject jsonObject = gson.fromJson(frequencyResult, JsonObject.class);
        JsonArray array = jsonObject.get("tables").getAsJsonArray();
        JsonObject obj = array.get(0).getAsJsonObject();
        JsonArray rows = obj.get("rows").getAsJsonArray();
        return rows.size() > 0;
    }

    private static int parseCountResult(String countResult) {
        JsonObject jsonObject = gson.fromJson(countResult, JsonObject.class);
        JsonArray array = jsonObject.get("tables").getAsJsonArray();
        JsonObject obj = array.get(0).getAsJsonObject();
        JsonArray rows = obj.get("rows").getAsJsonArray();
        if (rows.size() > 0) {
            return rows.get(0).getAsInt();
        }
        return 0;
    }

    private static String executeQuery(String funcName, String timespan) {
        String queryUrl = System.getenv(Constant.Alert.ApplicationInsightQueryUrl);
        String apiKey = System.getenv(Constant.Alert.ApplicationInsightAPIKey);

        Map<String, String> headers = new HashMap<>();
        headers.put("x-api-key", apiKey);

        JsonObject body = new JsonObject();
        body.addProperty("timespan", timespan);
        body.addProperty("query", funcName);
        return httpPost(queryUrl, headers, body.toString());
    }

    private static String secondsToISO8601Timespan(long duration) {
        int d = (int) (duration / 86400);
        int rest = (int) (duration % 86400);
        int h = rest / 3600;
        rest = rest % 3600;
        int m = rest / 60;
        rest = rest % 60;
        int s = rest;
        StringBuilder span = new StringBuilder("P");
        if (d > 0) {
            span.append(String.format("%dD", d));
        }
        if ((h | m | s) > 0) {
            span.append(String.format("T%dH%dM%dS", h, m, s));
        }
        return span.toString();
    }

    private static String httpPost(String url, Map<String, String> headers, String body) {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(url);
        if (headers != null && headers.size() > 0) {
            headers.forEach((k, v) -> post.addHeader(k, v));
        }
        StringEntity entity = new StringEntity(body, ContentType.APPLICATION_JSON);
        post.setEntity(entity);
        CloseableHttpResponse response = null;
        try {
            response = client.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                return null;
            }
            return EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                }
            }
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }
}
