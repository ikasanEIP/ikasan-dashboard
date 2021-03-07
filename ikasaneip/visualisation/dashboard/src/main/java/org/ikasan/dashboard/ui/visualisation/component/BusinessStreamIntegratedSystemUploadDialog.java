package org.ikasan.dashboard.ui.visualisation.component;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import org.ikasan.business.stream.metadata.model.BusinessStreamMetaDataImpl;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BusinessStreamIntegratedSystemUploadDialog extends AbstractCloseableResizableDialog
{
    private byte[] businessStreamFile;
    private String filename;
    private String uploadPath;
    private boolean uploaded=false;

    /**
     * Constructor
     *
     */
    public BusinessStreamIntegratedSystemUploadDialog(String uploadPath)
    {
        this.uploadPath = uploadPath;
        init();
    }


    private void init()
    {
        this.setModal(true);

        VerticalLayout verticalLayout = new VerticalLayout();

        Image mrSquidImage = new Image("/frontend/images/mr-squid-head.png", "");
        mrSquidImage.setHeight("35px");

        Label businessStreamHeader = new Label(String.format("Add Integrated System"));

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setHeight("40px");
        header.add(mrSquidImage, businessStreamHeader);
        header.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, mrSquidImage, businessStreamHeader);

        verticalLayout.add(header);
        verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, header);


        MemoryBuffer fileBuffer = new MemoryBuffer();
        Upload upload = new Upload(fileBuffer);
        upload.setMaxFiles(1);
        upload.addFinishedListener(event -> {
            InputStream inputStream =
                fileBuffer.getInputStream();

            try
            {
                this.businessStreamFile = new byte[inputStream.available()];
                inputStream.read(businessStreamFile);
                this.filename = event.getFileName();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        });

        Button saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
        {
            try {
                this.writeFile();
                this.uploaded = true;
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            this.close();
        });

        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener(buttonClickEvent -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(saveButton, cancelButton);

        verticalLayout.add(upload, buttonLayout);
        verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, upload, buttonLayout);
        this.content.add(verticalLayout);
        super.setWidth("400px");
        super.setHeight("300px");
        super.showResize(false);
    }

    private void writeFile() throws IOException {
        FileOutputStream fileWriter = null;
        try {
            fileWriter = new FileOutputStream(this.uploadPath + "/" + filename);
            fileWriter.write(this.businessStreamFile);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if(fileWriter != null) {
                fileWriter.flush();
                fileWriter.close();
            }
        }
    }

    public Path getFilePath() {
        return Paths.get(this.uploadPath + "/" +this.filename);
    }

    public boolean isUploaded() {
        return uploaded;
    }
}
