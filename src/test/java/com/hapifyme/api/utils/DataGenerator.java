package com.hapifyme.api.utils;

public class DataGenerator {
    public static String generateRandomEmail() {
        return "qa_user_" + System.currentTimeMillis() + "@example.com";
    }
}