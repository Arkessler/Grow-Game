package com.example.alexi.grow;

import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;

import java.text.DecimalFormat;

/**
 * Created by Alexi on 5/28/2015.
 */
public class MainThread extends Thread
{
    private static final String TAG = "MainThread";

    private static int MAX_FPS = 50;
    private static int MAX_FRAME_SKIPS = 5;
    private static int FRAME_PERIOD = 1000/MAX_FPS;
    //Things for statistics
    private DecimalFormat df = new DecimalFormat("0.##");
    //read stats every sec
    private final static int STAT_INTERVAL = 1000; //ms
    //The average will be calculated by storing the last n FPS's
    private final static int FPS_HISTORY_NR = 10;
    //last time the status was stored
    private long lastStatusStore = 0;
    //The status time counter
    private long statusIntervalTimer = 0l;
    //Number of frames skipped since the game started
    private long totalFramesSkipped = 0l;
    //number of frames skipped in a store cycle (1s)
    private long framesSkippedPerStatCycle = 0l;

    //number of rendered frames in an interval
    private int frameCountPerStatCycle = 0;
    private long totalFrameCount = 0l;
    //The last FPS values
    private double fpsStore[];
    //The number of times the stat has been read
    private long statsCount = 0;
    //The average FPS since the game started
    private double averageFPS = 0.0;

    //Surface holder that can access the physical surface
    private SurfaceHolder surfaceHolder;
    //The actual view that handles inputs and draws to the surface
    private MainGamePanel mainGamePanel;

    //Flag to hold game state
    private boolean running;
    public MainThread(SurfaceHolder surfaceHolder, MainGamePanel mainGamePanel)
    {
        super();
        this.surfaceHolder = surfaceHolder;
        this.mainGamePanel = mainGamePanel;
    }
    public void setRunning(boolean running)
    {
        this.running = running;
    }

    @Override
    public void run()
    {
        Canvas canvas;
        Log.d(TAG, "Starting game loop");
        initTimingElements();

        long beginTime;
        long diffTime;
        int sleepTime; //ms to sleep, <0 if behind
        int framesSkipped;

        sleepTime = 0;
        while (running) {
            canvas = null;
            try //try locking canvas for pixel editing
            {
                canvas = this.surfaceHolder.lockCanvas();
                synchronized (surfaceHolder) {
                    beginTime = System.currentTimeMillis();
                    framesSkipped = 0;
                    /*
                    //update game state
                    this.mainGamePanel.update();
                    //render state to screen, draw canvas to panel
                    this.mainGamePanel.onDraw(canvas);
                    */
                    this.mainGamePanel.update();
                    this.mainGamePanel.render(canvas);
                    //calculate cycle length
                    diffTime = System.currentTimeMillis() - beginTime;
                    //calculate sleep time
                    sleepTime = (int) (FRAME_PERIOD - diffTime);

                    if (sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                        }
                    }
                    while (sleepTime < 0 && framesSkipped < MAX_FRAME_SKIPS) {
                        //catch up update without rendering
                        this.mainGamePanel.update();
                        sleepTime += FRAME_PERIOD;
                        framesSkipped++;
                    }
                    if (framesSkipped > 0)
                    {
                        Log.d(TAG, "Skipped:" + framesSkipped);
                    }
                    //for statistics
                    framesSkippedPerStatCycle += framesSkipped;
                    //state store routine
                    storeStats();
                }
            } finally {
                //In case of an exception the surface is not left in an inconsistent state
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            } //end finally
        }//end while
    }
    /*
    The statistics: called every cycle, checks if the time since last cycle is greater than the statistics
    gathering period (1 second) and if so it calculates the FPS for the last period and stores it.

    Tracks the number of frames per period. The numbers of frames since the start of the period are summed up
    and the calculation takes part only if next period and the frame count is reset to 0
     */
    private void storeStats() {
        frameCountPerStatCycle++;
        totalFrameCount++;

        //check the actual time
        statusIntervalTimer += (System.currentTimeMillis()-statusIntervalTimer);

        if (statusIntervalTimer >= lastStatusStore + STAT_INTERVAL)
        {
            //calculate the actual frames per status check interval
            double actualFPS = (double)(frameCountPerStatCycle/(STAT_INTERVAL/1000));

            //stores the latest fps in the array
            fpsStore[(int)statsCount%FPS_HISTORY_NR] = actualFPS;

            //increase the count of how many times the stats have been calculated
            statsCount++;

            double totalFPS = 0.0;
            //sum up the stored values
            for (int i = 0; i<FPS_HISTORY_NR; i++)
            {
                totalFPS += fpsStore[i];
            }

            //obtain the average
            if (statsCount < FPS_HISTORY_NR)
            {
                averageFPS = totalFPS/statsCount;
            } else
            {
                averageFPS = totalFPS/FPS_HISTORY_NR;
            }
            //saving the total number of frames skipped
            totalFramesSkipped+=framesSkippedPerStatCycle;
            //resetting the counters after status recording (1 sec)
            framesSkippedPerStatCycle = 0;
            statusIntervalTimer = 0;
            frameCountPerStatCycle = 0;

            statusIntervalTimer = System.currentTimeMillis();
            lastStatusStore = statusIntervalTimer;
            Log.d(TAG, "Average FPS:"+df.format(averageFPS));
            mainGamePanel.setAverageFPS("FPS: "+df.format(averageFPS));
        }
    }

    private void initTimingElements()
    {
        //initialise timing elements
        fpsStore = new double[FPS_HISTORY_NR];
        for (int i = 0; i < FPS_HISTORY_NR; i++)
        {
            fpsStore[i]=0.0;
        }
        Log.d(TAG+".initTimingElements()", "Timing elements for stats inititialised");
    }
}
