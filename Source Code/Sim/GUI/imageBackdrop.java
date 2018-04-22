package GUI;

import microModel.jModel;

public class imageBackdrop implements graphic {
    
    private java.awt.image.BufferedImage im;
    private double x;
    private double y;
    private double w;
    private double h;
    
    // Constructor that loads the image and sets the span of the image
    public imageBackdrop(String im, double x, double y, double w, double h) {
        try {
            this.im = javax.imageio.ImageIO.read(new java.io.File(im));
        } catch (Exception e) {
            System.err.println("Image '"+im+"' could not be read.");
        }
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    // Not an object that can disappear
    public boolean exists(jModel model) {
        return true;
    }

    // Not part of the physical world
    public java.awt.Rectangle.Double getGlobalBounds() {
        return null;
    }

    // Paints the image if popup item is checked
    public void paint(java.awt.Graphics g, networkCanvas canvas) {
        if (canvas.popupItemChecked("Show backdrop")) {
            java.awt.Point p1 = canvas.getPoint(x, y);
            java.awt.Point p2 = canvas.getPoint(x+w, y+h);
            g.drawImage(im, p1.x, p1.y, p2.x-p1.x, p2.y-p1.y, null);
        }
    }
}