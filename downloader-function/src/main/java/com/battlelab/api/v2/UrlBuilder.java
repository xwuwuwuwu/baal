package com.battlelab.api.v2;

import com.battlelab.Constant;
import com.battlelab.TraceHelper;
import com.battlelab.api.DownloadUriPrifixHelper;
import com.battlelab.api.JwtTokenHelper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.storage.StorageException;
import org.apache.http.HttpHeaders;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UrlBuilder implements DownloadUriPrifixHelper, DomainRecordEntityHelper {

    private static final String REAL_HOST_HEADER_KEY = "X-Real-Host";

    @FunctionName("UrlBuilder")
    public HttpResponseMessage run(
        @HttpTrigger(name = "req",
            methods = {HttpMethod.GET, HttpMethod.POST},
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "{version}/UrlBuilder")
            HttpRequestMessage<Optional<String>> request,
        @BindingName("version") String version,
        final ExecutionContext context) throws UnsupportedEncodingException, InvalidKeyException, StorageException, URISyntaxException {
        long start = System.currentTimeMillis();

        TraceHelper trace = new TraceHelper(context.getLogger());

        trace.trace("UrlBuilder : starts.");

        String domain = request.getHeaders().get(REAL_HOST_HEADER_KEY.toLowerCase());

        trace.info("UrlBuilder : real domain " + domain);

        DomainRecordEntity entity = queryRecordEntity(Constant.ApiV2.DOMAIN_RECORD_TABLE_NAME,
            Constant.ApiV2.DOMAIN_PARTITION_KEY, domain);
        String tag = entity.getTag();

        String abi = request.getQueryParameters().get(Constant.Downloader.ABI);

        trace.info(String.format("UrlBuilder : tag %s , abi %s", tag, abi));

        if (abi == null || abi.isEmpty()) {
            trace.trace("UrlBuilder : wrong abi.");
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("wrong x64 flag.").build();
        }
        boolean isX64 = false;
        String lowerCase = abi.toLowerCase();
        if (Constant.Downloader.ABIS_32.contains(lowerCase)) {
            isX64 = false;
        } else if (Constant.Downloader.ABIS_64.contains(lowerCase)) {
            isX64 = true;
        } else {
            trace.trace("UrlBuilder : wrong abi.");
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("abi not supported yet.").build();
        }

        Map<String, Serializable> values = new HashMap<>();
        values.put(Constant.Downloader.TAG, tag);
        values.put(Constant.Downloader.IS_X64, isX64);

        String withTag = JwtTokenHelper.create(values, Constant.Downloader.TOKEN_EXPIRED_SECONDS);

        //String s = Base64.getEncoder().encodeToString(withTag.getBytes());
        String s = URLEncoder.encode(withTag, "UTF-8");
        //目前只有V1版本
        String prefix = getPrifix(1);
        String formatter = prefix.endsWith("/") ? "http://%s%s" : "http://%s/%s";

        HttpResponseMessage response = request.createResponseBuilder(HttpStatus.MOVED_PERMANENTLY)
            .header(HttpHeaders.LOCATION, String.format(formatter, prefix, s)).build();
        trace.trace("UrlBuilder : cost " + (System.currentTimeMillis() - start));
        return response;
    }

}
