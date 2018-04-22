package microModel;

/**
 * Default wrapper for a vehicle. It contains a driver and possibly an OBU.
 */
public class jVehicle extends jMovable {

    /** OBU within the vehicle, may be <tt>null</tt>. */
    public jOBU OBU;
    
    /** Driver of the vehicle. */
    public jDriver driver;
    
    /** Current lateral speed in 'amount of lane per time step' [0...1]. */
    public double dy;
    
    /** Total progress of a lane change in 'amount of lane' [0...1]. */
    public double lcProgress;
    
    /** Lane change direction. */
    public jModel.latDirection lcDirection;
    
    /** Temporary lane change vehicle, if any. */
    public jLcVehicle lcVehicle;
    
    /** Maximum vehicle speed [km/h]. */
    public double vMax;
    
    /** Maximum vehicle deceleration (a value below 0) [m/s^2]. */
    public double aMin;
    
    /** Route of the vehicle (or driver). */
    public jRoute route;
    
    /** Vehicle trajectory. */
    public jTrajectory trajectory;
    
    /** Vehicle class ID. */
    public int classID;
    
    /** Set of ordered RSUs in <tt>RSURange</tt>. */
    protected java.util.ArrayList<jRSU> RSUsInRange = 
            new java.util.ArrayList<jRSU>();
    
    /** Range within which RSUs are connected [m]. */
    protected double RSURange;
    
    /** Lane where the rear is located, used for heading determination. */ 
    protected jLane rearLane;

    /** Time step of last move. */
    protected int moved = -1;
    
    /** 
     * Constructor connecting the vehicle with the main model. 
     * @param model Main model.
     */
    public jVehicle(jModel model) {
        super(model);
    }

    /**
     * Sets the acceleration which is limited by vehicle capabilities and by
     * reaching a speed of zero.
     * @param a Desired acceleration.
     */
    public void setAcceleration(double a) {
        // limit acceleration to maximum deceleration
        a = a<aMin ? aMin : a;
        // limit acceleration to lack of speed (getting to zero at end of time step)
        double aZero = -v/model.dt;
        a = a<aZero ? aZero : a;
        this.a = a;
    }
    
    /**
     * Moves the vehicle in longitudinal and lateral direction based on 
     * <tt>a</tt> and <tt>dy</tt>.
     */
    public void move() {
        // skip if moved
        if (moved>=lane.model.k) {
            return;
        }
        // move leaders first
        if (down!=null) {
            down.getDriver().vehicle.move();
        }
        if (lcVehicle!=null && lcVehicle.down!=null) {
            lcVehicle.down.getDriver().vehicle.move();
        }
        // tag as moved
        moved = lane.model.k;
        // lateral
        lcProgress += dy;
        // longitudinal
        double dt = model.dt;
        double dx = dt*v + .5*a*dt*dt;
        dx = dx >= 0 ? dx : 0;
        v = v+dt*a;
        // limit speed to positive values
        v = v >= 0 ? v : 0;
        // clip very small values to zero for v==0 evaluations
        v = v<0.000001 ? 0 : v;
        translate(dx);
        setXY();
    }

