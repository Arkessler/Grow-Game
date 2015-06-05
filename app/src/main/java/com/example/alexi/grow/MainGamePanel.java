package com.example.alexi.grow; /**
 * Created by Alexi on 5/28/2015.
 */
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.alexi.grow.model.Droid;

public class MainGamePanel extends SurfaceView implements SurfaceHolder.Callback
{
    private static final String TAG = MainGamePanel.class.getSimpleName();
    private MainThread thread;
    private Droid droid;
    private String averageFPS;

    public MainGamePanel(Context context)
    {
        super(context);
        //add the callback (this) to the surface holder to intercept events
        getHolder().addCallback(this);
        //create character
        Bitmap charBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.droid_1);
        droid = new Droid(charBitmap, 50, 50);
        //create the game loop thread
        thread = new MainThread(getHolder(), this);
        //make the GamePanel focusable so it can handle events
        setFocusable(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        //tell the thread to shut down and wait for it to finish
        //clean shutdown
        boolean retry = true;
        while (retry)
        {
            try
            {
                thread.join();
                retry = false;
            }
            catch (InterruptedException e)
            {
                //try again to shut down the thread
            }
        }
        Log.d(TAG, "Thread was shut down cleanly");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        Log.d(TAG, "Handling touch event");
        if (event.getAction() == event.ACTION_DOWN) {
            Log.d(TAG, "Event is a down action");
            droid.handleActionDown((int) event.getX(), (int) event.getY());
            //check if in the lower part of the screen we exit
            if (event.getY() > getHeight() - 50) {
                thread.setRunning(false);
                ((Activity) getContext()).finish();
            } else {
                Log.d(TAG, "Coords: x = " + event.getX() + " y = " + event.getY());
            }
        }
        //handle movement
        if (event.getAction() == MotionEvent.ACTION_MOVE)
        {
            Log.d(TAG, "Event is a move action");
            if (droid.isTouched())
            {
                Log.d(TAG, "Changing droid position");
                //The droid is being moved
                droid.setX((int)event.getX());
                droid.setY((int)event.getY());
            }
        }
        if (event.getAction() == MotionEvent.ACTION_UP)
        {
            Log.d(TAG, "Event is an up action");
            if (droid.isTouched())
            {
                droid.setTouched(false);
            }
        }
        Log.d(TAG, "Event handled");
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        this.render(canvas);
    }

    public void update()
    {

    }

    public void render(Canvas canvas)
    {
        //fill the canvas with black
        canvas.drawColor(Color.BLACK);
        droid.draw(canvas);
        //display fps
        displayFps(canvas, averageFPS);
    }

    private void displayFps(Canvas canvas, String fps)
    {
        if (canvas!=null && fps!=null)
        {
            Paint paint = new Paint();
            paint.setARGB(255, 255, 255 ,255);
            canvas.drawText(fps, this.getWidth() - 50, 20, paint);
        }
    }

    public void setAverageFPS(String averageFPS)
    {
        this.averageFPS = averageFPS;
    }


}
