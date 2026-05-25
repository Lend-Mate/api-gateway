package com.lendmate.api_gateway.config;

import java.util.List;

public class SecurityPaths {

    public static final List<String> PUBLIC_PATHS = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/refresh"
    );
}