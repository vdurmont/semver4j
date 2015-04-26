package com.vdurmont.semver;


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
    @Test public void constructor_test() {
        // GIVEN
        String version = "1.2.3-beta.11+sha.0nsfgkjkjsdf";

        // WHEN
        Semver semver = new Semver(version);

        // THEN
        assertEquals(version, semver.getValue());
        assertEquals(1, semver.getMajor());
        assertEquals(2, semver.getMinor());
        assertEquals(3, semver.getPatch());
        assertEquals(2, semver.getSuffixTokens().length);
        assertEquals("beta", semver.getSuffixTokens()[0]);
        assertEquals("11", semver.getSuffixTokens()[1]);
        assertEquals("sha.0nsfgkjkjsdf", semver.getBuild());

        assertEquals("sha.0nsfgkjkjsdf", new Semver("1.2.3+sha.0nsfgkjkjsdf").getBuild());
    }

    @Test(expected = SemverException.class)
    public void constructor_with_empty_build_fails() {
        new Semver("1.0.0+");
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

    @Test public void test() {
        fail();
    }
}
