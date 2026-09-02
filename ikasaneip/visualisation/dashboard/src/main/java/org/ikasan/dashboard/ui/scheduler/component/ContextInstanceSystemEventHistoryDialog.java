package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.ikasan.dashboard.ui.administration.component.SystemEventDialog;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.esb.service.systemevent.SystemEventSearchFilterImpl;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventSearchFilter;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

public class ContextInstanceSystemEventHistoryDialog extends AbstractCloseableResizableDialog {
    private Logger logger = LoggerFactory.getLogger(ContextInstanceSystemEventHistoryDialog.class);
    protected ContextInstance contextInstance;
    protected SystemEventSearchService systemEventSearchService;

    protected ConfigurableFilterDataProvider<SystemEvent,Void, SystemEventSearchFilter> filteredDataProvider;
    protected Grid<SystemEvent> systemEventGrid = new Grid<>();
    protected String noSystemEventsMessage;
    protected SystemEventSearchFilter filter = new SystemEventSearchFilterImpl();

    /**
     * Initializes a new ContextInstanceSystemEventHistoryDialog with the given contextInstance and systemEventSearchService.
     * This constructor initializes the dialog and grid for displaying system event history.
     *
     * @param contextInstance The context instance related to the system event history
     * @param systemEventSearchService The service for searching system events
     */
    public ContextInstanceSystemEventHistoryDialog(ContextInstance contextInstance, SystemEventSearchService systemEventSearchService) {
        this.contextInstance = contextInstance;
        this.systemEventSearchService = systemEventSearchService;
        this.filter.setSearchTerm(this.contextInstance.getId());
        this.noSystemEventsMessage = getTranslation("paragraph.no-system-events-for-context-instance", UI.getCurrent().getLocale());
        this.init();
        this.initialiseGrid();
    }

    /**
     * Initializes the dialog with specific configurations.
     * This method sets the dialog to not show resize icon, sets the title of the dialog using a translation key,
     * and sets the height and width of the dialog.
     */
    private void init() {
        super.showResize(false);
        super.title.setText(getTranslation("tab-label.system-events", UI.getCurrent().getLocale()));
        this.setHeight("85vh");
        this.setWidth("85vw");
    }

