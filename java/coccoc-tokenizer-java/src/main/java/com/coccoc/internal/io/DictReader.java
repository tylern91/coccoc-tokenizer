package com.coccoc.internal.io;

import com.coccoc.internal.bigram.BigramScores;
import com.coccoc.internal.trie.MultitermTrie;
import com.coccoc.internal.trie.SyllableTrie;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Reads the three binary dict files written by DictCompile.
 * Format per file: magic(4) | version=1(4) | payload... | crc32(4) — all LE.
 * CRC covers every byte from position 4 (after magic) through end-of-payload;
 * the magic itself and the trailing CRC bytes are excluded from the CRC window.
 */
public final class DictReader {

    private DictReader() {}

    public static MultitermTrie readMultiterm(Path file) throws IOException {
        byte[] bytes = loadAndVerify(file, "CCMT");
        // Parse payload: buf positioned right after magic+version (byte offset 8)
        ByteBuffer buf = ByteBuffer.wrap(bytes, 8, bytes.length - 8).order(ByteOrder.LITTLE_ENDIAN);

        int alphaSize = buf.getInt();
        int[] codepoints = new int[alphaSize];
        for (int i = 0; i < alphaSize; i++) codepoints[i] = buf.getInt();

        int sz = buf.getInt();
        int[]   base   = new int[sz];
        int[]   parent = new int[sz];
        float[] weight = new float[sz];
        byte[]  flags  = new byte[sz];
        for (int i = 0; i < sz; i++) base[i]   = buf.getInt();
        for (int i = 0; i < sz; i++) parent[i] = buf.getInt();
        for (int i = 0; i < sz; i++) weight[i] = buf.getFloat();
        buf.get(flags);

        return new MultitermTrie(buildCharMap(codepoints), base, parent, weight, flags);
    }

    public static SyllableTrie readSyllable(Path file) throws IOException {
        byte[] bytes = loadAndVerify(file, "CCSY");
        ByteBuffer buf = ByteBuffer.wrap(bytes, 8, bytes.length - 8).order(ByteOrder.LITTLE_ENDIAN);

        int alphaSize = buf.getInt();
        int[] codepoints = new int[alphaSize];
        for (int i = 0; i < alphaSize; i++) codepoints[i] = buf.getInt();

        int sz = buf.getInt();
        int[]   base   = new int[sz];
        int[]   parent = new int[sz];
        float[] weight = new float[sz];
        int[]   index  = new int[sz];
        for (int i = 0; i < sz; i++) base[i]   = buf.getInt();
        for (int i = 0; i < sz; i++) parent[i] = buf.getInt();
        for (int i = 0; i < sz; i++) weight[i] = buf.getFloat();
        for (int i = 0; i < sz; i++) index[i]  = buf.getInt();
        // syllableCount field read and discarded; bigram.bin carries its own rowCount
        // buf.getInt(); // syllableCount

        return new SyllableTrie(buildCharMap(codepoints), base, parent, index, weight);
    }

    public static BigramScores readBigram(Path file) throws IOException {
        byte[] bytes = loadAndVerify(file, "CCBG");
        ByteBuffer buf = ByteBuffer.wrap(bytes, 8, bytes.length - 8).order(ByteOrder.LITTLE_ENDIAN);

        int n = buf.getInt();            // rowCount
        int[] rowOffset = new int[n + 1];
        for (int i = 0; i <= n; i++) rowOffset[i] = buf.getInt();

        int totalNnz = rowOffset[n];
        int[]   colIndex = new int[totalNnz];
        float[] value    = new float[totalNnz];
        for (int i = 0; i < totalNnz; i++) colIndex[i] = buf.getInt();
        for (int i = 0; i < totalNnz; i++) value[i]    = buf.getFloat();

        return new BigramScores(rowOffset, colIndex, value);
    }

    // =========================================================================
    // Shared low-level helpers (package-private for tests)
    // =========================================================================

    /** Load file bytes and verify magic, version, and CRC before parsing. */
    static byte[] loadAndVerify(Path file, String expectedMagic) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        if (bytes.length < 12)
            throw new IOException("truncated: " + file.getFileName() + " too short");

        // Magic check (bytes 0-3)
        if (bytes[0] != expectedMagic.charAt(0) || bytes[1] != expectedMagic.charAt(1)
                || bytes[2] != expectedMagic.charAt(2) || bytes[3] != expectedMagic.charAt(3))
            throw new IOException("bad magic: expected " + expectedMagic
                    + ", got " + new String(bytes, 0, 4));

        // Version check (bytes 4-7, LE)
        int version = leInt(bytes, 4);
        if (version != 1)
            throw new IOException("version mismatch: expected 1, got " + version);

        // CRC check: covers bytes[4..bytes.length-5] (after magic, before trailer)
        CRC32 crc = new CRC32();
        crc.update(bytes, 4, bytes.length - 8);
        int stored = leInt(bytes, bytes.length - 4);
        if ((int) crc.getValue() != stored)
            throw new IOException("crc mismatch: payload corrupted");

        return bytes;
    }

    /** Reconstruct charMap (codepoint→alphabet-index) from on-disk codepoints array. */
    static int[] buildCharMap(int[] codepoints) {
        if (codepoints.length == 0) return new int[0];
        int maxCp = 0;
        for (int cp : codepoints) if (cp > maxCp) maxCp = cp;
        int[] charMap = new int[maxCp + 1];
        Arrays.fill(charMap, -1);
        for (int i = 0; i < codepoints.length; i++) charMap[codepoints[i]] = i;
        return charMap;
    }

    private static int leInt(byte[] b, int off) {
        return (b[off] & 0xFF)
             | ((b[off + 1] & 0xFF) << 8)
             | ((b[off + 2] & 0xFF) << 16)
             | ((b[off + 3] & 0xFF) << 24);
    }
}
