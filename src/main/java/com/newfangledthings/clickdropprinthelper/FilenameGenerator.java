package com.newfangledthings.clickdropprinthelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class FilenameGenerator {

    private static final AtomicInteger counter = new AtomicInteger(0);

    public static String generateFilename(String originalFilename, String filetype) {
        return generateFilename(originalFilename,filetype,"pdf");
    }

    /**
     * Generate a timestamped filename
     *
     * @param originalFilename the original filename
     * @param filetype         the type of file
     * @param extension        the file extension
     * @return a timestamped filename
     */
    public static String generateFilename(String originalFilename, String filetype, String extension) {
        String baseName = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        int increment = counter.incrementAndGet();
        return timestamp + "-" + filetype + "-" + increment + "-" + baseName + "." + extension;
    }

}