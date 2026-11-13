package org.ikasan.mongo.persistence.general.dao;

import org.springframework.data.mongodb.core.query.Criteria;

import java.util.regex.Pattern;

/**
 * Builds MongoDB {@code $regex} criteria from user-supplied input safely.
 *
 * <p>User input must never be concatenated into a regular expression: metacharacters change the meaning of the query
 * (e.g. {@code .*} matches everything, {@code ^admin} anchors the match) and pathological patterns such as
 * {@code (a+)+$} cause catastrophic backtracking on the server (ReDoS). The methods here quote the input so it is
 * always matched literally.
 */
public final class RegexCriteria {

    private RegexCriteria() {
    }

    /**
     * Case-insensitive literal substring match (SQL {@code LIKE '%term%'}).
     *
     * <p>A {@code null} term is treated as empty, which matches any string value of the field.
     *
     * @param field the document field
     * @param term the user-supplied search term, matched literally
     * @return the criteria
     */
    public static Criteria containsIgnoreCase(String field, String term) {
        return Criteria.where(field).regex(quote(term), "i");
    }

    /**
     * Quote user input for literal use inside a regular expression. The returned pattern is unanchored, so on its own
     * it performs a substring match.
     *
     * @param term the user-supplied text
     * @return a regex matching the text literally
     */
    public static String quote(String term) {
        return Pattern.quote(term == null ? "" : term);
    }
}
