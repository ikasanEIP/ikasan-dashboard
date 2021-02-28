package org.ikasan.dashboard.ui.visualisation.view;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.componentfactory.Tooltip;
import com.vaadin.componentfactory.TooltipAlignment;
import com.vaadin.componentfactory.TooltipPosition;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.GeneratedVaadinDialog;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.broadcast.FlowStateBroadcaster;
import org.ikasan.dashboard.ui.general.component.TooltipHelper;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.visualisation.component.BusinessStreamIntegratedSystemUploadDialog;
import org.ikasan.dashboard.ui.visualisation.component.BusinessStreamUploadDialog;
import org.ikasan.dashboard.ui.visualisation.component.FlowSelectDialog;
import org.ikasan.designer.*;
import org.ikasan.designer.event.CanvasItemDoubleClickEvent;
import org.ikasan.designer.event.CanvasItemDoubleClickEventListener;
import org.ikasan.designer.event.CanvasItemRightClickEvent;
import org.ikasan.designer.event.CanvasItemRightClickEventListener;
import org.ikasan.designer.menu.LineContextMenu;
import org.ikasan.designer.menu.ShapeContextMenu;
import org.ikasan.designer.pallet.*;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;


@Route(value = "designer", layout = IkasanAppLayout.class)
@UIScope
@PageTitle("Ikasan - Designer")
@Component
        public class BusinessStreamDesignerView extends VerticalLayout implements BeforeEnterObserver, CanvasItemRightClickEventListener, CanvasItemDoubleClickEventListener, BeforeLeaveObserver
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
        DesignerPalletImageItem flowImage = new DesignerPalletIconImageItem("frontend/images/flow.png", designerPalletItem -> {
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

        DesignerPalletImageItem channelImage = new DesignerPalletIconImageItem("frontend/images/message-channel.png", designerPalletItem -> {

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

    private com.vaadin.flow.component.Component createIntegratedSystemsPalette() {
        DesignerPalletImageItem computerImage = new DesignerPalletIconImageItem("frontend/images/computer.png", designerPalletItem -> {

        }, 62, 62);

        computerImage.setWidth("35px");
        computerImage.setHeight("35px");
        computerImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                this.businessStreamDesigner.addItemToCanvas(computerImage);
            }
        });
        DragSource.create(computerImage);
        computerImage.getElement().getStyle().set("margin-right", "15px");
        computerImage.getElement().getStyle().set("margin-bottom", "15px");

        FlexLayout layout = new FlexLayout();
        layout.add(computerImage);
        layout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        layout.setAlignContent(FlexLayout.ContentAlignment.START);

        try {

            this.getIntegratedSystems().forEach(item -> {
                if(item instanceof Image) {
                    ((Image)item.getComponent()).setWidth("35px");
                    ((Image)item.getComponent()).addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
                        if(imageClickEvent.getClickCount() == 2) {
                            this.businessStreamDesigner.addItemToCanvas((DesignerPalletImageItem)item);
                        }
                    });
                    DragSource.create(item.getComponent());
                    item.getComponent().getElement().getStyle().set("margin-right", "15px");
                    item.getComponent().getElement().getStyle().set("margin-bottom", "15px");
                    layout.add(item.getComponent());
                }
            });
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        DesignerPalletButtonItem palletIconItem = new DesignerPalletButtonItem(VaadinIcon.PLUS.create(), designerPalletItem -> {
        }, "35px", "35px");

        palletIconItem.getComponent().addClickListener((ComponentEventListener<ClickEvent<Button>>) imageClickEvent -> {
            BusinessStreamIntegratedSystemUploadDialog uploadDialog = new BusinessStreamIntegratedSystemUploadDialog();
            uploadDialog.open();

//            uploadDialog.addOpenedChangeListener((ComponentEventListener<GeneratedVaadinDialog.OpenedChangeEvent<Dialog>>)
//                dialogOpenedChangeEvent -> populateBusinessStreamGrid());
        });
        palletIconItem.getComponent().getElement().getStyle().set("margin-right", "15px");
        palletIconItem.getComponent().getElement().getStyle().set("margin-bottom", "15px");
        layout.add(palletIconItem.getComponent());

        return layout;
    }

    private com.vaadin.flow.component.Component createBoundariesPalette(){

        DesignerPalletImageItem rectangleImage = new DesignerPalletRectangleImageItem("frontend/images/rectangle.png", designerPalletItem -> {

        }, 100, 100);
        rectangleImage.setWidth("30px");
        DragSource.create(rectangleImage);
        rectangleImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                this.businessStreamDesigner.addItemToCanvas(rectangleImage);
            }
        });

        DesignerPalletImageItem triangleImage = new DesignerPalletTriangleImageItem("frontend/images/triangle.png", designerPalletItem -> {

        }, 100, 100);
        triangleImage.setWidth("30px");
        DragSource.create(triangleImage);
        triangleImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                this.businessStreamDesigner.addItemToCanvas(triangleImage);
            }
        });

        DesignerPalletImageItem ovalImage = new DesignerPalletOvalImageItem("frontend/images/oval.png", designerPalletItem -> {
        }, 100, 100);
        ovalImage.setWidth("30px");
        DragSource.create(ovalImage);
        ovalImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                this.businessStreamDesigner.addItemToCanvas(ovalImage);
            }
        });

        DesignerPalletImageItem circleImage = new DesignerPalletCircleImageItem("frontend/images/circle.png", designerPalletItem -> {

        }, 100, 100);
        circleImage.setWidth("30px");
        DragSource.create(circleImage);
        circleImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                this.businessStreamDesigner.addItemToCanvas(circleImage);
            }
        });

        DesignerPalletImageItem labelImage = new DesignerPalletLabelImageItem("frontend/images/text.png", designerPalletItem   -> {
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

    private List<DesignerPalletItem> getIntegratedSystems() throws IOException {
        ArrayList<DesignerPalletItem> images = new ArrayList<>();

        Files.list(Paths.get("/sandbox/mick/images")).forEach(
            file -> {
                int width = 0;
                int height = 0;

                try {
                    FileInputStream fileInputStream = new FileInputStream(file.toFile());
                    BufferedImage bimg = ImageIO.read(fileInputStream);
                    width = bimg.getWidth();
                    height = bimg.getHeight();

                    if(width > 100) {
                        int multiple = width / 100;

                        height = height / multiple;
                        width = width / multiple;
                    }

                    fileInputStream.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                StreamResource res = new StreamResource(file.getFileName().toString(), () -> {
                    // eg. load image data from classpath (src/main/resources/images/image.png)
                    try {
                        return new FileInputStream(file.toFile());
                    }
                    catch (FileNotFoundException e) {
                        return null;
                    }
                    catch (IOException e) {
                        return null;
                    }
                });
                DesignerPalletImageItem palletIconItem = new DesignerPalletIconImageItem(res, designerPalletItem -> {
                    }, width, height);

                images.add(palletIconItem);
            }
        );

        return images;
    }


    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent)
    {
        if(!initialised)
        {
            this.init();
            initialised = true;
        }

        this.businessStreamDesigner.importJson();
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent beforeLeaveEvent) {
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

