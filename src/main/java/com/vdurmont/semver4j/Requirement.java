package com.vdurmont.semver4j;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A requirement will provide an easy way to check if a version is satisfying.
 * There are 2 types of requirements:
 * - Strict: checks if a version is equivalent to another
 * - NPM: follows the rules of NPM
 */
public class Requirement {
    private static final Pattern IVY_DYNAMIC_PATCH_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.\\+");
    private static final Pattern IVY_DYNAMIC_MINOR_PATTERN = Pattern.compile("(\\d+)\\.\\+");
    private static final Pattern IVY_LATEST_PATTERN = Pattern.compile("latest\\.\\w+");
    private static final Pattern IVY_MATH_BOUNDED_PATTERN = Pattern.compile(
            "(\\[|\\])" + // 1ST GROUP: a square bracket
                    "([\\d\\.]+)" + // 2ND GROUP: a version
                    "," + // a comma separator
                    "([\\d\\.]+)" + // 3RD GROUP: a version
                    "(\\[|\\])"  // 4TH GROUP: a square bracket
    );
    private static final Pattern IVY_MATH_LOWER_UNBOUNDED_PATTERN = Pattern.compile(
            "\\(," + // a parenthesis and a comma separator
                    "([\\d\\.]+)" + // 1ST GROUP: a version
                    "(\\[|\\])"  // 2ND GROUP: a square bracket
    );
    private static final Pattern IVY_MATH_UPPER_UNBOUNDED_PATTERN = Pattern.compile(
            "(\\[|\\])" + // 1ST GROUP: a square bracket
                    "([\\d\\.]+)" + // 2ND GROUP: a version
                    ",\\)" // a comma separator and a parenthesis
    );

    protected final Range range;
    protected final Requirement req1;
    protected final RequirementOperator op;
    protected final Requirement req2;

    /**
     * Builds a requirement. (private use only)
     *
     * A requirement has to be a range or a combination of an operator and 2 other requirements.
     */
    protected Requirement(Range range, Requirement req1, RequirementOperator op, Requirement req2) {
        this.range = range;
        this.req1 = req1;
        this.op = op;
        this.req2 = req2;
    }

    /**
     * @see #buildStrict(Semver)
     */
    public static Requirement buildStrict(String requirement) {
        return buildStrict(new Semver(requirement));
    }

    /**
     * Builds a strict requirement (will test that the version is equivalent to the requirement)
     *
     * @param requirement the version of the requirement
     *
     * @return the generated requirement
     */
    public static Requirement buildStrict(Semver requirement) {
        return new Requirement(new Range(requirement, Range.RangeOperator.EQ), null, null, null);
    }

    /**
     * @see #buildLoose(Semver)
     */
    public static Requirement buildLoose(String requirement) {
        return buildLoose(new Semver(requirement, Semver.SemverType.LOOSE));
    }

    /**
     * Builds a loose requirement (will test that the version is equivalent to the requirement)
     *
     * @param requirement the version of the requirement
     *
     * @return the generated requirement
     */
    public static Requirement buildLoose(Semver requirement) {
        return new Requirement(new Range(requirement, Range.RangeOperator.EQ), null, null, null);
    }

    /**
     * Builds a requirement following the rules of NPM.
     *
     * @param requirement the requirement as a string
     *
     * @return the generated requirement
     */
    public static Requirement buildNPM(String requirement) {
        return buildWithTokenizer(requirement, Semver.SemverType.NPM);
    }

    /**
     * Builds a requirement following the rules of Cocoapods.
     *
     * @param requirement the requirement as a string
     *
     * @return the generated requirement
     */
    public static Requirement buildCocoapods(String requirement) {
        return buildWithTokenizer(requirement, Semver.SemverType.COCOAPODS);
    }

    private static Requirement buildWithTokenizer(String requirement, Semver.SemverType type) {
        // Tokenize the string
        List<Tokenizer.Token> tokens = Tokenizer.tokenize(requirement, type);

        // Tranform the tokens list to a reverse polish notation list
        List<Tokenizer.Token> rpn = toReversePolishNotation(tokens);

        // Create the requirement tree by evaluating the rpn list
        return evaluateReversePolishNotation(rpn.iterator(), type);
    }

