package com.battlelab.downloader;

import com.battlelab.Constant;

public class SecretHelper {
    public static String makeSecret(String tag, int version) {
        switch (version) {
            case 1:
                String s = String.format(Constant.SECRET_V1_FORMATER, tag);
                return Sha256Helper.hash(s);
            default:
                throw new RuntimeException("version not supported yet.");
        }
    }
}
