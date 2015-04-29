package com.vdurmont.semver4j;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class TokenizerTest {
    @Test public void tokenize_tilde() {
        String requirement = "~ 1.2.7";
        List<Tokenizer.Token> tokens = Tokenizer.tokenize(requirement);
        assertEquals(2, tokens.size());

        assertEquals(Tokenizer.TokenType.TILDE, tokens.get(0).type);

        assertEquals(Tokenizer.TokenType.VERSION, tokens.get(1).type);
        assertEquals("1.2.7", tokens.get(1).value);
    }

    @Test public void tokenize_caret() {
        String requirement = "^ 1.2.7   ";
        List<Tokenizer.Token> tokens = Tokenizer.tokenize(requirement);
        assertEquals(2, tokens.size());

        assertEquals(Tokenizer.TokenType.CARET, tokens.get(0).type);

        assertEquals(Tokenizer.TokenType.VERSION, tokens.get(1).type);
        assertEquals("1.2.7", tokens.get(1).value);
    }

    @Test public void tokenize_lte() {
        String requirement = "<=1.2.7";
        List<Tokenizer.Token> tokens = Tokenizer.tokenize(requirement);
        assertEquals(2, tokens.size());

        assertEquals(Tokenizer.TokenType.LTE, tokens.get(0).type);

        assertEquals(Tokenizer.TokenType.VERSION, tokens.get(1).type);
        assertEquals("1.2.7", tokens.get(1).value);
    }

    @Test public void tokenize_or_hyphen() {
        String requirement = "1.2.7 || 1.2.9 - 2.0.0";
        List<Tokenizer.Token> tokens = Tokenizer.tokenize(requirement);
        assertEquals(5, tokens.size());

        assertEquals(Tokenizer.TokenType.VERSION, tokens.get(0).type);
        assertEquals("1.2.7", tokens.get(0).value);

        assertEquals(Tokenizer.TokenType.OR, tokens.get(1).type);

        assertEquals(Tokenizer.TokenType.VERSION, tokens.get(2).type);
        assertEquals("1.2.9", tokens.get(2).value);

        assertEquals(Tokenizer.TokenType.HYPHEN, tokens.get(3).type);

        assertEquals(Tokenizer.TokenType.VERSION, tokens.get(4).type);
        assertEquals("2.0.0", tokens.get(4).value);
    }

    @Test public void tokenize_or_lte_parenthesis() {
        String requirement = "1.2.7 || (<=1.2.9 || 2.0.0)";
        List<Tokenizer.Token> tokens = Tokenizer.tokenize(requirement);
        assertEquals(8, tokens.size());

        assertEquals(Tokenizer.TokenType.VERSION, tokens.get(0).type);
        assertEquals("1.2.7", tokens.get(0).value);

        assertEquals(Tokenizer.TokenType.OR, tokens.get(1).type);

        assertEquals(Tokenizer.TokenType.OPENING, tokens.get(2).type);

        assertEquals(Tokenizer.TokenType.LTE, tokens.get(3).type);

        assertEquals(Tokenizer.TokenType.VERSION, tokens.get(4).type);
        assertEquals("1.2.9", tokens.get(4).value);

        assertEquals(Tokenizer.TokenType.OR, tokens.get(5).type);

        assertEquals(Tokenizer.TokenType.VERSION, tokens.get(6).type);
        assertEquals("2.0.0", tokens.get(6).value);

        assertEquals(Tokenizer.TokenType.CLOSING, tokens.get(7).type);
    }
}