    /**
     * Builds a requirement following the rules of Ivy.
     *
     * @param requirement the requirement as a string
     *
     * @return the generated requirement
     */
    public static Requirement buildIvy(String requirement) {
        try {
            return buildLoose(requirement);
        } catch (SemverException ignored) {
        }

        Matcher matcher = IVY_DYNAMIC_PATCH_PATTERN.matcher(requirement);
        if (matcher.find()) {
            int major = Integer.valueOf(matcher.group(1));
            int minor = Integer.valueOf(matcher.group(2));
            Requirement lower = new Requirement(new Range(major + "." + minor + ".0", Range.RangeOperator.GTE), null, null, null);
            Requirement upper = new Requirement(new Range(major + "." + (minor + 1) + ".0", Range.RangeOperator.LT), null, null, null);
            return new Requirement(null, lower, RequirementOperator.AND, upper);
        }
        matcher = IVY_DYNAMIC_MINOR_PATTERN.matcher(requirement);
        if (matcher.find()) {
            int major = Integer.valueOf(matcher.group(1));
            Requirement lower = new Requirement(new Range(major + ".0.0", Range.RangeOperator.GTE), null, null, null);
            Requirement upper = new Requirement(new Range((major + 1) + ".0.0", Range.RangeOperator.LT), null, null, null);
            return new Requirement(null, lower, RequirementOperator.AND, upper);
        }
        matcher = IVY_LATEST_PATTERN.matcher(requirement);
        if (matcher.find()) {
            return new Requirement(new Range("0.0.0", Range.RangeOperator.GTE), null, null, null);
        }
        matcher = IVY_MATH_BOUNDED_PATTERN.matcher(requirement);
        if (matcher.find()) {
            Range.RangeOperator lowerOp = "[".equals(matcher.group(1)) ? Range.RangeOperator.GTE : Range.RangeOperator.GT;
            Semver lowerVersion = new Semver(matcher.group(2), Semver.SemverType.LOOSE);
            Semver upperVersion = new Semver(matcher.group(3), Semver.SemverType.LOOSE);
            Range.RangeOperator upperOp = "]".equals(matcher.group(4)) ? Range.RangeOperator.LTE : Range.RangeOperator.LT;
            Requirement lower = new Requirement(new Range(extrapolateVersion(lowerVersion), lowerOp), null, null, null);
            Requirement upper = new Requirement(new Range(extrapolateVersion(upperVersion), upperOp), null, null, null);
            return new Requirement(null, lower, RequirementOperator.AND, upper);
        }
        matcher = IVY_MATH_LOWER_UNBOUNDED_PATTERN.matcher(requirement);
        if (matcher.find()) {
            Semver version = new Semver(matcher.group(1), Semver.SemverType.LOOSE);
            Range.RangeOperator op = "]".equals(matcher.group(2)) ? Range.RangeOperator.LTE : Range.RangeOperator.LT;
            return new Requirement(new Range(extrapolateVersion(version), op), null, null, null);
        }
        matcher = IVY_MATH_UPPER_UNBOUNDED_PATTERN.matcher(requirement);
        if (matcher.find()) {
            Range.RangeOperator op = "[".equals(matcher.group(1)) ? Range.RangeOperator.GTE : Range.RangeOperator.GT;
            Semver version = new Semver(matcher.group(2), Semver.SemverType.LOOSE);
            return new Requirement(new Range(extrapolateVersion(version), op), null, null, null);
        }

        throw new SemverException("Invalid requirement");
    }

    /**
     * Adaptation of the shutting yard algorithm
     */
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

