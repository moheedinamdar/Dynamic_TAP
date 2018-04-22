package GUI;

import microModel.jModel;

/**
 * Window in which a model can be visualized and run. By default all lanes,
 * detectors and vehicles are displayed.
 */
public class jModelGUI extends javax.swing.JFrame {
    
    /** Play button. */
    protected javax.swing.JToggleButton play;
    
    /** Step button. */
    protected javax.swing.JButton step;
    
    /** Simulation speed selection. */
    protected javax.swing.JComboBox simSpeed;
    
    /** Visualization step selection. */
    protected javax.swing.JComboBox visStep;
    
    /** Label to display the model time. */
    protected javax.swing.JLabel timeLabel;
    
    /** Label to display the number of vehicles. */
    protected javax.swing.JLabel nVehicles;
    
    /** Label to display the simulation speed. */
    protected javax.swing.JLabel speedLabel;
    
    /** Label to display the mouse position. */
    protected javax.swing.JLabel posLabel;
    
    /** Label to display the screen size. */
    protected javax.swing.JLabel sizeLabel;
    
    /** Vehicle color selection. */
    protected javax.swing.JComboBox vehCol;
    
    /** Vehicle color information/legend. */
    protected javax.swing.JPanel vehColInfo;
    
    /** Recording button. */
    protected javax.swing.JToggleButton rec;
    
    /** Main model. */
    private jModel model;
    
    /** Frame number. */
    private int frame = 0;
    
    /** Directory to save frame images. */
    private String frameDir;
    
    /** 
     * Boolean that is set true when the window is closed, which stops the 
     * simulation. 
     */
    private boolean isDisposed = false;
    
    /** Thread that perform the model run. */
    private Thread runThread;
    
    /** Network canvas, which displays the network. */
    private networkCanvas canvas;
    
