package com.battlelab.api;

import com.battlelab.Constant;

import java.util.Random;

public interface DownloadUriPrifixHelper {
    default String getPrifix(int version){
        String uris = System.getenv(String.format(Constant.Downloader.DOWNLOADER_URIS_FORMATTER, version));
        if (uris == null || uris.isEmpty()) {
            throw new RuntimeException("missing hosts");
        }
        uris.replace('\\', '/');
        String[] split = uris.split(",");
        return split[split.length == 1 ? 0 : new Random().nextInt(split.length - 1)];
    }
}
