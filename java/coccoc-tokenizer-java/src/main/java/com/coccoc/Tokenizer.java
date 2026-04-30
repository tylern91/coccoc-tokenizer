package com.coccoc;

import com.coccoc.internal.bigram.BigramScores;
import com.coccoc.internal.io.DictReader;
import com.coccoc.internal.trie.MultitermTrie;
import com.coccoc.internal.trie.SyllableTrie;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Public facade for the CocCoc Vietnamese tokenizer.
 *
 * API is binary-compatible with the vendored Tokenizer in
 * elasticsearch-analysis-vietnamese so the plugin can swap to this Maven
 * artifact without changing any call sites.
 *
 * Lifecycle: singleton per dict-path, lazily initialized via getInstance().
 * All segment*() methods throw UnsupportedOperationException until M7 lands.
 */
public class Tokenizer {

    // Integer constants kept for source-level back-compat with old callers.
    public static final int TOKENIZE_NORMAL = 0;
    public static final int TOKENIZE_HOST   = 1;
    public static final int TOKENIZE_URL    = 2;

    private static volatile Tokenizer instance;
    private static String initializedDictPath;

    private final MultitermTrie multitermTrie;
    private final SyllableTrie  syllableTrie;
    private final BigramScores  bigramScores;
    private final String        dictPath;

    // -----------------------------------------------------------------------
    // Singleton factory
    // -----------------------------------------------------------------------

    public static synchronized Tokenizer getInstance(String dictPath) throws IOException {
        if (instance == null) {
            instance = new Tokenizer(dictPath);
            initializedDictPath = dictPath;
        } else if (!initializedDictPath.equals(dictPath)) {
            throw new IllegalStateException(
                    "Tokenizer already initialized with dictPath=" + initializedDictPath);
        }
        return instance;
    }

    /** Package-private: resets singleton state between tests. */
    static synchronized void resetForTesting() {
        instance = null;
        initializedDictPath = null;
    }

    Tokenizer(String dictPath) throws IOException {
        Path dir = Path.of(dictPath);
        this.multitermTrie = DictReader.readMultiterm(dir.resolve("multiterm.bin"));
        this.syllableTrie  = DictReader.readSyllable(dir.resolve("syllable.bin"));
        this.bigramScores  = DictReader.readBigram(dir.resolve("bigram.bin"));
        this.dictPath      = dictPath;
    }

    // -----------------------------------------------------------------------
    // Primary segment API (enum-based — used by the ES plugin)
    // -----------------------------------------------------------------------

    public List<Token> segment(String text, TokenizeOption option, boolean keepPunctuation) {
        throw new UnsupportedOperationException("Tokenizer not yet implemented (M7)");
    }

    // -----------------------------------------------------------------------
    // Convenience overloads (int-based — back-compat with original module)
    // -----------------------------------------------------------------------

    public ArrayList<Token> segment(String text, boolean forTransforming, int tokenizeOption, boolean keepPuncts) {
        throw new UnsupportedOperationException("Tokenizer not yet implemented (M7)");
    }

    public ArrayList<Token> segment(String text, boolean forTransforming, int tokenizeOption) {
        return segment(text, forTransforming, tokenizeOption, forTransforming);
    }

    public ArrayList<Token> segment(String text, int tokenizeOption) {
        return segment(text, false, tokenizeOption);
    }

    public ArrayList<Token> segment(String text, boolean forTransforming) {
        return segment(text, forTransforming, TOKENIZE_NORMAL);
    }

    public ArrayList<Token> segment(String text) {
        return segment(text, false);
    }

    public ArrayList<String> segmentToStringList(String text) {
        return Token.toStringList(segment(text, false));
    }

    public ArrayList<Token> segmentKeepPuncts(String text) {
        return segment(text, false, TOKENIZE_NORMAL, true);
    }

    public ArrayList<String> segmentKeepPunctsToStringList(String text) {
        return Token.toStringList(segmentKeepPuncts(text));
    }

    public ArrayList<Token> segmentUrl(String text) {
        return segment(text, false, TOKENIZE_URL);
    }

    public ArrayList<String> segmentUrlToStringList(String text) {
        return Token.toStringList(segmentUrl(text));
    }

    public ArrayList<Token> segment4Transforming(String text) {
        return segment(text, true, TOKENIZE_NORMAL);
    }

    public ArrayList<Token> segment4Transforming(String text, int tokenizeOption) {
        return segment(text, true, tokenizeOption);
    }
}
