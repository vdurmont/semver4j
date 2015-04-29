package com.vdurmont.semver4j;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Utility class to convert a NPM requirement string into a list of tokens.
 */
public class Tokenizer {
    private static final Map<Character, Token> SPECIAL_CHARS;

    static {
        SPECIAL_CHARS = new HashMap<>();
        for (TokenType type : TokenType.values()) {
            if (type.character != null) {
                SPECIAL_CHARS.put(type.character, new Token(type));
            }
        }
    }

    /**
     * Takes a NPM requirement string and creates a list of tokens by performing 3 operations:
     * - If the token is a version, it will add the version string
     * - If the token is an operator, it will add the operator
     * - It will insert missing "AND" operators for ranges
     *
     * @param requirement the requirement string
     *
     * @return the list of tokens
     */
    protected static List<Token> tokenize(String requirement) {
        // Replace the tokens made of 2 chars
        requirement = requirement.replace("||", "|")
                .replace("<=", "≤")
                .replace(">=", "≥");


        LinkedList<Token> tokens = new LinkedList<>();
        Token previousToken = null;

        char[] chars = requirement.toCharArray();
        Token token = null;
        for (char c : chars) {
            if (c == ' ') continue;

            if (SPECIAL_CHARS.containsKey(c)) {
                if (token != null) {
                    tokens.add(token);
                    previousToken = token;
                    token = null;
                }

                Token current = SPECIAL_CHARS.get(c);
                if (current.type.isUnary() && previousToken != null && previousToken.type == TokenType.VERSION) {
                    // Handling the ranges like "≥1.2.3 <4.5.6" by inserting a "AND" binary operator
                    tokens.add(new Token(TokenType.AND));
                }

                tokens.add(current);
                previousToken = current;
            } else {
                if (token == null) {
                    token = new Token(TokenType.VERSION);
                }
                token.append(c);
            }
        }

        if (token != null) {
            tokens.add(token);
        }

        return tokens;
    }

    /**
     * A token in a requirement string. Has a type and a value if it is of type VERSION
     */
    protected static class Token {
        public final TokenType type;
        public String value;

        public Token(TokenType type) {
            this(type, null);
        }

        public Token(TokenType type, String value) {
            this.type = type;
            this.value = value;
        }

        public void append(char c) {
            if (value == null) value = "";
            value += c;
        }
    }

    /**
     * The different types of tokens (unary operators, binary operators, delimiters and versions)
     */
    protected enum TokenType {
        // Unary operators: ~ ^ = < <= > >=
        TILDE('~', true),
        CARET('^', true),
        EQ('=', true),
        LT('<', true),
        LTE('≤', true),
        GT('>', true),
        GTE('≥', true),

        // Binary operators: - ||
        HYPHEN('-', false),
        OR('|', false),
        AND(null, false),

        // Delimiters: ( )
        OPENING('(', false),
        CLOSING(')', false),

        // Special
        VERSION(null, false);

        public final Character character;
        private final boolean unary;

        TokenType(Character character, boolean unary) {
            this.character = character;
            this.unary = unary;
        }

        public boolean isUnary() {
            return this.unary;
        }
    }
}