    @Override
    public void open() {

        if (this.getSystemEventRecords(-1,-1, null, null).getTotalNumberOfResults() == 0) {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("header.no-system-events", UI.getCurrent().getLocale()));
            confirmDialog.setText(this.noSystemEventsMessage);
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(false);
            confirmDialog.open();
        } else{
            DataProvider<SystemEvent, SystemEventSearchFilter> dataProvider =
                DataProvider.fromFilteringCallbacks(
                    // First callback fetches items based on a query
                    query -> {
                        // The index of the first item to load
                        int offset = query.getOffset();
                        // The number of items to load
                        int limit = query.getLimit();

                        SearchResults results;

                        if(query.getSortOrders().size() > 0) {
                            results = this.getSystemEventRecords(limit, offset, query.getSortOrders().get(0).getSorted(),
                                query.getSortOrders().get(0).getDirection().name());
                        }
                        else {
                            results = this.getSystemEventRecords(limit, offset, null, null);
                        }


                        return results.getResultList().stream();
                    },
                    // Second callback fetches the total number of items currently in the Grid.
                    // The grid can then use it to properly adjust the scrollbars.
                    query -> Math.toIntExact(this.getSystemEventRecords(-1, -1, null, null).getTotalNumberOfResults()));

            this.filteredDataProvider = dataProvider.withConfigurableFilter();
            this.filteredDataProvider.setFilter(this.filter);

            this.systemEventGrid.setDataProvider(dataProvider);
            this.systemEventGrid.getDataProvider().refreshAll();
            super.open();
        }
    }

    /**
     * Initialise the grid for displaying system event data.
     * This method configures the columns of the grid, sets renderers for the columns, adds event listeners for item double-click,
     * appends a header row for filtering, sets the size of the grid, adds select filtering and text field filtering to the header row,
     * and finally adds the grid to the content component.
     */
    private void initialiseGrid() {
        this.systemEventGrid.addColumn(new ComponentRenderer<>(systemEvent -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                Div label = new Div();
                label.getElement().getStyle().set("word-wrap", "normal");
                label.getElement().getStyle().set("white-space", "normal");

                verticalLayout.add(label);

                label.setText(systemEvent.getSubject());

                return verticalLayout;
            })).setFlexGrow(3)
            .setKey("systemEventSubject")
            .setHeader(getTranslation("text-field.system-event-context"))
            .setSortable(true)
            .setResizable(true);
        this.systemEventGrid.addColumn(new ComponentRenderer<>(systemEvent -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                Div label = new Div();
                label.getElement().getStyle().set("word-wrap", "normal");
                label.getElement().getStyle().set("white-space", "normal");

                verticalLayout.add(label);

                label.setText(systemEvent.getActor());

                return verticalLayout;
            })).setFlexGrow(1)
            .setKey("actor")
            .setHeader(getTranslation("text-field.action-performed-by"))
            .setResizable(true);
        this.systemEventGrid.addColumn(new ComponentRenderer<>(systemEvent -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                Div label = new Div();
                label.getElement().getStyle().set("word-wrap", "normal");
                label.getElement().getStyle().set("white-space", "normal");

                verticalLayout.add(label);

                label.setText(systemEvent.getAction());

                return verticalLayout;
            })).setFlexGrow(4)
            .setKey("action")
            .setHeader(getTranslation("label.system-event-details"))
            .setResizable(true);
        this.systemEventGrid.addColumn(new ComponentRenderer<>(systemEvent -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                Div label = new Div();
                label.getElement().getStyle().set("word-wrap", "normal");
                label.getElement().getStyle().set("white-space", "normal");

                verticalLayout.add(label);

                if(systemEvent.getAction() != null) {
                    label.setText(DateFormatter.instance().getFormattedDate(systemEvent.getTimestamp().getTime()));
                }

                return verticalLayout;
            })).setFlexGrow(3)
            .setHeader(getTranslation("table-header.timestamp"))
            .setSortable(true)
            .setKey("timestamp")
            .setResizable(true);

        this.systemEventGrid.addItemDoubleClickListener(event -> {
            SystemEventDialog systemEventDialog = new SystemEventDialog(DateFormatter.instance());
            systemEventDialog.populate(event.getItem());
        });
        HeaderRow hr = this.systemEventGrid.appendHeaderRow();
        this.systemEventGrid.setSizeFull();
        this.addSelectGridFiltering(hr, value -> filter.setSubject(value), SystemEventConstants.getSystemEventConstants(), "systemEventSubject");
        this.addGridFiltering(hr, value -> filter.setActor(value), "actor");
        this.addGridFiltering(hr, value -> filter.setAction(value), "action");
        super.content.add(this.systemEventGrid);
    }


    /**
     * Adds select filtering to a column in the grid header row.
     *
     * @param hr The HeaderRow where the filtering component will be added
     * @param setFilter The Consumer function to apply the selected filter value
     * @param options The list of options for the select filter
     * @param columnKey The key of the column where filtering will be applied
     */
    public void addSelectGridFiltering(HeaderRow hr, Consumer<String> setFilter, List<String> options, String columnKey) {
        ComboBox<String> select = new ComboBox<>();
        select.setItems(options);
        select.setWidthFull();
        select.setItemLabelGenerator(entry -> {
            if(entry == null) {
                return "";
            }

            return entry;
        });

        select.addValueChangeListener(ev-> {

            setFilter.accept(ev.getValue());

            if(this.filteredDataProvider != null) {
                this.filteredDataProvider.refreshAll();
            }
        });

        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");

        HorizontalLayout layout = new HorizontalLayout(select, filterIcon);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, filterIcon);

        hr.getCell(this.systemEventGrid.getColumnByKey(columnKey)).setComponent(layout);
    }

    /**
     * Adds text field filtering to a column in the grid header row.
     *
     * @param hr The HeaderRow where the filtering text field will be added
     * @param setFilter The Consumer function to apply the filter value entered in the text field
     * @param columnKey The key of the column where filtering will be applied
     */
    public void addGridFiltering(HeaderRow hr, Consumer<String> setFilter, String columnKey) {
        TextField textField = new TextField();
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        textField.setSuffixComponent(filterIcon);
        textField.setWidthFull();

        textField.addValueChangeListener(ev -> {

            setFilter.accept(ev.getValue());

            filteredDataProvider.refreshAll();
        });

        hr.getCell(this.systemEventGrid.getColumnByKey(columnKey)).setComponent(textField);
    }

    /**
     * Retrieves system event records based on the provided limit, offset, sort column, and sort order.
     *
     * @param limit the maximum number of records to retrieve
     * @param offset the offset to start retrieving records from
     * @param sortColumn the column to sort the records by
     * @param sortOrder the order in which the records should be sorted (ascending or descending)
     * @return SearchResults object containing the system event records based on the provided parameters
     */
    private SearchResults<SystemEvent> getSystemEventRecords(int limit, int offset, String sortColumn, String sortOrder) {
        return this.systemEventSearchService.findByFilter(this.filter, limit, offset, sortColumn, sortOrder);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
    }
}
