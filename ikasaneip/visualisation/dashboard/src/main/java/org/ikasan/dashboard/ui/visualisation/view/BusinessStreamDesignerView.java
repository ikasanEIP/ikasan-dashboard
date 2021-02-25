package org.ikasan.dashboard.ui.visualisation.view;

import com.github.appreciated.css.grid.sizes.Flex;
import com.github.appreciated.layout.FluentGridLayout;
import com.vaadin.componentfactory.Tooltip;
import com.vaadin.componentfactory.TooltipAlignment;
import com.vaadin.componentfactory.TooltipPosition;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.GeneratedVaadinDialog;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.broadcast.FlowStateBroadcaster;
import org.ikasan.dashboard.ui.general.component.TooltipHelper;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.visualisation.component.FlowSelectDialog;
import org.ikasan.designer.*;
import org.ikasan.designer.event.CanvasItemDoubleClickEvent;
import org.ikasan.designer.event.CanvasItemDoubleClickEventListener;
import org.ikasan.designer.event.CanvasItemRightClickEvent;
import org.ikasan.designer.event.CanvasItemRightClickEventListener;
import org.ikasan.designer.menu.LineContextMenu;
import org.ikasan.designer.menu.ShapeContextMenu;
import org.ikasan.designer.pallet.*;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;


@Route(value = "designer", layout = IkasanAppLayout.class)
@UIScope
@PageTitle("Ikasan - Designer")
@Component
public class BusinessStreamDesignerView extends VerticalLayout implements BeforeEnterObserver, CanvasItemRightClickEventListener, CanvasItemDoubleClickEventListener
{
    Logger logger = LoggerFactory.getLogger(BusinessStreamDesignerView.class);


    private Registration broadcasterRegistration;

    private boolean initialised = false;

    private Designer businessStreamDesigner;

    @Resource
    private ModuleMetaDataService moduleMetadataService;

    /**
     * Constructor
     */
    public BusinessStreamDesignerView()
    {
        this.setMargin(false);
        this.setSpacing(false);

        this.setHeight("100%");
        this.setWidth("100%");
    }

    private void init()
    {
        businessStreamDesigner = new Designer();
        businessStreamDesigner.addCanvasItemRightClickEventListener(this);
        businessStreamDesigner.addCanvasItemDoubleClickEventListener(this);
        businessStreamDesigner.setSizeFull();
        businessStreamDesigner.addItemPallet(new ItemPallet("General", this.createGeneralPalette()));
        businessStreamDesigner.addItemPallet(new ItemPallet("Integrated Systems", this.createIntegratedSystemsPalette()));
        businessStreamDesigner.addItemPallet(new ItemPallet("Boundaries", this.createBoundariesPalette()));

        this.add(businessStreamDesigner);
    }

