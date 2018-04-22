package GUI;

import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import microModel.jModel;
import microModel.jLane;
import microModel.jVehicle;
import microModel.jDetector;
import microModel.jTrafficLight;
import microModel.jConflict.conflictRSU;
import microModel.jRSU;

/**
 * Panel that displays the <tt>graphic</tt>s objects and allows user zooming and
 * panning.
 */
public class networkCanvas extends javax.swing.JPanel {
    
    /** Vertical zoom factor. */
    private double vZoomFactor = 1;
    
    /** Horizontal zoom factor. */
    private double hZoomFactor = 1;
    
    /** Western most coordinate of the model. */
    private double minGlobalX;
    
    /** Eastern most coordinate of the model. */
    private double maxGlobalX;
    
    /** Nothern most coordinate of the model. */
    private double minGlobalY;
    
    /** Southern most coordinate of the model. */
    private double maxGlobalY;
    
    /** Number of horizontal pixels of network that cannot be shown. */
    private double canvasX;
    
    /** Number of vertical pixels of network that cannot be shown. */
    private double canvasY;
    
    /** Width of the entire network in pixels. */
    private double canvasW = 1;
    
    /** Height of the entire network in pixels. */
    private double canvasH = 1;
    
    /** Horizontal slider. */
    private javax.swing.JScrollBar hSlider;
    
    /** Vertical slider. */
    private javax.swing.JScrollBar vSlider;
    
    /** Policy which keeps the center point center when updating the view. */
    private int POLICY_KEEP = 0;
    
    /** Policy which centers the network in view. */
    private int POLICY_CENTER = 1;
    
    /** Policy which keeps a given point under the mouse when updating the view. */
    private int POLICY_POINT = 2;
    
    /** Policy which will center the screen on the given point. */
    private int POLICY_CENTER_AT = 3;
    
    /** Stored center global point for policy <tt>POLICY_KEEP</tt>. */
    private java.awt.Point.Double centerPoint = new java.awt.Point.Double();
    
    /** Options menu. */
    private javax.swing.JPopupMenu popMenu;
    
    /** Corner square for if both sliders are shown. */
    private java.awt.Component corner;
    
    /** Layer with user graphics. */
    private java.util.ArrayList<layer> layers;
    
    /** Layer for user backdrops. */
    private final int LAYER_BACKDROP = 5;
    
    /** Layer for lanes. */
    private final int LAYER_LANE = 4;
    
    /** Layer for road-side units. */
    private final int LAYER_RSU = 3;
    
    /** Layer for vehicles. */
    private final int LAYER_VEHICLE = 2;
    
    /** Layer for labels. */
    private final int LAYER_LABEL = 1;
    
    /** Layer for user overlays. */
    private final int LAYER_OVERLAY = 0;
    
    /** Model object to visualize. */
    private jModel model;
    
    /** Parent GUI. */
    private jModelGUI gui;
    
    /** Whether destinations are shown upon the lanes. */
    protected boolean showDestinations;
    
    /** Vehicle to track with the camera. */
    protected jVehicle trackVehicle;
    
    private boolean isValidatingZoom = false;
    
    /** Default set of distinct colors. */
    private static double[][] defaultColors = {
        {1,0,0}, {0,1,0}, {0,0,1}, {1,1,0},  {1,0,1},  {0,1,1},
        {.5,0,0},{0,.5,0},{0,0,.5},{.5,.5,0},{.5,0,.5},{0,.5,.5},
        {1,.5,0},{1,0,.5},{0,1,.5},{.5,1,0}, {.5,0,1}, {0,.5,1}
    };
    
    /** Default color map (dark red, red, yellow, green, dark green). */
    private static int[][] defaultMap = {
        {192, 255, 255, 0, 0},
        {0, 0, 255, 255, 192},
        {0, 0, 0, 0, 0}
    };
    
    /** Width of the sliders */
    private final int SLIDERWIDTH = 17;
    
    /** Minimum portion of the canvas that will be empty at zoom = 1 in pixels. */
    private final int EDGE = 50;
    
