package com.vdurmont.semver;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

// TODO doc
public class Requirement {
    protected final Range range;
    protected final Requirement req1;
    protected final RequirementOperator op;
    protected final Requirement req2;

    protected Requirement(Range range, Requirement req1, RequirementOperator op, Requirement req2) {
        this.range = range;
        this.req1 = req1;
        this.op = op;
        this.req2 = req2;
    }

    public static Requirement buildStrict(String requirement) {
        return buildStrict(new Semver(requirement));
    }

    public static Requirement buildStrict(Semver requirement) {
        return new Requirement(new Range(requirement, Range.RangeOperator.EQ), null, null, null);
    }

    public static Requirement buildNPM(String requirement) {
        // Tokenize the string
        List<Tokenizer.Token> tokens = Tokenizer.tokenize(requirement);

        // Tranform the tokens list to a reverse polish notation list
        List<Tokenizer.Token> rpn = toReversePolishNotation(tokens);

        // Create the requirement tree by evaluating the rpn list
        return evaluateReversePolishNotation(rpn.iterator());
    }

    private static List<Tokenizer.Token> toReversePolishNotation(List<Tokenizer.Token> tokens) {
        LinkedList<Tokenizer.Token> queue = new LinkedList<>();
        Stack<Tokenizer.Token> stack = new Stack<>();

        for (Tokenizer.Token token : tokens) {
            switch (token.type) {
                case VERSION:
                    queue.add(token);
                    break;
                case CLOSING:
                    while (stack.peek().type != Tokenizer.TokenType.OPENING) {
                        queue.add(stack.pop());
                    }
                    stack.pop();
                    if (stack.size() > 0 && stack.peek().type.isUnary()) {
                        queue.add(stack.pop());
                    }
                    break;
                default:
                    stack.push(token);
            }
        }

        while (!stack.isEmpty()) {
            queue.add(stack.pop());
        }

        Collections.reverse(queue);
        return queue;
    }

    private static Requirement evaluateReversePolishNotation(Iterator<Tokenizer.Token> iterator) {
        try {
            Tokenizer.Token token = iterator.next();

            if (token.type == Tokenizer.TokenType.VERSION) {
                Range range = new Range(token.value, Range.RangeOperator.EQ);
                return new Requirement(range, null, null, null);
            } else if (token.type.isUnary()) {
                Tokenizer.Token token2 = iterator.next();

                Range.RangeOperator rangeOp;
                switch (token.type) {
                    case EQ:
                        rangeOp = Range.RangeOperator.EQ;
                        break;
                    case LT:
                        rangeOp = Range.RangeOperator.LT;
                        break;
                    case LTE:
                        rangeOp = Range.RangeOperator.LTE;
                        break;
                    case GT:
                        rangeOp = Range.RangeOperator.GT;
                        break;
                    case GTE:
                        rangeOp = Range.RangeOperator.GTE;
                        break;
                    case TILDE:
                        return tildeRequirement(token2.value);
                    case CARET:
                        return caretRequirement(token2.value);
                    default:
                        throw new SemverException("Invalid requirement");
                }

                Range range = new Range(token2.value, rangeOp);
                return new Requirement(range, null, null, null);
            } else {
                Requirement req1 = evaluateReversePolishNotation(iterator);
                Requirement req2 = evaluateReversePolishNotation(iterator);

                RequirementOperator requirementOp;
                switch (token.type) {
                    case OR:
                        requirementOp = RequirementOperator.OR;
                        break;
                    default:
                        throw new SemverException("Invalid requirement");
                }

                return new Requirement(null, req1, requirementOp, req2);
            }
        } catch (NoSuchElementException e) {
            throw new SemverException("Invalid requirement");
        }
    }

    protected static Requirement tildeRequirement(String version) {
        Semver semver = new Semver(version);
        Requirement req1 = new Requirement(new Range(semver, Range.RangeOperator.GTE), null, null, null);

        String next = semver.getMajor() + "." + (semver.getMinor() + 1) + ".0";
        Requirement req2 = new Requirement(new Range(next, Range.RangeOperator.LT), null, null, null);

        return new Requirement(null, req1, RequirementOperator.AND, req2);
    }

    protected static Requirement caretRequirement(String version) {
        return null;
    }

    public boolean isSatisfiedBy(String version) {
        return this.isSatisfiedBy(new Semver(version));
    }

    public boolean isSatisfiedBy(Semver version) {
        if (this.range != null) {
            // We are on a leaf
            return this.range.isSatisfiedBy(version);
        } else {
            // We have several sub-requirements
            switch (this.op) {
                case AND:
                    return this.req1.isSatisfiedBy(version) && this.req2.isSatisfiedBy(version);
                case OR:
                    return this.req1.isSatisfiedBy(version) || this.req2.isSatisfiedBy(version);
            }

            throw new RuntimeException("Code error. Unknown RequirementOperator: " + this.op); // Should never happen
        }
    }

    public enum RequirementOperator {
        AND, OR
    }
}
