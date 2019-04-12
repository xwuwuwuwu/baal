package com.lab.azure.bootloader.utils;

import java.util.UUID;

public class Helper {
    public static boolean checkKey(String keyString) {
        try {
            UUID uuid = UUID.fromString(keyString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