    /**
     * Constructor which generates the window content.
     * @param model Model to be visualized.
     */
    public jModelGUI(jModel model) { 
        // window title
        super("MOTUS: Microscopic Open Traffic Simulation");
        
        // set model
        this.model = model;
        
        // set icon
        try {
            setIconImage(javax.imageio.ImageIO.read(getClass().getResource("/GUI/resources/merge.png")));
        } catch (Exception e) {
            
        }
        
        // for use in Matlab, otherwise closes the entire JVM, including Matlab
        setDefaultCloseOperation(javax.swing.JFrame.HIDE_ON_CLOSE);
        
        // set output dir for frames by output dir in model
        if (model.settings.containsString("outputDir")) {
            java.io.File f = new java.io.File(model.settings.getString("outputDir"), "frames");
            try {
                f.mkdir();
                frameDir = f.getPath();
            } catch (Exception e) {
                System.out.println("Could not create directory to export frames.");
            }
        }
        
        // listener at frame level, within the networkCanvas does not work in Matlab
        addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            /**
             * Converts the point in the event to the coordinate system of the 
             * network canvas and invokes the appropiate zoom method.
             * @param e Mouse wheel event.
             */
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
                java.awt.Point point = javax.swing.SwingUtilities.convertPoint(jModelGUI.this, e.getPoint(), canvas);
                if (canvas.contains(point)) {
                    double factor = 0;
                    if (e.isControlDown()) {
                        factor = 1+.2*(Math.pow(1.05, -e.getUnitsToScroll())-1);
                    } else{
                        factor = Math.pow(1.05, -e.getUnitsToScroll());
                    }
                    if (e.isShiftDown()) {
                        canvas.zoomVertical(factor, point);
                    } else if (e.isAltDown()) {
                        canvas.zoomHorizontal(factor, point);
                    } else {
                        canvas.zoom(factor, point);
                    }
                }
            }
        });
        
        // toolbar component heights
        int hButton = 25;
        int h = 21;
        
        // toolbar
        javax.swing.JToolBar toolBar = new javax.swing.JToolBar("Simulation control");
        toolBar.setFloatable(false);
        
        // pause button
        play = new javax.swing.JToggleButton(getIcon("play"));
        play.setMinimumSize(new java.awt.Dimension(hButton, hButton));
        play.setPreferredSize(new java.awt.Dimension(hButton, hButton));
        play.setMaximumSize(new java.awt.Dimension(hButton, hButton));
        play.setToolTipText("Pause or restart the simulation");
        play.addActionListener(new java.awt.event.ActionListener() {
            /**
             * Starts or stops the simulation, enables or disables the step 
             * button and changes the play button icon.
             * @param e Action event.
             */
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!play.isSelected()) {
                    play.setIcon(getIcon("play"));
                    step.setEnabled(true);
                } else {
                    play.setIcon(getIcon("pause"));
                    step.setEnabled(false);
                    // Runner thread
                    runThread = new Thread(new runner());
                    runThread.start();
                }
            }
        });
        play.setSelected(false);
        toolBar.add(play);
        
        // step button
        step = new javax.swing.JButton(getIcon("step"));
        step.setMinimumSize(new java.awt.Dimension(hButton, hButton));
        step.setPreferredSize(new java.awt.Dimension(hButton, hButton));
        step.setMaximumSize(new java.awt.Dimension(hButton, hButton));
        step.setToolTipText("Perform a single step during pause");
        step.addActionListener(new java.awt.event.ActionListener() {
            /**
             * Performs a single step, updates the canvas and writes a frame.
             * @param e Action event.
             */
            public void actionPerformed(java.awt.event.ActionEvent e) {
                // run the model
                jModelGUI.this.model.run(1);
                // visualize the model
                canvas.update();
                // write movie frame
                writeFrame();
                // stop if period has passed
                if (jModelGUI.this.model.t()>=jModelGUI.this.model.period) {
                    close();
                }
            }
        });
        toolBar.add(step);
        toolBar.addSeparator();
        
        // simulation speed
        simSpeed = new javax.swing.JComboBox(simulationSpeed.values());
        simSpeed.setSelectedItem(simulationSpeed.X1);
        simSpeed.setEditable(true);
        simSpeed.setMinimumSize(new java.awt.Dimension(40, h));
        simSpeed.setPreferredSize(new java.awt.Dimension(80, h));
        simSpeed.setMaximumSize(new java.awt.Dimension(80, h));
        simSpeed.setToolTipText("Simulation speed");
        simSpeed.addActionListener(new java.awt.event.ActionListener() {
            /**
             * Sets the visualization step selection off, or on at "max" value.
             * @param e Action event.
             */
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (simSpeed.getSelectedItem().toString().toLowerCase().startsWith("max")) {
                    visStep.setEnabled(true);
                } else {
                    visStep.setEnabled(false);
                }
            }
        });
        toolBar.add(simSpeed);
        
        // visualization step
        visStep = new javax.swing.JComboBox(simulationStep.values());
        visStep.setSelectedItem(simulationStep.S1);
        visStep.setEditable(true);
        visStep.setEnabled(false);
        visStep.setMinimumSize(new java.awt.Dimension(40, h));
        visStep.setPreferredSize(new java.awt.Dimension(80, h));
        visStep.setMaximumSize(new java.awt.Dimension(80, h));
        visStep.setToolTipText("Visualization step");
        toolBar.add(visStep);
        toolBar.addSeparator();
        
        // vehicle color
        vehCol = new javax.swing.JComboBox(defaultVehicleColors.values());
        vehCol.setMinimumSize(new java.awt.Dimension(50, h));
        vehCol.setPreferredSize(new java.awt.Dimension(150, h));
        vehCol.setMaximumSize(new java.awt.Dimension(150, h));
        vehCol.setToolTipText("Vehicle color");
        vehCol.addActionListener(new java.awt.event.ActionListener() {
            /**
             * Updates the vehicle color information (e.g. legend) and repaints 
             * the network canvas.
             * @param e Action event.
             */
            public void actionPerformed(java.awt.event.ActionEvent e) {
                // remove contents of vehicle color information
                vehColInfo.removeAll();
                // set default layout for user defined classes
                vehColInfo.setLayout(new java.awt.FlowLayout());
                // call the selected objects setInfo() method
                vehicleColor vehColObject = (vehicleColor) vehCol.getSelectedItem();
                vehColObject.setInfo(vehColInfo);
                // in case of destination selected, let the lanes show it
                if (vehColObject!=defaultVehicleColors.DESTINATION) {
                    canvas.showDestinations = false;
                } else {
                    canvas.showDestinations = true;
                }
                // re-layout and repaint the vehicle color information
                vehColInfo.validate();
                vehColInfo.repaint();
                // repaint the network canvas
                canvas.repaint();
            }
        });
        toolBar.add(vehCol);
        
        // vehicle color info
        vehColInfo = new javax.swing.JPanel();
        vehColInfo.setMinimumSize(new java.awt.Dimension(50, hButton));
        vehColInfo.setPreferredSize(new java.awt.Dimension(225, hButton));
        vehColInfo.setMaximumSize(new java.awt.Dimension(225, hButton));
        vehColInfo.setToolTipText("Vehicle color info");
        vehColInfo.setOpaque(false);
        toolBar.add(vehColInfo);
        toolBar.addSeparator();
        // show the info
        vehicleColor vc = (vehicleColor) vehCol.getSelectedItem();
        vc.setInfo(vehColInfo);
        
        // record button
        rec = new javax.swing.JToggleButton(getIcon("rec"));
        rec.setMinimumSize(new java.awt.Dimension(hButton, hButton));
        rec.setPreferredSize(new java.awt.Dimension(hButton, hButton));
        rec.setMaximumSize(new java.awt.Dimension(hButton, hButton));
        rec.setToolTipText("Record each visualization to image");
        rec.addActionListener(new java.awt.event.ActionListener() {
            /**
             * Sets the window not resizable during recording to preserve an 
             * equal frame size.
             */
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (rec.isSelected()) {
                    setResizable(false);
                } else {
                    setResizable(true);
                }
            }
        });
        rec.setSelected(false);
        toolBar.add(rec);
        
        // status bar
        hButton = 15;
        javax.swing.JToolBar statusBar = new javax.swing.JToolBar("Status bar");
        statusBar.setFloatable(false);
        statusBar.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.LOWERED));
        
        // time label
        timeLabel = new javax.swing.JLabel();
        timeLabel.setMinimumSize(new java.awt.Dimension(40, hButton));
        timeLabel.setPreferredSize(new java.awt.Dimension(80, hButton));
        timeLabel.setMaximumSize(new java.awt.Dimension(80, hButton));
        timeLabel.setToolTipText("Simulation time");
        statusBar.add(timeLabel);
        statusBar.addSeparator();
        
        // time label
        nVehicles = new javax.swing.JLabel();
        nVehicles.setMinimumSize(new java.awt.Dimension(40, hButton));
        nVehicles.setPreferredSize(new java.awt.Dimension(80, hButton));
        nVehicles.setMaximumSize(new java.awt.Dimension(80, hButton));
        nVehicles.setToolTipText("Number of vehicles");
        statusBar.add(nVehicles);
        statusBar.addSeparator();
        
        // speed label
        speedLabel = new javax.swing.JLabel();
        speedLabel.setMinimumSize(new java.awt.Dimension(40, hButton));
        speedLabel.setPreferredSize(new java.awt.Dimension(80, hButton));
        speedLabel.setMaximumSize(new java.awt.Dimension(80, hButton));
        speedLabel.setToolTipText("Simulation speed");
        statusBar.add(speedLabel);
        statusBar.addSeparator();
        
        // position label
        posLabel = new javax.swing.JLabel();
        posLabel.setMinimumSize(new java.awt.Dimension(70, hButton));
        posLabel.setPreferredSize(new java.awt.Dimension(140, hButton));
        posLabel.setMaximumSize(new java.awt.Dimension(140, hButton));
        posLabel.setToolTipText("Mouse position");
        statusBar.add(posLabel);
        statusBar.addSeparator();
        
        // window size label
        sizeLabel = new javax.swing.JLabel();
        sizeLabel.setMinimumSize(new java.awt.Dimension(100, hButton));
        sizeLabel.setPreferredSize(new java.awt.Dimension(200, hButton));
        sizeLabel.setMaximumSize(new java.awt.Dimension(200, hButton));
        sizeLabel.setToolTipText("Windows size");
        statusBar.add(sizeLabel);
        statusBar.addSeparator();
        
        // canvas
        canvas = new networkCanvas(model, this);
        canvas.init();
        
        // frame
        setLayout(new java.awt.BorderLayout());
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override 
            public void windowClosing(java.awt.event.WindowEvent e) {
                close();
            }
        });
        setSize(new java.awt.Dimension(700, 400));
        add(toolBar, java.awt.BorderLayout.NORTH);
        add(canvas, java.awt.BorderLayout.CENTER);
        add(statusBar, java.awt.BorderLayout.SOUTH);
        
        // final preparation
        setVisible(true);
        canvas.resetZoom();
    }
    
    /**
     * Adds a user-defined vehicle color indication.
     * @param userVehCol User-defined vehicle color indication.
     */
    public void addVehicleColor(vehicleColor userVehCol) {
        vehCol.addItem(userVehCol);
    }
    
    /**
     * Adds a grahpic to be drawn as a backdrop.
     * @param g Graphics object.
     */
    public void addBackdrop(graphic g) {
        canvas.addBackdrop(g);
    }
    
    /**
     * Adds a grahpic to be drawn as an overlay.
     * @param g Graphics object.
     */
    public void addOverlay(graphic g) {
        canvas.addOverlay(g);
    }
    
    /**
     * Adds a new check box menu item to the popup menu. After it is selected or
     * deselected the network canvas is repainted.
     * @param label Label for the menu item.
     * @param checked Initial checked state.
     */
    public void addPopupItem(String label, boolean checked) {
        canvas.addPopupItem(label, checked);
    }
    
    /**
     * Sets the background color of the network canvas.
     * @param r Red [0...255].
     * @param g Green [0...255].
     * @param b Blue [0...255].
     */
    public void setBackground(int r, int g, int b) {
        canvas.setBackground(new java.awt.Color(r, g, b));
    }
    
    /**
     * Draws and returns any of a number of icons.
     * @param label Label of icon: <tt>"play"</tt>, <tt>"pause"</tt>, 
     * <tt>"step"</tt> or <tt>"rec"</tt>.
     * @return Requested icon.
     */
    private javax.swing.ImageIcon getIcon(String label) {
        java.awt.image.BufferedImage im = 
                new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) im.createGraphics();
        if (label.equals("play")) {
            g2.setColor(new java.awt.Color(61, 152, 222));
            g2.fillPolygon(new int[] {6, 6, 12}, new int[] {1, 13, 7}, 3);
        } else if (label.equals("pause")) {
            g2.setColor(new java.awt.Color(61, 152, 222));
            g2.fillPolygon(new int[] {4, 4, 7, 7}, new int[] {2, 13, 13, 2}, 4);
            g2.fillPolygon(new int[] {10, 10, 13, 13}, new int[] {2, 13, 13, 2}, 4);
        } else if (label.equals("step")) {
            g2.setColor(new java.awt.Color(61, 152, 222));
            g2.fillPolygon(new int[] {6, 6, 10, 6, 6, 12}, new int[] {1, 3, 7, 11, 13, 7}, 6);
        } else if (label.equals("rec")) {
            g2.setColor(new java.awt.Color(236, 127, 44));
            g2.fillOval(3, 3, 10, 10);
        } else if (label.equals("icon")) {
            
        }
        return new javax.swing.ImageIcon(im);
    }
    
    /**
     * Writes a frame, if appropiate.
     */
    private void writeFrame() {
        if (rec.isSelected() && frameDir!=null) {
            java.awt.image.BufferedImage im = canvas.createImage();
            java.text.DecimalFormat nf = new java.text.DecimalFormat("00000");
            java.io.File f = new java.io.File(frameDir, "frame"+nf.format(frame) +".png");
            try {
                javax.imageio.ImageIO.write(im, "png", f);
            } catch (Exception ex) {

            }
            frame++;
        }
    }
    
    /**
     * Lets a thread wait untill the simulation has finished.
     */
    public synchronized void waitFor() {
        try {
            wait();
        } catch (Exception e) {
            
        }
    }
    
    /**
     * Closes the figure by waiting for the model run to stop, storing the 
     * required data and notifying any waiting thread.
     */
    private void close() {
        isDisposed = true; // trigger the runThread to stop
        if (runThread!=null && runThread!=Thread.currentThread()) {
            try {
                runThread.join();
            } catch (Exception ex) {
                
            }
        }
        model.storeData();
        setVisible(false);
        synchronized (this) {
            notifyAll();
        }
    }
    
    
    /**
     * Runnable that runs the model and updates the GUI.
     */
    private class runner implements java.lang.Runnable {
        
        private double aggTSim;
        
        /**
         * Runs the simulation while applicable.
         */
        public void run() {
            while (play.isSelected() && !isDisposed && model.t()<model.period) {
                
                // store time to keep track of speed
                long t = System.currentTimeMillis();
                 
                // get the selected speed
                Object obj = simSpeed.getSelectedItem();
                boolean isMax = false;
                double speed = 1;
                if (obj instanceof simulationSpeed) {
                    // a predefined enum is selected
                    simulationSpeed item = (simulationSpeed) simSpeed.getSelectedItem();
                    if (item==simulationSpeed.MAXIMUM) {
                        speed = Double.POSITIVE_INFINITY;
                        isMax = true;
                    } else {
                        speed = Double.parseDouble(item.toString().substring(1));
                    }
                } else if (obj.toString().toLowerCase().startsWith("max")) {
                    // user typed something that starts with "max"
                    speed = Double.POSITIVE_INFINITY;
                    isMax = true;
                } else {
                    // user typed something else
                    String str = obj.toString();
                    // remove possible first "x"
                    if (str.startsWith("x") || str.startsWith("X")) {
                        str = str.substring(1);
                    }
                    // try to translate to a number
                    try {
                        speed = Double.parseDouble(str);
                    } catch (Exception e) {
                        // keep speed = 1
                    }
                    // no negative speed
                    speed = speed < 0 ? 1 : speed;
                }
                
                // get the number of steps to perform
                int n = 1;
                if (isMax) {
                    // at maximum speed, the number of steps can be set
                    try {
                        // predefined enum or user typed string, both to string
                        n = Integer.parseInt(visStep.getSelectedItem().toString());
                    } catch (Exception e) {
                        // keep n = 1
                    }
                }   
                
                // run the model
                model.run(n);
                // visualize the model
                canvas.update();
                // write movie frame
                writeFrame();
                
                // sleep any remaining time for the correct simulation speed
                boolean update = true;
                long dtAct = System.currentTimeMillis()-t;
                if (!isMax) {
                    long dtReq = (long) (1000*(n*model.dt/speed));
                    if (dtReq>dtAct) {
                        try {
                            Thread.sleep((long)(dtReq-dtAct));
                        } catch (Exception e) {

                        }
                        dtAct = dtReq; // actual duration after wait
                    } 
                    speed = speed * dtReq/dtAct;
                } else {
                    aggTSim = aggTSim + 1000*n*model.dt;
                    if (dtAct>0) {
                        speed = aggTSim / (double) dtAct;
                        aggTSim = 0;
                    } else {
                        update = false;
                    }
                }
                
                if (update) {
                    java.text.DecimalFormatSymbols dfs = new java.text.DecimalFormatSymbols();
                    dfs.setDecimalSeparator('.');
                    java.text.DecimalFormat df;
                    if (speed >= 100) {
                        df = new java.text.DecimalFormat("0", dfs);
                    } else if (speed >= 10) {
                        df = new java.text.DecimalFormat("0.0", dfs);
                    } else {
                        df = new java.text.DecimalFormat("0.000", dfs);
                    }
                    speedLabel.setText(df.format(speed)+"x");
                }
            }
            // close if stopped for simulation end
            if (model.t()>=model.period && !isDisposed) {
                close();
            }
        }
    }
    
    /**
     * Default simulation speeds enumeration.
     */
    private enum simulationSpeed {
        /** 0.5 times reality. */
        X05, 
        /** 1 time reality. */
        X1, 
        /** 2 times reality. */
        X2, 
        /** 5 times reality. */
        X5, 
        /** 10 times reality. */
        X10, 
        /** 25 times reality. */
        X25, 
        /** 100 times reality. */
        X100, 
        /** Maximum speed (does not wait between steps). */
        MAXIMUM;

        /**
         * Returns the name as a more readable string.
         * @return More readable string of the name.
         */
        @Override
        public java.lang.String toString() {
            return name().replace("X0", "X0.").toLowerCase();
        }
    }
    
    /**
     * Default simulation steps enumeration.
     */
    private enum simulationStep {
        /** Every time step. */
        S1, 
        /** Every 2 time steps. */
        S2, 
        /** Every 5 time steps. */
        S5, 
        /** Every 10 time steps. */
        S10, 
        /** Every 25 time steps. */
        S25, 
        /** Every 100 time steps. */
        S100;

        /**
         * Returns the name as a more readable string.
         * @return More readable string of the name.
         */
        @Override
        public String toString() {
            if (name().startsWith("S")) {
                return name().substring(1);
            } else {
                return name().toLowerCase();
            }
        }
    }
}