    /**
     * Constructs the network canvas. 
     * @param model Model object to be visualized.
     * @param gui GUI within which the canvas is placed.
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    protected networkCanvas(jModel model, jModelGUI gui) {
        this.model = model;
        this.gui = gui;
        setBackground(new java.awt.Color(0, 0, 0));
        setLayout(null);
        // add resize listener
        addComponentListener(
            new java.awt.event.ComponentAdapter() {
                /** Invokes <tt>validateZoom()</tt> after a resize. */
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    validateZoom(POLICY_KEEP);
                }
            }
        );
        // mouse listener
        generalMouseListener gml = new generalMouseListener();
        addMouseListener(gml);
        addMouseMotionListener(gml);
        // sliders
        hSlider = new javax.swing.JScrollBar(javax.swing.SwingConstants.HORIZONTAL, 0, 0, 0, 0);
        vSlider = new javax.swing.JScrollBar(javax.swing.SwingConstants.VERTICAL, 0, 0, 0, 0);
        java.awt.event.AdjustmentListener sl = new java.awt.event.AdjustmentListener() {
            /** Invokes <tt>validateZoom()</tt> after a value change. */
            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent e) {
                validateZoom();
            }
        };
        hSlider.addAdjustmentListener(sl);
        vSlider.addAdjustmentListener(sl);
        corner = new java.awt.Component() {
            /**
             * Paints the square.
             * @param g Graphics to paint with.
             */
            @Override
            public void paint(java.awt.Graphics g) {
                g.setColor(vSlider.getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        // popup menu
        popMenu = new javax.swing.JPopupMenu("Visualization options");
        addPopupItem("Vehicles as dots", false);
        addPopupItem("Show downstream", false);
        addPopupItem("Show upstream", false);
        popMenu.addSeparator();
        addPopupItem("Show lanes", true);
        addPopupItem("Show lane IDs", false);
        addPopupItem("Show generator queue", false);
        popMenu.addSeparator();
        addPopupItem("Show detectors", false);
        addPopupItem("Show detector IDs", false);
        popMenu.addSeparator();
        addPopupItem("Traffic lights", true);
        addPopupItem("Conflicts", false);
        popMenu.addSeparator();
        add(hSlider);
        add(vSlider);
        add(corner);
        // canvas layers
        layers = new java.util.ArrayList<layer>(6);
        for (int i=0; i<6; i++) {
            layers.add(new layer());
            add(layers.get(i));
        }
    }
    
    /**
     * Returns an interpolated color from a color map.
     * @param map Color map with values in the range 0-255. <tt>map[0][:]</tt> 
     * contains the red values, <tt>map[1][:]</tt> contains the green values and 
     * <tt>map[2][:]</tt> contains the blue values.
     * @param mapValues Ordered values of the colors at the indices <tt>[:]</tt> of the map.
     * @param value Value in the range <tt>mapValues</tt>.
     * @return Interpolated color from the given map.
     */
    public static java.awt.Color fromMap(int[][] map, double[] mapValues, double value) {
        java.awt.Color c = null;
        int m = map[0].length-1;
        boolean asc = true;
        if (mapValues[1]<mapValues[0]) {
            asc = false;
        }
        if ((asc && value<=mapValues[0]) || (!asc && value>=mapValues[m])) {
            // below lower bound
            c = new java.awt.Color(map[0][0], map[1][0], map[2][0]);
        } else if ((asc && value>=mapValues[m]) || (!asc && value<=mapValues[0])) {
            // above upper bound
            c = new java.awt.Color(map[0][m], map[1][m], map[2][m]);
        } else {
            int n=0;
            while ((asc && value>mapValues[n+1]) || (!asc && value<mapValues[n+1])) {
                n++;
            }
            double f = Math.abs((value-mapValues[n])/(mapValues[n+1]-mapValues[n]));
            c = new java.awt.Color(
                    (int)((1-f)*map[0][n]+f*map[0][n+1]),
                    (int)((1-f)*map[1][n]+f*map[1][n+1]),
                    (int)((1-f)*map[2][n]+f*map[2][n+1]) );
        }
        return c;
    }
    
    /**
     * Returns an interpolated color from the default color map. The default 
     * color map is: dark red, red, yellow, green, dark green.
     * @param mapValues 5 ordered values of the colors in the map.
     * @param value Value in the range <tt>mapValues</tt>.
     * @return Interpolated color from the default map.
     */
    public static java.awt.Color fromDefaultMap(double[] mapValues, double value) {
        return fromMap(defaultMap, mapValues, value);
    }
    
    /**
     * Returns 1 of 18 distinct colors depending on the value. For values above 
     * 18, the colors are repeated.
     * @param n Value for the color.
     * @return Color of the value.
     */
    public static java.awt.Color nToColor(int n) {
        n--;
        int m = n - (int) Math.floor(n/18);
        return new java.awt.Color((float)defaultColors[m][0], 
                (float)defaultColors[m][1], (float)defaultColors[m][2]);
    }
    
    /**
     * Method that fills the supplied panel with a legend displaying labels for
     * upto 6 different colors.
     * @param panel Panel to show the legend.
     * @param colors Colors for in the legend.
     * @param labels Labels for the colors.
     */
    public static void defaultLegend(javax.swing.JPanel panel, java.awt.Color[] colors, String[] labels) {
        panel.setLayout(null);
        // absolute location info
        int h = (int) panel.getMaximumSize().getHeight();
        int w = (int) panel.getMaximumSize().getWidth();
        int b = 1;
        int width = (w-4*b)/3;
        int height = (h-3*b)/2;
        for (int k=0; k<colors.length; k++) {
            // indeces
            int j = (int) Math.floor(k/2)+1;
            int i = k-(j-1)*2+1;
            // square
            square s = new square(colors[k]);
            s.setBounds(j*b+(j-1)*width, i*b+(i-1)*height, height, height);
            panel.add(s);
            // label
            javax.swing.JLabel lab = new javax.swing.JLabel(labels[k]);
            lab.setBounds(j*b+(j-1)*width + b+height, i*b+(i-1)*height, width-b-height, height);
            lab.setFont(lab.getFont().deriveFont(9f).deriveFont(java.awt.Font.PLAIN));
            panel.add(lab);
        }
    }
    
    /** 
     * Inner class to display a colored square in a legend.
     */
    private static class square extends javax.swing.JComponent {

        /** Color of the square. */
        private java.awt.Color color;

        /**
         * Constructor that sets the color.
         * @param color Color of the square.
         */
        public square(java.awt.Color color) {
            this.color = color;
        }

        /**
         * Paints the square.
         * @param g Graphics to paint with.
         */
        @Override
        public void paint(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
            g2.setColor(color);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }
    
    /**
     * Returns the width of the area inside possible scrollbars.
     * @return Width of the area inside possible scrollbars.
     */
    public int getNetWidth() {
        int w = super.getWidth();
        if (vSlider.isVisible()) {
            w = w - SLIDERWIDTH;
        }
        return w;
    }
    
    /**
     * Returns the height of the area inside possible scrollbars.
     * @return Height of the area inside possible scrollbars.
     */
    public int getNetHeight() {
        int h = super.getHeight();
        if (hSlider.isVisible()) {
            h = h - SLIDERWIDTH;
        }
        return h;
    }
    
    /**
     * Validates the zoom bounds, keeping the centre point at the centre.
     */
    private void validateZoom() {
        validateZoom(new java.awt.Point(getNetWidth()/2, getNetHeight()/2), POLICY_POINT);
    }
    
    private void validateZoom(int sliderPolicy) {
        validateZoom(new java.awt.Point(getNetWidth()/2, getNetHeight()/2), sliderPolicy);
    }
    
    private void validateZoom(java.awt.Point point) {
        validateZoom(point, POLICY_POINT);
    }
    
    /**
     * Validates the zoom bounds and slider values following one of the slider
     * policies. For <tt>POLICY_KEEP</tt> the slider value is not changed which
     * is appropiate for if the canvas resized. This keeps the left and top of 
     * the network fixed, unless the value is outside the new valid value range.
     * For <tt>POLICY_CENTER</tt> the network is centered in the canvas which is 
     * appropiate for resetting. Finally for <tt>POLICY_POINT</tt> the location
     * at the mouse position will remain at that screen point which is 
     * appropiate for zooming.
     */
    private void validateZoom(java.awt.Point point, int sliderPolicy) {
        
        if (isValidatingZoom) {
            // This method changed a slider property which causes another call 
            // to this method, which should be ignored.
            return;
        }
        
        isValidatingZoom = true;
        
        // amount to the top/left of the point
        double left = (point.getX() + hSlider.getValue()) / canvasW;
        double top = (point.getY() + vSlider.getValue()) / canvasH;
        
        // get pixel size of network
        int w = getNetWidth();
        int h = getNetHeight();
        
        setPanelBounds(w, h);
        
        int valH = 0;
        if (sliderPolicy==POLICY_KEEP) {
            // temporary
            valH = (int) ((centerPoint.x-minGlobalX)*canvasW/(maxGlobalX-minGlobalX) - getNetWidth()/2);
        } else if (sliderPolicy==POLICY_CENTER) {
            // center the network
            valH = (int) (-canvasX/2);
        } else if (sliderPolicy==POLICY_POINT) {
            // keep the global point under the mouse at the same location on screen
            valH = (int) (left*canvasW-point.getX());
        } else if (sliderPolicy==POLICY_CENTER_AT) {
            // center at the given location
            valH = (int) (left*canvasW-(double)w/2);
        }
        int valV = 0;
        if (sliderPolicy==POLICY_KEEP) {
            // temporary
            valV = (int) ((centerPoint.y-minGlobalY)*canvasH/(maxGlobalY-minGlobalY) - getNetHeight()/2);
        } else if (sliderPolicy==POLICY_CENTER) {
            // center the network
            valV = (int) (-canvasY/2);
        } else if (sliderPolicy==POLICY_POINT) {
            // keep the global point under the mouse at the same location on screen
            valV = (int) (top*canvasH-point.getY());
        } else if (sliderPolicy==POLICY_CENTER_AT) {
            // center at the given location
            valV = (int) (top*canvasH-h/2);
        }
        
        int min = 0;
        int max = 0;
        hSlider.setBounds(0, h, w, SLIDERWIDTH);
        min = (int)(-w/2);
        hSlider.setMinimum(min);
        max = (int)(canvasW+w/2);
        hSlider.setMaximum(max);
        max = max-w;
        valH = valH>max ? max : valH;
        valH = valH<min ? min : valH;
        hSlider.setValue(valH);
        hSlider.setVisibleAmount(w);
        hSlider.setBlockIncrement(w);

        vSlider.setBounds(w, 0, SLIDERWIDTH, h);
        min = (int)(-h/2);
        vSlider.setMinimum(min);
        max = (int)(canvasH+h/2);
        vSlider.setMaximum(max);
        max = max-h;
        valV = valV>max ? max : valV;
        valV = valV<min ? min : valV;
        vSlider.setValue(valV);
        vSlider.setVisibleAmount(h);
        vSlider.setBlockIncrement(h);

        corner.setBounds(w, h, SLIDERWIDTH, SLIDERWIDTH);
        
        // set the layers at the full size
        for (int i=0; i<layers.size(); i++) {
            layers.get(i).setBounds(0, 0, w, h);
        }
        
        // store center
        centerPoint = getGlobalPoint(w/2, h/2);
        
        // repaint
        repaint();
        
        isValidatingZoom = false;
        
    }
    
    /**
     * Calculates the pixel width and height of the entire network and the 
     * amount that cannot be shown given the provided size.
     * @param w Net width of canvas [px].
     * @param h Net height of canvas [px].
     */
    private void setPanelBounds(int w, int h) {
        // get size at zoom level = 1
        double mppH = (maxGlobalX-minGlobalX) / (w-2*EDGE);
        double mppV = (maxGlobalY-minGlobalY) / (h-2*EDGE);
        double mpp = mppH > mppV ? mppH : mppV;
        canvasW = hZoomFactor*(maxGlobalX-minGlobalX)/mpp;
        canvasH = vZoomFactor*(maxGlobalY-minGlobalY)/mpp;
        canvasX = w-canvasW;
        canvasY = h-canvasH;
        java.text.DecimalFormat df = new java.text.DecimalFormat("0");
        gui.sizeLabel.setText("width="+df.format(mpp*w/hZoomFactor)+"m, height="
                +df.format(mpp*h/vZoomFactor)+"m");
    }
    
    /**
     * Translates a global position to a position within the canvas. This 
     * accounts for the zoom factors.
     * @param x Global x coordinate.
     * @param y Global y coordinate.
     * @return Point within the canvas.
     */
    public java.awt.Point getPoint(double x, double y) {
        double x2 = -hSlider.getValue() + canvasW * (x-minGlobalX)/(maxGlobalX-minGlobalX);
        double y2 = -vSlider.getValue() + canvasH * (y-minGlobalY)/(maxGlobalY-minGlobalY);
        java.awt.Point p = new java.awt.Point();
        p.setLocation(x2, y2);
        return p;
    }
    
    /** 
     * Translates a position within the canvas to a global position.
     * @param x Pane x coordinate.
     * @param y Pane y coordinate.
     * @return Point in global coordinates.
     */
    private java.awt.Point.Double getGlobalPoint(int x, int y) {
        double x2 = minGlobalX + (x+hSlider.getValue())*(maxGlobalX-minGlobalX)/canvasW;
        double y2 = minGlobalY + (y+vSlider.getValue())*(maxGlobalY-minGlobalY)/canvasH;
        return new java.awt.Point.Double(x2, y2);
    }
    
    /**
     * Zooms in both horizontal and vertical direction with a certain zoom factor.
     * @param factor Zoom factor.
     */
    protected void zoom(double factor, java.awt.Point point) {
        hZoomFactor = hZoomFactor*factor;
        vZoomFactor = vZoomFactor*factor;
        validateZoom(point);
    }
    
    /**
     * Zooms in horizontal direction with a certain zoom factor.
     * @param factor Zoom factor.
     */
    protected void zoomHorizontal(double factor, java.awt.Point point) {
        hZoomFactor = hZoomFactor*factor;
        validateZoom(point);
    }
    
    /**
     * Zooms in vertical direction with a certain zoom factor.
     * @param factor Zoom factor.
     */
    protected void zoomVertical(double factor, java.awt.Point point) {
        vZoomFactor = vZoomFactor*factor;
        validateZoom(point);
    }
    
    /**
     * Resets the zoom level to encapsulate everything.
     */
    protected void resetZoom() {
        hZoomFactor = 1;
        vZoomFactor = 1;
        validateZoom(POLICY_CENTER);
    }
    
    /**
     * Adds a grahpic to be drawn as a backdrop.
     * @param g Graphics object.
     */
    protected void addBackdrop(graphic g) {
        layers.get(LAYER_BACKDROP).graphics.add(g);
    }
    
    /**
     * Adds a grahpic to be drawn as an overlay.
     * @param g Graphics object.
     */
    protected void addOverlay(graphic g) {
        layers.get(LAYER_OVERLAY).graphics.add(g);
    }
    
    /**
     * Initializes the canvas which adds graphics for lanes and detectors aswell
     * as determining the global network bounding box.
     */
    protected synchronized void init() {
        // add graphics for lane objects
        for (int i=0; i<model.network.length; i++) {
            // first add taper (so the others are drawn over it)
            if (model.network[i].taper == model.network[i]) {
                layers.get(LAYER_LANE).graphics.add(new laneGraphic(model.network[i]));
                layers.get(LAYER_LABEL).graphics.add(new laneLabel(model.network[i]));
                // add graphics for rsu objects
                for (int j=0; j<model.network[i].RSUcount(); j++) {
                    jRSU rsu = model.network[i].getRSU(j);
                    if (rsu instanceof jDetector) {
                        layers.get(LAYER_RSU).graphics.add(new detectorGraphic((jDetector) rsu));
                        layers.get(LAYER_LABEL).graphics.add(new detectorLabel((jDetector) rsu));
                    } else if (rsu instanceof jTrafficLight) {
                        layers.get(LAYER_RSU).graphics.add(new trafficLightGraphic((jTrafficLight) rsu));
                    } else if (rsu instanceof conflictRSU) {
                        layers.get(LAYER_RSU).graphics.add(new conflictGraphic((conflictRSU) rsu));
                    }
                }
                // add graphics for generator
                if (model.network[i].generator != null) {
                    layers.get(LAYER_LABEL).graphics.add(new generatorQueue(model.network[i]));
                }
            }
        }
        // now add non-tapers
        for (int i=0; i<model.network.length; i++) {
            if (model.network[i].taper != model.network[i]) {
                layers.get(LAYER_LANE).graphics.add(new laneGraphic(model.network[i]));
                layers.get(LAYER_LABEL).graphics.add(new laneLabel(model.network[i]));
                // add graphics for detector objects
                for (int j=0; j<model.network[i].RSUcount(); j++) {
                    jRSU rsu = model.network[i].getRSU(j);
                    if (rsu instanceof jDetector) {
                        layers.get(LAYER_RSU).graphics.add(new detectorGraphic((jDetector) rsu));
                        layers.get(LAYER_LABEL).graphics.add(new detectorLabel((jDetector) rsu));
                    } else if (rsu instanceof jTrafficLight) {
                        layers.get(LAYER_RSU).graphics.add(new trafficLightGraphic((jTrafficLight) rsu));
                    } else if (rsu instanceof conflictRSU) {
                        layers.get(LAYER_RSU).graphics.add(new conflictGraphic((conflictRSU) rsu));
                    }
                }
                // add graphics for generator
                if (model.network[i].generator != null) {
                    layers.get(LAYER_LABEL).graphics.add(new generatorQueue(model.network[i]));
                }
            }
        }
        // derive global ranges of network
        minGlobalX = Double.POSITIVE_INFINITY;
        maxGlobalX = Double.NEGATIVE_INFINITY;
        minGlobalY = Double.POSITIVE_INFINITY;
        maxGlobalY = Double.NEGATIVE_INFINITY;
        java.awt.geom.Rectangle2D.Double bounds;
        for (int i=0; i<layers.get(LAYER_LANE).graphics.size(); i++) {
            bounds = layers.get(LAYER_LANE).graphics.get(i).getGlobalBounds();
            if (bounds!=null) {
                minGlobalX = bounds.getMinX() < minGlobalX ? bounds.getMinX() : minGlobalX;
                maxGlobalX = bounds.getMaxX() > maxGlobalX ? bounds.getMaxX() : maxGlobalX;
                minGlobalY = bounds.getMinY() < minGlobalY ? bounds.getMinY() : minGlobalY;
                maxGlobalY = bounds.getMaxY() > maxGlobalY ? bounds.getMaxY() : maxGlobalY;
            }
        }
        for (int i=0; i<layers.get(LAYER_BACKDROP).graphics.size(); i++) {
            bounds = layers.get(LAYER_BACKDROP).graphics.get(i).getGlobalBounds();
            if (bounds!=null) {
                minGlobalX = bounds.getMinX() < minGlobalX ? bounds.getMinX() : minGlobalX;
                maxGlobalX = bounds.getMaxX() > maxGlobalX ? bounds.getMaxX() : maxGlobalX;
                minGlobalY = bounds.getMinY() < minGlobalY ? bounds.getMinY() : minGlobalY;
                maxGlobalY = bounds.getMaxY() > maxGlobalY ? bounds.getMaxY() : maxGlobalY;
            }
        }
        for (int i=0; i<layers.get(LAYER_OVERLAY).graphics.size(); i++) {
            bounds = layers.get(LAYER_OVERLAY).graphics.get(i).getGlobalBounds();
            if (bounds!=null) {
                minGlobalX = bounds.getMinX() < minGlobalX ? bounds.getMinX() : minGlobalX;
                maxGlobalX = bounds.getMaxX() > maxGlobalX ? bounds.getMaxX() : maxGlobalX;
                minGlobalY = bounds.getMinY() < minGlobalY ? bounds.getMinY() : minGlobalY;
                maxGlobalY = bounds.getMaxY() > maxGlobalY ? bounds.getMaxY() : maxGlobalY;
            }
        }
    }
    
    /**
     * Updates the canvas to show the current status of the model.
     */
    protected synchronized void update() {
        // copy vehicle array
        java.util.ArrayList<jVehicle> vehs = model.getVehicles();
        int n = vehs.size();
        // loop existing graphics
        java.util.Iterator<graphic> iter = layers.get(LAYER_VEHICLE).graphics.iterator();
        while (iter.hasNext()) {
            vehicleGraphic g = (vehicleGraphic) iter.next();
            if (!g.exists(model)) {
                iter.remove();
            } else {
                vehs.remove(g.getVehicle());
            }
        }
        // add graphics objects for all vehicles still in vehs (new vehicles)
        for (int i=0; i<vehs.size(); i++) {
            layers.get(LAYER_VEHICLE).graphics.add(new vehicleGraphic(vehs.get(i)));
        }
        // time
        java.util.Date date = model.currentTime();
        if (date==null) {
            java.text.DecimalFormatSymbols dfs = new java.text.DecimalFormatSymbols();
            dfs.setDecimalSeparator('.');
            java.text.DecimalFormat format = new java.text.DecimalFormat("0.0", dfs);
            gui.timeLabel.setText("t="+format.format(model.t())+"s");
        } else {
            java.text.DateFormat df = new java.text.SimpleDateFormat("HH:mm:ss");
            gui.timeLabel.setText("t="+df.format(date));
        }
        // number of vehicles
        gui.nVehicles.setText(n+" vehs");
        // center on vehicle 
        if (!vehs.contains(trackVehicle)) {
            trackVehicle = null;
        }
        if (trackVehicle!=null) {
            java.awt.Point point = getPoint(trackVehicle.globalX, trackVehicle.globalY);
            validateZoom(point, POLICY_CENTER_AT);
        }
        repaint();
    }
    
    /**
     * Creates an image of the canvas.
     * @return Image of the canvas.
     */
    protected synchronized java.awt.image.BufferedImage createImage() {
        // assure a size as a multiple of 4
        int w = (int) Math.ceil((double)getNetWidth()/4)*4;
        int h = (int) Math.ceil((double)getNetHeight()/4)*4;
        java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(
                w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics g = bi.createGraphics();
        paintComponent(g);
        for (int i=layers.size()-1; i>=0; i--) {
            layers.get(i).paintComponent(g);
        }
        return bi; 
    }
    
    /**
     * Class which forms a layer of graphics objects. These are painted 
     * automatichally as the layer is a child of the network canvas.
     */
    private class layer extends javax.swing.JPanel {
        
        /** Array of graphics of this layer. */
        private java.util.ArrayList<graphic> graphics = new java.util.ArrayList<graphic>();
        
        /**
         * Default constructor which sets the layer transparent.
         */
        public layer() {
            setOpaque(false);
        }
        
        /**
         * Paints the graphics of this layer.
         * @param g Graphics object of the layer.
         */
        @Override
        public void paintComponent(java.awt.Graphics g) {
            // paint graphics of this layer
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            try {
                for (int i=0; i<graphics.size(); i++) {
                    if (graphics.get(i).exists(model)) {
                        graphics.get(i).paint(g.create(), networkCanvas.this);
                    }
                }
            } catch (NullPointerException npe) {
                // may occur due to lack of synchronicity, which we allow
            } catch (IndexOutOfBoundsException ioobe) {
                // may occur due to lack of synchronicity, which we allow
            }
        }
    }
    
    /**
     * Graphic class for lanes.
     */
    private class laneGraphic implements graphic {
        
        /** The concerned lane object. */
        private jLane lane;
        
        /** Nested poly object that houses shape information. */
        private poly pol;
        
        /** Whether lines should be drawn. */
        private final boolean drawLines;
        
        /**
         * Constructor based on a lane object.
         * @param lane The lane object.
         */
        public laneGraphic(jLane lane) {
            this.lane = lane;
            pol = new poly(lane);
            if (lane.taper==lane) {
                drawLines = false;
            } else {
                drawLines = true;
            }
        }
        
        /**
         * Whether the lane exists.
         * @param model Model object.
         * @return Always <tt>true</tt>.
         */
        public boolean exists(jModel model) {
            return true;
        }
        
        /**
         * Returns the bounding box of the lane coordinate arrays with an 
         * additional border of 1.75m (half a default lane width).
         * @return Bounding box of the lane.
         */
        public java.awt.Rectangle.Double getGlobalBounds() {
            double minGlobalX = Double.POSITIVE_INFINITY;
            double maxGlobalX = Double.NEGATIVE_INFINITY;
            double minGlobalY = Double.POSITIVE_INFINITY;
            double maxGlobalY = Double.NEGATIVE_INFINITY;
            for (int j=0; j<lane.x.length; j++) {
                minGlobalX = lane.x[j] < minGlobalX ? lane.x[j] :  minGlobalX;
                maxGlobalX = lane.x[j] > maxGlobalX ? lane.x[j] :  maxGlobalX;
                minGlobalY = lane.y[j] < minGlobalY ? lane.y[j] :  minGlobalY;
                maxGlobalY = lane.y[j] > maxGlobalY ? lane.y[j] :  maxGlobalY;
            }
            return new java.awt.Rectangle.Double(minGlobalX-1.75, minGlobalY-1.75, 
                    (maxGlobalX-minGlobalX)+3.5, (maxGlobalY-minGlobalY)+3.5);
        }
        
        /** 
         * Paints the lane on the canvas.
         */
        public void paint(java.awt.Graphics g, networkCanvas canvas) {
            if (popupItemChecked("Show lanes")) {
                // area
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                g2.setColor(new java.awt.Color(128, 128, 128));
                g2.fillPolygon(pol.area());

                // lines
                polyLine line;
                if (drawLines) {
                    // initialize graphics for lines
                    g2.setColor(new java.awt.Color(255, 255, 255));
                    setStroke(g2, 1, 1f, 0f);

                    // up line
                    if (lane.up==null && lane.generator==null && !lane.isMerge()) {
                        line = pol.upEdge();
                        g2.drawPolyline(line.x, line.y, line.n);
                    }

                    // down line
                    if (lane.down==null && lane.destination==0 && !lane.isSplit()) {
                        line = pol.downEdge();
                        g2.drawPolyline(line.x, line.y, line.n);
                    }

                    // left line
                    if (lane.left==null || (!lane.goLeft && !lane.left.goRight)) {
                        // continuous normal
                        line = pol.leftEdge();
                        setStroke(g2, 1, 1f, line.x[0]);
                    } else if (lane.goLeft && lane.left.goRight) {
                        // dashed normal
                        line = pol.leftEdge();
                        setStroke(g2, 2, 1f, line.x[0]);
                    } else if (!lane.goLeft && lane.left.goRight) {
                        // continuous near
                        line = pol.leftNearEdge();
                        setStroke(g2, 1, 1f, line.x[0]);
                    } else {
                        // dashed near
                        line = pol.leftNearEdge();
                        setStroke(g2, 2, 1f, line.x[0]);
                    }
                    g2.drawPolyline(line.x, line.y, line.n);

                    // right line
                    boolean drawRight = false;
                    if (lane.right==null || (!lane.goRight && !lane.right.goLeft)) {
                        // continuous normal
                        // also right if both not allowed, may be non-adjacent but 
                        // linked lanes for synchronization
                        line = pol.rightEdge();
                        setStroke(g2, 1, 1f, line.x[0]);
                        drawRight = true;
                    } else if (lane.right.goLeft && !lane.goRight) {
                        // continuous near
                        line = pol.rightNearEdge();
                        setStroke(g2, 1, 1f, line.x[0]);
                        drawRight = true;
                    } else if (!lane.right.goLeft && lane.goRight) {
                        // dashed near
                        line = pol.rightNearEdge();
                        setStroke(g2, 2, 1f, line.x[0]);
                        drawRight = true;
                    }
                    if (drawRight) {
                        g2.drawPolyline(line.x, line.y, line.n);
                    }
                }

                // destination
                if (showDestinations && lane.destination!=0) {
                    g2.setColor(nToColor(lane.destination));
                    setStroke(g2, 1, 3f, 0f);
                    line = pol.downEdge();
                    g2.drawPolyline(line.x, line.y, line.n);
                }
                setStroke(g2, 3, 1f, 0f);
            }
        }
        
        /**
         * Sets a stroke for the Graphics object.
         * @param g2 Graphics object.
         * @param type 1=continuous, 2=dashed, 3=default <tt>BasicStroke</tt>
         * @param width Width of the line in pixels (type 1 or 2 only)
         * @param x First x-coordinate to determine the phase for horizontal lines
         */
        private void setStroke(java.awt.Graphics2D g2, int type, float width, float x) {
            if (type==3) {
                // 3 = basic stroke
                g2.setStroke(new java.awt.BasicStroke());
            } else {
                float[] dash = null; // 1 = continuous
                if (type==2) {
                    // 2 = dashed
                    dash = new float[2];
                    dash[0] = 3f;
                    dash[1] = 9f;
                }
                float phase = (float) (x - Math.floor((double)x/12)*12);
                g2.setStroke(new java.awt.BasicStroke(width, java.awt.BasicStroke.CAP_BUTT, 
                        java.awt.BasicStroke.JOIN_MITER, 1.0f, dash, phase));
            }
        }
        
        /**
         * Nested class that derives and houses shape information.
         */
        private class poly {
            
            /** x coordinates of entire area. */
            private double[] xArea;
            
            /** y coordinates of entire area. */
            private double[] yArea;
            
            /** x coordinates of left side. */
            private double[] xLeftEdge;
            
            /** y coordinates of left side. */
            private double[] yLeftEdge;
            
            /** x coordinates of right side. */
            private double[] xRightEdge;
            
            /** y coordinates of right side. */
            private double[] yRightEdge;
            
            /** x coordinates of left side in case of dual line markings. */
            private double[] xNearLeftEdge;
            
            /** y coordinates of left side in case of dual line markings. */
            private double[] yNearLeftEdge;
            
            /** x coordinates of right side in case of dual line markings. */
            private double[] xNearRightEdge;
            
            /** y coordinates of right side in case of dual line markings. */
            private double[] yNearRightEdge;
            
            /** Number of points in lane position vector. */
            private int n;
            
            /**
             * Constructor that derives all needed information.
             * @param lane Lane object.
             */
            private poly(jLane lane) {
                // check whether the lane is a taper
                boolean mergeTaper = false;
                boolean divergeTaper = false;
                boolean addRight = false;
                boolean addLeft = false;
                boolean subRight = false;
                boolean subLeft = false;
                if (lane.taper==lane && lane.down==null) {
                    mergeTaper = true;
                } else if (lane.taper==lane && lane.up==null) {
                    divergeTaper = true;
                }
                if (lane.down==null && lane.right==null && lane.left!=null) {
                    subRight = true;
                } else if (lane.down==null && lane.right!=null && lane.left==null) {
                    subLeft = true;
                } 
                if (lane.up==null && lane.right==null && lane.left!=null) {
                    addRight = true;
                } else if (lane.up==null && lane.right!=null && lane.left==null) {
                    addLeft = true;
                }
                
                // set width numbers
                double w = 1.75; // half lane width
                double near = 0.375; // half distance between dual lane marking
                double f = 1; // factor that reduces width along tapers
                
                // first point
                n = lane.x.length;
                xLeftEdge = new double[n];
                yLeftEdge = new double[n];
                xRightEdge = new double[n];
                yRightEdge = new double[n];
                xNearLeftEdge = new double[n];
                yNearLeftEdge = new double[n];
                xNearRightEdge = new double[n];
                yNearRightEdge = new double[n];
                java.awt.geom.Point2D.Double[] start = new java.awt.geom.Point2D.Double[0];
                java.awt.geom.Point2D.Double[] startNear = new java.awt.geom.Point2D.Double[0];
                if (divergeTaper) {
                    f = -1;  
                }
                if (lane.up!=null) {
                    int nUp = lane.up.x.length;
                    start = intersect(w*f, w,
                            lane.up.x[nUp-2], lane.x[0], lane.x[1],
                            lane.up.y[nUp-2], lane.y[0], lane.y[1]);
                    startNear = intersect(w*f-near, w-near,
                            lane.up.x[nUp-2], lane.x[0], lane.x[1],
                            lane.up.y[nUp-2], lane.y[0], lane.y[1]);
                } else {
                    start = intersect(w*f, w,
                            lane.x[0] - (lane.x[1]-lane.x[0]), lane.x[0], lane.x[1],
                            lane.y[0] - (lane.y[1]-lane.y[0]), lane.y[0], lane.y[1]);
                    startNear = intersect(w*f-near, w-near,
                            lane.x[0] - (lane.x[1]-lane.x[0]), lane.x[0], lane.x[1],
                            lane.y[0] - (lane.y[1]-lane.y[0]), lane.y[0], lane.y[1]);
                }
                xLeftEdge[0] = start[0].x;
                yLeftEdge[0] = start[0].y;
                xRightEdge[0] = start[1].x;
                yRightEdge[0] = start[1].y;
                xNearLeftEdge[0] = startNear[0].x;
                yNearLeftEdge[0] = startNear[0].y;
                xNearRightEdge[0] = startNear[1].x;
                yNearRightEdge[0] = startNear[1].y;
                
                // middle points
                java.awt.geom.Point2D.Double[] point = new java.awt.geom.Point2D.Double[0];
                java.awt.geom.Point2D.Double[] pointNear = new java.awt.geom.Point2D.Double[0];
                f = 1; // default for no taper
                double dx = lane.x[1]-lane.x[0];
                double dy = lane.y[1]-lane.y[0];
                double len = Math.sqrt(dx*dx + dy*dy); // cumulative length for tapers
                for (int i=1; i<n-1; i++) {
                    if (mergeTaper) {
                        // reducing width
                        f = 1 - 2*len/lane.l;
                        dx = lane.x[i+1]-lane.x[i];
                        dy = lane.y[i+1]-lane.y[i];
                        len = len + Math.sqrt(dx*dx + dy*dy);
                    } else if (divergeTaper) {
                        // increasing width
                        f = -1 + 2*len/lane.l;
                        dx = lane.x[i+1]-lane.x[i];
                        dy = lane.y[i+1]-lane.y[i];
                        len = len + Math.sqrt(dx*dx + dy*dy);
                    }
                    point = intersect(w*f, w,
                            lane.x[i-1], lane.x[i], lane.x[i+1],
                            lane.y[i-1], lane.y[i], lane.y[i+1]);
                    pointNear = intersect(w*f-near, w-near,
                            lane.x[i-1], lane.x[i], lane.x[i+1],
                            lane.y[i-1], lane.y[i], lane.y[i+1]);
                    xLeftEdge[i] = point[0].x;
                    yLeftEdge[i] = point[0].y;
                    xRightEdge[i] = point[1].x;
                    yRightEdge[i] = point[1].y;
                    xNearLeftEdge[i] = pointNear[0].x;
                    yNearLeftEdge[i] = pointNear[0].y;
                    xNearRightEdge[i] = pointNear[1].x;
                    yNearRightEdge[i] = pointNear[1].y;
                }
                
                // last point
                java.awt.geom.Point2D.Double[] end = new java.awt.geom.Point2D.Double[0];
                java.awt.geom.Point2D.Double[] endNear = new java.awt.geom.Point2D.Double[0];
                if (mergeTaper) {
                    f = -1;
                } else {
                    f = 1;
                }
                if (lane.down!=null) {
                    end = intersect(w*f, w,
                            lane.x[n-2], lane.x[n-1], lane.down.x[1],
                            lane.y[n-2], lane.y[n-1], lane.down.y[1]);
                    endNear = intersect(w*f-near, w-near,
                            lane.x[n-2], lane.x[n-1], lane.down.x[1],
                            lane.y[n-2], lane.y[n-1], lane.down.y[1]);
                } else {
                    end = intersect(w*f, w,
                            lane.x[n-2], lane.x[n-1], lane.x[n-1]+(lane.x[n-1]-lane.x[n-2]),
                            lane.y[n-2], lane.y[n-1], lane.y[n-1]+(lane.y[n-1]-lane.y[n-2]));
                    endNear = intersect(w*f-near, w-near,
                            lane.x[n-2], lane.x[n-1], lane.x[n-1]+(lane.x[n-1]-lane.x[n-2]),
                            lane.y[n-2], lane.y[n-1], lane.y[n-1]+(lane.y[n-1]-lane.y[n-2]));
                }
                xLeftEdge[n-1] = end[0].x;
                yLeftEdge[n-1] = end[0].y;
                xRightEdge[n-1] = end[1].x;
                yRightEdge[n-1] = end[1].y;
                xNearLeftEdge[n-1] = endNear[0].x;
                yNearLeftEdge[n-1] = endNear[0].y;
                xNearRightEdge[n-1] = endNear[1].x;
                yNearRightEdge[n-1] = endNear[1].y;
                
                // combine area from edges
                xArea = new double[n*2+1];
                yArea = new double[n*2+1];
                for (int i=0; i<n; i++) {
                    xArea[i] = xRightEdge[i];
                    yArea[i] = yRightEdge[i];
                }
                for (int i=0; i<n; i++) {
                    xArea[i+n] = xLeftEdge[n-i-1];
                    yArea[i+n] = yLeftEdge[n-i-1];
                }
                xArea[n*2] = xRightEdge[0];
                yArea[n*2] = yRightEdge[0];
            }
            
            /**
             * Method that finds the intersection of the left and right side of 
             * two lane sections.
             * @param wLeft Width towards the left [m].
             * @param wRight Width towards the right [m].
             * @param x1 1st x coordinate of upstream section [m].
             * @param x2 x coordinate of common point [m].
             * @param x3 2nd coordinate of downstream section [m].
             * @param y1 1st y coordinate of upstream section [m].
             * @param y2 y coordinate of common point [m].
             * @param y3 2nd y coordinate of downstream section [m].
             * @return Two points at the left and right side intersections.
             */
            private java.awt.geom.Point2D.Double[] intersect(
                    double wLeft, double wRight,
                    double x1, double x2, double x3,  
                    double y1, double y2, double y3) {
                
                // get headings
                double dx1 = x2-x1;
                double dy1 = y2-y1;
                double dx2 = x3-x2;
                double dy2 = y3-y2;
                
                // normalization factors
                double f1 = 1/Math.sqrt(dx1*dx1 + dy1*dy1);
                double f2 = 1/Math.sqrt(dx2*dx2 + dy2*dy2);
                
                // get coordinates of left adjacent lanes
                double xLeft1  = x1+dy1*f1*wLeft;
                double xLeft2a = x2+dy1*f1*wLeft;
                double xLeft2b = x2+dy2*f2*wLeft;
                double xLeft3  = x3+dy2*f2*wLeft;
                double yLeft1  = y1-dx1*f1*wLeft;
                double yLeft2a = y2-dx1*f1*wLeft;
                double yLeft2b = y2-dx2*f2*wLeft;
                double yLeft3  = y3-dx2*f2*wLeft;
                
                // get coordinates of right adjacent lanes
                double xRight1  = x1-dy1*f1*wRight;
                double xRight2a = x2-dy1*f1*wRight;
                double xRight2b = x2-dy2*f2*wRight;
                double xRight3  = x3-dy2*f2*wRight;
                double yRight1  = y1+dx1*f1*wRight;
                double yRight2a = y2+dx1*f1*wRight;
                double yRight2b = y2+dx2*f2*wRight;
                double yRight3  = y3+dx2*f2*wRight;
                
                // intersect left lines
                double a1 = (yLeft2a-yLeft1)/(xLeft2a-xLeft1);
                double b1 = yLeft1 - xLeft1*a1;
                double a2 = (yLeft3-yLeft2b)/(xLeft3-xLeft2b);
                double b2 = yLeft2b - xLeft2b*a2;
                double xLeft;
                double yLeft;
                if (Math.abs(a1-a2)<0.001 || (Double.isInfinite(a1) && Double.isInfinite(a2))) {
                    xLeft = xLeft2a;
                    yLeft = yLeft2a;
                } else if (Double.isInfinite(a1)) {
                    xLeft = xLeft1;
                    yLeft = a2*xLeft+b2;
                } else if (Double.isInfinite(a2)) {
                    xLeft = xLeft3;
                    yLeft = a1*xLeft+b1;
                } else {
                    xLeft = -(b1-b2)/(a1-a2);
                    yLeft = a1*xLeft+b1;
                }
                
                // intersect right lines
                a1 = (yRight2a-yRight1)/(xRight2a-xRight1);
                b1 = yRight1 - xRight1*a1;
                a2 = (yRight3-yRight2b)/(xRight3-xRight2b);
                b2 = yRight2b - xRight2b*a2;
                double xRight;
                double yRight;
                if (Math.abs(a1-a2)<0.001 || (Double.isInfinite(a1) && Double.isInfinite(a2))) {
                    xRight = xRight2a;
                    yRight = yRight2a;
                } else if (Double.isInfinite(a1)) {
                    xRight = xRight1;
                    yRight = a2*xRight+b2;
                } else if (Double.isInfinite(a2)) {
                    xRight = xRight3;
                    yRight = a1*xRight+b1;
                } else {
                    xRight = -(b1-b2)/(a1-a2);
                    yRight = a1*xRight+b1;
                }
                
                // gather output
                java.awt.geom.Point2D.Double[] out = new java.awt.geom.Point2D.Double[2];
                out[0] = new java.awt.geom.Point2D.Double(xLeft, yLeft);
                out[1] = new java.awt.geom.Point2D.Double(xRight, yRight);
                return out;
            }
            
            /**
             * Returns a polygon of the lane area.
             * @return Polygon of lane area.
             */
            private java.awt.Polygon area() {
                int[] x = new int[2*n+1];
                int[] y = new int[2*n+1];
                java.awt.Point point = new java.awt.Point();
                for (int i=0; i<xArea.length; i++) {
                    point = getPoint(xArea[i], yArea[i]);
                    x[i] = point.x;
                    y[i] = point.y;
                }
                return new java.awt.Polygon(x, y, n*2);
            }
            
            /**
             * Returns a <tt>polyLine</tt> of the left side.
             * @return Polyline of the left side.
             */
            private polyLine leftEdge() {
                int[] x = new int[n];
                int[] y = new int[n];
                java.awt.Point point = new java.awt.Point();
                for (int i=0; i<xLeftEdge.length; i++) {
                    point = getPoint(xLeftEdge[i], yLeftEdge[i]);
                    x[i] = point.x;
                    y[i] = point.y;
                }
                return new polyLine(x, y, n);
            }
            
            /**
             * Returns a <tt>polyLine</tt> of the right side.
             * @return Polyline of the right side.
             */
            private polyLine rightEdge() {
                int[] x = new int[n];
                int[] y = new int[n];
                java.awt.Point point = new java.awt.Point();
                for (int i=0; i<xRightEdge.length; i++) {
                    point = getPoint(xRightEdge[i], yRightEdge[i]);
                    x[i] = point.x;
                    y[i] = point.y;
                }
                return new polyLine(x, y, n);
            }
            
            /**
             * Returns a <tt>polyLine</tt> of the left side in case of dual line markings.
             * @return Polyline of the left side.
             */
            private polyLine leftNearEdge() {
                int[] x = new int[n];
                int[] y = new int[n];
                java.awt.Point point = new java.awt.Point();
                for (int i=0; i<xNearLeftEdge.length; i++) {
                    point = getPoint(xNearLeftEdge[i], yNearLeftEdge[i]);
                    x[i] = point.x;
                    y[i] = point.y;
                }
                return new polyLine(x, y, n);
            }
            
            /**
             * Returns a <tt>polyLine</tt> of the right side in case of dual line markings.
             * @return Polyline of the right side.
             */
            private polyLine rightNearEdge() {
                int[] x = new int[n];
                int[] y = new int[n];
                java.awt.Point point = new java.awt.Point();
                for (int i=0; i<xNearRightEdge.length; i++) {
                    point = getPoint(xNearRightEdge[i], yNearRightEdge[i]);
                    x[i] = point.x;
                    y[i] = point.y;
                }
                return new polyLine(x, y, n);
            }
            
            /**
             * Returns a <tt>polyLine</tt> of the upstream side.
             * @return Polyline of the upstream side.
             */
            private polyLine upEdge() {
                int[] x = new int[2];
                int[] y = new int[2];
                java.awt.Point point = new java.awt.Point();
                point = getPoint(xLeftEdge[0], yLeftEdge[0]);
                x[0] = point.x;
                y[0] = point.y;
                point = getPoint(xRightEdge[0], yRightEdge[0]);
                x[1] = point.x;
                y[1] = point.y;
                return new polyLine(x, y, 2);
            }
            
            /**
             * Returns a <tt>polyLine</tt> of the downstream side.
             * @return Polyline of the downstream side.
             */
            private polyLine downEdge() {
                int[] x = new int[2];
                int[] y = new int[2];
                java.awt.Point point = new java.awt.Point();
                point = getPoint(xLeftEdge[n-1], yLeftEdge[n-1]);
                x[0] = point.x;
                y[0] = point.y;
                point = getPoint(xRightEdge[n-1], yRightEdge[n-1]);
                x[1] = point.x;
                y[1] = point.y;
                return new polyLine(x, y, 2);
            }
        }
        
        /**
         * Nested class that defines a poly-line.
         */
        private class polyLine {
            
            /** x coordinates. */
            private int[] x;
            
            /** y coordinates. */
            private int[] y;
            
            /** Number of coordinates. */
            private int n;
            
            /**
             * Constructor for the poly line.
             * @param x x coordinates [m].
             * @param y y coordinates [m].
             * @param n Number of coordinates.
             */
            private polyLine(int[] x, int[] y, int n) {
                this.x = x;
                this.y = y;
                this.n = n;
            }
        }
    }
    
    /**
     * Graphic class for lane labels.
     */
    private class laneLabel implements graphic {
        
        /** The concerned lane object. */
        private jLane lane;
        
        /**
         * Constructor based on a lane object.
         * @param lane The lane object.
         */
        public laneLabel(jLane lane) {
            this.lane = lane;
        }
        
        /**
         * Whether the lane exists.
         * @param model Model object.
         * @return Always <tt>true</tt>.
         */
        public boolean exists(jModel model) {
            return true;
        }
        
        /**
         * Returns the bounding box.
         * @return Bounding box of the label.
         */
        public java.awt.Rectangle.Double getGlobalBounds() {
            return null;
        }
        
        /** 
         * Paints the lane id on the canvas.
         */
        public void paint(java.awt.Graphics g, networkCanvas canvas) {
            // id
            if (popupItemChecked("Show lane IDs")) {
                java.awt.Point point;
                if (lane.taper!=null && lane.taper==lane && lane.up==null) {
                    // diverge taper, move id location to right for overlap
                    point = getPoint((lane.x[0]+lane.right.x[0])/2, (lane.y[0]+lane.right.y[0])/2);
                } else {
                    point = getPoint(lane.x[0], lane.y[0]);
                }
                g.setColor(new java.awt.Color(255, 255, 255));
                g.drawString(""+lane.id(), point.x, point.y);
            }
        }
    }
    
    /**
     * Graphic class for generator queue label.
     */
    private class generatorQueue implements graphic {
        
        /** The concerned lane object. */
        private jLane lane;
        
        /**
         * Constructor based on a lane object.
         * @param lane The lane object.
         */
        public generatorQueue(jLane lane) {
            this.lane = lane;
        }
        
        /**
         * Whether the generator exists.
         * @param model Model object.
         * @return Always <tt>true</tt>.
         */
        public boolean exists(jModel model) {
            return true;
        }
        
        /**
         * Returns the bounding box.
         * @return Bounding box of the generator.
         */
        public java.awt.Rectangle.Double getGlobalBounds() {
            return null;
        }
        
        /** 
         * Paints the generator queu on the canvas.
         */
        public void paint(java.awt.Graphics g, networkCanvas canvas) {
            // id
            if (popupItemChecked("Show generator queue")) {
                java.awt.Point point = getPoint(lane.x[0], lane.y[0]);
                g.setColor(new java.awt.Color(255, 255, 255));
                g.drawString(""+lane.generator.getQueue(), point.x, point.y);
            }
        }
    }
    
    /**
     * Graphic class for vehicles.
     */
    private class vehicleGraphic implements graphic {
        
        /** The concerned vehicle object. */
        private jVehicle vehicle;
        
        /**
         * Constructor that sets the vehicle.
         * @param vehicle Concerned vehicle.
         */
        public vehicleGraphic(jVehicle vehicle) {
            this.vehicle = vehicle;
        }
        
        /**
         * Returnes the concerned vehicle.
         * @return Vehicle of this vehicle graphic.
         */
        public jVehicle getVehicle() {
            return vehicle;
        }
        
        /**
         * Checks whether the vehicle is still in the simulation.
         * @param model The model object.
         * @return Whether the vehicle is still in the simulation.
         */
        public boolean exists(jModel model) {
            return model.getVehicles().contains(vehicle);
        }
        
        /**
         * Returns the bounding box of the vehicle position.
         * @return By default <tt>null</tt>.
         */
        public java.awt.Rectangle.Double getGlobalBounds() {
            return null;
        }
        
        /**
         * Paints the vehicle on the canvas.
         * @param g Graphics to paint with.
         * @param canvas Canvas that is painted on.
         */
        public void paint(java.awt.Graphics g, networkCanvas canvas) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
            vehicleColor vehCol = (vehicleColor) gui.vehCol.getSelectedItem();
            g2.setColor(vehCol.getColor(vehicle));
            if (popupItemChecked("Vehicles as dots")) {
                java.awt.Point point = getPoint(vehicle.globalX, vehicle.globalY);
                if (vehicle.marker!=null && vehicle.marker.equals("s")) {
                    g2.fillRect(point.x-3, point.y-3, 6, 6);
                } else {
                    g2.fillOval(point.x-4, point.y-4, 7, 7);
                }
            } else {
                java.awt.Polygon pol = new java.awt.Polygon();
                double w = 2;
                java.awt.Point point = new java.awt.Point();
                point = getPoint(vehicle.globalX + vehicle.heading.y*w/2, 
                        vehicle.globalY - vehicle.heading.x*w/2);
                pol.addPoint(point.x, point.y);
                point = getPoint(vehicle.globalX - vehicle.heading.y*w/2, 
                        vehicle.globalY + vehicle.heading.x*w/2);
                pol.addPoint(point.x, point.y);
                double x2 = vehicle.globalX - vehicle.heading.x*vehicle.l;
                double y2 = vehicle.globalY - vehicle.heading.y*vehicle.l;
                point = getPoint(x2 - vehicle.heading.y*w/2, 
                        y2 + vehicle.heading.x*w/2);
                pol.addPoint(point.x, point.y);
                point = getPoint(x2 + vehicle.heading.y*w/2, 
                        y2 - vehicle.heading.x*w/2);
                pol.addPoint(point.x, point.y);
                g2.fillPolygon(pol);
            }
            
            if (popupItemChecked("Show downstream")) {
                if (vehicle.leftDown!=null) {
                    g2.setColor(new java.awt.Color(255, 0, 0));
                    java.awt.Point p1 = getPoint(vehicle.globalX, vehicle.globalY);
                    java.awt.Point.Double h = vehicle.leftDown.heading;
                    java.awt.Point p2 = getPoint(vehicle.leftDown.globalX-h.x*vehicle.leftDown.l, 
                            vehicle.leftDown.globalY-h.y*vehicle.leftDown.l);
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
                if (vehicle.down!=null) {
                    g2.setColor(new java.awt.Color(0, 255, 0));
                    java.awt.Point p1 = getPoint(vehicle.globalX, vehicle.globalY);
                    java.awt.Point.Double h = vehicle.down.heading;
                    java.awt.Point p2 = getPoint(vehicle.down.globalX-h.x*vehicle.down.l,
                            vehicle.down.globalY-h.y*vehicle.down.l);
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
                if (vehicle.rightDown!=null) {
                    g2.setColor(new java.awt.Color(0, 0, 255));
                    java.awt.Point p1 = getPoint(vehicle.globalX, vehicle.globalY);
                    java.awt.Point.Double h = vehicle.rightDown.heading;
                    java.awt.Point p2 = getPoint(vehicle.rightDown.globalX-h.x*vehicle.rightDown.l,
                            vehicle.rightDown.globalY-h.y*vehicle.rightDown.l);
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
            if (popupItemChecked("Show upstream")) {
                if (vehicle.leftUp!=null) {
                    g2.setColor(new java.awt.Color(255, 0, 255));
                    java.awt.Point p1 = getPoint(vehicle.globalX, vehicle.globalY);
                    java.awt.Point p2 = getPoint(vehicle.leftUp.globalX, vehicle.leftUp.globalY);
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
                if (vehicle.up!=null) {
                    g2.setColor(new java.awt.Color(255, 255, 0));
                    java.awt.Point p1 = getPoint(vehicle.globalX, vehicle.globalY);
                    java.awt.Point p2 = getPoint(vehicle.up.globalX, vehicle.up.globalY);
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
                if (vehicle.rightUp!=null) {
                    g2.setColor(new java.awt.Color(0, 255, 255));
                    java.awt.Point p1 = getPoint(vehicle.globalX, vehicle.globalY);
                    java.awt.Point p2 = getPoint(vehicle.rightUp.globalX, vehicle.rightUp.globalY);
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }
    }
    
    /**
     * Graphic class for detectors.
     */
    private class detectorGraphic implements graphic {
        
        /** The concerned detector. */
        private jDetector detector;
        
        /** 
         * Constructor that sets the detector.
         */
        public detectorGraphic(jDetector detector) {
            this.detector = detector;
        }
        
        /**
         * Whether the detector still exists in simulation.
         * @param model The model object.
         * @return By default <tt>true</tt>.
         */
        public boolean exists(jModel model) {
            return true;
        }
        
        /**
         * Returns the bounding box of the detector position.
         * @return By default <tt>null</tt>.
         */
        public java.awt.Rectangle.Double getGlobalBounds() {
            return null;
        }
        
        /**
         * Paints the detector on the canvas.
         * @param g Graphics to paint with.
         * @param canvas Canvas that is painted on.
         */
        public void paint(java.awt.Graphics g, networkCanvas canvas) {
            if (popupItemChecked("Show detectors")) {
                java.awt.Point.Double p = detector.lane.XY(detector.x());
                java.awt.Point.Double h = detector.lane.heading(detector.x());
                java.awt.Polygon pol = new java.awt.Polygon();

                java.awt.Point point;
                point = getPoint(p.x+h.y, p.y-h.x);
                pol.addPoint(point.x, point.y);
                point = getPoint(p.x-h.y, p.y+h.x);
                pol.addPoint(point.x, point.y);
                point = getPoint(p.x-h.y-h.x, p.y+h.x-h.y);
                pol.addPoint(point.x, point.y);
                point = getPoint(p.x+h.y-h.x, p.y-h.x-h.y);
                pol.addPoint(point.x, point.y);
                point = getPoint(p.x+h.y+h.x, p.y-h.x+h.y);
                pol.addPoint(point.x, point.y);
                point = getPoint(p.x-h.y+h.x, p.y+h.x+h.y);
                pol.addPoint(point.x, point.y);
                point = getPoint(p.x-h.y, p.y+h.x);
                pol.addPoint(point.x, point.y);

                g.setColor(new java.awt.Color(64, 64, 64));
                g.drawPolyline(pol.xpoints, pol.ypoints, pol.npoints);
            }
        }
    }
    
    /**
     * Graphic class for detector ids.
     */
    private class detectorLabel implements graphic {
        /** The concerned detector. */
        private jDetector detector;
        
        /** 
         * Constructor that sets the detector.
         */
        public detectorLabel(jDetector detector) {
            this.detector = detector;
        }
        
        /**
         * Whether the detector still exists in simulation.
         * @param model The model object.
         * @return By default <tt>true</tt>.
         */
        public boolean exists(jModel model) {
            return true;
        }
        
        /**
         * Returns the bounding box of the detector position.
         * @return By default <tt>null</tt>.
         */
        public java.awt.Rectangle.Double getGlobalBounds() {
            return null;
        }
        
        /**
         * Paints the detector id on the canvas.
         * @param g Graphics to paint with.
         * @param canvas Canvas that is painted on.
         */
        public void paint(java.awt.Graphics g, networkCanvas canvas) {
            // id
            if (popupItemChecked("Show detector IDs")) {
                java.awt.Point.Double p = detector.lane.XY(detector.x());
                java.awt.Point point = getPoint(p.x, p.y);
                g.setColor(new java.awt.Color(255, 255, 255));
                g.drawString(""+detector.id(), point.x, point.y);
            }
        }
    }
    
    /**
     * Graphic class for traffic lights.
     */
    private class trafficLightGraphic implements graphic {
        
        /** The concerned traffic light. */
        private jTrafficLight trafficLight;
        
        /** 
         * Constructor that sets the traffic light.
         */
        public trafficLightGraphic(jTrafficLight trafficLight) {
            this.trafficLight = trafficLight;
        }
        
        /**
         * Whether the traffic light still exists in simulation.
         * @param model The model object.
         * @return By default <tt>true</tt>.
         */
        public boolean exists(jModel model) {
            return true;
        }
        
        /**
         * Returns the bounding box of the traffic light position.
         * @return By default <tt>null</tt>.
         */
        public java.awt.Rectangle.Double getGlobalBounds() {
            return null;
        }
        
        /**
         * Paints the traffic light on the canvas.
         * @param g Graphics to paint with.
         * @param canvas Canvas that is painted on.
         */
        public void paint(java.awt.Graphics g, networkCanvas canvas) {
            if (popupItemChecked("Traffic lights")) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                java.awt.geom.Point2D.Double head = trafficLight.lane.heading(trafficLight.x());
                java.awt.geom.Point2D.Double pos = trafficLight.lane.XY(trafficLight.x());
                double xLeft = pos.x-1.75*head.y;
                double yLeft = pos.y+1.75*head.x;
                java.awt.Point left = getPoint(xLeft, yLeft);
                double xRight = pos.x+1.75*head.y;
                double yRight = pos.y-1.75*head.x;
                java.awt.Point right = getPoint(xRight, yRight);
                if (trafficLight.isGreen()) {
                    g2.setColor(new java.awt.Color(0, 255, 0));
                } else if (trafficLight.isYellow()) {
                    g2.setColor(new java.awt.Color(255, 255, 0));
                } else {
                    g2.setColor(new java.awt.Color(255, 0, 0));
                }
                g2.setStroke(new java.awt.BasicStroke(2, java.awt.BasicStroke.CAP_BUTT, 
                    java.awt.BasicStroke.JOIN_MITER, 1.0f));
                g2.drawLine(left.x, left.y, right.x, right.y);
            }
        }
    }
    
    /**
     * Graphic class for conflicts.
     */
    private class conflictGraphic implements graphic {
        
        /** The concerned conflict. */
        private conflictRSU conflict;
        
        /** 
         * Constructor that sets the conflict.
         */
        public conflictGraphic(conflictRSU conflict) {
            this.conflict = conflict;
        }
        
        /**
         * Whether the conflict still exists in simulation.
         * @param model The model object.
         * @return By default <tt>true</tt>.
         */
        public boolean exists(jModel model) {
            return true;
        }
        
        /**
         * Returns the bounding box of the conflict position.
         * @return By default <tt>null</tt>.
         */
        public java.awt.Rectangle.Double getGlobalBounds() {
            return null;
        }
        
        /**
         * Paints the conflict on the canvas.
         * @param g Graphics to paint with.
         * @param canvas Canvas that is painted on.
         */
        public void paint(java.awt.Graphics g, networkCanvas canvas) {
            if (popupItemChecked("Conflicts")) {
                // downstream line
                java.awt.geom.Point2D.Double h = conflict.lane.heading(conflict.x());
                java.awt.geom.Point2D.Double p = conflict.lane.XY(conflict.x());
                java.awt.Point p1 = getPoint(p.x-h.y*1.75, p.y+h.x*1.75);
                java.awt.Point p2 = getPoint(p.x+h.y*1.75, p.y-h.x*1.75);
                if (conflict.isSplit()) {
                    g.setColor(new java.awt.Color(0, 0, 255));
                } else if (conflict.isPriority()) {
                    g.setColor(new java.awt.Color(0, 255, 0));
                } else {
                    // draw visibility line
                    if (!Double.isInfinite(conflict.visibility())) {
                        g.setColor(new java.awt.Color(255, 128, 0));
                        double dx = conflict.visibility();
                        double x = conflict.x();
                        jLane l = conflict.lane;
                        while (l.up!=null && x<dx) {
                            dx -= x;
                            l = l.up;
                            x = l.l;
                        }
                        if (x>dx) {
                            h = l.heading(x-dx);
                            p = l.XY(x-dx);
                            java.awt.Point p3 = getPoint(p.x-h.y*1.75, p.y+h.x*1.75);
                            java.awt.Point p4 = getPoint(p.x+h.y*1.75, p.y-h.x*1.75);
                            g.drawLine((int)p3.getX(), (int)p3.getY(), (int)p4.getX(), (int)p4.getY());
                        }
                    }
                    g.setColor(new java.awt.Color(255, 0, 0));
                }
                if (conflict.length()==0) {
                    g.drawLine((int)p1.getX(), (int)p1.getY(), (int)p2.getX(), (int)p2.getY());
                } else {
                    java.util.ArrayList<Integer> xsA = new java.util.ArrayList<Integer>();
                    java.util.ArrayList<Integer> xsB = new java.util.ArrayList<Integer>();
                    java.util.ArrayList<Integer> ysA = new java.util.ArrayList<Integer>();
                    java.util.ArrayList<Integer> ysB = new java.util.ArrayList<Integer>();
                    xsA.add(p1.x);
                    xsB.add(p2.x);
                    ysA.add(p1.y);
                    ysB.add(p2.y);
                    double xCumul = conflict.lane.l;
                    for (int j=conflict.lane.x.length-2; j>=0; j--) {
                        if (xCumul<conflict.x() && xCumul>conflict.x()-conflict.length()) {
                            h = conflict.lane.heading(xCumul);
                            p = conflict.lane.XY(xCumul);
                            java.awt.Point p3 = getPoint(p.x-h.y*1.75, p.y+h.x*1.75);
                            java.awt.Point p4 = getPoint(p.x+h.y*1.75, p.y-h.x*1.75);
                            xsA.add(p3.x);
                            xsB.add(0, p4.x);
                            ysA.add(p3.y);
                            ysB.add(0, p4.y);
                        }
                        double dx = conflict.lane.x[j+1]-conflict.lane.x[j];
                        double dy = conflict.lane.y[j+1]-conflict.lane.y[j];
                        xCumul -= Math.sqrt(dx*dx + dy*dy);
                    }
                    h = conflict.lane.heading(conflict.x()-conflict.length());
                    p = conflict.lane.XY(conflict.x()-conflict.length());
                    java.awt.Point p3 = getPoint(p.x-h.y*1.75, p.y+h.x*1.75);
                    java.awt.Point p4 = getPoint(p.x+h.y*1.75, p.y-h.x*1.75);
                    xsA.add(p3.x);
                    xsB.add(0, p4.x);
                    ysA.add(p3.y);
                    ysB.add(0, p4.y);
                    // merge coordinates
                    int n = xsA.size();
                    int[] xs = new int[2*n];
                    int[] ys = new int[2*n];
                    for (int j=0; j<n; j++) {
                        xs[j] = xsA.get(j);
                        ys[j] = ysA.get(j);
                    }
                    for (int j=0; j<n; j++) {
                        xs[j+n] = xsB.get(j);
                        ys[j+n] = ysB.get(j);
                    }
                    g.drawPolygon(xs, ys, 2*n);
                }
                // upstream line
                if (conflict.length()>0) {
                    h = conflict.lane.heading(conflict.x()-conflict.length());
                    p = conflict.lane.XY(conflict.x()-conflict.length());
                    p1 = getPoint(p.x-h.y*1.75, p.y+h.x*1.75);
                    p2 = getPoint(p.x+h.y*1.75, p.y-h.x*1.75);
                    g.drawLine((int)p1.getX(), (int)p1.getY(), (int)p2.getX(), (int)p2.getY());
                }
            }
        }
    }
    
    /**
     * Listener for mouse actions on the canvas.
     */
    private class generalMouseListener implements 
            java.awt.event.MouseListener, 
            java.awt.event.MouseMotionListener {
        
        /** Last point during a drag. */
        private java.awt.Point lastDragPoint;
        
        /** Initial point of a drag. */
        private java.awt.Point initDragPoint;
        
        /** The mouse button number that was last clicked. */
        private int button;
        
        /**
         * Resets the zoom in case of a double click.
         * @param e Mouse event.
         */
        public void mouseClicked(java.awt.event.MouseEvent e) {
            if (e.getClickCount()==2) {
                resetZoom();
            } else if (e.getButton()==3 && e.getClickCount()==1) {
                popMenu.show(e.getComponent(), e.getX(), e.getY());
            } else if (e.getButton()==1 && e.isControlDown()) {
                java.util.ArrayList<jVehicle> vehs = model.getVehicles();
                if (trackVehicle==null && !vehs.isEmpty()) {
                    double minR = Double.POSITIVE_INFINITY;
                    java.awt.Point.Double point = getGlobalPoint(e.getX(), e.getY());
                    double r;
                    for (jVehicle veh : vehs) {
                        r = Math.sqrt((point.x-veh.globalX)*(point.x-veh.globalX) + 
                                (point.y-veh.globalY)*(point.y-veh.globalY));
                        if (r<minR) {
                            trackVehicle = veh;
                            minR = r;
                        }
                    }                
                } else {
                    trackVehicle = null;
                }
            }
        }

        /**
         * Sets initial information for if the press becomes a drag.
         * @param e Mouse event.
         */
        public void mousePressed(java.awt.event.MouseEvent e) {
            lastDragPoint = e.getPoint();
            initDragPoint = e.getPoint();
            button = e.getButton();
        }

        /** Empty. */
        public void mouseReleased(java.awt.event.MouseEvent e) {}

        /** Empty. */
        public void mouseEntered(java.awt.event.MouseEvent e) {}

        /** Empty. */
        public void mouseExited(java.awt.event.MouseEvent e) {}

        /**
         * Pans or zooms, depending on the button that was last pressed.
         * @param e Mouse event.
         */
        public void mouseDragged(java.awt.event.MouseEvent e) {
            int dx = lastDragPoint.x-e.getPoint().x;
            int dy = lastDragPoint.y-e.getPoint().y;
            if (button==java.awt.event.MouseEvent.BUTTON1) {
                hSlider.setValue(hSlider.getValue()+dx);
                vSlider.setValue(vSlider.getValue()+dy);
            } else if (button==java.awt.event.MouseEvent.BUTTON3) {
                double factor = 0;
                if (e.isControlDown()) {
                    factor = 1+.2*(Math.pow(1.01, dy)-1);
                } else{
                    factor = Math.pow(1.01, dy);
                }
                if (e.isShiftDown()) {
                    zoomVertical(factor, initDragPoint);
                } else if (e.isAltDown()) {
                    zoomHorizontal(factor, initDragPoint);
                } else {
                    zoom(factor, initDragPoint);
                }
            }
            lastDragPoint = e.getPoint();
        }

        /**
         * Updates the current position information.
         * @param e Mouse event.
         */
        public void mouseMoved(java.awt.event.MouseEvent e) {
            java.awt.Point.Double p = getGlobalPoint(e.getX(), e.getY());
            java.text.DecimalFormat df = new java.text.DecimalFormat("0");
            gui.posLabel.setText("x="+df.format(p.getX())+"m, y="+df.format(p.getY())+"m");
        }
    }
    
    /**
     * Adds a new check box menu item to the popup menu. After it is selected or
     * deselected the network canvas is repainted.
     * @param label Label for the menu item.
     * @param checked Initial checked state.
     */
    protected void addPopupItem(String label, boolean checked) {
        javax.swing.JCheckBoxMenuItem item = new javax.swing.JCheckBoxMenuItem(label, checked);
        item.addActionListener(
            new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    repaint();
                }
            }
        );
        popMenu.add(item);
    }
    
    /**
     * Return whether the popup menu item with given label is selected.
     * @param label Label of requested menu item.
     * @return Whether the popup menu item with given label is selected.
     */
    public boolean popupItemChecked(String label) {
        for (int i=0; i<popMenu.getSubElements().length; i++) {
            javax.swing.JCheckBoxMenuItem item = (javax.swing.JCheckBoxMenuItem) popMenu.getSubElements()[i];
            if (item.getText().equals(label) && item.isSelected()) {
                return true;
            }
        }
        return false;
    }
}