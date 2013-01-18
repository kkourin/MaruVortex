package org.av.maruvortex;
// Class for the berzerk powerup positions and radius
public class BerzerkUp {
    private int x, y;
    private int radius = 12;

    public BerzerkUp(int x, int y){ // Constructor of berzerk powerup 
	this.x = x;
	this.y = y;
    }
    public int getx(){
	return x;
    }
    public int gety(){
	return y;
    }
    public int getRadius(){
	return radius;
    }
}
