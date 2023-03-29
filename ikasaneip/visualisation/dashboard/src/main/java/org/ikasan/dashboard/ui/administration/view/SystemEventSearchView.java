package org.ikasan.dashboard.ui.administration.view;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import org.ikasan.dashboard.ui.administration.component.SystemEventDialog;
import org.ikasan.dashboard.ui.administration.component.SystemEventFilteringGrid;
import org.ikasan.dashboard.ui.administration.component.SystemEventSearchForm;
import org.ikasan.dashboard.ui.administration.util.SystemEventFormatter;
import org.ikasan.dashboard.ui.search.listener.SearchListener;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventSearchFilter;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.ikasan.systemevent.model.SolrSystemEvent;
import org.ikasan.systemevent.model.SolrSystemEventSearchFilter;
import org.ikasan.systemevent.model.SystemEventImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SystemEventSearchView extends VerticalLayout implements SearchListener
{
    private Logger logger = LoggerFactory.getLogger(SystemEventSearchView.class);

    private SystemEventSearchService systemEventSearchService;

    private SystemEventFilteringGrid searchResultsGrid;
    private SystemEventSearchFilter searchFilter = new SolrSystemEventSearchFilter();

    private SystemEventSearchForm searchForm;

    private Label resultsLabel = new Label();

    private DateFormatter dateFormatter;

    /**
     * Constructor
     */
    public SystemEventSearchView(SystemEventSearchService systemEventSearchService,
                                 DateFormatter dateFormatter)
    {
        super();
        this.systemEventSearchService = systemEventSearchService;
        if(this.systemEventSearchService ==  null)
        {
            throw new IllegalArgumentException("systemEventSearchService cannot be null!");
        }
        this.dateFormatter = dateFormatter;
        if(this.dateFormatter ==  null)
        {
            throw new IllegalArgumentException("dateFormatter cannot be null!");
        }
    }

    protected void init()
    {
        this.setSizeFull();
        this.setSpacing(false);
        this.setPadding(false);
        this.setMargin(false);
        this.setId("identifier");

        this.getElement().getThemeList().remove("padding");
        this.getElement().getThemeList().remove("spacing");

        resultsLabel.setVisible(false);

        this.createSearchForm();

        this.searchResultsGrid = new SystemEventFilteringGrid(this.systemEventSearchService, this.searchFilter, this.resultsLabel);

        ObjectMapper objectMapper = new ObjectMapper();

        this.searchResultsGrid.addColumn(new ComponentRenderer<>(ikasanSolrDocument ->
        {
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            horizontalLayout.setWidth("100%");
            horizontalLayout.setJustifyContentMode(JustifyContentMode.START);
            if(ikasanSolrDocument.getActor() != null && ! ikasanSolrDocument.getActor().isEmpty()) {
                horizontalLayout.add(ikasanSolrDocument.getActor());
            }
            else {
                try {
                    horizontalLayout.add(objectMapper.readValue(((SolrSystemEvent)ikasanSolrDocument).getPayload()
                        , SystemEventImpl.class).getActor());
                } catch (JsonProcessingException e) {
                    // Not much we can do if the event is not valid json.
                }
            }

            return horizontalLayout;
        })).setFlexGrow(2)
            .setHeader(getTranslation("text-field.action-performed-by", UI.getCurrent().getLocale()))
            .setKey("actor")
            .setResizable(true);
        this.searchResultsGrid.addColumn(new ComponentRenderer<>(ikasanSolrDocument ->
        {
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            horizontalLayout.setWidth("100%");
            horizontalLayout.setJustifyContentMode(JustifyContentMode.START);

            if(ikasanSolrDocument.getSubject() != null && ! ikasanSolrDocument.getSubject().isEmpty()) {
                horizontalLayout.add(ikasanSolrDocument.getSubject());
            }
            else {
                try {
                    horizontalLayout.add(objectMapper.readValue(((SolrSystemEvent)ikasanSolrDocument).getPayload()
                        , SystemEventImpl.class).getSubject());
                } catch (JsonProcessingException e) {
                    // Not much we can do if the event is not valid json.
                }
            }

            return horizontalLayout;
        })).setFlexGrow(4).setKey("context")
            .setHeader(getTranslation("text-field.system-event-context", UI.getCurrent().getLocale()))
            .setResizable(true);
        this.searchResultsGrid.addColumn(new ComponentRenderer<>(ikasanSolrDocument ->
        {
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            horizontalLayout.setWidth("100%");
            horizontalLayout.setJustifyContentMode(JustifyContentMode.START);
            if(ikasanSolrDocument.getAction() != null && ! ikasanSolrDocument.getAction().isEmpty()) {
                horizontalLayout.add(ikasanSolrDocument.getAction());
            }
            else {
                try {
                    horizontalLayout.add(objectMapper.readValue(((SolrSystemEvent)ikasanSolrDocument).getPayload()
                        , SystemEventImpl.class).getAction());
                } catch (JsonProcessingException e) {
                    // Not much we can do if the event is not valid json.
                }
            }

            return horizontalLayout;
        })).setFlexGrow(12)
            .setHeader(getTranslation("header.system-event", UI.getCurrent().getLocale()))
            .setKey("action")
            .setResizable(true);
        this.searchResultsGrid.addColumn(TemplateRenderer.<SystemEvent>of(
            "<div>[[item.date]]</div>")
            .withProperty("date",
                ikasanSolrDocument -> this.dateFormatter.getFormattedDate(((SolrSystemEvent)ikasanSolrDocument).getTimestampLong())))
            .setHeader(getTranslation("table-header.timestamp", UI.getCurrent().getLocale()))
            .setSortable(true)
            .setKey("timestamp")
            .setFlexGrow(4)
            .setResizable(true);

        HeaderRow hr = searchResultsGrid.appendHeaderRow();
        this.searchResultsGrid.addGridFiltering(hr, value -> searchFilter.setActor(value), "actor");
        this.searchResultsGrid.addSelectGridFiltering(hr, value -> searchFilter.setSubject(value), SystemEventConstants.getSystemEventConstants(), "context");
        this.searchResultsGrid.addGridFiltering(hr, value -> searchFilter.setAction(value), "action");
        this.searchResultsGrid.setVisible(true);

        this.searchResultsGrid.setWidthFull();
        this.searchResultsGrid.setHeight("100%");

        this.searchResultsGrid.addItemDoubleClickListener((ComponentEventListener<ItemDoubleClickEvent<SystemEvent>>)
            ikasanSolrDocumentItemDoubleClickEvent -> {
                SystemEventDialog systemEventDialog = new SystemEventDialog(this.dateFormatter);
                systemEventDialog.populate(ikasanSolrDocumentItemDoubleClickEvent.getItem());
            });

        HorizontalLayout resultsLayout = new HorizontalLayout();
        resultsLayout.setWidthFull();
        resultsLayout.add(resultsLabel);

        add(searchForm, resultsLayout, this.searchResultsGrid);
    }

    /**
     * Create the search form that appears at the top of the screen.
     */
    protected void createSearchForm()
    {
        this.searchForm = new SystemEventSearchForm();
        this.searchForm.addSearchListener(this);
    }

    @Override
    public void search(String searchTerm, List<String> entityTypes, boolean negateQuery, long startDate, long endDate) {
        this.searchResultsGrid.init(startDate, endDate, searchTerm, List.of("systemEvent"), false, this.searchFilter);
        this.resultsLabel.setVisible(true);
    }


}
