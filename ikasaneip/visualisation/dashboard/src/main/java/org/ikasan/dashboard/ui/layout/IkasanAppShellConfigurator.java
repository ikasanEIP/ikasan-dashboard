package org.ikasan.dashboard.ui.layout;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.material.Material;

import java.util.HashMap;

@Push
@JsModule("./styles/shared-styles.js")
@Viewport("width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes")
@Theme(themeClass = Material.class)
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
