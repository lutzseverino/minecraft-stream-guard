package com.lutzseverino.streamguard.config;

import java.util.List;
import java.util.Set;

public interface SettingsReader {

    String string(String path, String fallback);

    boolean bool(String path, boolean fallback);

    int integer(String path, int fallback);

    List<String> stringList(String path);

    Set<String> keys(String path);
}
