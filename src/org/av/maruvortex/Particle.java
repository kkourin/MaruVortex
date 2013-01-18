package org.av.maruvortex;

import java.util.Random;

import android.util.Log;
// General particle class for particle objects
abstract public class Particle {
    int radius;
    double x, y, dx, dy, angle;
    int screenLength, screenHeight;
    protected static final double EPSILON = 0.00005;
    double c=5; 

    public boolean onscreen() { //Checks if particle in on screen
        return !(x > screenLength+20 || x < -20 || y > screenHeight+20 || y < -20);
    }
    public void update(double dt) { // default updater, usually overrided depending on class
        double norm = Math.sqrt(dx*dx + dy*dy);
        dx *= c/norm;
        dy *= c/norm;
        if (Math.abs(dx)<EPSILON) dx = 0; 
        if (Math.abs(dy)<EPSILON) dy = 0;
        x += dx*dt;
        y += dy*dt;
        angle = Math.toDegrees(Math.atan2(dy, dx));
    }
    public int getx() {
        return (int)Math.round(x);
    }

    public int gety() {
        return (int)Math.round(y);
    }
    public float getAngle() {
        return (float)angle;
    }
    public void updateAngle() { // Updates angle of particle depending on velocity
        angle = Math.toDegrees(Math.atan2(dy, dx)); 
    }
    public int getRadius() {
        return radius;
    }
}
// Character class
class Character extends Particle {
    int w,h;
    public Character(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        dx = dy = 0;
        radius = 10;
    }
    public void updateOrientation(double pitch, double roll) { //Updates orientation by accelrometer values
        double absp = Math.abs(pitch /= -30);
        double absr = Math.abs(roll = (-30-roll)/20);
        // non-linear response
        if (absp<=1) dx = Math.signum(pitch)*Math.pow(absp,1.2)*w*0.7;
        if (Math.abs(dx) < 2) dx = 0;
        if (absr<=1) dy = Math.signum(roll)*Math.pow(absr,1.2)*h*0.7;
        if (Math.abs(dy) < 2) dy = 0;
    }

    public void update(double dt) { // Updates coordinates based on velocity
        x += dx*dt;
        y += dy*dt;
        if (x < radius) x = radius;
        else if (x > w-radius) x = w-radius;
        if (y < radius) y = radius;
        else if (y > h-radius) y = h-radius;
        angle = Math.toDegrees(Math.atan2(dy, dx));
    }
}
class Bullet extends Particle {
    int x0, y0;
    int targetX, targetY;

    public Bullet(int targetX, int targetY, int x0, int y0, int h, int l) {
        c = 250;
        radius = 4;
        this.targetX = targetX;
        this.targetY = targetY;
        this.x0 = x0;
        this.y0 = y0;
        x = x0;
        y = y0;
        this.screenLength = l;
        this.screenHeight = h;
        dx = ((double)targetX-x0);
        dy = ((double)targetY-y0);
        double norm = Math.sqrt(dx*dx + dy*dy);
        dx *= c/norm;
        dy *= c/norm;
        if (Math.abs(dx)<EPSILON) dx = 0;
        if (Math.abs(dy)<EPSILON) dy = 0;

        angle = Math.toDegrees(Math.atan2(dy, dx));
        //Log.d("STUFF", "angle0: " + angle);
    }

}
class BoxParticle extends Particle {
    Random r;

    private int dir; //0 for left right, 1 for down up, 2 for right left, 3 for up down

    public BoxParticle(Random r, int h, int l, int mcx, int mcy) { //Constructor for box particle
        c = 40;
        radius = 13;
        this.screenLength = l;
        this.screenHeight = h;
        dir = r.nextInt(4);
        do{ 
            if (dir == 0){dx = 50; x = 0; y = r.nextInt(h);}  //Randomizing where particle starts
            else if (dir == 1){dy = -50; x = r.nextInt(l); y = h;}
            else if (dir == 2){dx = -50; x = l; y = r.nextInt(h);}
            else if (dir == 3){dy = 50; x = r.nextInt(l); y = 0;}
        } while (Math.abs(x - mcx) < 50 && Math.abs(y - mcy) < 50);  //Prevents enemy spawning too close
        angle = Math.toDegrees(Math.atan2(dy, dx));

    }
    @Override
    public void update(double dt){
        x += dx*dt;
        y += dy*dt;
    }

}
class ParabolicParticle extends Particle { //Constructor for parabolic particle
    int dir;
    public ParabolicParticle(Random r, int h, int l, int mcx, int mcy) {
        c = 40;
        radius = 11;
        this.screenLength = l;
        this.screenHeight = h;
        dir = r.nextInt(4);
        do {  //Randomizing wall where particle starts
            if (dir == 0){dy = 70; dx = 20; x = 0; y = r.nextInt(h);}  
            else if (dir == 1){dx = 70; dy = -20; x = r.nextInt(l); y = h;}
            else if (dir == 2){dy = 70; dx = -20; x = l; y = r.nextInt(h);}
            else if (dir == 3){dx = 70; dy = 20; x = r.nextInt(l); y = 0;}
        } while (Math.abs(x - mcx) < 50 && Math.abs(y - mcy) < 50); //Prevents enemy spawning too close
        angle = Math.toDegrees(Math.atan2(dy, dx));

    }
}

class TurningParticle extends Particle {
    int dir;
    int t;
    public TurningParticle(Random r, int h, int l, int mcx, int mcy) {
        c = 40;
        radius = 13;
        this.screenLength = l;
        this.screenHeight = h;
        dir = r.nextInt(4);
        do{ 
            if (dir == 0){dx = 50; x = 0; y = r.nextInt(h);} 
            else if (dir == 1){dy = -50; x = r.nextInt(l); y = h;}
            else if (dir == 2){dx = -50; x = l; y = r.nextInt(h);}
            else if (dir == 3){dy = 50; x = r.nextInt(l); y = 0;}
        } while (Math.abs(x - mcx) < 50 && Math.abs(y - mcy) < 50); //Prevents enemy spawning too close
        angle = Math.toDegrees(Math.atan2(dy, dx));

    }
    @Override
    public void update(double dt) {

        if (dir == 1 || dir == 3) // Curves in different direction depending on dir
            dx+=10*Math.sin(t/25);
        else
            dy+=10*Math.sin(t++/25);
        double norm = Math.sqrt(dx*dx + dy*dy);
        dx *= c/norm;
        dy *= c/norm;
        if (Math.abs(dx)<EPSILON) dx = 0;
        if (Math.abs(dy)<EPSILON) dy = 0;
        x += dx*dt;
        y += dy*dt;
        angle = Math.toDegrees(Math.atan2(dy, dx));

    }

}
class HomingParticle extends Particle {
    Random r;
    float noise;
    public HomingParticle(Random r, double c, float noise, int h, int l, int mcx, int mcy) { //Constructor for homing particle
        this.r = r;
        this.c = c;
        this.noise = noise;
        this.screenLength = l;
        this.screenHeight = h;
        radius = 13;
        do {
            x = r.nextInt(l);
            y = r.nextInt(h);
        } while (Math.abs(x-mcx)<50 && Math.abs(y-mcy)<50); //Prevents enemy spawning too close
        updateMC(mcx, mcy);
    }

    public void updateMC(int mcx, int mcy) { // Updates velocity to follow character
        // with some noise
        dx = mcx-x+2*noise*r.nextFloat()-noise; 
        dy = mcy-y+2*noise*r.nextFloat()-noise;
        //Log.d("MaruVortex", "dx = " + dx + ", dy = " + dy);
    }
}