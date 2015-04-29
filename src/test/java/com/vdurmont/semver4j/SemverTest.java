package com.vdurmont.semver4j;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class SemverTest {
    @Test(expected = SemverException.class)
    public void constructor_with_empty_build_fails() {
        new Semver("1.0.0+");
    }

    @Test public void default_constructor_test_full_version() {
        String version = "1.2.3-beta.11+sha.0nsfgkjkjsdf";
        Semver semver = new Semver(version);
        assertIsSemver(semver, version, 1, 2, 3, new String[]{"beta", "11"}, "sha.0nsfgkjkjsdf");
    }

    @Test(expected = SemverException.class)
    public void default_constructor_test_only_major_and_minor() {
        String version = "1.2-beta.11+sha.0nsfgkjkjsdf";
        new Semver(version);
    }

    @Test(expected = SemverException.class)
    public void default_constructor_test_only_major() {
        String version = "1-beta.11+sha.0nsfgkjkjsdf";
        new Semver(version);
    }

    @Test public void npm_constructor_test_full_version() {
        String version = "1.2.3-beta.11+sha.0nsfgkjkjsdf";
        Semver semver = new Semver(version, Semver.SemverType.NPM);
        assertIsSemver(semver, version, 1, 2, 3, new String[]{"beta", "11"}, "sha.0nsfgkjkjsdf");
    }

    @Test public void npm_constructor_test_only_major_and_minor() {
        String version = "1.2-beta.11+sha.0nsfgkjkjsdf";
        Semver semver = new Semver(version, Semver.SemverType.NPM);
        assertIsSemver(semver, version, 1, 2, null, new String[]{"beta", "11"}, "sha.0nsfgkjkjsdf");
    }

    @Test public void npm_constructor_test_only_major() {
        String version = "1-beta.11+sha.0nsfgkjkjsdf";
        Semver semver = new Semver(version, Semver.SemverType.NPM);
        assertIsSemver(semver, version, 1, null, null, new String[]{"beta", "11"}, "sha.0nsfgkjkjsdf");
    }

    @Test public void npm_constructor_with_leading_v() {
        String version = "v1.2.3-beta.11+sha.0nsfgkjkjsdf";
        Semver semver = new Semver(version, Semver.SemverType.NPM);
        assertIsSemver(semver, "1.2.3-beta.11+sha.0nsfgkjkjsdf", 1, 2, 3, new String[]{"beta", "11"}, "sha.0nsfgkjkjsdf");

        String versionWithSpace = "v 1.2.3-beta.11+sha.0nsfgkjkjsdf";
        Semver semverWithSpace = new Semver(versionWithSpace, Semver.SemverType.NPM);
        assertIsSemver(semverWithSpace, "1.2.3-beta.11+sha.0nsfgkjkjsdf", 1, 2, 3, new String[]{"beta", "11"}, "sha.0nsfgkjkjsdf");
    }

    private static void assertIsSemver(Semver semver, String value, Integer major, Integer minor, Integer patch, String[] suffixTokens, String build) {
        assertEquals(value, semver.getValue());
        assertEquals(major, semver.getMajor());
        assertEquals(minor, semver.getMinor());
        assertEquals(patch, semver.getPatch());
        assertEquals(2, semver.getSuffixTokens().length);
        assertEquals(suffixTokens.length, semver.getSuffixTokens().length);
        for (int i = 0; i < suffixTokens.length; i++) {
            assertEquals(suffixTokens[i], semver.getSuffixTokens()[i]);
        }
        assertEquals(build, semver.getBuild());
    }

    @Test public void isGreaterThan_test() {
        // 1.0.0-alpha < 1.0.0-alpha.1 < 1.0.0-alpha.beta < 1.0.0-beta < 1.0.0-beta.2 < 1.0.0-beta.11 < 1.0.0-rc.1 < 1.0.0

        assertTrue(new Semver("1.0.0-alpha.1").isGreaterThan("1.0.0-alpha"));
        assertTrue(new Semver("1.0.0-alpha.beta").isGreaterThan("1.0.0-alpha.1"));
        assertTrue(new Semver("1.0.0-beta").isGreaterThan("1.0.0-alpha.beta"));
        assertTrue(new Semver("1.0.0-beta.2").isGreaterThan("1.0.0-beta"));
        assertTrue(new Semver("1.0.0-beta.11").isGreaterThan("1.0.0-beta.2"));
        assertTrue(new Semver("1.0.0-rc.1").isGreaterThan("1.0.0-beta.11"));
        assertTrue(new Semver("1.0.0").isGreaterThan("1.0.0-rc.1"));

        assertFalse(new Semver("1.0.0-alpha").isGreaterThan("1.0.0-alpha.1"));
        assertFalse(new Semver("1.0.0-alpha.1").isGreaterThan("1.0.0-alpha.beta"));
        assertFalse(new Semver("1.0.0-alpha.beta").isGreaterThan("1.0.0-beta"));
        assertFalse(new Semver("1.0.0-beta").isGreaterThan("1.0.0-beta.2"));
        assertFalse(new Semver("1.0.0-beta.2").isGreaterThan("1.0.0-beta.11"));
        assertFalse(new Semver("1.0.0-beta.11").isGreaterThan("1.0.0-rc.1"));
        assertFalse(new Semver("1.0.0-rc.1").isGreaterThan("1.0.0"));

        assertFalse(new Semver("1.0.0").isGreaterThan("1.0.0"));
        assertFalse(new Semver("1.0.0-alpha.12").isGreaterThan("1.0.0-alpha.12"));

        assertFalse(new Semver("0.0.1").isGreaterThan("5.0.0"));
    }

    @Test public void isLowerThan_test() {
        // 1.0.0-alpha < 1.0.0-alpha.1 < 1.0.0-alpha.beta < 1.0.0-beta < 1.0.0-beta.2 < 1.0.0-beta.11 < 1.0.0-rc.1 < 1.0.0

        assertFalse(new Semver("1.0.0-alpha.1").isLowerThan("1.0.0-alpha"));
        assertFalse(new Semver("1.0.0-alpha.beta").isLowerThan("1.0.0-alpha.1"));
        assertFalse(new Semver("1.0.0-beta").isLowerThan("1.0.0-alpha.beta"));
        assertFalse(new Semver("1.0.0-beta.2").isLowerThan("1.0.0-beta"));
        assertFalse(new Semver("1.0.0-beta.11").isLowerThan("1.0.0-beta.2"));
        assertFalse(new Semver("1.0.0-rc.1").isLowerThan("1.0.0-beta.11"));
        assertFalse(new Semver("1.0.0").isLowerThan("1.0.0-rc.1"));

        assertTrue(new Semver("1.0.0-alpha").isLowerThan("1.0.0-alpha.1"));
        assertTrue(new Semver("1.0.0-alpha.1").isLowerThan("1.0.0-alpha.beta"));
        assertTrue(new Semver("1.0.0-alpha.beta").isLowerThan("1.0.0-beta"));
        assertTrue(new Semver("1.0.0-beta").isLowerThan("1.0.0-beta.2"));
        assertTrue(new Semver("1.0.0-beta.2").isLowerThan("1.0.0-beta.11"));
        assertTrue(new Semver("1.0.0-beta.11").isLowerThan("1.0.0-rc.1"));
        assertTrue(new Semver("1.0.0-rc.1").isLowerThan("1.0.0"));

        assertFalse(new Semver("1.0.0").isLowerThan("1.0.0"));
        assertFalse(new Semver("1.0.0-alpha.12").isLowerThan("1.0.0-alpha.12"));
    }

    @Test public void isEquivalentTo_isEqualTo_and_build() {
        Semver semver = new Semver("1.0.0+ksadhjgksdhgksdhgfj");
        String version2 = "1.0.0+sdgfsdgsdhsdfgdsfgf";
        assertFalse(semver.isEqualTo(version2));
        assertTrue(semver.isEquivalentTo(version2));
    }

    @Test public void statisfiescalls_the_requirement() {
        Requirement req = mock(Requirement.class);
        Semver semver = new Semver("1.2.2");
        semver.satisfies(req);
        verify(req).isSatisfiedBy(semver);
    }

    @Test public void test_all_the_methods() {
        fail();
    }
}
