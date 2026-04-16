package org.ikasan.relational.persistence.module.metadata.model;

import org.ikasan.spec.metadata.model.Transition;

import java.util.Objects;

/**
 * Hibernate/PostgreSQL implementation of Transition.
 *
 * This is a POJO implementation used for JSON serialization within the
 * HibernateFlowMetaDataImpl.
 */
public class HibernateTransitionImpl implements Transition {

    private String from;
    private String to;
    private String name;

    public HibernateTransitionImpl() {
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "HibernateTransitionImpl{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HibernateTransitionImpl that = (HibernateTransitionImpl) o;
        return Objects.equals(from, that.from) &&
            Objects.equals(to, that.to) &&
            Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, name);
    }
}
