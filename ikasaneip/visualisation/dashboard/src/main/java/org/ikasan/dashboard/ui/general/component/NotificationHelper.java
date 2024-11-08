package org.ikasan.dashboard.ui.general.component;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotificationHelper
{
    private static Integer errorNotificationDuration = 1000;

    private static String lastMessage;

    @Value("${error.notification.duration}")
    public void setErrorNotificationDuration(Integer duration)
    {
        NotificationHelper.errorNotificationDuration = duration;
    }

    public static void showErrorNotification(String errorMessage)
    {
        Notification notification = new Notification();
        notification.setPosition(Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.setDuration(errorNotificationDuration);

        Div textDiv = new Div();
        textDiv.setSizeFull();
        Text text = new Text(errorMessage);
        textDiv.add(text);
        textDiv.getElement().getStyle().set("text-align", "center");
        notification.add(textDiv);

        notification.open();
        lastMessage = errorMessage;
    }

    public static void showUserNotification(String message)
    {
        Notification notification = new Notification();
        notification.setPosition(Notification.Position.MIDDLE);
        notification.setDuration(errorNotificationDuration);

        Div textDiv = new Div();
        textDiv.setSizeFull();
        Text text = new Text(message);
        textDiv.add(text);
        textDiv.getElement().getStyle().set("text-align", "center");
        notification.add(textDiv);

        notification.open();
        lastMessage = message;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void resetLastMessage() {
        lastMessage = null;
    }
}
