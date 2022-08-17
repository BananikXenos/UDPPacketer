package com.github.bananikxenos.udppacketer.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ByteUtils {

    /**
     * Merges 2 byte arrays
     * @param a array
     * @param b array
     * @return merged array
     * @throws IOException exception
     */
    public static byte[] concatenate(byte[] a, byte[] b) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(a);
        outputStream.write(b);

        return outputStream.toByteArray();
    }

    /**
     * Removes bytes from the front of array
     * @param a array
     * @param num number of bytes
     * @return edited array
     */
    public static byte[] remove(byte[] a, int num)
    {
        byte[] b = new byte[a.length - num];

        for(int i=num;i<a.length;i++)
        {
            b[i-num]=a[i];
        }

        return b;
    }
}
