package com.coccoc.internal.bigram;

/**
 * CSR-format bigram frequency scores loaded from bigram.bin.
 * Populated by DictReader; score lookup is M6.
 */
public final class BigramScores {
    private final int[]   rowOffset;
    private final int[]   colIndex;
    private final float[] value;

    public BigramScores(int[] rowOffset, int[] colIndex, float[] value) {
        this.rowOffset = rowOffset;
        this.colIndex  = colIndex;
        this.value     = value;
    }

    public int[]   rowOffsets() { return rowOffset; }
    public int[]   colIndex()   { return colIndex; }
    public float[] values()     { return value; }
    public int     rowCount()   { return rowOffset.length - 1; }
}
