package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.model.SchedulerJobSearchFilter;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.function.Consumer;

public class SchedulerJobFilteringGrid extends Grid<SchedulerJobRecord> {
    private Logger logger = LoggerFactory.getLogger(SchedulerJobFilteringGrid.class);

    private SchedulerJobService schedulerJobService;

    private DataProvider<SchedulerJobRecord, SchedulerJobSearchFilter> dataProvider;
    private ConfigurableFilterDataProvider<SchedulerJobRecord, Void, SchedulerJobSearchFilter> filteredDataProvider;

    private SchedulerJobSearchFilter searchFilter;

    private long resultSize = 0;

    private String contextName;

    /**
     * Constructor
     *
     * @param schedulerJobService
     * @param searchFilter
     */
    public SchedulerJobFilteringGrid(SchedulerJobService schedulerJobService,
                                     SchedulerJobSearchFilter searchFilter) {
        this.schedulerJobService = schedulerJobService;
        if(this.schedulerJobService ==  null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }
        this.searchFilter = searchFilter;
        if(this.searchFilter ==  null) {
            throw new IllegalArgumentException("searchFilter cannot be null!");
        }
    }

    /**
     * Add filtering to a column.
     *
     * @param hr
     * @param setFilter
     * @param columnKey
     */
    public void addGridFiltering(HeaderRow hr, Consumer<String> setFilter, String columnKey) {
        TextField textField = new TextField();
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        textField.setSuffixComponent(filterIcon);
        textField.setWidthFull();

        textField.addValueChangeListener(ev->{

            setFilter.accept(ev.getValue());

            filteredDataProvider.refreshAll();
        });

        hr.getCell(getColumnByKey(columnKey)).setComponent(textField);
    }

    /**
     * Add filtering to a column.
     *
     * @param hr
     * @param setFilter
     * @param columnKey
     */
    public void addComboBoxGridFiltering(HeaderRow hr, Consumer<String> setFilter, Set<Map.Entry<String, String>> options, String columnKey) {
        ComboBox<Map.Entry<String, String>> select = new ComboBox<>();
        select.setItems(options);
        select.setWidthFull();
        select.setClearButtonVisible(true);
        select.setItemLabelGenerator(entry -> {
            if(entry == null) {
                return "";
            }

            return entry.getKey();
        });

        select.addValueChangeListener(ev-> {
            if(ev.getValue() != null) {
                setFilter.accept(ev.getValue().getValue());
            }
            else {
                setFilter.accept(null);
            }

            filteredDataProvider.refreshAll();
        });

        HorizontalLayout layout = new HorizontalLayout(select);
        hr.getCell(getColumnByKey(columnKey)).setComponent(layout);
    }

    public void addSelectGridFiltering(HeaderRow hr, Consumer<String> setFilter, Set<Map.Entry<String, String>> options, String columnKey) {
        Select<Map.Entry<String, String>> select = new Select<>();
        select.setItems(options);
        select.setWidthFull();
        select.setEmptySelectionAllowed(true);
        select.setItemLabelGenerator(entry -> {
            if(entry == null) {
                return "";
            }

            return entry.getKey();
        });

        select.addValueChangeListener(ev-> {
            if(ev.getValue() != null) {
                setFilter.accept(ev.getValue().getValue());
            }
            else {
                setFilter.accept(null);
            }

            filteredDataProvider.refreshAll();
        });

        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");

        HorizontalLayout layout = new HorizontalLayout(select, filterIcon);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, filterIcon);

        hr.getCell(getColumnByKey(columnKey)).setComponent(layout);
    }

    /**
     * Add filtering to a column.
     *
     * @param hr
     * @param setFilter
     * @param columnKey
     */
    public void addCheckboxGridFiltering(HeaderRow hr, Consumer<Boolean> setFilter, String columnKey) {
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("14pt");
        filterIcon.getElement().getStyle().set("margin-bottom", "0px");

        Checkbox checkbox = new Checkbox();
        checkbox.setWidthFull();
        checkbox.getElement().getStyle().set("margin-bottom", "0px");
        checkbox.addValueChangeListener(ev-> {
            if(ev.getValue()) {
                setFilter.accept(ev.getValue());
            }
            else {
                setFilter.accept(null);
            }
            filteredDataProvider.refreshAll();
        });

        HorizontalLayout layout = new HorizontalLayout(checkbox, filterIcon);
        layout.setWidth("120px");
        layout.getElement().getStyle().set("margin-bottom", "0px");
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, filterIcon);

        hr.getCell(getColumnByKey(columnKey)).setComponent(layout);
    }

    /**
     * Add general filter
     *
     * @param textField
     * @param setFilter
     */
    public void addGridFiltering(TextField textField, Consumer<String> setFilter) {
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        textField.setSuffixComponent(filterIcon);
        textField.setSuffixComponent(filterIcon);
        textField.addValueChangeListener(ev->{

            setFilter.accept(ev.getValue());

            filteredDataProvider.refreshAll();
        });
    }

    /**
     * Initialise the grid.
     */
    public void init() {
        dataProvider = DataProvider.fromFilteringCallbacks(query -> {
            Optional<SchedulerJobSearchFilter> filter = query.getFilter();

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            SearchResults results;

            if(query.getSortOrders().size() > 0) {
                results = this.getResults(filter.get(), offset, limit, query.getSortOrders().get(0).getSorted(),
                    query.getSortOrders().get(0).getDirection().name());
            }
            else {
                results = this.getResults(filter.get(), offset, limit, null, null);
            }

            return results.getResultList().stream();
        }, query -> {
            Optional<SchedulerJobSearchFilter> filter = query.getFilter();

            SearchResults results;

            results = this.getResults(filter.get(), 0, 0, null, null);

            this.resultSize = results.getTotalNumberOfResults();

            return (int) this.resultSize;
        });

        filteredDataProvider = dataProvider.withConfigurableFilter();
        filteredDataProvider.setFilter(this.searchFilter);

        this.setDataProvider(filteredDataProvider);
    }

    private SearchResults getResults(SchedulerJobSearchFilter filter, int offset, int limit, String sortColumn, String sortDirection) {
        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        SearchResults results;

        try {
            results = this.schedulerJobService.findByFilter(filter, limit, offset, sortColumn, sortDirection);
        }
        catch (Exception e) {
            e.printStackTrace();
            final UI current = UI.getCurrent();
            final I18NProvider i18NProvider = VaadinService.getCurrent().getInstantiator().getI18NProvider();
            NotificationHelper.showErrorNotification(i18NProvider.getTranslation("error.solr-unavailable"
                , current.getLocale()));

            results = new SearchResultsImpl(new ArrayList<>(), 0, 0);
        }

        return results;
    }

    public long getResultSize()
    {
        return resultSize;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
        this.searchFilter.setContextSearchFilter(contextName);
    }

    public void refresh() {
        this.dataProvider.refreshAll();
        this.filteredDataProvider.refreshAll();
    }
}
