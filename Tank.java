package airforcezero;
import battlecode.common.*;

public class Tank {
    static RobotController rc;
    static MapLocation[] initialEnemyLocs;
    static MapLocation[] initialFriendLocs;
    static boolean importantDest = false;
    static int whichDest = -1;
    static int microCreepDir = 1;
    static boolean microCreeping = false;
    static int creepStart = 0;
    static int stoppedCreeping = 0;
    static RobotInfo pastTarget = null;
    static int pastTargetSet = 0;

    public static void run(RobotController rc) throws GameActionException {
        initialEnemyLocs = rc.getInitialArchonLocations(rc.getTeam().opponent());
        initialFriendLocs = rc.getInitialArchonLocations(rc.getTeam());
        Tank.rc = rc;
        pickDest();
        boolean oneArchon = initialEnemyLocs.length == 1;
        int oldLoc = 31;
        while (true) {
            //System.out.println(bugging);
            TreeInfo[] trees = rc.senseNearbyTrees();
            RobotInfo[] robots = rc.senseNearbyRobots();
            BulletInfo[] bullets = rc.senseNearbyBullets();
            MapLocation toMove = null;
            RobotInfo[] friend = new RobotInfo[robots.length];
            int friends = -1;
            RobotInfo[] enemy = new RobotInfo[robots.length];
            int enemies = -1;
            int len = robots.length;
            for (int i = 0; i < len; i++) {
                RobotInfo r = robots[i];
                //if (r.team==Team.NEUTRAL) System.out.println("NEUTRAL DETECTED");
                if (r.team == rc.getTeam()) {
                    friend[++friends] = r;
                } else {
                    enemy[++enemies] = r;
                }
            }
            if (enemies == -1 && rc.getRoundNum() - stoppedCreeping > 50) {
                microCreeping = false;
            }
            //int bcode = Clock.getBytecodeNum();
            if (enemies > 0 || bullets.length != 0 || (enemies == 0 && (enemy[0].type!=RobotType.ARCHON || (oneArchon && !importantDest)))) {
                //if (enemies==-1 || enemy[0].location.distanceTo(rc.getLocation())>10) {
                //    toMove=twoMovetwoDistMicro(rc, trees, friend, friends, enemy, enemies, bullets);
                //} else {
                toMove = oneMoveTwoDistMicro(rc, trees, friend, friends, enemy, enemies, bullets);
                //}
            } else {
                gotoHack:
                {
                    //for (int i = trees.length - 1; i >= 0; i--) {
                    //    if (trees[i].team == rc.getTeam().opponent()) {
                    //        //toMove = rc.approachATree(rc, trees, friend, friends, enemy[0]);
                    //        //break gotoHack;
                    //    }
                    //}
                    toMove = Nav.tankNav(rc, trees, robots);
                }
            }
            //System.out.println("Moving: " + (Clock.getBytecodeNum() - bcode));
            //bcode = Clock.getBytecodeNum();
            shootOrMove(rc, toMove, trees, enemy, enemies, friend, friends, bullets, robots);
            //System.out.println("Shooting: " + (Clock.getBytecodeNum() - bcode));
            //bcode = Clock.getBytecodeNum();

            if (Clock.getBytecodeNum() < 12000) {
                shakeATree(rc);
                int newLoc = rc.readBroadcast(30);
                if (newLoc != oldLoc) {
                    float minDist = rc.getLocation().distanceTo(Nav.dest);
                    MapLocation bestDest = null;
                    int chan = -1;
                    int archonDest = 0;
                    for (int i = oldLoc; i <= newLoc; i++) {
                        int m = rc.readBroadcast(i);
                        if (m != 0) {
                            MapLocation map = getLocation(m);
                            float dist = map.distanceTo(rc.getLocation());
                            if (dist < minDist && dist > 8) {
                                minDist = dist;
                                bestDest = map;
                                chan = i;
                                archonDest = m & 0b10000000;
                            }
                        }
                        if (i == 45) i = 30;
                    }
                    if (bestDest != null) {
                        if (bestDest != Nav.dest) {
                            importantDest = true;
                            Nav.setDest(bestDest);
                            whichDest = chan;
                            if (archonDest != 0) {
                                reportCombatLocation(bestDest, 0b10000000 | roundToTime(rc.getRoundNum()), chan);
                            }
                        }
                        //rc.setIndicatorDot(bestDest,255,0,0);
                    }
                }
                //System.out.println("pickDest " + Clock.getBytecodeNum());
                oldLoc = newLoc;
                if (enemies >= 0) {

                    MapLocation loc = enemy[0].location;
                    gotoHacks:
                    {
                        for (int i = 31; i <= 45; i++) {
                            int m = rc.readBroadcast(i);
                            if (m != 0) {
                                MapLocation map = getLocation(m);
                                if (map.distanceTo(loc) < 8) {
                                    break gotoHacks;
                                }
                            }
                        }
                        //System.out.println("I signaled");
                        //rc.setIndicatorDot(loc,0,0,255);
                        if (enemies == 0 && enemy[0].type == RobotType.ARCHON) {
                            reportCombatLocation(loc, 0b10000000 | roundToTime(rc.getRoundNum()));
                        } else {
                            reportCombatLocation(loc, 0);
                        }
                    }
                }
            }
            //System.out.println("Signaling: " + (Clock.getBytecodeNum() - bcode));
            //bcode = Clock.getBytecodeNum();
            //rc.move(toMove);
            PublicMethods.donateBullets(rc);
            Clock.yield();
        }
    }