    /**
     * Function to translate a distance, moving onto downstream lanes as needed.
     * If a destination is reached the vehicle is deleted.
     * @param dx Distance [m] to translate.
     */
    public void translate(double dx) {
        
        // Update RSUs in range and pass them if appropiate
        jRSU rsu = null;
        double s = 0;
//        java.util.Iterator<jRSU> it = RSUsInRange.iterator();
//        while (it.hasNext()) {
//            rsu = it.next();
//            s = getDistanceToRSU(rsu) - dx;
//            if (s<0) {
//                if (rsu.passable || rsu.noticeable) {
//                    rsu.pass(this);
//                }
//                it.remove();
//            }
//        }
        // Get point to search from
        jLane lastLane;
        double lastX;
        if (RSUsInRange.isEmpty()) {
            // search downstream of vehicle
            lastLane = lane;
            lastX = x;//+dx;
            s = 0;
        } else {
            // search downstream of last RSU
            rsu = RSUsInRange.get(RSUsInRange.size()-1);
            if (rsu instanceof jLane.splitRSU) {
                // at a split, search from x=0 at the next lane
                lastLane = ((jLane.splitRSU) rsu).getLaneForRoute(route);
                lastX = 0;
            } else {
                lastLane = rsu.lane;
                lastX = rsu.x;
            }
            s = getDistanceToRSU(rsu)-dx;
        }
        // Add new RSUs in range
        java.util.ArrayList<jRSU> next = new java.util.ArrayList<jRSU>();
        while (s<RSURange && lastLane!=null) {
            // lastLane may become null at a split where the route has no
            // appropiate downstream lane, as the vehicle has to change lane
            // before the split
            next = lastLane.findRSU(lastX, RSURange-s);
            if (!next.isEmpty()) {
                s = getDistanceToRSU(next.get(0))-dx;
                for (jRSU j : next) {
                    RSUsInRange.add(j);
                    if (j instanceof jLane.splitRSU) {
                        // continue search on next lane after split
                        lastLane = ((jLane.splitRSU) j).getLaneForRoute(route);
                        lastX = 0;
                    } else {
                        // continue search after rsu
                        lastLane = j.lane;
                        lastX = j.x;
                    }
                }
            } else {
                s = RSURange; // stop loop
            }
        }
        // Pass and delete RSUs with negative distance
        java.util.Iterator<jRSU> it = RSUsInRange.iterator();
        while (it.hasNext()) {
            rsu = it.next();
            s = getDistanceToRSU(rsu) - dx;
            if (s<0) {
                if (rsu.passable || rsu.noticeable) {
                    rsu.pass(this);
                }
                it.remove();
            }
        }
        
        // Move movable downstream
        x += dx;
        justExceededLane = false;
        while (x > lane.l) {
            justExceededLane = true;
            if (lane.down==null && lane.destination==0) {
                model.deleted++;
                System.out.println("Vehicle deleted as lane "+lane.id+" is exceeded ("+model.deleted+"), dead end");
                delete();
                return;
            } else if (route.destinations().length==1 && lane.destination==route.destinations()[0]) {
                // vehicle has reached its destination
                if (model.settings.getBoolean("storeTrajectoryData") && trajectory!=null) {
                    model.saveTrajectoryData(trajectory);
                }
                delete();
                return;
            } else if (!route.canBeFollowedFrom(lane.down)) {
                model.deleted++;
                System.out.println("Vehicle deleted as lane "+lane.id+" is exceeded ("+model.deleted+"), route unreachable");
                delete();
                return;
            } else {
                // update route
                if (lane.destination>0) {
                    route = route.subRouteAfter(lane.destination);
                }
                // abort impossible lane change
                if (lcVehicle!=null) {
                    if ( (lcDirection==jModel.latDirection.RIGHT && 
                            (lane.down.right==null || lane.down.right!=lcVehicle.lane.down)) ||
                            (lcDirection==jModel.latDirection.LEFT && 
                            (lane.down.left==null || lane.down.left!=lcVehicle.lane.down)) ) {
                        abortLaneChange();
                    }
                }
                // check whether adjacent neighbours need to be reset
                // these will be found automatically by updateNeighbour() in
                // the main model loop
                if (lane.left!=null && lane.left.down!=lane.down.left) {
                    leftUp = null;
                    leftDown = null;
                }
                if (lane.right!=null && lane.right.down!=lane.down.right) {
                    rightUp = null;
                    rightDown = null;
                }
                // put on downstream lane
                x -= lane.l;
                lane.vehicles.remove(this);
                lane.down.vehicles.add(this);
                if (lane.down.isMerge()) {
                    // register the current lane as the origin of the last  
                    // entering vehicle on the next lane
                    lane.down.mergeOrigin = lane;
                }
                lane = lane.down;
                if (lane.isMerge()) {
                    jLane lTmp = lane;
                    cut();
                    paste(lTmp, x);
                }
            }
        }
        
        // Move lane change vehicle
        if (lcVehicle != null) {
            double xNew = 0;
            double xAdj = 0;
            if (lcDirection==jModel.latDirection.LEFT) {
                xNew = getAdjacentX(jModel.latDirection.LEFT);
                xAdj = lcVehicle.lane.xAdj(lane.left);
            }
            else {
                xNew = getAdjacentX(jModel.latDirection.RIGHT);
                xAdj = lcVehicle.lane.xAdj(lane.right);
            }
            lcVehicle.translate(xNew+xAdj - lcVehicle.x);
            lcVehicle.a = a;
            lcVehicle.v = v;
            lcVehicle.setXY();
        }
        
        // Append trajectory
        if (model.settings.getBoolean("storeTrajectoryData") && trajectory!=null) {
            trajectory.append();
        }
    }
    
    /**
     * Sets the range within which RSUs are linked to the vehicle.
     * @param range Range within which RSUs are linked [m].
     */
    public void setRSURange(double range) {
        RSURange = range;
    }

