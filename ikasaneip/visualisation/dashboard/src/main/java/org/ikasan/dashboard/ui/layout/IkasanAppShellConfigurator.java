package org.ikasan.dashboard.ui.layout;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.aura.Aura;

import java.util.HashMap;

@Push
@StyleSheet(Aura.STYLESHEET)
@CssImport("./styles/styles.css")
@JsModule("./styles/shared-styles.js")
@JavaScript("@polymer/font-roboto/roboto.js")
@CssImport(value = "./styles/dialog-overlay.css", themeFor = "vaadin-dialog-overlay")
@Viewport("width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes")
@PWA(name = "Ikasan Visualisation Dashboard",
    shortName = "Ikasan")
public class IkasanAppShellConfigurator implements AppShellConfigurator {

    @Override
    public void configurePage(AppShellSettings settings) {
        HashMap<String, String> attributes = new HashMap<>();
        attributes.put("rel", "shortcut icon");
        attributes.put("type", "image/png");
        settings.addLink("icons/icon.png", attributes);
        AppShellConfigurator.super.configurePage(settings);
    }

}
