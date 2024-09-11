package org.ikasan.designer;

import com.vaadin.flow.component.dialog.Dialog;

public class PositionedDialog extends Dialog {
    private static final String SET_PROPERTY_IN_OVERLAY_JS = "this.$.overlay.$.overlay.style[$0]=$1";

    public void setPosition(Position position) {
        enablePositioning(true);
        getElement().executeJs(SET_PROPERTY_IN_OVERLAY_JS, "left", position.getLeft());
        getElement().executeJs(SET_PROPERTY_IN_OVERLAY_JS, "top", position.getTop());
    }

    private void enablePositioning(boolean positioningEnabled) {
        getElement()
            .executeJs(SET_PROPERTY_IN_OVERLAY_JS, "align-self", positioningEnabled ? "flex-start" : "unset");
        getElement()
            .executeJs(SET_PROPERTY_IN_OVERLAY_JS, "position", positioningEnabled ? "absolute" : "relative");
    }

    public static class Position {
        private String top;
        private String left;

        public Position(String top, String left) {
            this.top = top;
            this.left = left;
        }

        public String getTop() {
            return top;
        }

        public void setTop(String top) {
            this.top = top;
        }

        public String getLeft() {
            return left;
        }

        public void setLeft(String left) {
            this.left = left;
        }
    }
}