    /**
     * Starts a lane change by creating an <tt>lcVehicle</tt>.
     */
    public void startLaneChange() {
        lcVehicle = new jLcVehicle(this);//
        model.addVehicle(lcVehicle);//
        jLane atLane;
        if (lcDirection==jModel.latDirection.LEFT) {
            atLane = lane.left;
        } else {
            atLane = lane.right;
        }
        double atX = getAdjacentX(lcDirection);
        lcVehicle.paste(atLane, atX);
    }

    /**
     * Ends a lane change by deleting the <tt>lcVehicle</tt> and changing the lane.
     */
    public void endLaneChange() {
        // set vehicle at target lane and delete temporary vehicle
        lcVehicle.delete();
        jLane targetLane;
        double targetX = getAdjacentX(lcDirection);
        if (lcDirection==jModel.latDirection.LEFT) {
            targetLane = lane.left;
        } else {
            targetLane = lane.right;
        }
        cut();
        paste(targetLane, targetX);
        lcProgress = 0;
        dy = 0;
        // remove RSUs in range due to new lane
        RSUsInRange.clear();
        rearLane = null;
    }
    
    /**
     * Aborts a lane change by deleting the <tt>lcVehicle</tt>.
     */
    public void abortLaneChange() {
        // instantaneous abort of lane change
        lcVehicle.delete();
        lcProgress = 0;
        dy = 0;
        rearLane = null;
    }

    /**
     * Initiates a lateral movement to the left.
     * @param dy Initial speed of the lateral movement in 'amount of lane per time step'.
     */
    public void changeLeft(double dy) {
        lcDirection = jModel.latDirection.LEFT;
        this.dy = dy;
    }

    /**
     * Initiates a lateral movement to the right.
     * @param dy Initial speed of the lateral movement in 'amount of lane per time step'.
     */
    public void changeRight(double dy) {
        lcDirection = jModel.latDirection.RIGHT;
        this.dy = dy;
    }

    /**
     * Sets global x and y coordinates and heading. This may be in between two 
     * lanes in case of a lane change.
     */
    public void setXY() {
        java.awt.geom.Point2D.Double coord = atLaneXY();
        if (lcVehicle!=null) {
            // interpolate between own and lcVehicle global X and Y
            globalX = coord.x*(1-lcProgress) + lcVehicle.globalX*lcProgress;
            globalY = coord.y*(1-lcProgress) + lcVehicle.globalY*lcProgress;
        }
        else {
            globalX = coord.x;
            globalY = coord.y;
        }
        setHeading();
        if (lcVehicle!=null) {
            lcVehicle.heading = heading;
        }
    }
    
    /**
     * Sets the heading of this vehicle based on lane curvature.
     */
    public void setHeading() {
        java.awt.geom.Point2D.Double p1 = lane.XY(x);
        java.awt.geom.Point2D.Double p2;
        if (x>l || rearLane==null) {
            rearLane = lane;
            p2 = lane.XY(x-l);
        } else {
            // update rearlane
            double xRear = rearLane.xAdj(lane)+x-l;
            while (xRear > rearLane.l) {
                if (rearLane.down!=null) {
                    rearLane = rearLane.down;
                } else if (rearLane.isSplit()) {
                    for (int i=0; i<rearLane.RSUcount(); i++) {
                        if (rearLane.getRSU(i) instanceof jLane.splitRSU) {
                            jLane.splitRSU split = (jLane.splitRSU) rearLane.getRSU(i);
                            jLane tmp = split.getLaneForRoute(route);
                            if (tmp!=null) {
                                rearLane = tmp;
                                break;
                            }
                        }
                    }
                }
                xRear = rearLane.xAdj(lane)+x-l;
            }
            p2 = rearLane.XY(xRear);
        }
        double xx = p1.x-p2.x;
        double yy = p1.y-p2.y;
        double f = Math.sqrt(xx*xx + yy*yy);
        heading = new java.awt.geom.Point2D.Double(xx/f, yy/f);
    }

    /**
     * Returns the maximum vehicle speed in m/s.
     * @return Maximum vehicle speed [m/s].
     */
    public double getVMax() {
        return vMax/3.6;
    }

    /**
     * Correlates some parameters to other stochastic parameters. By default
     * this method is empty. Sub classes can define content for this method. The
     * method will be called after the stochastic parameters are set.
     */
    public void correlateParameters() {}

    /**
     * Returns whether the vehicle is equipped with an OBU.
     * @return Whether the car is equiped.
     */
    public boolean isEquipped() {
        return OBU!=null;
    }
    
    /**
     * Returns the driver of this vehicle.
     * @return Driver of this vehicle.
     */
    public jDriver getDriver() {
        return driver;
    }
}