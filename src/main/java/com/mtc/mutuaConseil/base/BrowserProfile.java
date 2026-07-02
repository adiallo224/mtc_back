package com.mtc.mutuaConseil.base;

public record BrowserProfile(
        String name,
        String userAgent,
        String locale,
        String timezone,
        int width,
        int height,
        double latitude,
        double longitude,
        int cpuCores,
        int memoryGb
) { }