    static int roundToTime(int r) {
        return (int) (128 * r / 3000.0);
    }

    static void pickDest() throws GameActionException {
        float minDist = 9999999999f;
        MapLocation bestDest = null;
        int chan = -1;
        int archonDest = 0;
        for (int i = 31; i <= 45; i++) {
            int m = rc.readBroadcast(i);
            if (m != 0) {
                MapLocation map = getLocation(m);
                float dist = map.distanceTo(rc.getLocation());
                if ((m & 0b10000000) != 0) {
                    dist = 100 * (m & 0b1111111) + dist;
                }
                if (dist < minDist && dist > 7) {
                    minDist = dist;
                    bestDest = map;
                    chan = i;
                    archonDest = m & 0b10000000;
                }
            }
        }
        if (bestDest != null) {
            if (bestDest != Nav.dest) {
                importantDest = true;
                Nav.setDest(bestDest);
                whichDest = chan;
                if (archonDest != 0) {
                    reportCombatLocation(bestDest, 0b10000000 | roundToTime(rc.getRoundNum()), chan);
                }
            }
            //rc.setIndicatorDot(bestDest,255,0,0);
        } else {
            importantDest = false;
            Nav.setDest(rc.getLocation().add(new Direction((float) (Math.random() * Math.PI * 2)), 30));
        }
    }