    private com.vaadin.flow.component.Component createGeneralPalette(){
        DesignerPalletItem flowImage = new DesignerPalletIconItem("frontend/images/flow.png", designerPalletItem -> {
            FlowSelectDialog dialog = new FlowSelectDialog(this.moduleMetadataService);

            dialog.open();

            dialog.addOpenedChangeListener((ComponentEventListener<GeneratedVaadinDialog.OpenedChangeEvent<Dialog>>) dialogOpenedChangeEvent -> {
                if(!dialogOpenedChangeEvent.isOpened() && dialog.getFlow() != null) {
                    businessStreamDesigner.addLabelToItem(designerPalletItem
                        , dialog.getFlow().getModuleName() + "." + dialog.getFlow().getFlowName());
                }
            });

        }, 95, 63);
        flowImage.setWidth("30px");
        flowImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                this.businessStreamDesigner.addItemToCanvas(flowImage);
            }
        });
        DragSource.create(flowImage);

        Tooltip tooltip = TooltipHelper.getTooltip(flowImage,"This icon represents an Ikasan flow.", TooltipPosition.RIGHT, TooltipAlignment.RIGHT);

        DesignerPalletItem channelImage = new DesignerPalletIconItem("frontend/images/message-channel.png", designerPalletItem -> {

        }, 95, 63);
        channelImage.setWidth("30px");
        channelImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                this.businessStreamDesigner.addItemToCanvas(channelImage);
            }
        });
        DragSource.create(channelImage);

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(flowImage, channelImage);

        layout.add(tooltip);

        return layout;
    }

    private com.vaadin.flow.component.Component createIntegratedSystemsPalette(){
        DesignerPalletItem computerImage = new DesignerPalletIconItem("frontend/images/computer.png", designerPalletItem -> {

        }, 62, 62);

        computerImage.setWidth("30px");
        computerImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                this.businessStreamDesigner.addItemToCanvas(computerImage);
            }
        });
        DragSource.create(computerImage);


        FluentGridLayout layout = new FluentGridLayout()
            .withTemplateRows(new Flex(1))
            .withTemplateColumns(new Flex(1))
            .withRowAndColumn(computerImage, 1, 1, 1, 1)
            .withPadding(false)
            .withSpacing(true)
            .withOverflow(FluentGridLayout.Overflow.AUTO);

        return layout;
    }

    private com.vaadin.flow.component.Component createBoundariesPalette(){

        DesignerPalletItem rectangleImage = new DesignerPalletRectangleItem("frontend/images/rectangle.png", designerPalletItem -> {

        }, 100, 100);
        rectangleImage.setWidth("30px");
        DragSource.create(rectangleImage);
        rectangleImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                this.businessStreamDesigner.addItemToCanvas(rectangleImage);
            }
        });

        DesignerPalletItem triangleImage = new DesignerPalletTriangleItem("frontend/images/triangle.png", designerPalletItem -> {

        }, 100, 100);
        triangleImage.setWidth("30px");
        DragSource.create(triangleImage);
        triangleImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                this.businessStreamDesigner.addItemToCanvas(triangleImage);
            }
        });

        DesignerPalletItem ovalImage = new DesignerPalletOvalItem("frontend/images/oval.png", designerPalletItem -> {
        }, 100, 100);
        ovalImage.setWidth("30px");
        DragSource.create(ovalImage);
        ovalImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                this.businessStreamDesigner.addItemToCanvas(ovalImage);
            }
        });

        DesignerPalletItem circleImage = new DesignerPalletCircleItem("frontend/images/circle.png", designerPalletItem -> {

        }, 100, 100);
        circleImage.setWidth("30px");
        DragSource.create(circleImage);
        circleImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                this.businessStreamDesigner.addItemToCanvas(circleImage);
            }
        });

        DesignerPalletItem labelImage = new DesignerPalletLabelItem("frontend/images/text.png", designerPalletItem   -> {
        }, 100, 100);
        labelImage.setWidth("30px");
        DragSource.create(labelImage);
        labelImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                this.businessStreamDesigner.addItemToCanvas(labelImage);
            }
        });


        HorizontalLayout layout = new HorizontalLayout();
        layout.add(rectangleImage, triangleImage, ovalImage, circleImage, labelImage);

        return layout;
    }


    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent)
    {
        if(!initialised)
        {
            this.init();
            initialised = true;
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent)
    {
        UI ui = attachEvent.getUI();

        broadcasterRegistration = FlowStateBroadcaster.register(flowState ->
        {
            ui.access(() ->
            {
                // do something interesting here.
                logger.debug("Received flow state: " + flowState);
            });
        });

    }

    @Override
    protected void onDetach(DetachEvent detachEvent)
    {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    @Override
    public void rightClickEvent(CanvasItemRightClickEvent canvasItemRightClickEvent) {

        if(canvasItemRightClickEvent.getFigure().getType().equals("draw2d.Connection")) {
            LineContextMenu lineContextMenu = new LineContextMenu(this.businessStreamDesigner,
                canvasItemRightClickEvent.getClickLocationX(), canvasItemRightClickEvent.getClickLocationY());
            lineContextMenu.open();
        }
        else {
            ShapeContextMenu shapeContextMenu = new ShapeContextMenu(this.businessStreamDesigner,
                canvasItemRightClickEvent.getClickLocationX(), canvasItemRightClickEvent.getClickLocationY());
            shapeContextMenu.open();
        }
    }

    @Override
    public void doubleClickEvent(CanvasItemDoubleClickEvent canvasItemDoubleClickEvent) {
        Dialog dialog = new Dialog();

        dialog.add(new H1("Double click!"), new Text(canvasItemDoubleClickEvent.getFigure().toString()));
        dialog.open();
    }
}

