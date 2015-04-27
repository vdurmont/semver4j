package com.vdurmont.semver;

/**
 * Semver is a tool that provides useful methods to manipulate versions that follow the "semantic versioning" specification
 * (see http://semver.org)
 */
public class Semver implements Comparable<Semver> {
    private final String value;
    private final Integer major;
    private final Integer minor;
    private final Integer patch;
    private final String[] suffixTokens;
    private final String build;
    private final SemverType type;

    public Semver(String value) {
        this(value, SemverType.DEFAULT);
    }

    public Semver(String value, SemverType type) {
        this.type = type;
        value = value.trim().toLowerCase();
        if (type == SemverType.NPM && value.startsWith("v")) {
            value = value.substring(1).trim();
        }
        this.value = value;

        String[] tokens = value.split("-");
        String build = null;
        Integer major = null;
        Integer minor = null;
        Integer patch = null;
        try {
            String[] mainTokens;
            if (tokens.length == 1) {
                // The build version may be in the main tokens
                if (tokens[0].endsWith("+")) {
                    throw new SemverException("The build cannot be empty.");
                }
                String[] tmp = tokens[0].split("\\+");
                mainTokens = tmp[0].split("\\.");
                if (tmp.length == 2) {
                    build = tmp[1];
                }
            } else {
                mainTokens = tokens[0].split("\\.");
            }

            try {
                major = Integer.valueOf(mainTokens[0]);
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                throw new SemverException("Invalid version (no major version): " + value);
            }

            try {
                minor = Integer.valueOf(mainTokens[1]);
            } catch (IndexOutOfBoundsException e) {
                if (type != SemverType.NPM) {
                    throw new SemverException("Invalid version (no minor version): " + value);
                }
            } catch (NumberFormatException e) {
                if (type != SemverType.NPM || (!"x".equalsIgnoreCase(mainTokens[1]))) {
                    throw new SemverException("Invalid version (no minor version): " + value);
                }
            }
            try {
                patch = Integer.valueOf(mainTokens[2]);
            } catch (IndexOutOfBoundsException e) {
                if (type != SemverType.NPM) {
                    throw new SemverException("Invalid version (no patch version): " + value);
                }
            } catch (NumberFormatException e) {
                if (type != SemverType.NPM || (!"x".equalsIgnoreCase(mainTokens[2]))) {
                    throw new SemverException("Invalid version (no patch version): " + value);
                }
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new SemverException("The version is invalid: " + value);
        }
        this.major = major;
        this.minor = minor;
        this.patch = patch;

        String[] suffix = new String[0];
        try {
            // The build version may be in the suffix tokens
            if (tokens[1].endsWith("+")) {
                throw new SemverException("The build cannot be empty.");
            }
            String[] tmp = tokens[1].split("\\+");
            if (tmp.length == 2) {
                suffix = tmp[0].split("\\.");
                build = tmp[1];
            } else {
                suffix = tokens[1].split("\\.");
            }
        } catch (IndexOutOfBoundsException ignored) {
        }
        this.suffixTokens = suffix;

        this.build = build;

        this.validate(type);
    }

    public void validate(SemverType type) {
        if (this.minor == null && type != SemverType.NPM) {
            throw new SemverException("Invalid version (no minor version): " + value);
        }
        if (this.patch == null && type != SemverType.NPM) {
            throw new SemverException("Invalid version (no patch version): " + value);
        }
    }

    /**
     * Check if the version satisfies a requirement
     *
     * @param requirement the requirement
     *
     * @return true if the version satisfies the requirement
     */
    public boolean satisfies(Requirement requirement) {
        return requirement.isSatisfiedBy(this);
    }

    /**
     * @see #isGreaterThan(Semver)
     */
    public boolean isGreaterThan(String version) {
        return this.isGreaterThan(new Semver(version));
    }

    /**
     * Checks if the version is greater than another version
     *
     * @param version the version to compare
     *
     * @return true if the current version is greater than the provided version
     */
    public boolean isGreaterThan(Semver version) {
        // Compare the main part
        if (this.getMajor() > version.getMajor()) return true;
        if (this.getMinor() > version.getMinor()) return true;
        if (this.getPatch() > version.getPatch()) return true;

        // Let's take a look at the suffix
        String[] tokens1 = this.getSuffixTokens();
        String[] tokens2 = version.getSuffixTokens();

        // If one of the versions has no suffix, it's greater!
        if (tokens1.length == 0 && tokens2.length > 0) return true;
        if (tokens2.length == 0 && tokens1.length > 0) return false;

        // Let's see if one of suffixes is greater than the other
        int i = 0;
        while (i < tokens1.length && i < tokens2.length) {
            int cmp;
            try {
                // Trying to resolve the suffix part with an integer
                int t1 = Integer.valueOf(tokens1[i]);
                int t2 = Integer.valueOf(tokens2[i]);
                cmp = t1 - t2;
            } catch (NumberFormatException e) {
                // Else, do a string comparison
                cmp = tokens1[i].compareToIgnoreCase(tokens2[i]);
            }
            if (cmp < 0) return false;
            else if (cmp > 0) return true;
            i++;
        }

        // If one of the versions has some remaining suffixes, it's greater
        return tokens1.length > tokens2.length;
    }

    /**
     * @see #isLowerThan(Semver)
     */
    public boolean isLowerThan(String version) {
        return this.isLowerThan(new Semver(version));
    }

    /**
     * Checks if the version is lower than another version
     *
     * @param version the version to compare
     *
     * @return true if the current version is lower than the provided version
     */
    public boolean isLowerThan(Semver version) {
        return !this.isGreaterThan(version) && !this.isEquivalentTo(version);
    }

    /**
     * @see #isEquivalentTo(Semver)
     */
    public boolean isEquivalentTo(String version) {
        return this.isEquivalentTo(new Semver(version));
    }

    /**
     * Checks if the version equals another version, without taking the build into account.
     *
     * @param version the version to compare
     *
     * @return true if the current version equals the provided version (build excluded)
     */
    public boolean isEquivalentTo(Semver version) {
        // Get versions without build
        Semver sem1 = this.getBuild() == null ? this : new Semver(this.getValue().replace("+" + this.getBuild(), ""));
        Semver sem2 = version.getBuild() == null ? version : new Semver(version.getValue().replace("+" + version.getBuild(), ""));
        // Compare those new versions
        return sem1.isEqualTo(sem2);
    }

    /**
     * @see #isEqualTo(Semver)
     */
    public boolean isEqualTo(String version) {
        return this.isEqualTo(new Semver(version));
    }

    /**
     * Checks if the version equals another version
     *
     * @param version the version to compare
     *
     * @return true if the current version equals the provided version
     */
    public boolean isEqualTo(Semver version) {
        return this.equals(version);
    }

    /**
     * @see #diff(Semver)
     */
    public VersionDiff diff(String version) {
        return this.diff(new Semver(version));
    }

    /**
     * Returns the greatest difference between 2 versions.
     * For example, if the current version is "1.2.3" and compared version is "1.3.0", the biggest difference
     * is the 'MINOR' number.
     *
     * @param version the version to compare
     *
     * @return the greatest difference
     */
    public VersionDiff diff(Semver version) {
        // TODO code me
        return null;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Semver)) return false;
        Semver version = (Semver) o;
        return value.equals(version.value);

    }

    @Override public int hashCode() {
        return value.hashCode();
    }

    @Override public int compareTo(Semver version) {
        if (this.isGreaterThan(version)) return -1;
        else if (this.equals(version)) return 0;
        return 1;
    }

    @Override public String toString() {
        return "Semver(" + this.value + ")";
    }

    public String getValue() {
        return value;
    }

    public Integer getMajor() {
        return this.major;
    }

    public Integer getMinor() {
        return this.minor;
    }

    public Integer getPatch() {
        return this.patch;
    }

    public String[] getSuffixTokens() {
        return suffixTokens;
    }

    public String getBuild() {
        return build;
    }

    public SemverType getType() {
        return type;
    }

    /**
     * The types of diffs between two versions.
     */
    public enum VersionDiff {
        NONE, MAJOR, MINOR, PATCH, SUFFIX, BUILD
    }

    // TODO doc
    public enum SemverType {
        DEFAULT, NPM
    }
}
