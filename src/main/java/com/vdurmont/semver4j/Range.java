package com.vdurmont.semver4j;

// TODO doc
public class Range {
    protected final Semver version;
    protected final RangeOperator op;

    public Range(Semver version, RangeOperator op) {
        this.version = version;
        this.op = op;
    }

    public Range(String version, RangeOperator op) {
        this(new Semver(version, Semver.SemverType.LOOSE), op);
    }

    public boolean isSatisfiedBy(String version) {
        return this.isSatisfiedBy(new Semver(version, this.version.getType()));
    }

    public boolean isSatisfiedBy(Semver version) {
        switch (this.op) {
            case EQ:
                return version.isEquivalentTo(this.version);
            case LT:
                return version.isLowerThan(this.version);
            case LTE:
                return version.isLowerThan(this.version) || version.isEquivalentTo(this.version);
            case GT:
                return version.isGreaterThan(this.version);
            case GTE:
                return version.isGreaterThan(this.version) || version.isEquivalentTo(this.version);
        }

        throw new RuntimeException("Code error. Unknown RangeOperator: " + this.op); // Should never happen
    }

    @Override public String toString() {
        return "(" + this.op + ", " + this.version + ")";
    }

    public enum RangeOperator {
        /**
         * The version and the requirement are equivalent
         */
        EQ,

        /**
         * The version is lower than the requirent
         */
        LT,

        /**
         * The version is lower than or equivalent to the requirement
         */
        LTE,

        /**
         * The version is greater than the requirement
         */
        GT,

        /**
         * The version is greater than or equivalent to the requirement
         */
        GTE
    }
}
