package com.coccoc.internal.segment;

import com.coccoc.Token;
import com.coccoc.internal.build.TriePacker;
import com.coccoc.internal.lang.VnLangTool;
import com.coccoc.internal.trie.MultitermTrie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SegmenterTest {

    @BeforeAll
    static void init() {
        VnLangTool.initSimple();
    }

    private static MultitermTrie simpleTrie() {
        TriePacker.HashNode root = TriePacker.buildHashTrie(new String[]{"a"});
        return TriePacker.pack(root);
    }

    // Slice 1 — empty input edge case
    @Test
    void segment_emptyString_returnsEmpty() {
        Segmenter seg = new Segmenter(simpleTrie());
        assertTrue(seg.segment("").isEmpty());
    }

    // Slice 2 — ASCII word via single-char fallback merge
    @Test
    void segment_singleAsciiWord_returnsWordToken() {
        Segmenter seg = new Segmenter(simpleTrie());
        List<Token> tokens = seg.segment("hello");
        assertEquals(1, tokens.size());
        assertEquals("hello", tokens.get(0).getText());
        assertEquals(Token.Type.WORD, tokens.get(0).getType());
    }

    // Slice 3 — space as explicit SPACE token between two words
    @Test
    void segment_twoWordsWithSpace_returnsWordSpaceWord() {
        Segmenter seg = new Segmenter(simpleTrie());
        List<Token> tokens = seg.segment("a b");
        assertEquals(3, tokens.size());
        assertEquals("a",  tokens.get(0).getText());
        assertEquals(Token.Type.WORD,  tokens.get(0).getType());
        assertEquals(" ",  tokens.get(1).getText());
        assertEquals(Token.Type.SPACE, tokens.get(1).getType());
        assertEquals("b",  tokens.get(2).getText());
        assertEquals(Token.Type.WORD,  tokens.get(2).getType());
    }

    // Slice 4 — punctuation character after a word
    @Test
    void segment_punctuation_returnsPunctToken() {
        Segmenter seg = new Segmenter(simpleTrie());
        List<Token> tokens = seg.segment("hello!");
        assertEquals(2, tokens.size());
        assertEquals("hello", tokens.get(0).getText());
        assertEquals(Token.Type.WORD,  tokens.get(0).getType());
        assertEquals("!",     tokens.get(1).getText());
        assertEquals(Token.Type.PUNCT, tokens.get(1).getType());
    }

    // Slice 5 — digit sequence classified as NUMBER
    @Test
    void segment_number_returnsNumberToken() {
        Segmenter seg = new Segmenter(simpleTrie());
        List<Token> tokens = seg.segment("42");
        assertEquals(1, tokens.size());
        assertEquals("42",       tokens.get(0).getText());
        assertEquals(Token.Type.NUMBER, tokens.get(0).getType());
    }

    // Slice 6 — known trie entry returned as one token (exercises trie path, not fallback)
    @Test
    void segment_knownDictWord_returnsSingleToken() {
        TriePacker.HashNode root = TriePacker.buildHashTrie(new String[]{"hello"});
        MultitermTrie trie = TriePacker.pack(root);
        Segmenter seg = new Segmenter(trie);
        List<Token> tokens = seg.segment("hello world");
        assertEquals(3, tokens.size());
        assertEquals("hello", tokens.get(0).getText());
        assertEquals(Token.Type.WORD,  tokens.get(0).getType());
        assertEquals(Token.Type.SPACE, tokens.get(1).getType());
        assertEquals("world", tokens.get(2).getText());
        assertEquals(Token.Type.WORD,  tokens.get(2).getType());
    }

    // M7c — multi-syllable Vietnamese: space (0x20) is a valid trie edge
    @Test
    void segment_multiSyllableVietnamese_spaceEdge_returnsSingleToken() {
        // Build a trie with "xin chao" as one entry (ASCII approximation avoids NFD issues)
        TriePacker.HashNode root = TriePacker.buildHashTrie(new String[]{"xin chao"});
        MultitermTrie trie = TriePacker.pack(root);
        Segmenter seg = new Segmenter(trie);
        List<Token> tokens = seg.segment("xin chao");
        // space-edge trie match should return one WORD, not [WORD, SPACE, WORD]
        assertEquals(1, tokens.size());
        assertEquals("xin chao", tokens.get(0).getText());
        assertEquals(Token.Type.WORD, tokens.get(0).getType());
    }

    // M7d — sticky syllable segmentation via SyllableTrie Viterbi
    @Test
    void splitSyllables_knownStickyPhrase_returnsCorrectSyllables() {
        com.coccoc.internal.trie.SyllableTrie sylTrie =
            com.coccoc.tools.DictCompileTestSupport.buildSyllableTrie("xin", "chao");
        // Empty bigram scores (2 rows, no entries)
        com.coccoc.internal.bigram.BigramScores noScores =
            new com.coccoc.internal.bigram.BigramScores(
                new int[]{0, 0, 0}, new int[]{}, new float[]{});

        TriePacker.HashNode root = TriePacker.buildHashTrie(new String[0]);
        MultitermTrie emptyMt = TriePacker.pack(root);
        Segmenter seg = new Segmenter(emptyMt, sylTrie, noScores);

        java.util.List<String> syllables = seg.splitSyllables("xinchao");
        assertEquals(2, syllables.size());
        assertEquals("xin",  syllables.get(0));
        assertEquals("chao", syllables.get(1));
    }
}
