package org.ikasan.solr.util;

import org.apache.solr.client.solrj.util.ClientUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * "Contains" filter clauses for tokenised (text_general) fields, such as the user and system event filters typed in
 * the dashboard.
 * <p>
 * A plain {@code field:*value*} wildcard is matched against single indexed tokens, so a value the tokenizer splits
 * (e.g. 'harness-user-1', 'svc-orders' or 'Dashboard login') never matched. The value is split the same way and every
 * part must be contained in a token: {@code (field:*harness* AND field:*user* AND field:*1*)}. Parts are escaped, so
 * characters with a query meaning (':', '(', '*', ...) are taken literally.
 */
public final class SolrContainsClause
{
    /** Characters Solr's StandardTokenizer keeps inside a token (besides letters and digits), e.g. john.smith. */
    private static final Pattern TOKEN_SEPARATORS = Pattern.compile("[^\\p{L}\\p{N}._']+");

    private SolrContainsClause() {}

    /**
     * @param field the field to filter
     * @param value the filter text as typed
     * @return the clause; matches everything when the value has no searchable characters
     */
    public static String of(String field, String value)
    {
        List<String> parts = new ArrayList<>();
        if(value != null)
        {
            for(String part : TOKEN_SEPARATORS.split(value.trim()))
            {
                // the tokenizer drops leading/trailing '.', '_' and quote characters from a token
                String trimmed = part.replaceAll("^[._']+|[._']+$", "");
                if(!trimmed.isEmpty())
                {
                    parts.add(field + ":*" + ClientUtils.escapeQueryChars(trimmed) + "*");
                }
            }
        }
        return parts.isEmpty() ? field + ":*" : "(" + String.join(" AND ", parts) + ")";
    }
}
