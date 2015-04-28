package com.vdurmont.semver;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// TODO doc
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


    public static List<Token> tokenize(String requirement) {
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

    public static class Token {
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

    public enum TokenType {
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
