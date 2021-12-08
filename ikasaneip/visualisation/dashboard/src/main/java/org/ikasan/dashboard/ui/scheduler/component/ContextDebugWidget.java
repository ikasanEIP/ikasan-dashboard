package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.scheduler.context.cache.ContextMachineCache;
import org.ikasan.scheduler.core.machine.ContextMachine;
import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@CssImport("./styles/dashboard-view.css")
public class ContextDebugWidget extends Div implements BeforeEnterListener {

    Logger logger = LoggerFactory.getLogger(ContextDebugWidget.class);

    protected AceEditor aceEditor;

    private Tab fullContextInstance;
    private Tab contextStatus;
    private Tab contextEvents;
    private Tabs tabs;

    private Select<String> contextInstances;
    private ObjectMapper objectMapper;

    private StringBuffer events = new StringBuffer();

    private UI ui;

    /**
     * Constructor
     */
    public ContextDebugWidget(ScheduledContextInstanceService scheduledContextInstanceService, SchedulerService schedulerService) {
        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("100%");

        this.initialiseEditor();

        this.objectMapper = new ObjectMapper();

        this.ui = UI.getCurrent();

        this.contextInstances = new Select<>();
        this.contextInstances.setLabel("Context Instance");
        this.contextInstances.addValueChangeListener(listener -> {
            try {
                ContextMachine contextMachine = ContextMachineCache.instance().get(this.contextInstances.getValue());
                if(contextMachine != null) {
                    contextMachine.addSchedulerJobInitiationEventRaisedListener(event -> {
                        events.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(event));
                    });
                }
                else {
                    return;
                }
                if(tabs.getSelectedTab().equals(this.fullContextInstance)) {
                        this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(contextMachine.getContext()));
                }
                else if(tabs.getSelectedTab().equals(this.contextStatus)) {
                    this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(contextMachine.getContextInstanceStatus()));
                }
            }
            catch (JsonProcessingException e){
                e.printStackTrace();
            }
        });

        Button addContextButton = new Button("Add Context");
        addContextButton.addClickListener(buttonClickEvent -> {
            ContextUploadDialog contextUploadDialog = new ContextUploadDialog(scheduledContextInstanceService, schedulerService);
            contextUploadDialog.open();

            contextUploadDialog.addOpenedChangeListener(event -> {
                if(!event.isOpened()){
                    this.contextInstances.removeAll();
                    this.contextInstances.setItems(ContextMachineCache.instance().keys());
                }
            });
        });

        Button resetContextButton = new Button("Reset Context");
        resetContextButton.addClickListener(buttonClickEvent -> {
            ContextMachine contextMachine = ContextMachineCache.instance().get(this.contextInstances.getValue());
            try {
                contextMachine.resetContextInstance();
                if(tabs.getSelectedTab().equals(this.fullContextInstance)) {
                    if(this.contextInstances.getValue() != null && !this.contextInstances.getValue().isEmpty()){
                        this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(contextMachine.getContext()));
                    }
                    else {
                        this.aceEditor.setValue("");
                    }
                }
                else if(tabs.getSelectedTab().equals(this.contextStatus)) {
                    if(this.contextInstances.getValue() != null && !this.contextInstances.getValue().isEmpty()){
                        contextMachine = ContextMachineCache.instance().get(this.contextInstances.getValue());
                        this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(contextMachine.getContextInstanceStatus()));
                    }
                    else {
                        this.aceEditor.setValue("");
                    }
                }
                else if(tabs.getSelectedTab().equals(this.contextEvents)) {
                    this.aceEditor.setValue(this.events.toString());
                }
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });

        HorizontalLayout controlsLayout = new HorizontalLayout();
        controlsLayout.add(this.contextInstances, addContextButton, resetContextButton);

        this.fullContextInstance = new Tab("Full Context Instance");
        this.contextStatus = new Tab("Context Instance Status");
        this.contextEvents = new Tab("Context Instance Events");
        this.tabs = new Tabs(fullContextInstance, contextStatus, contextEvents);

        tabs.addSelectedChangeListener(event -> {
            try {
                if(tabs.getSelectedTab().equals(this.fullContextInstance)) {
                    if(this.contextInstances.getValue() != null && !this.contextInstances.getValue().isEmpty()){
                        ContextMachine contextMachine = ContextMachineCache.instance().get(this.contextInstances.getValue());
                        contextMachine.addSchedulerJobStateChangeEventListener(stateChange
                            -> {
                            ui.access(() -> {
                            try {
                                 this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContext()));
                            }
                            catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }});
                        });
                        this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(contextMachine.getContext()));
                    }
                    else {
                        this.aceEditor.setValue("");
                    }
                }
                else if(tabs.getSelectedTab().equals(this.contextStatus)) {
                    if(this.contextInstances.getValue() != null && !this.contextInstances.getValue().isEmpty()){
                        ContextMachine contextMachine = ContextMachineCache.instance().get(this.contextInstances.getValue());
                        contextMachine.addSchedulerJobStateChangeEventListener(stateChange
                            -> {
                            ui.access(() -> {
                            try {
                                 this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()));
                            }
                            catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }});
                        });
                        this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(contextMachine.getContextInstanceStatus()));
                    }
                    else {
                        this.aceEditor.setValue("");
                    }
                }
                else if(tabs.getSelectedTab().equals(this.contextEvents)) {
                    this.aceEditor.setValue(this.events.toString());
                }
            }
            catch (JsonProcessingException e){
                e.printStackTrace();
            }
        });

        HorizontalLayout tabLayout = new HorizontalLayout();
        tabLayout.add(tabs);

        div.add(controlsLayout, tabLayout, this.aceEditor);

        this.setSizeFull();
        this.add(div);
    }

    protected void initialiseEditor()
    {
        aceEditor = new AceEditor();

        aceEditor.setTheme(AceTheme.dracula);
        aceEditor.setMode(AceMode.json);
        aceEditor.setFontSize(11);
        aceEditor.setTabSize(4);
        aceEditor.setWidth("auto");
        aceEditor.setHeight("80vh");
        aceEditor.setReadOnly(true);
        aceEditor.setWrap(false);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.contextInstances.setItems(ContextMachineCache.instance().keys());
    }
}
