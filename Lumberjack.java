package airforcezero;
import battlecode.common.*;

import java.util.Random;
import java.lang.Math;

public class Lumberjack {
    //global stuff

    //constants
    private static final int MIN_GARDENER_RANGE = 2, MAX_GARDENER_RANGE = 6;    //if protecting gardeners, will try to stay between these distances away from them
    private static final float MAX_TREE_UNCERT = 0.5f, PRIORITY_THRESHHOLD = 10f;   //maximum uncertainty when reporting trees

    //general
    private static RobotController rc;
    private static Random rng;

    //variables related to my robot's behavior
    private static MapLocation prevGardenerPos;   //previous gardener sighting
    private static boolean stayNear, isIdle, foundGardener, firstRound, printedThisTurn;

    //info about my tree target
    private static TreeInfo treeInfo;  //used when tree is seen
    private static MapLocation treeLoc; //can be an approximation
    private static boolean nearTree;    //if able to chop tree
    private static int treeChannel;

    //channels
    private static int prevTreeNext, treeNext = -1; //previous and current turn next tree channel to write to

    //info about my surroundings, updates every turn
    private static RobotInfo[] allRobots, friendlyRobots, enemyRobots, friendlyGardeners;
    private static TreeInfo[] allTrees, friendlyTrees, neutralTrees, enemyTrees;
    private static int friendlyRobotCount, enemyRobotCount, friendlyTreeCount, neutralTreeCount, enemyTreeCount, friendlyGardenerCount;
    
    /*
    -2/3 of lumberjacks wander off to search for trees
    -When a neutral or enemy tree is found, report it
    -If nothing to chop, wander/stay near gardeners
    -If trees reported, go to first in list, chop
        
    -TODO:
    -Choosing a good location to stand while cutting trees
    -(Important) running away
    -better travel code
    -bullet dodging
    -strategic determination of which trees to chop
    -combat (if necessary)
    */
    public static void run(RobotController rcArg) throws GameActionException{
        //general
        rc = rcArg;
        rng = new Random(rc.getID());

        //variables related to my robot's behavior
        prevGardenerPos = rc.getInitialArchonLocations(rc.getTeam())[0];
        stayNear = false;
        if (rc.getID() % 3 == 0) {
            stayNear = true;
            System.out.print("\nI protect gardeners.");
        } else {
            System.out.print("\nI explore.");
        }
        isIdle = true;
        foundGardener = false;
        firstRound = true;
        printedThisTurn = true;
        System.out.print("\nI am a lumberjack!");

        //info about my tree target
        setTreeTarget(null, -1);

        //channels
        treeNext = rc.readBroadcast(15);
        if (treeNext == 0) { //in case this is the first lumberjack, set the index to default
            rc.broadcast(15, 16);
            treeNext = 16;
        }
        prevTreeNext = treeNext - 1;
        if (prevTreeNext == 15)
            prevTreeNext = 16;

        //code below repeats every turn
        while (true) {
            //updating info about robots and trees around me
            updateInfo();
            prevTreeNext = treeNext;
            treeNext = rc.readBroadcast(15);

            //report + check trees
            reportTreesInRange(rc, neutralTrees, neutralTreeCount, enemyTrees, enemyTreeCount);

            //gardener update
            foundGardener = false;
            if (stayNear && friendlyGardenerCount > 0) {
                foundGardener = true;
                prevGardenerPos = friendlyGardeners[0].location;
            }

            //check for changes
            checkTreeArrayChange();

            //move
            moveAndChop();
            shakeATree();
            
            if(printedThisTurn){
               System.out.println();
               printedThisTurn = false;
            }

            PublicMethods.donateBullets(rc);
            
            //end of while loop - yield to end
            firstRound = false;
            Clock.yield();
        }
    }