    static MapLocation oneMoveTwoDistMicro(RobotController rc, TreeInfo[] trees, RobotInfo[] friend, int friends, RobotInfo[] enemy, int enemies, BulletInfo[] allBullets) throws GameActionException {
        //int prebyte=Clock.getBytecodeNum();
        int nbullets = 0;
        MapLocation loc = rc.getLocation();
        BulletInfo[] bullets = new BulletInfo[allBullets.length];
        for (int i = allBullets.length - 1; i >= 0; i--) {
            MapLocation oloc = allBullets[i].location;
            float dist = loc.distanceTo(oloc);
            if (dist < 2.5 || Math.asin(2.5 / dist) >= Math.abs(loc.directionTo(oloc).radiansBetween(allBullets[i].dir))) {
                bullets[nbullets++] = allBullets[i];
            }
        }
        nbullets--;
        float[] dists = new float[bullets.length]; //the distance to the first impact
        for (int i = nbullets; i >= 0; i--) {
            BulletInfo b = bullets[i];
            float minDist = b.speed * 2;
            for (int k = trees.length - 1; k >= 0; k--) {
                MapLocation tree = trees[k].location;
                float theta = Math.abs(b.location.directionTo(tree).radiansBetween(b.dir));
                float dist = tree.distanceTo(b.location);
                float r = trees[k].radius;
                if (Math.asin(r / dist) > theta) {
                    double sintheta = Math.sin(theta);
                    float y = (float) Math.asin(dist * sintheta / r) - theta;
                    float impact = (float) (r * Math.sin(y) / sintheta);
                    minDist = Math.min(minDist, impact);
                }
            }
            for (int k = friends; k >= 0; k--) {
                if (friend[k].type == RobotType.TANK || friend[k].type == RobotType.ARCHON) {
                    MapLocation tree = friend[k].location;
                    float theta = Math.abs(b.location.directionTo(tree).radiansBetween(b.dir));
                    float dist = tree.distanceTo(b.location);
                    float r = friend[k].getRadius();
                    if (Math.asin(r / dist) > theta) {
                        double sintheta = Math.sin(theta);
                        float y = (float) Math.asin(dist * sintheta / r) - theta;
                        float impact = (float) (r * Math.sin(y) / sintheta);
                        minDist = Math.min(minDist, impact);
                    }
                }
            }
            dists[i] = minDist + 1.5f;
        }
        MapLocation[] jack = new MapLocation[enemies + 1];
        int jacks = -1;
        int enemySoldiersNTanks = 0;
        for (int i = enemies; i >= 0; i--) {
            if (enemy[i].health > 30 || enemy[i].moveCount != 0 || enemy[i].attackCount != 0)
                switch (enemy[i].type) {
                    case LUMBERJACK:
                        if (enemy[i].location.distanceTo(loc) < 5.25) jack[++jacks] = enemy[i].location;
                        break;
                    case SOLDIER:
                        enemySoldiersNTanks++;
                        break;
                    case TANK:
                        enemySoldiersNTanks += 3;
                }
        }
        //int newByte=Clock.getBytecodeNum();
        //System.out.println("Precomputation: "+(newByte-prebyte));
        int minDamage = 0;
        float minDist = 99999;
        for (int k = nbullets; k >= 0; k--) {
            BulletInfo b = bullets[k];
            float dist = b.location.distanceTo(loc);
            if (Math.asin(2 / dist) > Math.abs(b.location.directionTo(loc).radiansBetween(b.dir))) {
                if (dist < dists[k]) minDamage += b.damage;
            }
        }

        for (int k = jacks; k >= 0; k--) {
            if (loc.distanceTo(jack[k]) <= 3.8) minDamage += 2;
        }
        boolean attackOrRun = (enemySoldiersNTanks >= friends + 2) || (enemySoldiersNTanks > 0 && friends <= 1 && rc.getHealth() < 20); //true means run
        //if (attackOrRun) rc.setIndicatorDot(rc.getLocation(), 0, 0, 0);
        if (attackOrRun) {
            for (int i = enemies; i >= 0; i--) {
                if (enemy[i].type == RobotType.SOLDIER || enemy[i].type == RobotType.TANK)
                    minDist = Math.min(rc.getLocation().distanceTo(enemy[i].location), minDist);
            }
            minDist = -minDist;
        } else {
            for (int i = enemies; i >= 0; i--) {
                minDist = Math.min(enemy[i].type == RobotType.ARCHON ? rc.getLocation().distanceTo(enemy[i].location) * 99 : rc.getLocation().distanceTo(enemy[i].location), minDist);
            }
            if (enemies == -1) minDist = rc.getLocation().distanceTo(Nav.dest);
        }
        MapLocation best = rc.getLocation();
        Direction dir = Direction.getEast();

        for (int i = 8; i > 0; i--) {
            //MapLocation move = rc.getLocation().add(dir, 1.9f);
            //if (!rc.isCircleOccupied(move,1))
            MapLocation move = rc.getLocation().add(dir, 0.5f);
            if (rc.canMove(move)) {
                int damage = 0;
                for (int k = nbullets; k >= 0; k--) {
                    BulletInfo b = bullets[k];
                    float dist = b.location.distanceTo(move);
                    if (dist <= 2 || Math.asin(2 / dist) > Math.abs(b.location.directionTo(move).radiansBetween(b.dir))) {
                        if (dist < dists[k]) damage += b.damage;
                    }
                }

                for (int k = jacks; k >= 0; k--) {
                    if (move.distanceTo(jack[k]) <= 4.8) damage += 2;
                }
                if (damage <= minDamage) {
                    float theDist = 9999999;
                    if (attackOrRun) {
                        for (int x = enemies; x >= 0; x--) {
                            if (enemy[x].type == RobotType.SOLDIER || enemy[x].type == RobotType.TANK)
                                theDist = Math.min(move.distanceTo(enemy[x].location), theDist);
                        }
                        theDist = -theDist;
                    } else {
                        for (int x = enemies; x >= 0; x--) {
                            theDist = Math.min(enemy[x].type == RobotType.ARCHON ? move.distanceTo(enemy[x].location) * 99 : move.distanceTo(enemy[x].location), theDist);
                        }
                    }
                    if (enemies == -1) theDist = move.distanceTo(Nav.dest);
                    if (damage < minDamage || damage == minDamage && theDist < minDist) {
                        minDamage = damage;
                        best = move;
                        minDist = theDist;
                    }

                }
            }
            dir = dir.rotateLeftDegrees(45);
        }
        //System.out.println("Other: "+(Clock.getBytecodeNum()-newByte));
        return best;
    }

