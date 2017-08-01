package com.vdurmont.semver4j;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Utility class to convert a NPM requirement string into a list of tokens.
 */
public class Tokenizer {
    private static final Map<Semver.SemverType, Map<Character, Token>> SPECIAL_CHARS;

    static {
        SPECIAL_CHARS = new HashMap<Semver.SemverType, Map<Character, Token>>();

        for (Semver.SemverType type : Semver.SemverType.values()) {
            SPECIAL_CHARS.put(type, new HashMap<Character, Token>());
        }

        for (TokenType tokenType : TokenType.values()) {
            if (tokenType.character != null) {
                for (Semver.SemverType type : Semver.SemverType.values()) {
                    if (tokenType.supports(type)) {
                        SPECIAL_CHARS.get(type).put(tokenType.character, new Token(tokenType));
                    }
                }
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
    protected static List<Token> tokenize(String requirement, Semver.SemverType type) {
        Map<Character, Token> specialChars = SPECIAL_CHARS.get(type);

        // Replace the tokens made of 2 chars
        if (type == Semver.SemverType.COCOAPODS) {
            requirement = requirement.replace("~>", "~");
        } else if (type == Semver.SemverType.NPM) {
            requirement = requirement.replace("||", "|");
        }
        requirement = requirement.replace("<=", "≤").replace(">=", "≥");


        LinkedList<Token> tokens = new LinkedList<Token>();
        Token previousToken = null;

        char[] chars = requirement.toCharArray();
        Token token = null;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == ' ') continue;

            if (specialChars.containsKey(c)) {
                // Then the only valid hypens are the spaced ones, i.e. 1.2.3 - 2.0.0
                if(specialChars.get(c).type == TokenType.HYPHEN && type == Semver.SemverType.NPM && !(i-1 >= 0 && i+1 < chars.length && chars[i-1] == ' ' && chars[i+1] == ' ')) {
                    // this must be part of a version
                    if(token.type == TokenType.VERSION) {
                        token.append(c);
                    }
                    else {
                        throw new RuntimeException("Unable to tokenize " + requirement);
                    }
                }
                else {

                    if (token != null) {
                        tokens.add(token);
                        previousToken = token;
                        token = null;
                    }

                    Token current = specialChars.get(c);
                    if (current.type.isUnary() && previousToken != null && previousToken.type == TokenType.VERSION) {
                        // Handling the ranges like "≥1.2.3 <4.5.6" by inserting a "AND" binary operator
                        tokens.add(new Token(TokenType.AND));
                    }

                    tokens.add(current);
                    previousToken = current;
                }
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
        TILDE('~', true, Semver.SemverType.COCOAPODS, Semver.SemverType.NPM),
        CARET('^', true, Semver.SemverType.NPM),
        EQ('=', true, Semver.SemverType.NPM),
        LT('<', true, Semver.SemverType.COCOAPODS, Semver.SemverType.NPM),
        LTE('≤', true, Semver.SemverType.COCOAPODS, Semver.SemverType.NPM),
        GT('>', true, Semver.SemverType.COCOAPODS, Semver.SemverType.NPM),
        GTE('≥', true, Semver.SemverType.COCOAPODS, Semver.SemverType.NPM),

        // Binary operators: - ||
        HYPHEN('-', false, Semver.SemverType.NPM),
        OR('|', false, Semver.SemverType.NPM),
        AND(null, false),

        // Delimiters: ( )
        OPENING('(', false, Semver.SemverType.NPM),
        CLOSING(')', false, Semver.SemverType.NPM),

        // Special
        VERSION(null, false);

        public final Character character;
        private final boolean unary;
        private final Semver.SemverType[] supportedTypes;

        TokenType(Character character, boolean unary, Semver.SemverType... supportedTypes) {
            this.character = character;
            this.unary = unary;
            this.supportedTypes = supportedTypes;
        }

        public boolean isUnary() {
            return this.unary;
        }

        public boolean supports(Semver.SemverType type) {
            for (Semver.SemverType t : this.supportedTypes) {
                if (t == type) {
                    return true;
                }
            }
            return false;
        }
    }
}
