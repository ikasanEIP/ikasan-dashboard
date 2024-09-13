package org.ikasan.designer;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PositionedDialog extends Dialog {
    private Logger logger = LoggerFactory.getLogger(PositionedDialog.class);
    private static final String SET_PROPERTY_IN_OVERLAY_JS = "this.$.overlay.$.overlay.style[$0]=$1";

    private final int heightPx;
    private final int widthPx;

    public PositionedDialog(int heightPx, int widthPx) {
        this.heightPx = heightPx;
        this.widthPx = widthPx;

        super.setHeight(heightPx + "px");
        super.setWidth(widthPx + "px");
    }

    @Override
    public void setHeight(String value) {
        // no implementation as want to make sure size is only set from constructor
        throw new UnsupportedOperationException("Cannot change height after construction");
    }

    @Override
    public void setWidth(String value) {
        // no implementation as want to make sure size is only set from constructor
        throw new UnsupportedOperationException("Cannot change width after construction");
    }

    @Override
    public void setSizeFull() {
        // no implementation as want to make sure size is only set from constructor
        throw new UnsupportedOperationException("Cannot change size after construction");
    }

    public void setPosition(Position position) {
        enablePositioning(true);
        Dimension dimension = this.getBrowserDimension();

        if(position.top + this.heightPx > dimension.height) {
            getElement().executeJs(SET_PROPERTY_IN_OVERLAY_JS, "top", dimension.height - this.heightPx - 40 + "px");
        }
        else {
            getElement().executeJs(SET_PROPERTY_IN_OVERLAY_JS, "top", position.getTop()+"px");
        }

        if(position.left + this.widthPx > dimension.width) {
            getElement().executeJs(SET_PROPERTY_IN_OVERLAY_JS, "left", dimension.width - this.widthPx - 40 + "px");
        }
        else {
            getElement().executeJs(SET_PROPERTY_IN_OVERLAY_JS, "left", position.getLeft()+"px");
        }
    }

    private void enablePositioning(boolean positioningEnabled) {
        getElement()
            .executeJs(SET_PROPERTY_IN_OVERLAY_JS, "align-self", positioningEnabled ? "flex-start" : "unset");
        getElement()
            .executeJs(SET_PROPERTY_IN_OVERLAY_JS, "position", positioningEnabled ? "absolute" : "relative");
    }

    private Dimension getBrowserDimension() {
        Dimension dimension = new Dimension();
        UI.getCurrent().getPage().retrieveExtendedClientDetails(evt -> {
            int height = evt.getWindowInnerHeight();
            int width = evt.getWindowInnerWidth();
            dimension.height = height;
            dimension.width = width;
        });
        return dimension;
    }

    private class Dimension {
        private int height;
        private int width;

        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }


    }

    public static class Position {
        private int top;
        private int left;

        public Position(int top, int left) {
            this.top = top;
            this.left = left;
        }

        public int getTop() {
            return top;
        }

        public void setTop(int top) {
            this.top = top;
        }

        public int getLeft() {
            return left;
        }

        public void setLeft(int left) {
            this.left = left;
        }
    }


}
