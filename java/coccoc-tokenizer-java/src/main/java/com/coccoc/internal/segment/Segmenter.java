package com.coccoc.internal.segment;

import com.coccoc.Token;
import com.coccoc.internal.lang.VnLangTool;
import com.coccoc.internal.trie.MultitermTrie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pure-Java Viterbi segmenter — NORMAL mode, M7b scope.
 * Forward DP over MultitermTrie; single-char fallback for trie misses;
 * consecutive same-type spans merged during traceback.
 */
public final class Segmenter {

    private final MultitermTrie multitermTrie;

    public Segmenter(MultitermTrie multitermTrie) {
        this.multitermTrie = multitermTrie;
    }

    public List<Token> segment(String text) {
        if (text.isEmpty()) return Collections.emptyList();

        int[] cps = VnLangTool.normalizeNfd(text.codePoints().toArray());
        int n = cps.length;

        float[] best = new float[n + 1];
        Arrays.fill(best, Float.NEGATIVE_INFINITY);
        best[0] = 0.0f;
        int[] trace = new int[n + 1];
        Arrays.fill(trace, -1);

        for (int i = 0; i < n; i++) {
            if (best[i] == Float.NEGATIVE_INFINITY) continue;

            // Trie matches starting at i
            int node = 0;
            for (int j = i; j < n; j++) {
                node = multitermTrie.findChild(node, cps[j]);
                if (node == -1) break;
                if (multitermTrie.isEnding(node)) {
                    float w = best[i] + multitermTrie.getWeight(node);
                    if (w > best[j + 1]) {
                        best[j + 1] = w;
                        trace[j + 1] = i;
                    }
                }
            }

            // Single-char fallback: cover i+1 if still unreachable
            if (i + 1 <= n && best[i + 1] == Float.NEGATIVE_INFINITY) {
                best[i + 1] = best[i];
                trace[i + 1] = i;
            }
        }

        // Traceback: collect (start, end) spans in reverse
        List<int[]> spans = new ArrayList<>();
        for (int pos = n; pos > 0; ) {
            int start = trace[pos];
            spans.add(new int[]{start, pos});
            pos = start;
        }
        Collections.reverse(spans);

        // Convert spans to tokens; merge consecutive same-type WORD/NUMBER spans
        List<Token> tokens = new ArrayList<>();
        for (int[] span : spans) {
            String spanText = new String(cps, span[0], span[1] - span[0]);
            Token.Type type = classifySpan(cps, span[0], span[1]);
            if (!tokens.isEmpty()) {
                Token last = tokens.get(tokens.size() - 1);
                if (canMerge(last.getType(), type)) {
                    tokens.set(tokens.size() - 1,
                            new Token(last.getText() + spanText, type, last.getPos(), span[1]));
                    continue;
                }
            }
            tokens.add(new Token(spanText, type, span[0], span[1]));
        }
        return tokens;
    }

    private Token.Type classifySpan(int[] cps, int from, int to) {
        if (to - from == 1 && cps[from] == ' ') return Token.Type.SPACE;
        boolean hasAlpha = false, hasNumeric = false;
        for (int i = from; i < to; i++) {
            if (VnLangTool.isAlphabetic(cps[i])) { hasAlpha = true; break; }
            if (VnLangTool.isNumeric(cps[i]))     hasNumeric = true;
        }
        if (hasAlpha)   return Token.Type.WORD;
        if (hasNumeric) return Token.Type.NUMBER;
        return Token.Type.PUNCT;
    }

    private boolean canMerge(Token.Type a, Token.Type b) {
        return a == b && (a == Token.Type.WORD || a == Token.Type.NUMBER);
    }
}
