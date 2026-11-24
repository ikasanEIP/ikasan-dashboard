package org.ikasan.dashboard.ui.util;

import com.vaadin.flow.server.VaadinSession;

import java.util.Locale;

/**
 * A user's locale, readable from other users' sessions (e.g. by an administrator's Session Management view).
 * <p>
 * Since Vaadin 25 {@link VaadinSession#getLocale()} is backed by a signal that can only be read from its own session,
 * and a session's own attributes need that session's lock, which a request for another session cannot take while it
 * holds its own. So the locale is also kept in the underlying HTTP session (like the username), which any thread can
 * read: {@link #remember} it wherever the session's locale is set.
 */
public final class SessionLocale {

    /** HTTP session attribute holding the session's {@link Locale}. */
    public static final String ATTRIBUTE = "ikasan.locale";

    private SessionLocale() {
    }

    /** Record the session's locale in its HTTP session. Call on the session's own thread, where the locale is set. */
    public static void remember(VaadinSession session, Locale locale) {
        if (session != null && session.getSession() != null && locale != null) {
            session.getSession().setAttribute(ATTRIBUTE, locale);
        }
    }

    /**
     * The session's locale: the live value for the current session, the remembered one for any other session (null if
     * it never recorded one).
     */
    public static Locale of(VaadinSession session) {
        if (session == null) {
            return null;
        }
        if (session == VaadinSession.getCurrent()) {
            return session.getLocale();
        }
        Object locale = session.getSession() != null ? session.getSession().getAttribute(ATTRIBUTE) : null;
        return locale instanceof Locale ? (Locale) locale : null;
    }
}
