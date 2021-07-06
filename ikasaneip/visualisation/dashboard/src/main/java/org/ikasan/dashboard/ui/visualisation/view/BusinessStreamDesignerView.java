package org.ikasan.dashboard.ui.visualisation.view;

import com.vaadin.componentfactory.Tooltip;
import com.vaadin.componentfactory.TooltipAlignment;
import com.vaadin.componentfactory.TooltipPosition;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.GeneratedVaadinDialog;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.html.Div;
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
import org.ikasan.dashboard.ui.visualisation.actions.BusinessStreamManageFunction;
import org.ikasan.dashboard.ui.visualisation.actions.BusinessStreamOpenFunction;
import org.ikasan.dashboard.ui.visualisation.actions.BusinessStreamSaveAsFunction;
import org.ikasan.dashboard.ui.visualisation.actions.BusinessStreamSaveFunction;
import org.ikasan.dashboard.ui.visualisation.component.BusinessStreamIntegratedSystemUploadDialog;
import org.ikasan.dashboard.ui.visualisation.component.FlowSelectDialog;
import org.ikasan.dashboard.ui.visualisation.component.MessageChannelNameDialog;
import org.ikasan.dashboard.ui.visualisation.util.BusinessStreamItemTypes;
import org.ikasan.designer.Designer;
import org.ikasan.designer.ItemPallet;
import org.ikasan.designer.event.CanvasItemDoubleClickEvent;
import org.ikasan.designer.event.CanvasItemDoubleClickEventListener;
import org.ikasan.designer.event.CanvasItemRightClickEvent;
import org.ikasan.designer.event.CanvasItemRightClickEventListener;
import org.ikasan.designer.menu.LabelContextMenu;
import org.ikasan.designer.menu.LineContextMenu;
import org.ikasan.designer.menu.ShapeContextMenu;
import org.ikasan.designer.pallet.*;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.UUID;


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

    private FlexLayout integratedSystemPalette;

    @Resource
    private ModuleMetaDataService moduleMetadataService;

    @Resource
    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;

    @Value(value = "${integrated.systems.image.path}")
    private String integratedSystemsImagePath;

    private ArrayList<Image> integratedSystems;

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
        this.integratedSystemPalette = this.createIntegratedSystemsPalette();

        businessStreamDesigner = new Designer(new BusinessStreamOpenFunction(this.businessStreamMetaDataService, this.integratedSystems)
            ,new BusinessStreamSaveFunction(this.businessStreamMetaDataService), new BusinessStreamSaveAsFunction(this.businessStreamMetaDataService),
            new BusinessStreamManageFunction(this.businessStreamMetaDataService), this.integratedSystemsImagePath);

        businessStreamDesigner.addCanvasItemRightClickEventListener(this);
        businessStreamDesigner.addCanvasItemDoubleClickEventListener(this);
        businessStreamDesigner.setSizeFull();
        businessStreamDesigner.addItemPallet(new ItemPallet("General", this.createGeneralPalette()));
        businessStreamDesigner.addItemPallet(new ItemPallet("Integrated Systems", this.integratedSystemPalette));
        businessStreamDesigner.addItemPallet(new ItemPallet("Shapes", this.createShapesPalette()));


        this.add(businessStreamDesigner);
    }

    private com.vaadin.flow.component.Component createGeneralPalette(){
        DesignerPalletImageItem flowImage = new DesignerPalletIconImageItem("frontend/images/flow.png", designerPalletItem -> {
            FlowSelectDialog dialog = new FlowSelectDialog(this.moduleMetadataService);

            dialog.open();

            dialog.addOpenedChangeListener((ComponentEventListener<GeneratedVaadinDialog.OpenedChangeEvent<Dialog>>) dialogOpenedChangeEvent -> {
                if(!dialogOpenedChangeEvent.isOpened() && dialog.getFlow() != null) {
                    designerPalletItem.setIdentifier(new DesignerItemIdentifier(BusinessStreamItemTypes.FLOW.name(),
                        dialog.getFlow().getModuleName() + "." + dialog.getFlow().getFlowName(), UUID.randomUUID().toString()));

                    this.businessStreamDesigner.addItemToCanvas(designerPalletItem);

                    businessStreamDesigner.addLabelToItem(designerPalletItem
                        , dialog.getFlow().getModuleName() + "." + dialog.getFlow().getFlowName());

                }
            });

        }, 95, 63);
        flowImage.setWidth("30px");
        flowImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                flowImage.executeCanvasAddAction();
            }
        });
        DragSource.create(flowImage);

        Tooltip tooltip = TooltipHelper.getTooltip(flowImage,"Ikasan flow"
            , TooltipPosition.BOTTOM, TooltipAlignment.BOTTOM);

        DesignerPalletImageItem channelImage = new DesignerPalletIconImageItem("frontend/images/message-channel.png", designerPalletItem -> {
            MessageChannelNameDialog messageChannelNameDialog =  new MessageChannelNameDialog();
            messageChannelNameDialog.open();

            messageChannelNameDialog.addOpenedChangeListener((ComponentEventListener<GeneratedVaadinDialog.OpenedChangeEvent<Dialog>>) dialogOpenedChangeEvent -> {
                if(!dialogOpenedChangeEvent.isOpened() && messageChannelNameDialog.isOkPressed()) {
                    designerPalletItem.setIdentifier(new DesignerItemIdentifier(BusinessStreamItemTypes.MESSAGE_CHANNEL.name(),
                        messageChannelNameDialog.getMessageChannelName(), UUID.randomUUID().toString()));

                    this.businessStreamDesigner.addItemToCanvas(designerPalletItem);

                    businessStreamDesigner.addLabelToItem(designerPalletItem, messageChannelNameDialog.getMessageChannelName());

                }
            });

        }, 95, 63);
        channelImage.setWidth("30px");
        channelImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                channelImage.executeCanvasAddAction();
            }
        });
        DragSource.create(channelImage);

        Tooltip channelImageTooltip = TooltipHelper.getTooltip(channelImage,"Message channel"
            , TooltipPosition.BOTTOM, TooltipAlignment.BOTTOM);


        DesignerPalletImageItem labelImage = new DesignerPalletLabelImageItem("frontend/images/text.png", designerPalletItem   -> {
            designerPalletItem.setIdentifier(new DesignerItemIdentifier(DesignerPalletItemType.LABEL.name(),
                DesignerItemIdentifier.NOT_APPLICABLE, UUID.randomUUID().toString()));
            this.businessStreamDesigner.addItemToCanvas(designerPalletItem);
        }, 95, 63);
        labelImage.setWidth("20px");
        DragSource.create(labelImage);
        labelImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                labelImage.executeCanvasAddAction();
            }
        });

        Tooltip labelImageTooltip = TooltipHelper.getTooltip(labelImage,"Text"
            , TooltipPosition.BOTTOM, TooltipAlignment.BOTTOM);

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(flowImage, channelImage, labelImage);

        layout.add(tooltip, channelImageTooltip, labelImageTooltip);

        return layout;
    }

    private FlexLayout createIntegratedSystemsPalette() {
        DesignerPalletImageItem computerImage = new DesignerPalletIconImageItem("frontend/images/computer.png", designerPalletItem -> {
            designerPalletItem.setIdentifier(new DesignerItemIdentifier(BusinessStreamItemTypes.INTEGRATED_SYSTEM.name(),
                DesignerItemIdentifier.NOT_APPLICABLE, UUID.randomUUID().toString()));
            this.businessStreamDesigner.addItemToCanvas(designerPalletItem);
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
            this.integratedSystems = this.getIntegratedSystems();
            this.integratedSystems.forEach(item -> {
                if(item instanceof DesignerPalletImageItem) {
                    this.addItemToLayout((DesignerPalletImageItem) item, layout);
                }
            });
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        DesignerPalletButtonItem palletIconItem = new DesignerPalletButtonItem(VaadinIcon.PLUS.create(), designerPalletItem -> {
        }, "35px", "35px");

        palletIconItem.getComponent().addClickListener((ComponentEventListener<ClickEvent<Button>>) imageClickEvent -> {
            BusinessStreamIntegratedSystemUploadDialog uploadDialog
                = new BusinessStreamIntegratedSystemUploadDialog(this.integratedSystemsImagePath);
            uploadDialog.open();

            uploadDialog.addOpenedChangeListener((ComponentEventListener<GeneratedVaadinDialog.OpenedChangeEvent<Dialog>>)
                dialogOpenedChangeEvent -> {
                if(!dialogOpenedChangeEvent.isOpened()) {
                    if(uploadDialog.isUploaded()) {
                        this.addItemToLayout(this.getImageItem(uploadDialog.getFilePath(), BusinessStreamItemTypes.INTEGRATED_SYSTEM.name())
                            , this.integratedSystemPalette);
                    }
                }
            });
        });
        palletIconItem.getComponent().getElement().getStyle().set("margin-right", "15px");
        palletIconItem.getComponent().getElement().getStyle().set("margin-bottom", "15px");
        layout.add(palletIconItem.getComponent());

        return layout;
    }

    private void addItemToLayout(DesignerPalletImageItem item, FlexLayout layout) {
        item.getComponent().setWidth("35px");
        item.getComponent().addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                item.executeCanvasAddAction();
            }
        });
        DragSource.create(item.getComponent());
        item.getComponent().getElement().getStyle().set("margin-right", "15px");
        item.getComponent().getElement().getStyle().set("margin-bottom", "15px");

        com.vaadin.flow.component.Component component = layout.getComponentAt(layout.getComponentCount()-1);
        layout.remove(component);
        layout.add(new Div(item.getComponent()));
        layout.add(component);
    }

    private com.vaadin.flow.component.Component createShapesPalette(){

        DesignerPalletImageItem rectangleImage = new DesignerPalletRectangleImageItem("frontend/images/rectangle.png", designerPalletItem -> {
            designerPalletItem.setIdentifier(new DesignerItemIdentifier(DesignerPalletItemType.RECTANGLE.name(),
                DesignerItemIdentifier.NOT_APPLICABLE, UUID.randomUUID().toString()));
            this.businessStreamDesigner.addItemToCanvas(designerPalletItem);
        }, 100, 100);
        rectangleImage.setWidth("30px");
        Tooltip rectangleImageTooltip = TooltipHelper.getTooltip(rectangleImage,"Rectangle"
            , TooltipPosition.BOTTOM, TooltipAlignment.BOTTOM);
        DragSource.create(rectangleImage);
        rectangleImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                rectangleImage.executeCanvasAddAction();
            }
        });

        DesignerPalletImageItem triangleImage = new DesignerPalletTriangleImageItem("frontend/images/triangle.png", designerPalletItem -> {
            designerPalletItem.setIdentifier(new DesignerItemIdentifier(DesignerPalletItemType.TRIANGLE.name(),
                DesignerItemIdentifier.NOT_APPLICABLE, UUID.randomUUID().toString()));
            this.businessStreamDesigner.addItemToCanvas(designerPalletItem);
        }, 100, 100);
        triangleImage.setWidth("30px");
        Tooltip triangleImageTooltip = TooltipHelper.getTooltip(triangleImage,"Triangle"
            , TooltipPosition.BOTTOM, TooltipAlignment.BOTTOM);
        DragSource.create(triangleImage);
        triangleImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                triangleImage.executeCanvasAddAction();
            }
        });

        DesignerPalletImageItem ovalImage = new DesignerPalletOvalImageItem("frontend/images/oval.png", designerPalletItem -> {
            designerPalletItem.setIdentifier(new DesignerItemIdentifier(DesignerPalletItemType.OVAL.name(),
                DesignerItemIdentifier.NOT_APPLICABLE, UUID.randomUUID().toString()));
            this.businessStreamDesigner.addItemToCanvas(designerPalletItem);
        }, 200, 100);
        ovalImage.setWidth("30px");
        Tooltip ovalImageTooltip = TooltipHelper.getTooltip(ovalImage,"Oval"
            , TooltipPosition.BOTTOM, TooltipAlignment.BOTTOM);
        DragSource.create(ovalImage);
        ovalImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                ovalImage.executeCanvasAddAction();
            }
        });

        DesignerPalletImageItem circleImage = new DesignerPalletCircleImageItem("frontend/images/circle.png", designerPalletItem -> {
            designerPalletItem.setIdentifier(new DesignerItemIdentifier(DesignerPalletItemType.CIRCLE.name(),
                DesignerItemIdentifier.NOT_APPLICABLE, UUID.randomUUID().toString()));
            this.businessStreamDesigner.addItemToCanvas(designerPalletItem);
        }, 200, 200);
        circleImage.setWidth("30px");
        Tooltip circleImageTooltip = TooltipHelper.getTooltip(circleImage,"Circle"
            , TooltipPosition.BOTTOM, TooltipAlignment.BOTTOM);
        DragSource.create(circleImage);
        circleImage.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            if(imageClickEvent.getClickCount() == 2) {
                circleImage.executeCanvasAddAction();
            }
        });


        HorizontalLayout layout = new HorizontalLayout();
        layout.add(rectangleImage, rectangleImageTooltip, triangleImage, triangleImageTooltip, ovalImage, ovalImageTooltip, circleImage, circleImageTooltip);

        return layout;
    }

    private ArrayList<Image> getIntegratedSystems() throws IOException {
        ArrayList<Image> images = new ArrayList<>();

        Files.list(Paths.get(this.integratedSystemsImagePath)).forEach(
            file -> {
                DesignerPalletImageItem item = getImageItem(file, BusinessStreamItemTypes.INTEGRATED_SYSTEM.name());
                if(item != null) {
                    images.add(item);
                }
            });

        return images;
    }

    private DesignerPalletImageItem getImageItem(Path file, String type) {
        int width = 0;
        int height = 0;

        try {
            FileInputStream fileInputStream = new FileInputStream(file.toFile());
            BufferedImage bimg = ImageIO.read(fileInputStream);
            if(bimg == null) {
                return null;
            }
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
            designerPalletItem.setIdentifier(new DesignerItemIdentifier(type,
                DesignerItemIdentifier.NOT_APPLICABLE, UUID.randomUUID().toString()));
            this.businessStreamDesigner.addItemToCanvas(designerPalletItem);
        }, width, height);

        return palletIconItem;
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
        else if(canvasItemRightClickEvent.getFigure().getType().equals("draw2d.shape.basic.Label")) {
            LabelContextMenu lineContextMenu = new LabelContextMenu(this.businessStreamDesigner, canvasItemRightClickEvent.getFigure(),
                canvasItemRightClickEvent.getClickLocationX(), canvasItemRightClickEvent.getClickLocationY());
            lineContextMenu.open();
        }
        else if(!canvasItemRightClickEvent.getFigure().getType().equals("draw2d.shape.basic.Image")) {
            ShapeContextMenu shapeContextMenu = new ShapeContextMenu(this.businessStreamDesigner, canvasItemRightClickEvent.getFigure(),
                canvasItemRightClickEvent.getClickLocationX(), canvasItemRightClickEvent.getClickLocationY());
            shapeContextMenu.open();
        }
    }

    @Override
    public void doubleClickEvent(CanvasItemDoubleClickEvent canvasItemDoubleClickEvent) {
//        Dialog dialog = new Dialog();
//
//        dialog.add(new H1("Double click!"), new Text(canvasItemDoubleClickEvent.getFigure().toString()));
//        dialog.open();
    }
}

