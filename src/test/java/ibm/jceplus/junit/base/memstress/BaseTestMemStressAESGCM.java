/*
 * Copyright IBM Corp. 2023, 2024
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms provided by IBM in the LICENSE file that accompanied
 * this code, including the "Classpath" Exception described therein.
 */

package ibm.jceplus.junit.base.memstress;

import ibm.jceplus.junit.base.BaseTestJunit5;
import java.lang.reflect.Method;
import java.security.AlgorithmParameters;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BaseTestMemStressAESGCM extends BaseTestJunit5 {

    // 16 bytes: PASSED
    static final byte[] plainText16 = "1234567812345678".getBytes();

    static final byte[] plainText1024 = new byte[1024];

    protected KeyGenerator aesKeyGen;
    protected SecretKey key;
    protected AlgorithmParameters params = null;
    protected Cipher cp = null;
    protected boolean success = true;
    protected Method methodCipherUpdateAAD = null;
    protected Method methodGCMParameterSpecSetAAD = null;
    protected int specifiedKeySize = 0;
    int numTimes = 100;
    boolean printheapstats = false;

    @BeforeEach
    public void setUp() throws Exception {
        aesKeyGen = KeyGenerator.getInstance("AES", getProviderName());
        if (specifiedKeySize > 0) {
            aesKeyGen.init(specifiedKeySize);
        }
        key = aesKeyGen.generateKey();
        String numTimesStr = System.getProperty("com.ibm.jceplus.memstress.numtimes");
        if (numTimesStr != null) {
            numTimes = Integer.valueOf(numTimesStr);
        }
        printheapstats = Boolean
                .valueOf(System.getProperty("com.ibm.jceplus.memstress.printheapstats"));
        System.out.println("Testing AESGCM");
    }

    @Test
    public void testAES_GCM() throws Exception {
        // Test AES GCM Cipher
        cp = Cipher.getInstance("AES/GCM/NoPadding", getProviderName());
        Runtime rt = Runtime.getRuntime();
        long prevTotalMemory = 0;
        long prevFreeMemory = rt.freeMemory();
        long currentTotalMemory = 0;
        long currentFreeMemory = 0;
        long currentUsedMemory = 0;
        long prevUsedMemory = 0;


        for (int i = 0; i < numTimes; i++) {
            encryptDecrypt(cp);
            currentTotalMemory = rt.totalMemory();
            currentFreeMemory = rt.freeMemory();
            currentUsedMemory = currentTotalMemory - currentFreeMemory;
            prevUsedMemory = prevTotalMemory - prevFreeMemory;
            if (currentTotalMemory != prevTotalMemory || currentFreeMemory != prevFreeMemory) {
                if (printheapstats) {
                    System.out.println("AESGCM " + specifiedKeySize + " Iteration = " + i + " "
                            + "Total: = " + currentTotalMemory + " " + "currentUsed: = "
                            + currentUsedMemory + " " + "freeMemory: " + currentFreeMemory
                            + " prevUsedMemory: " + prevUsedMemory);
                }
                prevTotalMemory = currentTotalMemory;
                prevFreeMemory = currentFreeMemory;
            }
        }
    }

    protected void encryptDecrypt(Cipher cp) throws Exception {
        cp.init(Cipher.ENCRYPT_MODE, key);
        byte[] cipherText = cp.doFinal(plainText1024);
        params = cp.getParameters();

        // Verify the text
        cp.init(Cipher.DECRYPT_MODE, key, params);
        byte[] newPlainText = cp.doFinal(cipherText, 0, cipherText.length);
        assertTrue(java.util.Arrays.equals(plainText1024, newPlainText));
    }

    protected void encryptDecrypt(String algorithm) throws Exception {
        encryptDecrypt(algorithm, false);
    }

    protected void encryptDecrypt(String algorithm, boolean requireLengthMultipleBlockSize)
            throws Exception {
        encryptDecrypt(algorithm, requireLengthMultipleBlockSize, null);
    }

    protected void encryptDecrypt(String algorithm, boolean requireLengthMultipleBlockSize,
            AlgorithmParameters algParams) throws Exception {
        encryptDecrypt(algorithm, requireLengthMultipleBlockSize, algParams, plainText1024);
    }

    protected void encryptDecrypt(String algorithm, boolean requireLengthMultipleBlockSize,
            AlgorithmParameters algParams, byte[] message) throws Exception {
        encryptDecryptDoFinal(algorithm, requireLengthMultipleBlockSize, algParams, message);
    }

    // --------------------------------------------------------------------------
    // Run encrypt/decrypt test using just doFinal calls
    //
    protected void encryptDecryptDoFinal(String algorithm, boolean requireLengthMultipleBlockSize,
            AlgorithmParameters algParams, byte[] message) throws Exception

    {
        cp = Cipher.getInstance(algorithm, getProviderName());
        if (algParams == null) {
            cp.init(Cipher.ENCRYPT_MODE, key);
        } else {
            cp.init(Cipher.ENCRYPT_MODE, key, algParams);
        }
        int blockSize = cp.getBlockSize();
        try {
            byte[] cipherText = cp.doFinal(message);
            params = cp.getParameters();

            if (requireLengthMultipleBlockSize) {
                assertTrue(((blockSize > 0) && (message.length % blockSize) == 0),
                        "Did not get expected IllegalBlockSizeException, blockSize=" + blockSize
                                + ", msglen=" + message.length);
            }

            // Verify the text
            cp.init(Cipher.DECRYPT_MODE, key, params);
            byte[] newPlainText = cp.doFinal(cipherText);



            // Verify the text again
            cp.init(Cipher.DECRYPT_MODE, key, params);
            byte[] newPlainText2 = cp.doFinal(cipherText, 0, cipherText.length);
            assertTrue(java.util.Arrays.equals(newPlainText, newPlainText2));

        } catch (IllegalBlockSizeException e) {
            assertTrue((!requireLengthMultipleBlockSize || (message.length % blockSize) != 0),
                    "Unexpected IllegalBlockSizeException, blockSize=" + blockSize + ", msglen="
                            + message.length);
        }
    }


}
