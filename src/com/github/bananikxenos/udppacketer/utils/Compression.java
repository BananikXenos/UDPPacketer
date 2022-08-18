package com.github.bananikxenos.udppacketer.utils;

import java.io.ByteArrayOutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

public class Compression {

    public static final byte[] MAGIC_HEADER = "§ˇ".getBytes();

    /**
     * Compresses byte array
     * @param in array
     * @return compressed array
     */
    public static byte[] compress(byte[] in) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DeflaterOutputStream defl = new DeflaterOutputStream(out);
            defl.write(in);
            defl.flush();
            defl.close();

            byte[] compressed =  out.toByteArray();

            return ByteUtils.concatenate(MAGIC_HEADER, compressed);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(150);
            return null;
        }
    }

    /**
     * Decompresses array
     * @param in array
     * @return decompressed array
     */
    public static byte[] decompress(byte[] in) {
        try {
            byte[] in2 = ByteUtils.remove(in, MAGIC_HEADER.length);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InflaterOutputStream infl = new InflaterOutputStream(out);
            infl.write(in2);
            infl.flush();
            infl.close();

            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(150);
            return null;
        }
    }

    /**
     * Checks if array is compressed
     * @param in array
     * @return is compressed
     */
    public static boolean isCompressed(byte[] in){
        for(int i = 0; i < MAGIC_HEADER.length; i++){
            if(in[i] != MAGIC_HEADER[i])
                return false;
        }

        return true;
    }
}
