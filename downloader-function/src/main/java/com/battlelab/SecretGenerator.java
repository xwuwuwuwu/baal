package com.battlelab;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Optional;

public class SecretGenerator {

    private static final int CURRENT_VERSION = 1;

    @FunctionName("secret")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "v1/secret")
                    HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        String tag = request.getQueryParameters().get("tag");
        if (tag == null || tag.isEmpty() || tag.length() > Constant.MAX_TAG_LENGTH) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please pass a tag on the query string or in the request body").build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .body(SecretHelper.makeSecret(tag, CURRENT_VERSION)).build();
    }

}