    /**
     * Evaluates a reverse polish notation token list
     */
    private static Requirement evaluateReversePolishNotation(Iterator<Tokenizer.Token> iterator, Semver.SemverType type) {
        try {
            Tokenizer.Token token = iterator.next();

            if (token.type == Tokenizer.TokenType.VERSION) {
                if ("*".equals(token.value) || (type == Semver.SemverType.NPM && "latest".equals(token.value))) {
                    // Special case for "*" and "latest" in NPM
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
        if (type != Semver.SemverType.NPM && type != Semver.SemverType.COCOAPODS) {
            throw new SemverException("The tilde requirements are only compatible with NPM and Cocoapods.");
        }
        Semver semver = new Semver(version, type);
        Requirement req1 = new Requirement(new Range(extrapolateVersion(semver), Range.RangeOperator.GTE), null, null, null);

        String next;

        switch (type) {
            case COCOAPODS: {
                if (semver.getPatch() != null) {
                    next = semver.getMajor() + "." + (semver.getMinor() + 1) + ".0";
                } else if (semver.getMinor() != null) {
                    next = (semver.getMajor() + 1) + ".0.0";
                } else {
                    return req1;
                }
            }
            break;
            case NPM: {
                if (semver.getMinor() != null) {
                    next = semver.getMajor() + "." + (semver.getMinor() + 1) + ".0";
                } else {
                    next = (semver.getMajor() + 1) + ".0.0";
                }
            }
            break;
            default:
                throw new SemverException("The tilde requirements are only compatible with NPM and Cocoapods.");
        }

        Requirement req2 = new Requirement(new Range(next, Range.RangeOperator.LT), null, null, null);

        return new Requirement(null, req1, RequirementOperator.AND, req2);
    }

    /**
     * Allows changes that do not modify the left-most non-zero digit in the [major, minor, patch] tuple.
     */
    protected static Requirement caretRequirement(String version, Semver.SemverType type) {
        if (type != Semver.SemverType.NPM) {
            throw new SemverException("The caret requirements are only compatible with NPM.");
        }
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

    /**
     * Creates a requirement that satisfies "x1.y1.z1 - x2.y2.z2".
     */
    protected static Requirement hyphenRequirement(String lowerVersion, String upperVersion, Semver.SemverType type) {
        if (type != Semver.SemverType.NPM) {
            throw new SemverException("The hyphen requirements are only compatible with NPM.");
        }
        Semver lower = extrapolateVersion(new Semver(lowerVersion, type));
        Semver upper = new Semver(upperVersion, type);

        Range.RangeOperator upperOperator = Range.RangeOperator.LTE;
        if (upper.getMinor() == null || upper.getPatch() == null) {
            upperOperator = Range.RangeOperator.LT;
            if (upper.getMinor() == null) {
                upper = extrapolateVersion(upper).withIncMajor();
            } else {
                upper = extrapolateVersion(upper).withIncMinor();
            }
        }
        Requirement req1 = new Requirement(new Range(lower, Range.RangeOperator.GTE), null, null, null);
        Requirement req2 = new Requirement(new Range(upper, upperOperator), null, null, null);

        return new Requirement(null, req1, RequirementOperator.AND, req2);
    }

    /**
     * Extrapolates the optional minor and patch numbers.
     * - 1 = 1.0.0
     * - 1.2 = 1.2.0
     * - 1.2.3 = 1.2.3
     *
     * @param semver the original semver
     *
     * @return a semver with the extrapolated minor and patch numbers
     */
    private static Semver extrapolateVersion(Semver semver) {
        StringBuilder sb = new StringBuilder()
                .append(semver.getMajor())
                .append(".")
                .append(semver.getMinor() == null ? 0 : semver.getMinor())
                .append(".")
                .append(semver.getPatch() == null ? 0 : semver.getPatch());
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

    /**
     * @see #isSatisfiedBy(Semver)
     */
    public boolean isSatisfiedBy(String version) {
        return this.isSatisfiedBy(new Semver(version));
    }

    /**
     * Checks if the requirement is satisfied by a version.
     *
     * @param version the version that will be checked
     *
     * @return true if the version satisfies the requirement
     */
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

    @Override public String toString() {
        if (this.range != null) {
            return "Requirement{" + this.range + "}";
        }
        return "Requirement{" + this.req1 + " " + this.op + " " + this.req2 + "}";
    }

    /**
     * The operators that can be used in a requirement.
     */
    protected enum RequirementOperator {
        AND, OR
    }
}
