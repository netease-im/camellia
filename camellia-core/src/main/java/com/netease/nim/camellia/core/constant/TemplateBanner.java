package com.netease.nim.camellia.core.constant;

import java.io.PrintStream;


/**
 * Created by caojiajun on 2026/3/8
 */
public class TemplateBanner {

    private String banner = "\n" +
            "_________                       .__  .__  .__        \n" +
            "\\_   ___ \\_____    _____   ____ |  | |  | |__|____   \n" +
            "/    \\  \\/\\__  \\  /     \\_/ __ \\|  | |  | |  \\__  \\  \n" +
            "\\     \\____/ __ \\|  Y Y  \\  ___/|  |_|  |_|  |/ __ \\_\n" +
            " \\______  (____  /__|_|  /\\___  >____/____/__(____  /\n" +
            "        \\/     \\/      \\/     \\/                  \\/ \n" +
            " :: ${camellia.application.name} ::     (${camellia.version})\n";

    public TemplateBanner(String camelliaApplicationName) {
        banner = banner.replace("${camellia.application.name}", camelliaApplicationName);
        banner = banner.replace("${camellia.version}", CamelliaVersion.version);
    }

    protected final void print(PrintStream out) {
        out.print(banner);
    }
}