    static MapLocation twoMovetwoDistMicro(RobotController rc, TreeInfo[] trees, RobotInfo[] friend, int friends, RobotInfo[] enemy, int enemies, BulletInfo[] allBullets) throws GameActionException {
        //int prebyte=Clock.getBytecodeNum();
        int nbullets = 0;
        MapLocation loc = rc.getLocation();
        BulletInfo[] bullets = new BulletInfo[allBullets.length];
        for (int i = allBullets.length - 1; i >= 0; i--) {
            MapLocation oloc = allBullets[i].location;
            float dist = loc.distanceTo(oloc);
            if (dist < 2.9 || Math.asin(2.9 / dist) >= Math.abs(loc.directionTo(oloc).radiansBetween(allBullets[i].dir))) {
                bullets[nbullets++] = allBullets[i];
            }
        }
        nbullets--;
        float[] dists = new float[bullets.length]; //the distance to the first impact
        for (int i = nbullets; i >= 0; i--) {
            BulletInfo b = bullets[i];
            float minDist = b.speed * 2;
            for (int k = trees.length - 1; k >= 0; k--) {
                MapLocation tree = trees[k].location;
                float theta = Math.abs(b.location.directionTo(tree).radiansBetween(b.dir));
                float dist = tree.distanceTo(b.location);
                float r = trees[k].radius;
                if (Math.asin(r / dist) > theta) {
                    double sintheta = Math.sin(theta);
                    float y = (float) Math.asin(dist * sintheta / r) - theta;
                    float impact = (float) (r * Math.sin(y) / sintheta);
                    minDist = Math.min(minDist, impact);
                }
            }
            for (int k = friends; k >= 0; k--) {
                if (friend[k].type == RobotType.TANK || friend[k].type == RobotType.ARCHON) {
                    MapLocation tree = friend[k].location;
                    float theta = Math.abs(b.location.directionTo(tree).radiansBetween(b.dir));
                    float dist = tree.distanceTo(b.location);
                    float r = friend[k].getRadius();
                    if (Math.asin(r / dist) > theta) {
                        double sintheta = Math.sin(theta);
                        float y = (float) Math.asin(dist * sintheta / r) - theta;
                        float impact = (float) (r * Math.sin(y) / sintheta);
                        minDist = Math.min(minDist, impact);
                    }
                }
            }
            dists[i] = minDist + 0.7f;
        }
        MapLocation[] jack = new MapLocation[enemies + 1];
        int jacks = -1;
        for (int i = enemies; i >= 0; i--) {
            if (enemy[i].type == RobotType.LUMBERJACK) {
                jack[++jacks] = enemy[i].location;
            }
        }
        //int newByte=Clock.getBytecodeNum();
        //System.out.println("Precomputation: "+(newByte-prebyte));
        int minDamage = 0;
        float minDist = 99999;
        for (int k = nbullets; k >= 0; k--) {
            BulletInfo b = bullets[k];
            float dist = b.location.distanceTo(rc.getLocation());
            if (Math.asin(1 / dist) > Math.abs(b.location.directionTo(rc.getLocation()).radiansBetween(b.dir))) {
                if (dist < dists[k]) minDamage += b.damage;
            }
        }

        for (int k = jacks; k >= 0; k--) {
            if (rc.getLocation().distanceTo(jack[k]) <= 4.8) minDamage += 2;
        }
        for (int i = enemies; i >= 0; i--) {
            minDist = Math.min(enemy[i].type == RobotType.ARCHON ? rc.getLocation().distanceTo(enemy[i].location) * 99 : rc.getLocation().distanceTo(enemy[i].location), minDist);
        }
        MapLocation best = rc.getLocation();
        Direction dir = Direction.getEast();
        for (int i = 8; i > 0; i--) {
            MapLocation move = rc.getLocation().add(dir, 1.9f);
            if (!rc.isCircleOccupied(move, 1))
                move = rc.getLocation().add(dir, 0.95f);
            if (rc.canMove(move)) {
                int damage = 0;
                for (int k = nbullets; k >= 0; k--) {
                    BulletInfo b = bullets[k];
                    float dist = b.location.distanceTo(move);
                    if (dist <= 1 || Math.asin(1 / dist) > Math.abs(b.location.directionTo(move).radiansBetween(b.dir))) {
                        if (dist < dists[k]) damage += b.damage;
                    }
                }

                for (int k = jacks; k >= 0; k--) {
                    if (move.distanceTo(jack[k]) <= 4.8) damage += 2;
                }
                if (damage <= minDamage) {
                    float theDist = 99;
                    for (int x = enemies; x >= 0; x--) {
                        theDist = Math.min(enemy[x].type == RobotType.ARCHON ? move.distanceTo(enemy[x].location) * 4 : move.distanceTo(enemy[x].location), theDist);
                    }
                    if (damage < minDamage || damage == minDamage && theDist < minDist) {
                        minDamage = damage;
                        best = move;
                        minDist = theDist;
                    }
                }
            }
            dir = dir.rotateLeftDegrees(45);
        }
        //System.out.println("Other: "+(Clock.getBytecodeNum()-newByte));
        return best;
    }

