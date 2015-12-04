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
            { "2.3.4", "1.2.3 - 2.3", true, },
            // (7)
            { "2.3.4", "1.2.3 - 2", true, },
            // (8)
            { "1.0.0", "1.2.3 - 2.3.4", false, },
            // (9)
            { "3.0.0", "1.2.3 - 2.3.4", false, },
            // (10)
            { "2.4.3", "1.2.3 - 2.3", false, },
            // (11)
            { "3.0.0", "1.2.3 - 2", false, },

            // X ranges:

            // (12) TODO
            { "3.1.5", "", true, },
            // (13)
            { "3.1.5", "*", true, },
            // (14)
            { "0.0.0", "*", true, },
            // (15)
            { "1.0.0-beta", "*", true, },
            // (16)
            { "3.1.5-beta+exp.sha.5114f85", "3.1.x", true, },
            // (17)
            { "3.1.5", "3.1.x", true, },
            // (18)
            { "3.1.5", "3.1.X", true, },
            // (19)
            { "3.1.5", "3.x", true, },
            // (20)
            { "3.1.5", "3.*", true, },
            // (21)
            { "3.1.5", "3.1", true, },
            // (22)
            { "3.1.5", "3", true, },
            // (23)
            { "3.2.5", "3.1.x", false, },
            // (24)
            { "3.0.5", "3.1.x", false, },
            // (25)
            { "4.0.0", "3.x", false, },
            // (26)
            { "2.0.0", "3.x", false, },
            // (27)
            { "3.2.5", "3.1", false, },
            // (28)
            { "3.0.5", "3.1", false, },
            // (29)
            { "4.0.0", "3", false, },
            // (30)
            { "2.0.0", "3", false, },

            // Tilde ranges:

            // (31)
            { "1.2.4-beta+exp.sha.5114f85", "~1.2.3", true, },
            // (32)
            { "1.2.3", "~1.2.3", true, },
            // (33)
            { "1.2.7", "~1.2.3", true, },
            // (34)
            { "1.2.2", "~1.2", true, },
            // (35)
            { "1.2.0", "~1.2", true, },
            // (36)
            { "1.3.0", "~1", true, },
            // (37)
            { "1.0.0", "~1", true, },
            // (38) TODO
//            { "1.2.3", "~1.2.3-beta.2", true, },
            // (39) TODO
//            { "1.2.3-beta.4", "~1.2.3-beta.2", true, },
            // (40) TODO
//            { "1.2.4", "~1.2.3-beta.2", true, },
            // (41)
            { "1.3.0", "~1.2.3", false, },
            // (42)
            { "1.2.2", "~1.2.3", false, },
            // (43)
            { "1.1.0", "~1.2", false, },
            // (44)
            { "1.3.0", "~1.2", false, },
            // (45)
            { "2.0.0", "~1", false, },
            // (46)
            { "0.0.0", "~1", false, },
            // (47) TODO
//            { "1.2.3-beta.1", "~1.2.3-beta.2", false, },
            // (48) TODO
//            { "1.2.4-beta.4", "~1.2.3-beta.2", false, },

            // Caret ranges:

            // (49)
            { "1.2.3", "^1.2.3", true, },
            // (50)
            { "1.2.4", "^1.2.3", true, },
            // (51)
            { "1.3.0", "^1.2.3", true, },
            // (52)
            { "0.2.3", "^0.2.3", true, },
            // (53)
            { "0.2.4", "^0.2.3", true, },
            // (54)
            { "0.0.3", "^0.0.3", true, },
            // (55)
            { "0.0.3+exp.sha.5114f85", "^0.0.3", true, },
            // (56) TODO
//            { "0.0.3", "^0.0.3-beta", true, },
            // (57) TODO
//            { "0.0.3-pr.2", "^0.0.3-beta", true, },
            // (58)
            { "1.2.2", "^1.2.3", false, },
            // (59)
            { "2.0.0", "^1.2.3", false, },
            // (60)
            { "0.2.2", "^0.2.3", false, },
            // (61)
            { "0.3.0", "^0.2.3", false, },
            // (62)
            { "0.0.4", "^0.0.3", false, },
            // (63) TODO
//            { "0.0.3-alpha", "^0.0.3-beta", false, },
            // (64) TODO
//            { "0.0.4", "^0.0.3-beta", false, },

            // Complex ranges:
            // TODO: Add more!

            // (65) TODO
//            { "1.2.0", "1.2 <1.2.8 || >2.0.0", true, },
            // (66) TODO
//            { "1.2.7", "1.2 <1.2.8 || >2.0.0", true, },
            // (67) TODO
//            { "2.0.1", "1.2 <1.2.8 || >2.0.0", true, },
            // (68)
            { "1.1.0", "1.2 <1.2.8 || >2.0.0", false, },
            // (69)
            { "1.2.9", "1.2 <1.2.8 || >2.0.0", false, },
            // (70)
            { "2.0.0", "1.2 <1.2.8 || >2.0.0", false, },

            // (71)
            { "1.2.3", "1.2.3 || 1.2.4", true, },
            // (72)
            { "1.2.4", "1.2.3 || 1.2.4", true, },
            // (73)
            { "1.2.5", "1.2.3 || 1.2.4", false, },

            // (74)
            { "1.2.0", "1.2 <1.2.8", true, },
            // (75)
            { "1.2.7", "1.2 <1.2.8", true, },
            // (76)
            { "1.1.9", "1.2 <1.2.8", false, },
            // (77)
            { "1.2.9", "1.2 <1.2.8", false, },

            // Comparators:

            // (78)
            { "2.0.1", "> 2.0.0", true, },
            // (79)
            { "2.0.0", "= 2.0.0", true, },
            // (80)
            { "1.9.9", "< 2.0.0", true, },
            // (81)
            { "2.0.0", "<=2.0.0", true, },
            // (82)
            { "1.9.9", "<=2.0.0", true, },
            // (83)
            { "2.0.0", ">=2.0.0", true, },
            // (84)
            { "2.0.1", ">=2.0.0", true, },
            // (85)
            { "2.0.0", "> 2.0.0", false, },
            // (86)
            { "1.9.9", "> 2.0.0", false, },
            // (87)
            { "2.0.1", "= 2.0.0", false, },
            // (88)
            { "2.0.0", "< 2.0.0", false, },
            // (89)
            { "2.0.1", "< 2.0.0", false, },
            // (90)
            { "2.0.1", "<=2.0.0", false, },
            // (91)
            { "1.9.9", ">=2.0.0", false, },
        });
    }

    @Test
    public void test() {
        assertEquals(this.expected, new Semver(this.version, SemverType.NPM).satisfies(this.rangeExpression));
    }

}