    private static void moveAndChop() throws GameActionException{
        if(isIdle){
            if(stayNear && foundGardener){
                //staying in range of gardeners code
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
                for (int i = 0; i < 10; i++) {    //prevents infinite while loops if stuck
                    Direction d = new Direction(rng.nextFloat() * 2 * (float) Math.PI);
                    if (rc.canMove(d)) {
                        rc.move(d);
                        return;
                    }
                }
            }
            else{
                rc.move(Nav.lumberjackNav(rc, allTrees, allRobots));     //if looking for gardner
                return;
            }
        }
        else{   
            //going after tree code
            float d = rc.getLocation().distanceTo(treeLoc);
            if(treeInfo == null && d <= RobotType.LUMBERJACK.sensorRadius + GameConstants.NEUTRAL_TREE_MAX_RADIUS){  //17
               treeInfo = findTree(treeLoc);  //check tree
               if(treeInfo != null) {
                   treeLoc = treeInfo.location;
                   Nav.setDest(treeLoc);
               }
            }
            if(treeInfo != null && d <= RobotType.LUMBERJACK.bodyRadius + RobotType.LUMBERJACK.strideRadius + treeInfo.radius + GameConstants.INTERACTION_DIST_FROM_EDGE){ //3.25-12.75
                rc.move(approachATree(treeInfo));
            }
            else{
                rc.move(Nav.lumberjackNav(rc, allTrees, allRobots));     //bug to tree
                return;
            }
            if (nearTree) {
                chopTree();   //chop tree
            }
        }
    }
    private static MapLocation approachATree(TreeInfo tree) throws GameActionException{
        //Does exactly what it says.
        //Takes over approaching trees when close (dist <= 1 + maxTreeRadius).
        //Brings robot as close as possible to the tree.
        //Throws exceptions when tree is nonexistent 
        //or not completely inside of view range.
        //Tree location must be exact and not an approximation.
        //Returns location to move to.
        //TODO: make gardeners leave room for others.
        /*if(locAtTree) {
            locAtTree = false;
            Nav.setDest(tree.location.add(tree.location.directionTo(rc.getLocation()), tree.radius + RobotType.LUMBERJACK.bodyRadius));
        }
        if(Nav.lumberjackNav(rc, allTrees, allRobots))
            nearTree = true;
        return Nav.move;*/
        return null;
    }


