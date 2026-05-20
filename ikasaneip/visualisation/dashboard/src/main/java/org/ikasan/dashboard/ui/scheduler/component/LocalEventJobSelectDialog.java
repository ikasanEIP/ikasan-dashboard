package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.scheduler.listener.SchedulerJobSelectedListener;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.job.model.SolrSchedulerJobSearchFilterImpl;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.LocalEventJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobSearchFilter;
import org.ikasan.spec.search.SearchResults;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalEventJobSelectDialog extends AbstractCloseableResizableDialog {

    private DataProvider<LocalEventJob, SchedulerJobSearchFilter> dataProvider;
    private ConfigurableFilterDataProvider<LocalEventJob, Void, SchedulerJobSearchFilter> filteredDataProvider;
    private List<SchedulerJobSelectedListener> schedulerJobSelectedListeners = new ArrayList<>();
    private ContextTemplate contextTemplate;
    private Grid<LocalEventJob> localEventJobGrid;

    SchedulerJobSearchFilter searchFilter = new SolrSchedulerJobSearchFilterImpl();

    public LocalEventJobSelectDialog(ContextTemplate contextTemplate) {
        this.setHeight("70vh");
        this.setWidth("70vw");

        this.contextTemplate = contextTemplate;

        this.init();

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(this.localEventJobGrid);
        layout.getStyle().set("padding-bottom", "20px");

        super.title.setText("Select Local Event Job");
        super.content.add(layout);
    }

    private void init() {
        List<LocalEventJob> localEventJobs = ContextHelper.getLocalEventJobsFromContext(contextTemplate);
        dataProvider = DataProvider.fromFilteringCallbacks(query -> {
            Optional<SchedulerJobSearchFilter> filter = query.getFilter();

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            Stream<LocalEventJob> localEventJobStream;
            if(filter.isPresent() && filter.get().getJobNameFilter() != null &&
                !filter.get().getJobNameFilter().isEmpty()) {
                localEventJobStream =  localEventJobs.stream()
                    .filter(localEventJob
                        -> localEventJob.getJobName().toLowerCase().contains(filter.get().getJobNameFilter().toLowerCase()))
                    .collect(Collectors.toList())
                    .subList(offset, limit)
                    .stream();
            }
            else {
                localEventJobStream = localEventJobs.subList(offset, limit).stream();
            }

            if(query.getSortOrders().size() > 0 && query.getSortOrders().get(0).getDirection().equals(SortDirection.ASCENDING)) {
                return localEventJobStream.sorted(Comparator.comparing(SchedulerJob::getJobName));
            }
            else if(query.getSortOrders().size() > 0 && query.getSortOrders().get(0).getDirection().equals(SortDirection.DESCENDING)) {
                return localEventJobStream.sorted(Comparator.comparing(SchedulerJob::getJobName).reversed());
            }
            else {
                return localEventJobStream;
            }
        }, query -> {
            Optional<SchedulerJobSearchFilter> filter = query.getFilter();

            if(filter.isPresent() && filter.get().getJobNameFilter() != null &&
                !filter.get().getJobNameFilter().isEmpty()) {
                return localEventJobs.stream()
                    .filter(localEventJob
                        -> localEventJob.getJobName().toLowerCase().contains(filter.get().getJobNameFilter().toLowerCase()))
                    .collect(Collectors.toList()).size();
            }
            else {
                return localEventJobs.size();
            }
        });

        filteredDataProvider = dataProvider.withConfigurableFilter();
        filteredDataProvider.setFilter(this.searchFilter);

        this.localEventJobGrid = new Grid<>();
        this.localEventJobGrid.setDataProvider(filteredDataProvider);
        this.localEventJobGrid.setHeight("100%");
        this.localEventJobGrid.addColumn(new ComponentRenderer<>(localEventJob -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            NativeLabel jobNameLabel = new NativeLabel(localEventJob.getJobName());

            horizontalLayout.add(jobNameLabel);

            return horizontalLayout;
        })).setHeader(getTranslation("table-header.job-name", UI.getCurrent().getLocale()))
        .setResizable(true)
        .setSortable(true)
        .setKey("jobName")
        .setFlexGrow(12);

        HeaderRow hr = this.localEventJobGrid.appendHeaderRow();
        this.addGridFiltering(hr, searchFilter::setJobNameFilter, "jobName");

        this.localEventJobGrid.addItemDoubleClickListener(event -> {
            this.schedulerJobSelectedListeners.forEach(listener -> {
                listener.jobSelected(event.getItem());
            });

            this.close();
        });
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

        hr.getCell(this.localEventJobGrid.getColumnByKey(columnKey)).setComponent(textField);
    }

    public void addSchedulerJobSelectedListener(SchedulerJobSelectedListener listener) {
        this.schedulerJobSelectedListeners.add(listener);
    }

    public void removeSchedulerJobSelectedListener(SchedulerJobSelectedListener listener) {
        this.schedulerJobSelectedListeners.remove(listener);
    }
}
