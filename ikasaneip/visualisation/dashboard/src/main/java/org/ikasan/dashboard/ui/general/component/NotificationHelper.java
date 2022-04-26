package org.ikasan.dashboard.ui.general.component;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
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
        Icon closeIcon = new Icon("lumo", "cross");
        closeIcon.getStyle().set("color", "white");

        Button closeButton = new Button(closeIcon, click -> notification.close());
        closeButton.addClickListener(event -> {
            notification.close();
        });

        notification.add(closeButton);
        notification.add(new Text(errorMessage));

        notification.open();
        lastMessage = errorMessage;
    }

    public static void showUserNotification(String message)
    {
        Notification notification = new Notification(message);
        notification.setPosition(Notification.Position.MIDDLE);
        notification.setDuration(errorNotificationDuration);
        notification.setOpened(true);

        lastMessage = message;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void resetLastMessage() {
        lastMessage = null;
    }
}
