package airforcezero;
import battlecode.common.*;

import java.util.Random;
import java.lang.Math;

public class Lumberjack {
    //global stuff
    private static final int MIN_GARDENER_RANGE = 2, MAX_GARDENER_RANGE = 6;    //if protecting gardeners, will try to stay between these distances away from them

    private static Random rng;
    private static MapLocation dest1, dest2, prevGardenerPos;   //main and off-route destinations

    private static int prevTreeNext, treeNext, treeChannel, treeID;
    private static boolean stayNear, isIdle, foundGardener, firstRound;

    private static RobotInfo[] allRobots, friendlyRobots, enemyRobots, friendlyGardeners;
    private static TreeInfo[] allTrees, friendlyTrees, neutralTrees, enemyTrees;
    private static int friendlyRobotCount, enemyRobotCount, friendlyTreeCount, neutralTreeCount, enemyTreeCount, friendlyGardenerCount;
    
    /*
    -(Temporary)2/3 of lumberjacks wander off to search for trees
    -When a neutral(Temporary) or enemy tree is found, report it
    -If nothing to chop, wander/stay near gardeners
    -If trees reported, go to first in list, chop
    
    Logic:
    //Updating block:
    -Update info
    -If trees in my range:
        -Report
        -If previously idle:
            -Become active
            -Update goals
    -If staying near gardeners, and gardener is in range:
        -Update prev gardener position
    -If previously idle:
        -If a tree was reported:
            -Become active
            -Update goals
    -Else (previously active):
        -If the channel of tree has been marked with a 0:
            -If the next index is empty and located right next to previous tree index (no more trees):
                -Become idle
                -If stays near gardeners:
                    -Go to previous gardener location
                -Else:
                    -Continue exploring
            -Else:
                -Update dest to next tree in array
     //Moving block
     -If idle:
        -If staying near gardeners:
            -If looking for a gardener:
                -If moving reaches location:
                    -Pick new faraway location
            -Else (near gardener):
                -Move accordingly
        -Else (exploring):
            -If moving reaches location:
                -Pick new faraway location
     -Else (active):
        -If tree id is known:
            -Chop the tree
        -Else:
            -If moving reaches destination (tree):
                -Find the tree
                -Chop it

    -TODO:
    -(Important) running away
    -shaking trees
    -better travel code
    -bullet dodging
    -strategic determination of which trees to chop
    -combat (if necessary)
    */
    public static void run(RobotController rc) throws GameActionException {
        rng = new Random(rc.getID());
        dest1 = null;
        dest2 = null;
        prevGardenerPos = rc.getInitialArchonLocations(rc.getTeam())[0];
        treeChannel = -1;
        treeNext = rc.readBroadcast(15);
        if(treeNext == 0) { //in case this is the first lumberjack, set the index to default
            rc.broadcast(15, 16);
            treeNext = 16;
        }
        prevTreeNext = treeNext-1;
        if(prevTreeNext == 15)
            prevTreeNext = 29;
        treeID = -1;
        firstRound = true;
        stayNear = false; //near gardeners or exploring
        isIdle = true;   //whether the lumberjacks have trees to chop
        if(rc.getID() % 3 == 0)
            stayNear = true;

        //code below repeats every turn
        while(true) {
            //updating info about robots and trees around me
            updateInfo(rc);
            prevTreeNext = treeNext;
            treeNext = rc.readBroadcast(15);

            //check trees
            reportTreesInRange(rc);

            //gardener update
            foundGardener = false;
            if (stayNear && friendlyGardenerCount > 0) {
                foundGardener = true;
                prevGardenerPos = friendlyGardeners[0].location;
            }

            //read tree broadcasts
            updateTreeTarget(rc);

            //move
            moveAndChop(rc);

            //end of while loop - yield to end
            firstRound = false;
            Clock.yield();
        }
    }
    private static void updateInfo(RobotController rc){
        allRobots = rc.senseNearbyRobots();
        friendlyRobots = new RobotInfo[allRobots.length];
        enemyRobots = new RobotInfo[allRobots.length];
        friendlyGardeners = new RobotInfo[allRobots.length];
        allTrees = rc.senseNearbyTrees();
        friendlyTrees = new TreeInfo[allTrees.length];
        neutralTrees = new TreeInfo[allTrees.length];
        enemyTrees = new TreeInfo[allTrees.length];
        friendlyRobotCount = 0;
        enemyRobotCount = 0;
        friendlyGardenerCount = 0;
        friendlyTreeCount = 0;
        neutralTreeCount = 0;
        enemyTreeCount = 0;
        if(rc.getTeam() == Team.A) {
            for(RobotInfo info : allRobots) {
                switch(info.team) {
                    case A:
                        friendlyRobots[friendlyRobotCount] = info;
                        friendlyRobotCount++;
                        if(info.type == RobotType.GARDENER){
                            friendlyGardeners[friendlyGardenerCount] = info;
                            friendlyGardenerCount++;
                        }
                        break;
                    case B:
                        enemyRobots[enemyRobotCount] = info;
                        enemyRobotCount++;
                }
            }
            for(TreeInfo info : allTrees){
                switch(info.team) {
                    case A:
                        friendlyTrees[friendlyTreeCount] = info;
                        friendlyTreeCount++;
                        break;
                    case B:
                        enemyTrees[enemyTreeCount] = info;
                        enemyTreeCount++;
                        break;
                    case NEUTRAL:
                        neutralTrees[neutralTreeCount] = info;
                        neutralTreeCount++;
                }
            }
        }
        else{
            for (RobotInfo info : allRobots) {
                switch (info.team) {
                    case B:
                        if(info.type == RobotType.GARDENER){
                            friendlyGardeners[friendlyGardenerCount] = info;
                            friendlyGardenerCount++;
                        }
                        friendlyRobots[friendlyRobotCount] = info;
                        friendlyRobotCount++;
                        break;
                    case A:
                        enemyRobots[enemyRobotCount] = info;
                        enemyRobotCount++;
                }
            }
            for(TreeInfo info : allTrees){
                switch(info.team) {
                    case B:
                        friendlyTrees[friendlyTreeCount] = info;
                        friendlyTreeCount++;
                        break;
                    case A:
                        enemyTrees[enemyTreeCount] = info;
                        enemyTreeCount++;
                        break;
                    case NEUTRAL:
                        neutralTrees[neutralTreeCount] = info;
                        neutralTreeCount++;
                }
            }
        }
    }
    private static boolean approachDestStupid(RobotController rc) throws GameActionException{   //returns true if further movement not required (reached location or can not move further)
        // sort of like zombie code from last year
        MapLocation dest = dest1;
        if(dest2 != null)
            dest = dest2;

        //debug
        if(rc.getTeam() == Team.A)
            rc.setIndicatorDot(dest, 255, 0, 0);
        else
            rc.setIndicatorDot(dest, 0, 0, 255);

        MapLocation myLocation = rc.getLocation();
        Direction d = myLocation.directionTo(dest);
        if(d == null){
            if(dest2 != null){
                dest2 = null;
                return false;
            }
            return true;
        }
        if(rc.canMove(dest)){
            rc.move(dest);
            return true;
        }
        if(rc.canMove(d)){
            rc.move(d);
            return false;
        }
        if(myLocation.distanceTo(dest) <= GameConstants.NEUTRAL_TREE_MAX_RADIUS + RobotType.LUMBERJACK.bodyRadius){
            if(dest2 != null){
                dest2 = null;
                return false;
            }
            return true;
        }
        else{
            Direction dLeft = d.rotateLeftDegrees(45);
            Direction dRight = d.rotateRightDegrees(45);
            if(rc.canMove(dLeft)) rc.move(dLeft);
            else if(rc.canMove(dRight)) rc.move(dRight);
        }
        //can't go further - stuck
        Direction newd;
        if(rng.nextInt(2) == 0)
            newd = d.rotateLeftDegrees(90);
        else
            newd = d.rotateRightDegrees(90);
        dest2 = myLocation.add(newd);
        return false;
    }
    private static void updateTreeTarget(RobotController rc) throws GameActionException{
        if (isIdle) {
            if(prevTreeNext != treeNext) {
                isIdle = false;
                int target;
                target = rc.readBroadcast(prevTreeNext);
                if(target != 0) {
                    treeChannel = prevTreeNext;
                    dest1 = new MapLocation(target / 1000, target % 1000);
                    dest2 = null;
                }
            }
        }
        else{
            int diff = treeNext - prevTreeNext;
            if(rc.readBroadcast(treeChannel) == 0){
                if(rc.readBroadcast(treeNext) == 0 && (diff == 1 || diff == -13)){
                    isIdle = true;
                    if(stayNear){
                        dest1 = prevGardenerPos;
                    }
                    else{
                        dest1 = rc.getLocation().add(new Direction(rng.nextFloat() * 2 * (float)Math.PI), 10);
                    }
                }
                else{
                    int position = treeChannel + 1;
                    int newTree;
                    while(true){
                        if(position == 30)
                            position = 16;
                            
                        newTree = rc.readBroadcast(position);
                        if(newTree != 0)
                            break;
                        
                        position++;
                    }
                    treeChannel = position;
                    dest1 = new MapLocation(newTree/1000, newTree%1000);
                }
            }
        }
    }
    private static void reportTreesInRange(RobotController rc) throws GameActionException{
        if (enemyTreeCount + neutralTreeCount > 0) {
            TreeInfo target = null;
            int targetIndex = -1;
            //check for dupes
            Integer[] toReportIndex = new Integer[enemyTreeCount + neutralTreeCount];
            int index = 0;
            for (TreeInfo info : enemyTrees) {
                if(info == null)
                    break;
                if(target == null)
                    target = info;
                toReportIndex[index] = 1000 * round(info.location.x) + round(info.location.y);
                index++;
            }
            for (TreeInfo info : neutralTrees) {
                if(info == null)
                    break;
                if(target == null)
                    target = info;
                toReportIndex[index] = 1000 * round(info.location.x) + round(info.location.y);
                index++;
            }
            if (firstRound || !isIdle) {  //the beginning of the game, or trees already in array
                for (int i = 16; i < 30; i++) {
                    int existingTreeVal = rc.readBroadcast(i);
                    for (int i2 = 0; i2 < toReportIndex.length; i++) {
                        Integer newTreeVal = toReportIndex[i2];
                        if (newTreeVal != null) {
                            if (newTreeVal == existingTreeVal)
                                toReportIndex[i2] = null;   //removes tree to report
                        }
                    }
                }
            }
   
            for (Integer value : toReportIndex) {
                if (value == null)
                    continue;
                rc.broadcast(treeNext, value);
                treeNext++;
                if(treeNext == 30)
                    treeNext = 16;
            }
     
            rc.broadcast(15, treeNext);
            if (isIdle) {
                isIdle = false;
                treeChannel = prevTreeNext;
                dest1 = target.location;
                dest2 = null;
            }
        }
    }
    private static void reportTree(RobotController rc, TreeInfo t) throws GameActionException{
        //IMPORTANT NOTE:
        //If using this method in your code, make a global private static int treeNext
        //Also, copy over the method directly after this one
        for(int i = 16; i < 30; i++) {
            if(round(t.location.x) == rc.readBroadcast(i)/1000 && round(t.location.y) == rc.readBroadcast(i))
                return;
        }

        treeNext = rc.readBroadcast(15);

        int value = 0;  //value to later report
        value += 1000 * round(t.location.x);
        value += round(t.location.y);

        rc.broadcast(treeNext, value);
        treeNext++;
        if(treeNext == 30)
            treeNext = 16;

        rc.broadcast(15, treeNext);
    }
    private static int round(double a){
        //Math.round(double a), but takes less than 10 bytecodes (hopefully).
        int base = (int)a;
        if(a - base > 0.5)
            return base+1;
        return base;
    }
    private static void moveAndChop(RobotController rc) throws GameActionException{
        if(isIdle){
            if(stayNear){
                if(foundGardener) {
                    double distance = rc.getLocation().distanceTo(prevGardenerPos);
                    if(distance > MAX_GARDENER_RANGE) {
                        Direction d = rc.getLocation().directionTo(prevGardenerPos);
                        if(rc.canMove(d)){
                            rc.move(d);
                            return;
                        }
                    }
                    else if(distance < MIN_GARDENER_RANGE){
                        Direction d = prevGardenerPos.directionTo(rc.getLocation());
                        if(rc.canMove(d)){
                            rc.move(d);
                            return;
                        }
                    }
                    for(int i = 0; i < 10; i++){    //prevents infinite while loops if stuck
                        Direction d = new Direction(rng.nextFloat() * 2 * (float)Math.PI);
                        if(rc.canMove(d)){
                            rc.move(d);
                            break;
                        }
                    }
                }
                else {
                    if(approachDestStupid(rc))
                        dest1 = rc.getLocation().add(new Direction(rng.nextFloat() * 2 * (float)Math.PI));
                }
            }
            else{
                if(dest1 == null)
                    dest1 = rc.getLocation().add(new Direction(rng.nextFloat() * 2 * (float)Math.PI));
                if(approachDestStupid(rc))
                    dest1 = rc.getLocation().add(new Direction(rng.nextFloat() * 2 * (float)Math.PI));
            }
        }
        else {
            if(treeID != -1) {
                if(rc.canChop(treeID))
                    rc.chop(treeID);
             }
            else{
                if(approachDestStupid(rc)) {
                    TreeInfo[] trees = rc.senseNearbyTrees(dest1, 1, null);
                    if(trees.length > 0)
                        treeID = trees[0].getID();
                        if(rc.canChop(treeID))
                            rc.chop(treeID);
                }
            }
        }
    }
}