    //we're using 30-45 for combat
    //30 holds the next location to update
    //info&(1<<7) tells whether this is an archon loc
    //if archon: info&0b1111111 is the # of visits
    static void reportCombatLocation(MapLocation loc, int info) throws GameActionException {
        //if (info>255||info<0) System.out.println("BAD INFO "+info);
        int xpart = ((int) (loc.x * 4)) << 20;
        int ypart = ((int) (loc.y * 4)) << 8;
        int message = xpart | ypart | info;
        int chan = rc.readBroadcast(30);
        int prevChan = chan;
        //MapLocation transmitted=getLocation(message);
        //if (transmitted.distanceTo(loc)>1) {
        //    System.out.println("ERROR");
        //}
        gotoHacks:
        {
            for (int i = 31; i <= 45; i++) {
                if (rc.readBroadcast(i) == 0) {
                    rc.broadcast(i, message);
                    if (i == chan) {
                        chan++;
                    }
                    break gotoHacks;
                }
            }
            rc.broadcast(chan, message);
            chan++;
        }
        if (chan != prevChan) {
            if (chan == 46) chan = 31;
            rc.broadcast(30, chan);
        }
    }

    static void reportCombatLocation(MapLocation loc, int info, int chan) throws GameActionException {
        //if (info>255||info<0) System.out.println("BAD INFO "+info);
        int xpart = ((int) (loc.x * 4)) << 20;
        int ypart = ((int) (loc.y * 4)) << 8;
        int message = xpart | ypart | info;
        rc.broadcast(chan, message);
    }

    static MapLocation getLocation(int m) {
        return new MapLocation((m >>> 20) / 4.0f, ((m & 0b11111111111100000000) >> 8) / 4.0f);
    }

