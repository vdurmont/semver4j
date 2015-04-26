package com.vdurmont.semver;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class RequirementTest {
    @Test public void buildStrict() {
        String version = "1.2.3";
        Requirement requirement = Requirement.buildStrict(version);
        assertIsRange(requirement, version, Range.RangeOperator.EQ);
    }

    @Test public void buildNPM_with_a_strict_version() {
        String version = "1.2.3";
        Requirement requirement = Requirement.buildNPM(version);
        assertIsRange(requirement, version, Range.RangeOperator.EQ);
    }

    @Test public void buildNPM_with_a_version_with_a_leading_v() {
        assertIsRange(Requirement.buildNPM("v1.2.3"), "1.2.3", Range.RangeOperator.EQ);
        assertIsRange(Requirement.buildNPM("v 1.2.3"), "1.2.3", Range.RangeOperator.EQ);
    }

    @Test public void buildNPM_with_a_version_with_a_leading_equal() {
        assertIsRange(Requirement.buildNPM("=1.2.3"), "1.2.3", Range.RangeOperator.EQ);
        assertIsRange(Requirement.buildNPM("= 1.2.3"), "1.2.3", Range.RangeOperator.EQ);
    }

    @Test public void buildNPM() {
        fail();

        // TODO = operator
        // TODO hyphen ranges
        // TODO ranges (>1.0.0 <=1.2.3)
        // TODO ranges with '||'
        // TODO x and * ranges
        // TODO ~ ranges
        // TODO ^ ranges
    }

    @Test public void tildeRequirement_full_version() {
        Requirement req = Requirement.tildeRequirement("1.2.3");

        assertNull(req.range);
        assertEquals(Requirement.RequirementOperator.AND, req.op);

        Requirement req1 = req.req1;
        assertEquals(Range.RangeOperator.GTE, req1.range.op);
        assertEquals("1.2.3", req1.range.version.getValue());

        Requirement req2 = req.req2;
        assertEquals(Range.RangeOperator.LT, req2.range.op);
        assertEquals("1.3.0", req2.range.version.getValue());
    }

    @Test public void tildeRequirement_only_major_minor() {
        Requirement req = Requirement.tildeRequirement("1.2");

        assertNull(req.range);
        assertEquals(Requirement.RequirementOperator.AND, req.op);

        Requirement req1 = req.req1;
        assertEquals(Range.RangeOperator.GTE, req1.range.op);
        assertEquals("1.2.0", req1.range.version.getValue());

        Requirement req2 = req.req2;
        assertEquals(Range.RangeOperator.LT, req2.range.op);
        assertEquals("1.3.0", req2.range.version.getValue());
    }

    @Test public void tildeRequirement_only_major() {
        Requirement req = Requirement.tildeRequirement("1");

        assertNull(req.range);
        assertEquals(Requirement.RequirementOperator.AND, req.op);

        Requirement req1 = req.req1;
        assertEquals(Range.RangeOperator.GTE, req1.range.op);
        assertEquals("1.0.0", req1.range.version.getValue());

        Requirement req2 = req.req2;
        assertEquals(Range.RangeOperator.LT, req2.range.op);
        assertEquals("2.0.0", req2.range.version.getValue());
    }

    @Test public void tildeRequirement_full_version_major_0() {
        Requirement req = Requirement.tildeRequirement("0.2.3");

        assertNull(req.range);
        assertEquals(Requirement.RequirementOperator.AND, req.op);

        Requirement req1 = req.req1;
        assertEquals(Range.RangeOperator.GTE, req1.range.op);
        assertEquals("0.2.3", req1.range.version.getValue());

        Requirement req2 = req.req2;
        assertEquals(Range.RangeOperator.LT, req2.range.op);
        assertEquals("0.3.0", req2.range.version.getValue());
    }

    // TODO tests for tilde
//        ~0.2 := >=0.2.0 <0.(2+1).0 := >=0.2.0 <0.3.0 (Same as 0.2.x)
//        ~0 := >=0.0.0 <(0+1).0.0 := >=0.0.0 <1.0.0 (Same as 0.x)
//        ~1.2.3-beta.2 := >=1.2.3-beta.2 <1.3.0

    @Test public void caretRequirement() {
        fail();
    }

    @Test public void isSatisfiedBy_with_a_range() {
        Range range = mock(Range.class);
        Requirement requirement = new Requirement(range, null, null, null);
        Semver version = new Semver("1.2.3");
        requirement.isSatisfiedBy(version);
        verify(range).isSatisfiedBy(version);
    }

    @Test public void isSatisfiedBy_with_subRequirements_AND_first_is_true() {
        Semver version = new Semver("1.2.3");

        Requirement req1 = mock(Requirement.class);
        when(req1.isSatisfiedBy(version)).thenReturn(true);
        Requirement req2 = mock(Requirement.class);
        Requirement requirement = new Requirement(null, req1, Requirement.RequirementOperator.AND, req2);

        requirement.isSatisfiedBy(version);

        verify(req1).isSatisfiedBy(version);
        verify(req2).isSatisfiedBy(version);
    }

    @Test public void isSatisfiedBy_with_subRequirements_AND_first_is_false() {
        Semver version = new Semver("1.2.3");

        Requirement req1 = mock(Requirement.class);
        when(req1.isSatisfiedBy(version)).thenReturn(false);
        Requirement req2 = mock(Requirement.class);
        Requirement requirement = new Requirement(null, req1, Requirement.RequirementOperator.AND, req2);

        requirement.isSatisfiedBy(version);

        verify(req1).isSatisfiedBy(version);
        verifyZeroInteractions(req2);
    }

    @Test public void isSatisfiedBy_with_subRequirements_OR_first_is_true() {
        Semver version = new Semver("1.2.3");

        Requirement req1 = mock(Requirement.class);
        when(req1.isSatisfiedBy(version)).thenReturn(true);
        Requirement req2 = mock(Requirement.class);
        Requirement requirement = new Requirement(null, req1, Requirement.RequirementOperator.OR, req2);

        requirement.isSatisfiedBy(version);

        verify(req1).isSatisfiedBy(version);
        verifyZeroInteractions(req2);
    }

    @Test public void isSatisfiedBy_with_subRequirements_OR_first_is_false() {
        Semver version = new Semver("1.2.3");

        Requirement req1 = mock(Requirement.class);
        when(req1.isSatisfiedBy(version)).thenReturn(false);
        Requirement req2 = mock(Requirement.class);
        Requirement requirement = new Requirement(null, req1, Requirement.RequirementOperator.OR, req2);

        requirement.isSatisfiedBy(version);

        verify(req1).isSatisfiedBy(version);
        verify(req2).isSatisfiedBy(version);
    }

    private static void assertIsRange(Requirement requirement, String version, Range.RangeOperator operator) {
        assertNull(requirement.req1);
        assertNull(requirement.op);
        assertNull(requirement.req2);
        Range range = requirement.range;
        assertTrue(range.version.isEquivalentTo(version));
        assertEquals(operator, range.op);
    }
}
