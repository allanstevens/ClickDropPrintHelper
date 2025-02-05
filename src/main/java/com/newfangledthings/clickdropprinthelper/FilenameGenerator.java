package com.newfangledthings.clickdropprinthelper;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

public class FilenameGenerator {

    public static String generateTimestampedFilename(String originalFilename, String filetype) {
        return generateTimestampedFilename(originalFilename,filetype,"pdf");
    }

    /**
     * Generate a timestamped filename
     *
     * @param originalFilename the original filename
     * @param filetype         the type of file
     * @param extension        the file extension
     * @return a timestamped filename
     */
    public static String generateTimestampedFilename(String originalFilename, String filetype, String extension) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String uniqueID = generateShortUUID();
        //String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        String baseName = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
        return baseName + "_" + timestamp + "_" + uniqueID + "_" + filetype + "." + extension;
    }

    /**
     * Generate a short UUID
     *
     * @return a short UUID
     */
    public static String generateShortUUID() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(byteBuffer.array());
    }
}