    static void shootOrMove(RobotController rc, MapLocation toMove, TreeInfo[] trees, RobotInfo[] enemy, int enemies, RobotInfo[] friend, int friends, BulletInfo[] bullets, RobotInfo[] robots) throws GameActionException {

        if (toMove != rc.getLocation()) {
            rc.move(toMove);
            trees = rc.senseNearbyTrees();
            enemy = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            enemies = enemy.length - 1;
            friend = rc.senseNearbyRobots(-1, rc.getTeam());
            friends = friend.length - 1;
        }

        if (rc.canFireSingleShot()) {
            MapLocation cur = toMove;
            int treen = trees.length;
            double bestPri = 99999;
            Direction bestShot = null;
            boolean penta = false;
            for (int t = 0; t <= enemies; t++) {
                RobotInfo target = enemy[t];
                float d = toMove.distanceTo(target.location);
                float theta = (float) Math.asin(target.getRadius() / d);
                Direction dir = toMove.directionTo(target.location);
                Direction a1 = dir.rotateLeftRads(theta);
                Direction a2 = dir.rotateRightRads(theta);
                //rc.setIndicatorLine(toMove,toMove.add(a1,2),0,0,255);
                //rc.setIndicatorLine(toMove,toMove.add(a2,2),0,0,255);
                gotoHacks:
                {
                    boolean leftFriend = false;
                    boolean rightFriend = false;
                    for (int i = 0; i <= friends; i++) {
                        float dist = cur.distanceTo(friend[i].getLocation());
                        if (d + 2 < dist) break;
                        //rc.setIndicatorDot(avoid[i].getLocation(),255,0,0);
                        float thetai = (float) Math.asin(friend[i].getRadius() / dist);
                        Direction diri = cur.directionTo(friend[i].location);
                        boolean a1contained = Math.abs(a1.radiansBetween(diri)) <= thetai;
                        boolean a2contained = Math.abs(a2.radiansBetween(diri)) <= thetai;
                        if (a1contained && a2contained || a1.radiansBetween(a2) >= 0) {
                            break gotoHacks;
                        } else if (a1contained) {
                            a1 = diri.rotateRightRads(thetai + 0.0001f);
                            leftFriend = true;
                        } else if (a2contained) {
                            a2 = diri.rotateLeftRads(thetai + 0.0001f);
                            rightFriend = true;
                        }
                    }
                    for (int i = 0; i < treen; i++) {
                        float dist = cur.distanceTo(trees[i].location);
                        if (d + 2 < dist) break;
                        //rc.setIndicatorDot(avoid[i].getLocation(),255,0,0);
                        float thetai = (float) Math.asin(trees[i].radius / dist);
                        Direction diri = cur.directionTo(trees[i].location);
                        boolean a1contained = Math.abs(a1.radiansBetween(diri)) <= thetai;
                        boolean a2contained = Math.abs(a2.radiansBetween(diri)) <= thetai;
                        if (a1contained && a2contained || a1.radiansBetween(a2) >= 0) {
                            break gotoHacks;
                        } else if (a1contained) {
                            a1 = diri.rotateRightRads(thetai + 0.0001f);
                            leftFriend = false;
                        } else if (a2contained) {
                            rightFriend = false;
                            a2 = diri.rotateLeftRads(thetai + 0.0001f);
                        }
                    }
                    //rc.setIndicatorDot(target.location,0,255,0);
                    //rc.setIndicatorLine(toMove,toMove.add(a1.rotateRightDegrees(a2.degreesBetween(a1)/2.0f),8),0,0,255);
                    float degs = a2.degreesBetween(a1) / 2.0f;
                    if (target.type != RobotType.ARCHON || rc.getRoundNum() > 500) {
                        double pri = target.health;
                        switch (target.type) {
                            case ARCHON:
                                pri *= 100;
                                break;
                            case GARDENER:
                                pri *= 6;
                                break;
                            case LUMBERJACK:
                                pri *= 8;
                                break;
                            case SCOUT:
                                pri *= 4;
                                break;
                        }
                        if (pri < bestPri) {
                            bestPri = pri;
                            pastTarget = target;
                            pastTargetSet = rc.getRoundNum();
                            bestShot = a1.rotateRightDegrees(degs);
                            penta = rc.canFirePentadShot() && (degs > 61 || !(leftFriend && rightFriend)) && (target.type == RobotType.SOLDIER || target.type == RobotType.TANK || d < 4.8f && target.type == RobotType.LUMBERJACK || rc.getRoundNum() > 400);
                        }
                    }
                    if (bestShot != null && d <= 2 + target.getRadius()) {
                        break;
                    }
                }
            }
            if (pastTarget != null && bestShot == null && rc.canSensePartOfCircle(pastTarget.location, pastTarget.getRadius()))
                pastTarget = null;
            if (bestShot == null && pastTarget != null && rc.getRoundNum() - pastTargetSet < 15) {
                RobotInfo target = pastTarget;
                float d = toMove.distanceTo(target.location);
                float theta = (float) Math.asin(target.getRadius() / d);
                Direction dir = toMove.directionTo(target.location);
                Direction a1 = dir.rotateLeftRads(theta);
                Direction a2 = dir.rotateRightRads(theta);
                //rc.setIndicatorLine(toMove,toMove.add(a1,2),0,0,255);
                //rc.setIndicatorLine(toMove,toMove.add(a2,2),0,0,255);
                gotoHacks:
                {
                    boolean leftFriend = false;
                    boolean rightFriend = false;
                    for (int i = 0; i <= friends; i++) {
                        float dist = cur.distanceTo(friend[i].getLocation());
                        if (d + 2 < dist) break;
                        //rc.setIndicatorDot(avoid[i].getLocation(),255,0,0);
                        float thetai = (float) Math.asin(friend[i].getRadius() / dist);
                        Direction diri = cur.directionTo(friend[i].location);
                        boolean a1contained = Math.abs(a1.radiansBetween(diri)) <= thetai;
                        boolean a2contained = Math.abs(a2.radiansBetween(diri)) <= thetai;
                        if (a1contained && a2contained || a1.radiansBetween(a2) >= 0) {
                            break gotoHacks;
                        } else if (a1contained) {
                            a1 = diri.rotateRightRads(thetai + 0.0001f);
                            leftFriend = true;
                        } else if (a2contained) {
                            a2 = diri.rotateLeftRads(thetai + 0.0001f);
                            rightFriend = true;
                        }
                    }
                    for (int i = 0; i < treen; i++) {
                        float dist = cur.distanceTo(trees[i].location);
                        if (d + 2 < dist) break;
                        //rc.setIndicatorDot(avoid[i].getLocation(),255,0,0);
                        float thetai = (float) Math.asin(trees[i].radius / dist);
                        Direction diri = cur.directionTo(trees[i].location);
                        boolean a1contained = Math.abs(a1.radiansBetween(diri)) <= thetai;
                        boolean a2contained = Math.abs(a2.radiansBetween(diri)) <= thetai;
                        if (a1contained && a2contained || a1.radiansBetween(a2) >= 0) {
                            break gotoHacks;
                        } else if (a1contained) {
                            a1 = diri.rotateRightRads(thetai + 0.0001f);
                            leftFriend = false;
                        } else if (a2contained) {
                            rightFriend = false;
                            a2 = diri.rotateLeftRads(thetai + 0.0001f);
                        }
                    }
                    //rc.setIndicatorDot(target.location,0,255,0);
                    //rc.setIndicatorLine(toMove,toMove.add(a1.rotateRightDegrees(a2.degreesBetween(a1)/2.0f),8),0,0,255);
                    float degs = a2.degreesBetween(a1) / 2.0f;
                    if (target.type != RobotType.ARCHON || rc.getRoundNum() > 500) {
                        double pri = target.health;
                        switch (target.type) {
                            case ARCHON:
                                pri *= 100;
                                break;
                            case GARDENER:
                                pri *= 6;
                                break;
                            case LUMBERJACK:
                                pri *= 8;
                                break;
                            case SCOUT:
                                pri *= 4;
                                break;
                        }
                        if (pri < bestPri) {
                            bestShot = a1.rotateRightDegrees(degs);
                            penta = rc.canFirePentadShot() && (degs > 61 || !(leftFriend && rightFriend)) && (target.type == RobotType.SOLDIER || target.type == RobotType.TANK || d < 3.81f && target.type == RobotType.LUMBERJACK || rc.getRoundNum() > 400);
                        }
                    }
                }
            }
            if (bestShot != null) {
                if (penta) {
                    rc.firePentadShot(bestShot);
                } else {
                    rc.fireSingleShot(bestShot);
                }
            }
        }
        if (!rc.hasMoved() && !rc.hasAttacked()) {
            if (!microCreeping) {
                creepStart = rc.getRoundNum();
            }
            microCreeping = true;
            if (rc.getRoundNum() - creepStart > 20) {
                rc.move(Nav.tankNav(rc, trees, robots));
                //microCreeping=false;
            } else if (enemies != -1) {
                stoppedCreeping = rc.getRoundNum();
                float minDist = 99;
                RobotInfo closest = null;
                for (int i = enemies; i >= 0; i--) {
                    float dist = enemy[i].location.distanceTo(rc.getLocation());
                    if (dist < minDist) {
                        closest = enemy[i];
                        minDist = dist;
                    }
                }
                Direction theDir = closest.location.directionTo(rc.getLocation());
                if (rc.canMove(theDir.rotateLeftDegrees(microCreepDir * 80), 0.2f)) {
                    rc.move(theDir.rotateLeftDegrees(microCreepDir * 80), 0.2f);
                } else {
                    microCreepDir = -microCreepDir;
                }
            }
        }

    }
    static void shakeATree(RobotController rc) throws GameActionException {
        TreeInfo[] trees = rc.senseNearbyTrees(3);
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
}