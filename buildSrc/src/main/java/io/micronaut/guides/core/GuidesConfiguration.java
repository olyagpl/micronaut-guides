package io.micronaut.guides.core;

import java.util.List;

public interface GuidesConfiguration {
    String getHomePageUrl();
    String getTitle();
    String getLicensePath();
    String getPackageName();
    List<String> getFilesWithHeader();
}