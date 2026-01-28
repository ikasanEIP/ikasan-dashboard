package org.ikasan.dashboard.ui.administration.view;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import org.ikasan.dashboard.ui.administration.component.SystemEventDialog;
import org.ikasan.dashboard.ui.administration.component.SystemEventFilteringGrid;
import org.ikasan.dashboard.ui.administration.component.SystemEventSearchForm;
import org.ikasan.dashboard.ui.general.component.LazyDownloadButton;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.search.listener.SearchListener;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.IkasanSystemEventDocumentToCsvConverter;
import org.ikasan.dashboard.ui.util.SessionAttributeConstants;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventSearchFilter;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.ikasan.systemevent.model.SolrSystemEvent;
import org.ikasan.systemevent.model.SolrSystemEventSearchFilter;
import org.ikasan.systemevent.model.SystemEventImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SystemEventSearchView extends VerticalLayout implements SearchListener
{
    private Logger logger = LoggerFactory.getLogger(SystemEventSearchView.class);

    private SystemEventSearchService systemEventSearchService;

    private SystemEventFilteringGrid searchResultsGrid;
    private SystemEventSearchFilter searchFilter = new SolrSystemEventSearchFilter();

    private SystemEventSearchForm searchForm;

    private NativeLabel resultsLabel = new NativeLabel();

    private DateFormatter dateFormatter;

    private LazyDownloadButton csvExportButton;

    private int maxDownloadBytes;

    private UI ui;

    /**
     * Constructor
     */
    public SystemEventSearchView(SystemEventSearchService systemEventSearchService,
                                 DateFormatter dateFormatter, int maxDownloadBytes)
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

        this.maxDownloadBytes = maxDownloadBytes;

        this.ui = UI.getCurrent();
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
            if(ikasanSolrDocument.getAction() != null && !ikasanSolrDocument.getAction().isEmpty()) {
                String action = ikasanSolrDocument.getAction();
                horizontalLayout.add(action.length() > 200 ? action.substring(0, 200) + "..." : action);
            }
            else {
                try {
                    String action = objectMapper.readValue(((SolrSystemEvent)ikasanSolrDocument).getPayload()
                        , SystemEventImpl.class).getAction();
                    if(action != null) {
                        horizontalLayout.add(action.length() > 200 ? action.substring(0, 200) + "..." : action);
                    }
                } catch (JsonProcessingException e) {
                    // Not much we can do if the event is not valid json.
                }
            }

            return horizontalLayout;
        })).setFlexGrow(12)
            .setHeader(getTranslation("header.system-event", UI.getCurrent().getLocale()))
            .setKey("action")
            .setResizable(true);
        this.searchResultsGrid.addColumn(LitRenderer.<SystemEvent>of(
            "<div>${item.date}</div>")
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

        this.createCsvDownloadButton();

        HorizontalLayout resultsLayout = new HorizontalLayout();
        resultsLayout.setWidthFull();
        resultsLayout.setHeight("35px");
        resultsLayout.add(resultsLabel, this.csvExportButton);

        this.csvExportButton.getElement().getStyle().set("position", "absolute");
        this.csvExportButton.getElement().getStyle().set("right", "30px");

        add(searchForm, resultsLayout, this.searchResultsGrid);
    }

    private void createCsvDownloadButton() {
        Image excelImage = new Image("/frontend/images/excel.png", "");
        excelImage.setHeight("25px");
        Optional<UI> optionalUI = UI.getCurrent().getUI();
        this.csvExportButton = new LazyDownloadButton(excelImage,
            () -> "system-events-results-"+System.currentTimeMillis()+".csv",
            () -> {
                try {
                    ZoneId zoneId;

                    if(ui != null && ui.getSession().getAttribute(SessionAttributeConstants.TIMEZONE_ID) != null) {
                        zoneId = ZoneId.of((String) ui.getSession().getAttribute(SessionAttributeConstants.TIMEZONE_ID));

                    }
                    else {
                        zoneId = ZoneId.of("UTC");
                    }

                    IkasanSystemEventDocumentToCsvConverter csvConverter
                        = new IkasanSystemEventDocumentToCsvConverter(zoneId);

                    for (int i = 0; i < searchResultsGrid.getResultSize(); i += 100) {
                        List<SystemEvent> docs = (List<SystemEvent>) searchResultsGrid.getDataProvider().fetch
                            (new Query<>(i, 100, Collections.EMPTY_LIST, null, null)).collect(Collectors.toList());

                        for (SystemEvent document : docs) {
                            csvConverter.addDocument(document);
                        }

                        if (csvConverter.getCvsContents().getBytes().length > this.maxDownloadBytes) {
                            optionalUI.ifPresent(ui -> {
                                if (ui.isAttached()) {
                                    ui.access(() ->
                                        NotificationHelper.showUserNotification(String.format(getTranslation("notification.download-size-exceeded"
                                            , UI.getCurrent().getLocale()), this.maxDownloadBytes))
                                    );
                                }
                            });

                            break;
                        }
                    }

                    return new ByteArrayInputStream(csvConverter.getCvsContents().getBytes());
                }
                catch (JsonProcessingException jsonProcessingException) {

                }

                return new ByteArrayInputStream(new byte[0]);
            }
        );
        csvExportButton.getElement().setAttribute("title"
            , getTranslation("tooltip.export-to-csv", UI.getCurrent().getLocale()));
        csvExportButton.setHeight("35px");
        csvExportButton.setWidth("30px");

        csvExportButton.setDisableOnClick(true);
        csvExportButton.addClickListener(event -> {
            if(this.searchResultsGrid.getResultSize() == 0) {
                NotificationHelper.showUserNotification(getTranslation("notification.no-records-to-download"
                    , UI.getCurrent().getLocale()));
                csvExportButton.reset();
                csvExportButton.setEnabled(true);
            }
            else {
                NotificationHelper.showUserNotification(getTranslation("notification.preparing-download"
                    , UI.getCurrent().getLocale()));
            }
        });

        csvExportButton.addDownloadStartsListener(event -> {
            event.getSource().setEnabled(true);
            csvExportButton.reset();
        });
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
