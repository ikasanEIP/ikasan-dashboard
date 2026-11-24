package org.ikasan.security.dao;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The user filters query tokenised (text_general) fields, so a filter value must be split the way the index
 * tokenizer splits it - a single {@code *value*} wildcard never matched values containing '-', '@', spaces etc.
 */
public class SolrUserDaoImplContainsClauseTest
{
    @Test
    public void simpleValueIsASingleContainsWildcard()
    {
        assertEquals("(name:*admin*)", SolrUserDaoImpl.containsClause("name", "admin"));
    }

    @Test
    public void valueIsSplitWhereTheTokenizerSplits()
    {
        assertEquals("(name:*harness* AND name:*user* AND name:*1*)",
            SolrUserDaoImpl.containsClause("name", "harness-user-1"));
        assertEquals("(email:*harness* AND email:*example.com*)",
            SolrUserDaoImpl.containsClause("email", "harness@example.com"));
        assertEquals("(department:*order* AND department:*management*)",
            SolrUserDaoImpl.containsClause("department", "  order   management "));
    }

    @Test
    public void dotsAndUnderscoresInsideAValueStayInOneToken()
    {
        assertEquals("(name:*john.smith*)", SolrUserDaoImpl.containsClause("name", "john.smith"));
        assertEquals("(name:*john_smith*)", SolrUserDaoImpl.containsClause("name", "john_smith"));
        assertEquals("(name:*smith*)", SolrUserDaoImpl.containsClause("name", ".smith."));
    }

    @Test
    public void queryCharactersNeverReachTheQuery()
    {
        // characters with a query meaning act as separators, so a filter can't change the query's structure
        assertEquals("(name:*a* AND name:*b*)", SolrUserDaoImpl.containsClause("name", "a:b"));
        assertEquals("(name:*a* AND name:*b*)", SolrUserDaoImpl.containsClause("name", "a*b"));
        assertEquals("(name:*x* AND name:*OR*)", SolrUserDaoImpl.containsClause("name", "(x) OR"));
    }

    @Test
    public void valueWithNothingSearchableMatchesEverything()
    {
        assertEquals("name:*", SolrUserDaoImpl.containsClause("name", "-- "));
        assertEquals("name:*", SolrUserDaoImpl.containsClause("name", null));
    }
}