    //helper methods
    private static void updateInfo(){
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
    private static void checkTreeArrayChange() throws GameActionException{
        if (isIdle) {
            if(prevTreeNext != treeNext) {
                isIdle = false;
                int target;
                target = rc.readBroadcast(prevTreeNext);
                String locationString = "NaN, NaN";
                if(target != 0) {
                    setTreeTarget(target, prevTreeNext);
                    locationString = treeLoc.x + ", " + treeLoc.y;
                }
                System.out.print("\nTree update in the array sensed. Becoming active and going after tree at " + locationString + ".");
                printedThisTurn = true;
            }
        }
        else{
            int diff = treeNext - treeChannel;
            if(rc.readBroadcast(treeChannel) == 0){
                setTreeTarget(null, -1);
                if(rc.readBroadcast(treeNext) == 0 && (diff == 1 || diff == -13)){
                    System.out.print("\nTree target was removed and no other targets detected. Becoming idle.");
                    printedThisTurn = true;
                    isIdle = true;
                    if(stayNear)
                        Nav.setDest(prevGardenerPos);   //edit me later
                    else{
                        pickDest(true);
                    }
                }
                else{
                    chooseBestTree();
                    System.out.print("\nTree target was removed. Going after tree at " + treeLoc.x + ", " + treeLoc.y + ".");
                    printedThisTurn = true;
                }
            }
        }
    }
    private static TreeInfo findTree(MapLocation approx){
        //finds tree within max uncertainty of approx
        for(TreeInfo info : enemyTrees){
            if(info == null)
                break;

            float dx = info.location.x - approx.x;
            if(dx < 0)
                dx = 0 - dx;

            float dy = info.location.y - approx.y;
            if(dy < 0)
                dy = 0 - dy;

            float dmax = dx;
            if(dy > dx)
                dmax = dy;

            if(dmax <= MAX_TREE_UNCERT)   //if goes below max uncert limit
                return info;
        }
        for(TreeInfo info : neutralTrees){
            if(info == null)
                break;

            float dx = info.location.x - approx.x;
            if(dx < 0)
                dx = 0 - dx;

            float dy = info.location.y - approx.y;
            if(dy < 0)
                dy = 0 - dy;

            float dmax = dx;
            if(dy > dx)
                dmax = dy;

            if(dmax <= MAX_TREE_UNCERT)   //if goes below max uncert limit
                return info;
        }
        return null;
    }
    private static void chopTree() throws GameActionException{
        //chops trees
        boolean reset = false;
        if (treeInfo.getHealth() <= GameConstants.LUMBERJACK_CHOP_DAMAGE) {
            System.out.print("\nChopped down my target!");
            printedThisTurn = true;
            rc.broadcast(treeChannel, 0);
            reset = true;

            //not using setTreeTarget because still need to keep treeInfo
            treeLoc = null;
            nearTree = false;
            treeChannel = -1;
        }
        rc.chop(treeInfo.getID());
        if(reset) {
            treeInfo = null;
        }
        else{   //update the health of tree
            treeInfo = new TreeInfo(treeInfo.ID, treeInfo.team, treeInfo.location, treeInfo.radius, treeInfo.health - GameConstants.LUMBERJACK_CHOP_DAMAGE, treeInfo.containedBullets, treeInfo.containedRobot);
        }
    }

    //tree handling stuff
    public static void reportTreesInRange(RobotController rc, TreeInfo[] neutral, int neutralCount, TreeInfo[] enemy, int enemyCount) throws GameActionException{
        //Please use this method!
        //specify the arrays of and numbers of neutral and enemy trees within your vision,
        //and this method will handle everything else
        if (enemyCount + neutralCount > 0) {
            if(rc.getType() == RobotType.LUMBERJACK)
                treeNext = rc.readBroadcast(15);

            //for later reference
            Integer[] locationValueArray = new Integer[enemyCount + neutralCount];
            TreeInfo[] treeArray = new TreeInfo[enemyCount + neutralCount];

            //goes through all trees and combines arrays, while computing int from location
            int index = 0;
            for (TreeInfo info : enemy) {
                if(info == null)
                    break;
                locationValueArray[index] = locationToInt(info.location);
                treeArray[index] = info;
                index++;
            }
            for (TreeInfo info : neutral) {
                if(info == null)
                    break;
                locationValueArray[index] = locationToInt(info.location);
                treeArray[index] = info;
                index++;
            }
            //here it checks if trees have already been reported
            if (rc.getType() != RobotType.LUMBERJACK || (firstRound || !isIdle || prevTreeNext != treeNext)) {  //the beginning of the game, or trees already in array (for lumberjacks)
                for (int i = 16; i < 30; i++) {
                    int existingTreeLocationVal = isolateLocation(rc.readBroadcast(i));
                    for (int i2 = 0; i2 < locationValueArray.length; i2++) {
                        Integer newTreeLocationVal = locationValueArray[i2];
                        if (newTreeLocationVal != null) {
                            if (newTreeLocationVal == existingTreeLocationVal)
                                locationValueArray[i2] = null;   //removes tree to report
                        }
                    }
                }
            }

            for (int i = 0; i < locationValueArray.length; i++) {
                Integer locationValue = locationValueArray[i];
                if (locationValue == null)
                    continue;
                rc.broadcast(treeNext, priorityToInt(staticPriorityOfTree(treeArray[i])) + locationValue);
                System.out.print("\nReported tree " + treeArray[i].getID() + " to array location " + treeNext + ".");
                printedThisTurn = true;
                treeNext++;
                if(treeNext == 30)
                    treeNext = 16;
            }
            rc.broadcast(15, treeNext);
        }
    }
    private static void chooseBestTree() throws GameActionException{
        int bestPriority = -1;
        int bestChannel = -1;
        int bestValue = -1;

        for(int i = 16; i < 30; i++){
            int value = rc.readBroadcast(i);
            if(value == 0)
                continue;
            int priority = intToPriority(value) + dynamicPriorityOfTree(intToLocation(value));

            if(priority > bestPriority){
                bestPriority = priority;
                bestChannel = i;
                bestValue = value;
            }
        }

        setTreeTarget(bestValue, bestChannel);
    }
    private static int staticPriorityOfTree(TreeInfo tree){
        return 0;
    }
    private static int dynamicPriorityOfTree(MapLocation treeLoc){
        return 0;
    }
    private static void setTreeTarget(TreeInfo tree, int channel){
        treeInfo = tree;
        nearTree = false;
        if(tree == null){
            treeLoc = null;
            treeChannel = -1;
        }
        else{
            treeLoc = tree.location;
            treeChannel = channel;
        }
        Nav.setDest(treeLoc);
    }
    private static void setTreeTarget(int value, int channel){  //alternative way of setting target by integer value
        treeInfo = null;
        nearTree = false;
        treeLoc = intToLocation(value);
        treeChannel = channel;
        Nav.setDest(treeLoc);
    }
    //conversions
    private static int isolateLocation(int i){
        return i % 1000000;
    }
    private static int isolatePriority(int i){
        return i / 1000000 * 1000000;
    }
    private static int locationToInt(MapLocation loc){
        return 1000 * round(loc.x) + round(loc.y);
    }
    private static int priorityToInt(int p){
        return 1000000 * p;
    }
    private static MapLocation intToLocation(int i){
        return new MapLocation((i / 1000) % 1000, i % 1000);
    }
    private static int intToPriority(int i){
        return i / 1000000;
    }

    //integration methods
    public static void pickDest(boolean reached) {
        Nav.setDest(rc.getLocation().add(new Direction(rng.nextFloat() * 2 * (float) Math.PI), 20));   //explorer code
    }
    private static void shakeATree() throws GameActionException {   //shakes a tree within range
        TreeInfo[] trees = rc.senseNearbyTrees(2);
        if (trees.length == 0) return;
        int maxBullets = trees[0].containedBullets;
        int bestTree = 0;
        for (int i = trees.length - 1; i != 0; i--) {
            if (trees[i].containedBullets > maxBullets) {
                maxBullets = trees[i].containedBullets;
                bestTree = i;
            }
        }
        rc.shake(trees[bestTree].ID);
    }

    //methods to preserve bytecodes
    private static int round(double a){   //should take less bytecodes than Math.round
        return (int) (a+0.5);
    }
}