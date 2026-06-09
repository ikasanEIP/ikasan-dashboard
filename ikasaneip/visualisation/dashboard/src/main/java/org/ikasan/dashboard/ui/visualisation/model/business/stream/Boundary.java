package org.ikasan.dashboard.ui.visualisation.model.business.stream;

public class Boundary {
    private int x;
    private int y;
    private int w;
    private int h;
    private String colour;
    private String label;

    /**
     * Constructs a new {@code Boundary} object with specified position, dimensions, colour, and label.
     *
     * @param x the x-coordinate of the boundary
     * @param y the y-coordinate of the boundary
     * @param w the width of the boundary
     * @param h the height of the boundary
     * @param colour the colour of the boundary as a string
     * @param label the label associated with the boundary
     */
    public Boundary(int x, int y, int w, int h, String colour, String label) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.colour = colour;
        this.label = label;
    }

    /**
     * Retrieves the value of the x-coordinate.
     *
     * @return the x-coordinate value.
     */
    public int getX() {
        return x;
    }

    /**
     * Retrieves the y-coordinate value.
     *
     * @return the y-coordinate as an integer.
     */
    public int getY() {
        return y;
    }

    /**
     * Retrieves the width of the boundary.
     *
     * @return the width of the boundary as an integer.
     */
    public int getW() {
        return w;
    }

    /**
     * Retrieves the value of the height (h) attribute.
     *
     * @return the height value as an integer.
     */
    public int getH() {
        return h;
    }

    /**
     * Retrieves the colour associated with the object.
     *
     * @return the colour as a String.
     */
    public String getColour() {
        return colour;
    }

    /**
     * Retrieves the label associated with this object.
     *
     * @return the label of this object
     */
    public String getLabel() {
        return label;
    }
}
