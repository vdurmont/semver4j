package com.vdurmont.semver4j;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.vdurmont.semver4j.Semver.SemverType;

@RunWith(Parameterized.class)
public class NpmSemverTest {

    private String version;
    private String rangeExpression;
    private boolean expected;

    public NpmSemverTest(String version, String rangeExpression, boolean expected) {
        this.version = version;
        this.rangeExpression = rangeExpression;
        this.expected = expected;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
            // Concrete versions:
            // (0)
            { "1.2.3", "1.2.3", true, },
            // (1)
            { "1.2.4", "1.2.3", false, },

            // Hyphen ranges:

            // (2)
            { "1.2.4-beta+exp.sha.5114f85", "1.2.3 - 2.3.4", true, },
            // (3)
            { "1.2.4", "1.2.3 - 2.3.4", true, },
            // (4)
            { "1.2.3", "1.2.3 - 2.3.4", true, },
            // (5)
            { "2.3.4", "1.2.3 - 2.3.4", true, },
            // (6)
            { "2.3.0-alpha", "1.2.3 - 2.3.0-beta", true, },
            // (7)
            { "2.3.4", "1.2.3 - 2.3", true, },
            // (8)
            { "2.3.4", "1.2.3 - 2", true, },
            // (9)
            { "1.0.0", "1.2.3 - 2.3.4", false, },
            // (10)
            { "3.0.0", "1.2.3 - 2.3.4", false, },
            // (11)
            { "2.4.3", "1.2.3 - 2.3", false, },
            // (12)
            { "2.3.0-rc1", "1.2.3 - 2.3.0-beta", false, },
            // (13)
            { "3.0.0", "1.2.3 - 2", false, },

            // X ranges:

            // (14)
            { "3.1.5", "", true, },
            // (15)
            { "3.1.5", "*", true, },
            // (16)
            { "0.0.0", "*", true, },
            // (17)
            { "1.0.0-beta", "*", true, },
            // (18)
            { "3.1.5-beta+exp.sha.5114f85", "3.1.x", true, },
            // (19)
            { "3.1.5", "3.1.x", true, },
            // (20)
            { "3.1.5", "3.1.X", true, },
            // (21)
            { "3.1.5", "3.x", true, },
            // (22)
            { "3.1.5", "3.*", true, },
            // (23)
            { "3.1.5", "3.1", true, },
            // (24)
            { "3.1.5", "3", true, },
            // (25)
            { "3.2.5", "3.1.x", false, },
            // (26)
            { "3.0.5", "3.1.x", false, },
            // (27)
            { "4.0.0", "3.x", false, },
            // (28)
            { "2.0.0", "3.x", false, },
            // (29)
            { "3.2.5", "3.1", false, },
            // (30)
            { "3.0.5", "3.1", false, },
            // (31)
            { "4.0.0", "3", false, },
            // (32)
            { "2.0.0", "3", false, },

            // Tilde ranges:

            // (33)
            { "1.2.4-beta+exp.sha.5114f85", "~1.2.3", true, },
            // (34)
            { "1.2.3", "~1.2.3", true, },
            // (35)
            { "1.2.7", "~1.2.3", true, },
            // (36)
            { "1.2.2", "~1.2", true, },
            // (37)
            { "1.2.0", "~1.2", true, },
            // (38)
            { "1.3.0", "~1", true, },
            // (39)
            { "1.0.0", "~1", true, },
            // (40)
            { "1.2.3", "~1.2.3-beta.2", true, },
            // (41)
            { "1.2.3-beta.4", "~1.2.3-beta.2", true, },
            // (42)
            { "1.2.4", "~1.2.3-beta.2", true, },
            // (43)
            { "1.3.0", "~1.2.3", false, },
            // (44)
            { "1.2.2", "~1.2.3", false, },
            // (45)
            { "1.1.0", "~1.2", false, },
            // (46)
            { "1.3.0", "~1.2", false, },
            // (47)
            { "2.0.0", "~1", false, },
            // (48)
            { "0.0.0", "~1", false, },
            // (49)
            { "1.2.3-beta.1", "~1.2.3-beta.2", false, },

            // Caret ranges:

            // (50)
            { "1.2.3", "^1.2.3", true, },
            // (51)
            { "1.2.4", "^1.2.3", true, },
            // (52)
            { "1.3.0", "^1.2.3", true, },
            // (53)
            { "0.2.3", "^0.2.3", true, },
            // (54)
            { "0.2.4", "^0.2.3", true, },
            // (55)
            { "0.0.3", "^0.0.3", true, },
            // (56)
            { "0.0.3+exp.sha.5114f85", "^0.0.3", true, },
            // (57)
            { "0.0.3", "^0.0.3-beta", true, },
            // (58)
            { "0.0.3-pr.2", "^0.0.3-beta", true, },
            // (59)
            { "1.2.2", "^1.2.3", false, },
            // (60)
            { "2.0.0", "^1.2.3", false, },
            // (61)
            { "0.2.2", "^0.2.3", false, },
            // (62)
            { "0.3.0", "^0.2.3", false, },
            // (63)
            { "0.0.4", "^0.0.3", false, },
            // (64)
            { "0.0.3-alpha", "^0.0.3-beta", false, },
            // (65)
            { "0.0.4", "^0.0.3-beta", false, },

            // Complex ranges:
            // TODO: Add more!

            // (66)
             { "1.2.0", "1.2 <1.2.8 || >2.0.0", true, },
            // (67)
             { "1.2.7", "1.2 <1.2.8 || >2.0.0", true, },
            // (68)
             { "2.0.1", "1.2 <1.2.8 || >2.0.0", true, },
            // (69)
            { "1.1.0", "1.2 <1.2.8 || >2.0.0", false, },
            // (70)
            { "1.2.9", "1.2 <1.2.8 || >2.0.0", false, },
            // (71)
            { "2.0.0", "1.2 <1.2.8 || >2.0.0", false, },

            // (72)
            { "1.2.3", "1.2.3 || 1.2.4", true, },
            // (73)
            { "1.2.4", "1.2.3 || 1.2.4", true, },
            // (74)
            { "1.2.5", "1.2.3 || 1.2.4", false, },

            // (75)
            { "1.2.0", "1.2 <1.2.8", true, },
            // (76)
            { "1.2.7", "1.2 <1.2.8", true, },
            // (77)
            { "1.1.9", "1.2 <1.2.8", false, },
            // (78)
            { "1.2.9", "1.2 <1.2.8", false, },

            // Comparators:

            // (79)
            { "2.0.1", "> 2.0.0", true, },
            // (80)
            { "2.0.0", "= 2.0.0", true, },
            // (81)
            { "1.9.9", "< 2.0.0", true, },
            // (82)
            { "2.0.0", "<=2.0.0", true, },
            // (83)
            { "1.9.9", "<=2.0.0", true, },
            // (84)
            { "2.0.0", ">=2.0.0", true, },
            // (85)
            { "2.0.1", ">=2.0.0", true, },
            // (86)
            { "2.0.0", "> 2.0.0", false, },
            // (87)
            { "1.9.9", "> 2.0.0", false, },
            // (88)
            { "2.0.1", "= 2.0.0", false, },
            // (89)
            { "2.0.0", "< 2.0.0", false, },
            // (90)
            { "2.0.1", "< 2.0.0", false, },
            // (91)
            { "2.0.1", "<=2.0.0", false, },
            // (92)
            { "1.9.9", ">=2.0.0", false, }, });
    }

    @Test
    public void test() {
        assertEquals(this.expected, new Semver(this.version, SemverType.NPM).satisfies(this.rangeExpression));
    }

}
