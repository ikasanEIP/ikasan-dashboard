package org.ikasan.mongo.persistence.general.dao;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RegexCriteriaTest {

    @Test
    public void wildcard_is_anchored_and_star_matches_anything() {
        assertEquals("^.*\\Qbilling\\E.*$", RegexCriteria.wildcard("*billing*"));
        assertEquals("^\\Qbilling\\E.*$", RegexCriteria.wildcard("billing*"));
        assertEquals("^.*\\Qagent\\E$", RegexCriteria.wildcard("*agent"));
        assertEquals("^\\Qa\\E.*\\Qb\\E$", RegexCriteria.wildcard("a*b"));
        assertEquals("^.*$", RegexCriteria.wildcard("*"));
        assertEquals("^$", RegexCriteria.wildcard(null));
    }

    @Test
    public void wildcard_matches_whole_values() {
        assertTrue(matches("*bill*", "my-billing-agent"));
        assertTrue(matches("bill*", "billing-agent"));
        assertFalse(matches("bill*", "my-billing-agent"));
        assertTrue(matches("*agent", "billing-agent"));
        assertFalse(matches("*agent", "billing-agent-2"));
    }

    @Test
    public void wildcard_treats_regex_metacharacters_literally() {
        assertTrue(matches("*a.c*", "xa.cx"));
        assertFalse(matches("*a.c*", "xabcx"));
        assertTrue(matches("*(a+)+$*", "x(a+)+$x"));
        assertFalse(matches("*(a+)+$*", "aaaa"));
        assertTrue(matches("*\\E.*\\Q*", "x\\E.*\\Qx"));
        assertFalse(matches("*\\E.*\\Q*", "anything"));
    }

    private static boolean matches(String wildcard, String value) {
        return Pattern.compile(RegexCriteria.wildcard(wildcard)).matcher(value).matches();
    }
}
