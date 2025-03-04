// ConsoleApp.java
package com.newfangledthings.clickdropprinthelper;

import java.io.IOException;

public class ConsoleApp {
    public static void main(String[] args) throws IOException, InterruptedException {
        Config config;
        if (args.length == 1) {
            config = new Config(args[0]);
        } else {
            config = new Config("config.properties");
        }
        FileWatcher fileWatcher = new FileWatcher(config, true);
        fileWatcher.watch();
    }
}