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
        return evaluateReversePolishNotation(rpn.iterator(), Semver.SemverType.NPM);
    }

    private static List<Tokenizer.Token> toReversePolishNotation(List<Tokenizer.Token> tokens) {
        LinkedList<Tokenizer.Token> queue = new LinkedList<>();
        Stack<Tokenizer.Token> stack = new Stack<>();

        for (int i = 0; i < tokens.size(); i++) {
            Tokenizer.Token token = tokens.get(i);
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
                    if (token.type.isUnary()) {
                        queue.push(token);
                        i++;
                        queue.push(tokens.get(i));
                    } else {
                        stack.push(token);
                    }
                    break;
            }
        }

        while (!stack.isEmpty()) {
            queue.add(stack.pop());
        }

        Collections.reverse(queue);
        return queue;
    }

    private static Requirement evaluateReversePolishNotation(Iterator<Tokenizer.Token> iterator, Semver.SemverType type) {
        try {
            Tokenizer.Token token = iterator.next();

            if (token.type == Tokenizer.TokenType.VERSION) {
                if ("*".equals(token.value)) {
                    // Special case for "*"
                    return new Requirement(new Range("0.0.0", Range.RangeOperator.GTE), null, null, null);
                }
                Semver version = new Semver(token.value, type);
                if (version.getMinor() != null && version.getPatch() != null) {
                    Range range = new Range(version, Range.RangeOperator.EQ);
                    return new Requirement(range, null, null, null);
                } else {
                    // If we have a version with a wildcard char (like 1.2.x, 1.2.* or 1.2), we need a tilde requirement
                    return tildeRequirement(version.getValue(), type);
                }
            } else if (token.type == Tokenizer.TokenType.HYPHEN) {
                Tokenizer.Token token3 = iterator.next(); // Note that token3 is before token2!
                Tokenizer.Token token2 = iterator.next();
                return hyphenRequirement(token2.value, token3.value, type);
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
                        return tildeRequirement(token2.value, type);
                    case CARET:
                        return caretRequirement(token2.value, type);
                    default:
                        throw new SemverException("Invalid requirement");
                }

                Range range = new Range(token2.value, rangeOp);
                return new Requirement(range, null, null, null);
            } else {
                Requirement req1 = evaluateReversePolishNotation(iterator, type);
                Requirement req2 = evaluateReversePolishNotation(iterator, type);

                RequirementOperator requirementOp;
                switch (token.type) {
                    case OR:
                        requirementOp = RequirementOperator.OR;
                        break;
                    case AND:
                        requirementOp = RequirementOperator.AND;
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

    /**
     * Allows patch-level changes if a minor version is specified on the comparator. Allows minor-level changes if not.
     */
    protected static Requirement tildeRequirement(String version, Semver.SemverType type) {
        Semver semver = new Semver(version, type);
        Requirement req1 = new Requirement(new Range(extrapolateVersion(semver), Range.RangeOperator.GTE), null, null, null);

        String next;
        if (semver.getMinor() != null) {
            next = semver.getMajor() + "." + (semver.getMinor() + 1) + ".0";
        } else {
            next = (semver.getMajor() + 1) + ".0.0";
        }
        Requirement req2 = new Requirement(new Range(next, Range.RangeOperator.LT), null, null, null);

        return new Requirement(null, req1, RequirementOperator.AND, req2);
    }

    /**
     * Allows changes that do not modify the left-most non-zero digit in the [major, minor, patch] tuple.
     */
    protected static Requirement caretRequirement(String version, Semver.SemverType type) {
        Semver semver = new Semver(version, type);
        Requirement req1 = new Requirement(new Range(extrapolateVersion(semver), Range.RangeOperator.GTE), null, null, null);

        String next;
        if (semver.getMajor() == 0) {
            if (semver.getMinor() == null) {
                next = "1.0.0";
            } else if (semver.getMinor() == 0) {
                if (semver.getPatch() == null) {
                    next = "0.1.0";
                } else {
                    next = "0.0." + (semver.getPatch() + 1);
                }
            } else {
                next = "0." + (semver.getMinor() + 1) + ".0";
            }
        } else {
            next = (semver.getMajor() + 1) + ".0.0";
        }
        Requirement req2 = new Requirement(new Range(next, Range.RangeOperator.LT), null, null, null);

        return new Requirement(null, req1, RequirementOperator.AND, req2);
    }

    protected static Requirement hyphenRequirement(String lowerVersion, String upperVersion, Semver.SemverType type) {
        Semver lower = extrapolateVersion(new Semver(lowerVersion, type));
        Semver upper = new Semver(upperVersion, type);

        Range.RangeOperator upperOperator = Range.RangeOperator.LTE;
        if (upper.getMinor() == null || upper.getPatch() == null) {
            upperOperator = Range.RangeOperator.LT;
            if (upper.getMinor() == null) {
                upper = extrapolateVersion(upper).incMajor();
            } else {
                upper = extrapolateVersion(upper).incMinor();
            }
        }
        Requirement req1 = new Requirement(new Range(lower, Range.RangeOperator.GTE), null, null, null);
        Requirement req2 = new Requirement(new Range(upper, upperOperator), null, null, null);

        return new Requirement(null, req1, RequirementOperator.AND, req2);
    }

    /**
     * Extrapolates the optional minor and patch numbers.
     * - 1 => 1.0.0
     * - 1.2 => 1.2.0
     * - 1.2.3 => 1.2.3
     *
     * @param semver the original semver
     *
     * @return a semver with the extrapolated minor and patch numbers
     */
    private static Semver extrapolateVersion(Semver semver) {
        StringBuilder sb = new StringBuilder()
                .append(semver.getMajor())
                .append(".")
                .append(zeroifyIfNull(semver.getMinor()))
                .append(".")
                .append(zeroifyIfNull(semver.getPatch()));
        boolean first = true;
        for (int i = 0; i < semver.getSuffixTokens().length; i++) {
            if (first) {
                sb.append("-");
                first = false;
            } else {
                sb.append(".");
            }
            sb.append(semver.getSuffixTokens()[i]);
        }
        if (semver.getBuild() != null) {
            sb.append("+").append(semver.getBuild());
        }
        return new Semver(sb.toString(), semver.getType());
    }

    private static int zeroifyIfNull(Integer value) {
        return value == null ? 0 : value;
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
