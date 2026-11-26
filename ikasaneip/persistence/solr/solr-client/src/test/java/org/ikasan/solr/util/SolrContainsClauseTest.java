package org.ikasan.solr.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * "Contains" clauses for tokenised fields: the value is split as the tokenizer splits it, so names with hyphens,
 * spaces or dots (svc-orders, Dashboard login) are found, and dots stay inside a token as they do in the index.
 */
public class SolrContainsClauseTest
{
    @Test
    public void a_single_word_is_one_wildcard()
    {
        assertEquals("(actor:*admin*)", SolrContainsClause.of("actor", "admin"));
    }

    @Test
    public void a_hyphenated_name_is_matched_part_by_part()
    {
        assertEquals("(actor:*ingest* AND actor:*bot*)", SolrContainsClause.of("actor", "ingest-bot"));
    }

    @Test
    public void words_separated_by_spaces_must_all_be_contained()
    {
        assertEquals("(systemEventSubject:*Dashboard* AND systemEventSubject:*login*)",
            SolrContainsClause.of("systemEventSubject", "Dashboard login"));
    }

    @Test
    public void a_dot_stays_inside_the_token_as_the_tokenizer_keeps_it()
    {
        assertEquals("(actor:*john.smith*)", SolrContainsClause.of("actor", "john.smith"));
    }

    @Test
    public void characters_with_a_query_meaning_are_taken_literally_or_split_on()
    {
        assertEquals("(systemEventAction:*Flow* AND systemEventAction:*stopped* AND systemEventAction:*by*"
                + " AND systemEventAction:*a*)",
            SolrContainsClause.of("systemEventAction", "Flow stopped (by a*)"));
    }

    @Test
    public void a_value_without_searchable_characters_matches_everything()
    {
        assertEquals("actor:*", SolrContainsClause.of("actor", " - "));
    }
}
