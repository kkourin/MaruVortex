package org.av.maruvortex;

import android.os.Bundle;
import android.app.Activity;

import java.util.*;
import android.os.SystemClock;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

public class MaruVortex extends Activity {
    SensorManager mSensorManager;
    Panel _p;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	requestWindowFeature(Window.FEATURE_NO_TITLE);
	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		WindowManager.LayoutParams.FLAG_FULLSCREEN);
	setContentView(_p = new Panel(this));
    }

    @Override
    protected void onResume() {
	super.onResume();
	_p.start();
    }

    @Override
    protected void onPause() {
	super.onPause();
	_p.stop();
    }

    class Panel extends SurfaceView implements SurfaceHolder.Callback, SensorEventListener {
	private DrawThread _thread;
	
	// GAME VARS
	private int level = 1;
	private int score = 0;
	private volatile boolean over = false;
	private static final String OVER_STR = "GAME OVER";
	private static final String OVER_STR_2 = "TOUCH TO RESTART";
	private static final String BERZERK_STR = "BERZERK MODE ACTIVATED";
	private Random r = new Random();
	private volatile long berzerkStart;
	private int berzerkLength = 5000;
	private volatile boolean berzerk = false;

	// SENSOR VARs
	private Sensor mAccel, mCompass;
	private volatile boolean sensorsReady = false;
	private float[] accelValues = new float[3];
	private float[] compassValues = new float[3];
	private float[] inR = new float[9];
	private float[] inclineMatrix = new float[9];
	private float[] prefValues = new float[3];
	// in degrees
	private volatile float pitch = Float.NaN;
	private volatile float roll = Float.NaN;

	// PAINT VARS
	private Paint redTextPaint = new Paint();
	private Paint blackTextPaint = new Paint();
	private Paint whiteTextPaint = new Paint();
	private Paint largeTextPaint = new Paint();
	private Paint textPaint;
	private Paint bitmapPaint;
	private Paint aA = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint invert = new Paint();
	private float mx[] = { -1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, -1.0f, 0.0f, 1.0f,
		1.0f, 0.0f, 0.0f, -1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f,
		0.0f };
	
	// TIME VARS
	private volatile long t = -1000, f = -1000, g = -1000; // f is square particle, g is bullet

	private volatile Character mc = null; // main character
	// PARTICLE CONTAINERS
	private HashSet<BoxParticle> squares = new HashSet<BoxParticle>();
	private HashSet<Bullet> bullets = new HashSet<Bullet>();
	private HashSet<ParabolicParticle> parabolics = new HashSet<ParabolicParticle>();
	private HashSet<TurningParticle> turns = new HashSet<TurningParticle>();
	private HashSet <BerzerkUp> berzerkUps = new HashSet <BerzerkUp>();
	private HashSet<Bullet> rms = new HashSet<Bullet>();
	private HashSet<BoxParticle> rms2 = new HashSet<BoxParticle>();
	private HashSet<ParabolicParticle> rms3 = new HashSet<ParabolicParticle>();
	private HashSet<TurningParticle> rms4 = new HashSet<TurningParticle>();
	private HashSet <BerzerkUp> rms5 = new HashSet <BerzerkUp>();

	// TOUCH LOCATION
	private volatile int _x, _y;
	private volatile boolean firing = false;

	// BITMAP RELATED VARS
	private Matrix matrix = new Matrix();
	private Bitmap mcBitmap = BitmapFactory.decodeResource(getResources(),
		R.drawable.mc);
	private Bitmap squareBitmap = BitmapFactory.decodeResource(getResources(),
		R.drawable.square);
	private Bitmap parabolicBitmap = BitmapFactory.decodeResource(getResources(),
		R.drawable.parabolic);
	private Bitmap bulletBitmap = BitmapFactory.decodeResource(getResources(),
		R.drawable.bullet);
	private Bitmap turningBitmap = BitmapFactory.decodeResource(getResources(),
		R.drawable.turning);
	private Bitmap berzerkBitmap = BitmapFactory.decodeResource(getResources(),
		R.drawable.berzerk);
	private int bulletW, bulletH, squareW, squareH, parabolicW, parabolicH,
	turningW, turningH, berzerkW, berzerkH;
	private int screenW, screenH;

	// DEBUG TAGS
	private static final String LOG_TAG = "MaruVortex";

	private int sq(int x) {
	    return x * x;
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
	    // occ
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
	    setKeepScreenOn(true);
	    _thread.setRunning(true);
	    _thread.start();
	    whiteTextPaint.setColor(Color.WHITE);
	    whiteTextPaint.setAntiAlias(true);
	    whiteTextPaint.setTextSize(14);
	    blackTextPaint.setColor(Color.BLACK);
	    blackTextPaint.setAntiAlias(true);
	    blackTextPaint.setTextSize(14);
	    largeTextPaint.setColor(Color.WHITE);
	    largeTextPaint.setAntiAlias(true);
	    largeTextPaint.setTextSize(32);
	    redTextPaint.setColor(Color.RED);
	    redTextPaint.setAntiAlias(true);
	    redTextPaint.setTextSize(18);
	    ColorMatrix cm = new ColorMatrix(mx);
	    invert.setColorFilter(new ColorMatrixColorFilter(cm));
	    invert.setAntiAlias(true);
	    bulletW = bulletBitmap.getWidth();
	    bulletH = bulletBitmap.getHeight();
	    squareW = squareBitmap.getWidth();
	    squareH = squareBitmap.getHeight();
	    parabolicW = parabolicBitmap.getWidth();
	    parabolicH = parabolicBitmap.getHeight();
	    turningW = turningBitmap.getWidth();
	    turningH = turningBitmap.getHeight();
	    berzerkW = berzerkBitmap.getWidth();
	    berzerkH = berzerkBitmap.getHeight();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
	    stop();
	    _thread.setRunning(false);
	    while (true) {
		try {
		    _thread.join();
		    break;
		} catch (InterruptedException e) {

		}
	    }

	}

	public Panel(Context context) {
	    super(context);
	    getHolder().addCallback(this);
	    mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    mCompass = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	    assert (mAccel != null && mCompass != null); // otherwise we are screwed
	    _thread = new DrawThread(getHolder(), this);
	    setFocusable(true);
	}

	public void start() {
	    // register sensors
	    mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_GAME);
	    mSensorManager.registerListener(this, mCompass, SensorManager.SENSOR_DELAY_GAME);
	    pitch = roll = Float.NaN;
	    for (int i=0;i<3;i++)
		accelValues[i] = compassValues[i] = 0;
	    sensorsReady = false;
	}

	public void stop() {
	    mSensorManager.unregisterListener(this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	    // akb
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
	    switch (event.sensor.getType()) { // both accelerometer and magnetic data are needed to compute orientation
	    case Sensor.TYPE_ACCELEROMETER:
		System.arraycopy(event.values, 0, accelValues, 0, 3);
		if (compassValues[0]!=0) sensorsReady = true;
		break;
	    case Sensor.TYPE_MAGNETIC_FIELD:
		System.arraycopy(event.values, 0, compassValues, 0, 3);
		if (accelValues[2]!=0) sensorsReady = true;
		break;
	    default:
		break;
	    }
	    if (sensorsReady && SensorManager.getRotationMatrix(inR, inclineMatrix, accelValues, compassValues)) {
		SensorManager.getOrientation(inR, prefValues);
		pitch = (float) Math.toDegrees(prefValues[1]);
		roll = (float) Math.toDegrees(prefValues[2]);
		if (mc != null)
		    mc.updateOrientation(pitch, roll);
	    }
	}

	@Override
	public void onDraw(Canvas canvas) {
	    long nt = SystemClock.elapsedRealtime();
	    if (canvas == null) return;
	    if (t < 0) t = nt;

	    screenW = canvas.getWidth();
	    screenH = canvas.getHeight();

	    if(berzerk && nt - berzerkStart > berzerkLength)
		berzerk = false;
	    if (mc == null) mc = new Character(screenW/2, screenH - 30, screenW, screenH);
	    if (!over) {
		//add Enemies
		if (firing && nt - g >= 100) {
		    bullets.add(new Bullet(_x, _y, mc.getx(), mc.gety(), screenH, screenW));
		    g = nt;
		}
		if (nt - f >= 1000) {
		    if (level < 2)
			squares.add(new BoxParticle(r, screenH, screenW, _x, _y));
		    else if (level < 3) {
			parabolics.add(new ParabolicParticle(r, screenH, screenW, _x, _y));
			turns.add(new TurningParticle(r, screenH, screenW, _x, _y));
		    } else {
			turns.add(new TurningParticle(r, screenH, screenW, _x, _y));
			squares.add(new BoxParticle(r, screenH, screenW, _x, _y));
			parabolics.add(new ParabolicParticle(r, screenH, screenW, _x, _y));
		    }
		    f = nt;

		}
		//stop add Enemies
		rms.clear();
		rms2.clear();
		rms3.clear();
		rms4.clear();
		//Onscreen checking
		for (Bullet i : bullets)
		    if (!i.onscreen())
			rms.add(i);
		for (BoxParticle i : squares)
		    if (!i.onscreen())
			rms2.add(i);
		for (ParabolicParticle i : parabolics)
		    if (!i.onscreen())
			rms3.add(i);
		for (TurningParticle i : turns)
		    if (!i.onscreen())
			rms4.add(i);
		bullets.removeAll(rms);
		squares.removeAll(rms2);
		parabolics.removeAll(rms3);
		turns.removeAll(rms4);
		rms.clear();
		rms2.clear();
		rms3.clear();
		rms4.clear();
		//end Onscreen checking
		//Enemy collision with bullets
		for (Bullet i : bullets) {
		    for (BoxParticle j : squares) {
			if (sq(i.getx() - j.getx()) + sq(i.gety() - j.gety()) <= sq(i.getRadius() + j.getRadius())) {
			    rms.add(i);
			    rms2.add(j);
			    if (r.nextInt(20) == 0)
				berzerkUps.add(new BerzerkUp(j.getx(), j.gety()));
			    score++;
			}

		    }
		    for (ParabolicParticle j : parabolics) {
			if (sq(i.getx() - j.getx()) + sq(i.gety() - j.gety()) <= sq(i.getRadius() + j.getRadius())) {
			    rms.add(i);
			    rms3.add(j);
			    if (r.nextInt(20) == 0)
				berzerkUps.add(new BerzerkUp(j.getx(), j.gety()));
			    score++;
			}
		    }
		    for (TurningParticle j : turns) {
			if (sq(i.getx() - j.getx()) + sq(i.gety() - j.gety()) <= sq(i.getRadius() + j.getRadius())) {
			    rms.add(i);
			    rms4.add(j);
			    if (r.nextInt(20) == 0)
				berzerkUps.add(new BerzerkUp(j.getx(), j.gety()));
			    score++;
			}
		    }
		}
		squares.removeAll(rms2);
		parabolics.removeAll(rms3);
		turns.removeAll(rms4);
		bullets.removeAll(rms);
		rms.clear();
		rms2.clear();
		rms3.clear();
		rms4.clear();
		//end Enemy collision with bullets
		//Enemy collision with character
		// one cannot die in berzerk mode
		for (BoxParticle j : squares)
		    if (sq(mc.getx() - j.getx()) + sq(mc.gety() - j.gety()) <= sq(mc.getRadius() + j.getRadius())) {
			if (berzerk) {
			    rms2.add(j);
			    score++;
			} else
			    over = true;
		    }
		for (ParabolicParticle j : parabolics)
		    if (sq(mc.getx() - j.getx()) + sq(mc.gety() - j.gety()) <= sq(mc.getRadius() + j.getRadius())) {
			if (berzerk) {
			    rms3.add(j);
			    score++;
			} else
			    over = true;
		    }

		for (TurningParticle j : turns)
		    if (sq(mc.getx() - j.getx()) + sq(mc.gety() - j.gety()) <= sq(mc.getRadius() + j.getRadius())) {
			if (berzerk) {
			    rms4.add(j);
			    score++;
			} else
			    over = true;
		    }
		if (berzerk) {
		    squares.removeAll(rms2);
		    parabolics.removeAll(rms3);
		    turns.removeAll(rms4);
		    over = false;
		}
		//end Enemy collision with character
		if (over) {
		    berzerk = false;
		    stop();
		}
		//Berzerk powerup collision
		if (!berzerk) {
		    for (BerzerkUp i : berzerkUps)
			if (sq(mc.getx() - i.getx()) + sq(mc.gety() - i.gety()) <= sq(mc.getRadius() + i.getRadius())) {
			    berzerk = true;
			    berzerkStart = SystemClock.elapsedRealtime();
			    rms5.add(i);
			}

		}
		berzerkUps.removeAll(rms5);
		rms5.clear();
		//end Berzerk powerup collision
	    }
	    //Berzerk events
	    if (berzerk&&!over) {
		bitmapPaint = invert;
		textPaint = blackTextPaint;
		canvas.drawColor(Color.WHITE);
	    } else {
		bitmapPaint = aA;
		textPaint = whiteTextPaint;
		canvas.drawColor(Color.BLACK);
	    }
	    //end Berzerk events

	    if (!over) {
		//Rendering particles
		for (BoxParticle i : squares) {
		    i.update(((double) (nt - t)) / 1000);
		    matrix.setRotate(i.getAngle(), squareW/2, squareH/2);
		    matrix.postTranslate(i.getx() - squareW/2, i.gety() - squareH / 2);
		    canvas.drawBitmap(squareBitmap, matrix, bitmapPaint);
		}
		for (ParabolicParticle i : parabolics) {
		    i.update(((double) (nt - t)) / 1000);
		    matrix.setRotate(i.getAngle(), parabolicW / 2, parabolicH / 2);
		    matrix.postTranslate(i.getx() - parabolicW / 2, i.gety() - parabolicH / 2);
		    canvas.drawBitmap(parabolicBitmap, matrix, bitmapPaint);
		}
		for (TurningParticle i : turns) {
		    i.update(((double) (nt - t)) / 1000);
		    matrix.setRotate(i.getAngle(), turningW / 2, turningH / 2);
		    matrix.postTranslate(i.getx() - turningW / 2, i.gety() - turningH / 2);
		    canvas.drawBitmap(turningBitmap, matrix, bitmapPaint);
		}

		for (Bullet i : bullets) {
		    i.update(((double) (nt - t)) / 1000);

		    matrix.setRotate(i.getAngle(), bulletW / 2, bulletH / 2);
		    matrix.postTranslate(i.getx() - bulletW / 2, i.gety() - bulletH / 2);
		    canvas.drawBitmap(bulletBitmap, matrix, bitmapPaint);
		}
		for (BerzerkUp i : berzerkUps) {
		    canvas.drawBitmap(berzerkBitmap, i.getx() - berzerkW / 2, i.gety() - berzerkH / 2, bitmapPaint);
		}

		mc.update(((double) (nt - t)) / 1000);
		canvas.drawBitmap(mcBitmap, mc.getx() - mcBitmap.getWidth() / 2, mc.gety()
			- mcBitmap.getHeight() / 2, bitmapPaint);
		//end Rendering particles
		//leveling
		if (score >= 100)
		    level = 3;
		else if (score >= 25) 
		    level = 2;

	    }
	    //end leveling
	    //Text rendering 
	    canvas.drawText("Score: " + score, 50, 25, textPaint);
	    canvas.drawText("Level: " + level, screenW - 100, 25, textPaint);
	    if (over) {
		canvas.drawText(OVER_STR, (screenW-largeTextPaint.measureText(OVER_STR))/2, screenH/2-12, largeTextPaint);
		canvas.drawText(OVER_STR_2, (screenW-largeTextPaint.measureText(OVER_STR_2))/2, screenH/2+16, largeTextPaint);
	    } else if (berzerk)
		canvas.drawText(BERZERK_STR, (screenW-redTextPaint.measureText(BERZERK_STR))/2, 25, redTextPaint);
	    //end Text rendering 
	    t = nt;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
	    if (over && event.getAction() == MotionEvent.ACTION_DOWN) { // restart game
		mc = null;
		level = 1;
		score = 0;
		t = f = g = -1000;
		squares.clear();
		bullets.clear();
		parabolics.clear();
		turns.clear();
		firing = over = false;
		start();
	    } else {
		_x = (int) event.getX();
		_y = (int) event.getY();
		if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) { // fire bullet
		    if (!firing) g = -1000;
		    firing = true;
		} else firing = false;
	    }
	    return true;
	}

    }

    class DrawThread extends Thread {
	private SurfaceHolder _surfaceHolder;
	private Panel _panel;
	private boolean _run = false;
	public DrawThread(SurfaceHolder surfaceHolder, Panel panel) {
	    _surfaceHolder = surfaceHolder;
	    _panel = panel;

	}

	public void setRunning(boolean run) {
	    _run = run;
	}

	@Override
	public void run() {
	    Canvas c = null;
	    while (_run) {
		c = null;
		try {
		    c = _surfaceHolder.lockCanvas();
		    synchronized (_surfaceHolder) {
			_panel.onDraw(c);
		    }
		} finally {
		    if (c != null)
			_surfaceHolder.unlockCanvasAndPost(c);
		}
	    }
	}
    }

}
