/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.


Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package br.com.rescue_bots_android.app.UserDefinedTargets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.ImageTargetBuilder;
import com.vuforia.ObjectTracker;
import com.vuforia.State;
import com.vuforia.Trackable;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;
import com.vuforia.samples.SampleApplication.SampleApplicationControl;
import com.vuforia.samples.SampleApplication.SampleApplicationException;
import com.vuforia.samples.SampleApplication.SampleApplicationSession;
import com.vuforia.samples.SampleApplication.utils.LoadingDialogHandler;
import com.vuforia.samples.SampleApplication.utils.SampleApplicationGLView;
import com.vuforia.samples.SampleApplication.utils.Texture;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.Toast;
import br.com.rescue_bots_android.R;
import br.com.rescue_bots_android.bluetooth.Homescreen;
import br.com.rescue_bots_android.bluetooth.MainActivity;
import br.com.rescue_bots_android.sqlite.entity.Path;
import br.com.rescue_bots_android.ui.SampleAppMenu.SampleAppMenu;
import br.com.rescue_bots_android.ui.SampleAppMenu.SampleAppMenuGroup;
import br.com.rescue_bots_android.ui.SampleAppMenu.SampleAppMenuInterface;
import br.com.rescue_bots_control.JoystickView;

// The main activity for the UserDefinedTargets sample.
public class UserDefinedTargets extends Activity implements
    SampleApplicationControl, SampleAppMenuInterface
{
	
    private static final String LOGTAG = "UserDefinedTargets";
    
    private SampleApplicationSession vuforiaAppSession;
    
    // Our OpenGL view:
    private SampleApplicationGLView mGlView;
    
    // Our renderer:
    private UserDefinedTargetRenderer mRenderer;
    
    // The textures we will use for rendering:
    private Vector<Texture> mTextures;
    
    // View overlays to be displayed in the Augmented View
    private RelativeLayout mUILayout;
    private View mBottomBar;
    private View mCameraButton;
    
    // Alert dialog for displaying SDK errors
    private AlertDialog mDialog;
    
    int targetBuilderCounter = 1;
    
    DataSet dataSetUserDef = null;
    
    private GestureDetector mGestureDetector;
    
    private SampleAppMenu mSampleAppMenu;
    private ArrayList<View> mSettingsAdditionalViews;
    
    private boolean mExtendedTracking = false;
    
    private LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(
        this);
    
    RefFreeFrame refFreeFrame;
    
    // Alert Dialog used to display SDK errors
    private AlertDialog mErrorDialog;
    
    boolean mIsDroidDevice = false;
    
    
    
    private UUID mDeviceUUID;
	//private BluetoothSocket mBTSocket;
    private boolean mIsBluetoothConnected = false;
	private BluetoothDevice mDevice;
	private ProgressDialog progressDialog;
	private int mMaxChars = 50000;//Default
	private boolean learnedObject = false;
	
	public static final String DEVICE_EXTRA = "com.blueserial.SOCKET";
	public static final String DEVICE_UUID = "com.blueserial.uuid";
	private static final String DEVICE_LIST = "com.blueserial.devicelist";
	private static final String DEVICE_LIST_SELECTED = "com.blueserial.devicelistselected";
	public static final String BUFFER_SIZE = "com.blueserial.buffersize";
	
	private BluetoothSocket mBTSocket;
	private ReadInput mReadThread = null;
	private boolean mIsUserInitiatedDisconnect = false;
    // Called when the activity first starts or needs to be recreated after
    // resuming the application or a configuration change.
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);
        
        
        
        vuforiaAppSession = new SampleApplicationSession(this);
        
        vuforiaAppSession
            .initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        // Load any sample specific textures:
        mTextures = new Vector<Texture>();
       loadTextures();
        
        mGestureDetector = new GestureDetector(this, new GestureListener());
        
        mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith(
            "droid");
        
        
        Intent intent = getIntent();
		Bundle b = intent.getExtras();
		mDevice = b.getParcelable(Homescreen.DEVICE_EXTRA);
		mDeviceUUID = UUID.fromString(b.getString(Homescreen.DEVICE_UUID));
		mMaxChars = b.getInt(Homescreen.BUFFER_SIZE);
		//new RequestTask().execute("send");
    }
    
    // Process Single Tap event to trigger autofocus
    private class GestureListener extends
        GestureDetector.SimpleOnGestureListener
    {
        // Used to set autofocus one second after a manual focus is triggered
        private final Handler autofocusHandler = new Handler();
        
        
        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }
        
        
        @Override
        public boolean onSingleTapUp(MotionEvent e)
        {
            // Generates a Handler to trigger autofocus
            // after 1 second
            autofocusHandler.postDelayed(new Runnable()
            {
                public void run()
                {
                    boolean result = CameraDevice.getInstance().setFocusMode(
                        CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);
                    
                    if (!result)
                        Log.e("SingleTapUp", "Unable to trigger focus");
                }
            }, 1000L);
            
            return true;
        }
    }
    
    
    // We want to load specific textures from the APK, which we will later use
    // for rendering.
    private void loadTextures()
    {
        mTextures.add(Texture.loadTextureFromApk("TextureTeapotTransp.png",
            getAssets()));
    }
    
    
    // Called when the activity will start interacting with the user.
    @Override
    protected void onResume()
    {
    	  if (mBTSocket == null || !mIsBluetoothConnected) {
  			new ConnectBT().execute();
  		}
  		
        Log.d(LOGTAG, "onResume");
        super.onResume();
        
        // This is needed for some Droid devices to force portrait
        if (mIsDroidDevice)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        
        try
        {
            vuforiaAppSession.resumeAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
        
        // Resume the GL view:
        if (mGlView != null)
        {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
        
      
		
        
    }
    
    
    // Called when the system is about to start resuming a previous activity.
    @Override
    protected void onPause()
    {
        Log.d(LOGTAG, "onPause");
        if (mBTSocket != null && mIsBluetoothConnected) {
			new DisConnectBT().execute();
		}
		Log.d(LOGTAG, "Paused");
        super.onPause();
        
        if (mGlView != null)
        {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }
        
        try
        {
            vuforiaAppSession.pauseAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
    }
    
    
    // The final call you receive before your activity is destroyed.
    @Override
    protected void onDestroy()
    {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();
        
        try
        {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
        
        // Unload texture:
        mTextures.clear();
        mTextures = null;
        
        System.gc();
    }
    
    
    // Callback for configuration changes the activity handles itself
    @Override
    public void onConfigurationChanged(Configuration config)
    {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);
        
        vuforiaAppSession.onConfigurationChanged();
        
        // Removes the current layout and inflates a proper layout
        // for the new screen orientation
        
        if (mUILayout != null)
        {
            mUILayout.removeAllViews();
            ((ViewGroup) mUILayout.getParent()).removeView(mUILayout);
            
        }
        
        addOverlayView(false);
    }
    
    
    // Shows error message in a system dialog box
    private void showErrorDialog()
    {
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();
        
        mDialog = new AlertDialog.Builder(UserDefinedTargets.this).create();
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        };
        
        mDialog.setButton(DialogInterface.BUTTON_POSITIVE,
            getString(R.string.button_OK), clickListener);
        
        mDialog.setTitle(getString(R.string.target_quality_error_title));
        
        String message = getString(R.string.target_quality_error_desc);
        
        // Show dialog box with error message:
        mDialog.setMessage(message);
        mDialog.show();
    }
    
    
    // Shows error message in a system dialog box on the UI thread
    void showErrorDialogInUIThread()
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                showErrorDialog();
            }
        });
    }
    
    
    // Initializes AR application components.
    private void initApplicationAR()
    {
        // Do application initialization
        refFreeFrame = new RefFreeFrame(this, vuforiaAppSession);
        refFreeFrame.init();
        
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();
        
        mGlView = new SampleApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);
        
        mRenderer = new UserDefinedTargetRenderer(this, vuforiaAppSession);
        //mRenderer.setTextures(mTextures);
        mGlView.setRenderer(mRenderer);
        addOverlayView(true);
        
    }
    
    
    // Adds the Overlay view to the GLView
    private void addOverlayView(boolean initLayout)
    {
        // Inflates the Overlay Layout to be displayed above the Camera View
        LayoutInflater inflater = LayoutInflater.from(this);
        mUILayout = (RelativeLayout) inflater.inflate(
            R.layout.camera_overlay_udt, null, false);
        
        mUILayout.setVisibility(View.VISIBLE);
        
        // If this is the first time that the application runs then the
        // uiLayout background is set to BLACK color, will be set to
        // transparent once the SDK is initialized and camera ready to draw
        if (initLayout)
        {
            mUILayout.setBackgroundColor(Color.BLACK);
        }
        
        // Adds the inflated layout to the view
        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT));
        
        // Gets a reference to the bottom navigation bar
        mBottomBar = mUILayout.findViewById(R.id.bottom_bar);
        
        // Gets a reference to the Camera button
        mCameraButton = mUILayout.findViewById(R.id.camera_button);
        
        // Gets a reference to the loading dialog container
        loadingDialogHandler.mLoadingDialogContainer = mUILayout
            .findViewById(R.id.loading_layout);
        
        startUserDefinedTargets();
        initializeBuildTargetModeViews();
        
        mUILayout.bringToFront();
    }
    
    
    // Button Camera clicked
    public void onCameraClick(View v)
    {
        if (isUserDefinedTargetsRunning())
        {
            // Shows the loading dialog
            loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
            
            // Builds the new target
            startBuild();
        }
    }
    
    
    // Creates a texture given the filename
    Texture createTexture(String nName)
    {
        return Texture.loadTextureFromApk(nName, getAssets());
    }
    
    
    // Callback function called when the target creation finished
    void targetCreated()
    {
        // Hides the loading dialog
        loadingDialogHandler
            .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
        
        if (refFreeFrame != null)
        {
            refFreeFrame.reset();
        }
        
    }
    
    
    // Initialize views
    private void initializeBuildTargetModeViews()
    {
        // Shows the bottom bar
        mBottomBar.setVisibility(View.VISIBLE);
        mCameraButton.setVisibility(View.VISIBLE);
    }
    
    
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // Process the Gestures
        if (mSampleAppMenu != null && mSampleAppMenu.processEvent(event))
            return true;
        
        return mGestureDetector.onTouchEvent(event);
    }
    
    
    boolean startUserDefinedTargets()
    {
        Log.d(LOGTAG, "startUserDefinedTargets");
        
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) (trackerManager
            .getTracker(ObjectTracker.getClassType()));
        if (objectTracker != null)
        {
            ImageTargetBuilder targetBuilder = objectTracker
                .getImageTargetBuilder();
            
            if (targetBuilder != null)
            {
                // if needed, stop the target builder
                if (targetBuilder.getFrameQuality() != ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_NONE)
                    targetBuilder.stopScan();
                
                objectTracker.stop();
                
                targetBuilder.startScan();
                
            }
        } else
            return false;
        
        return true;
    }
    
    
    boolean isUserDefinedTargetsRunning()
    {
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
            .getTracker(ObjectTracker.getClassType());
        
        if (objectTracker != null)
        {
            ImageTargetBuilder targetBuilder = objectTracker
                .getImageTargetBuilder();
            if (targetBuilder != null)
            {
                Log.e(LOGTAG, "Quality> " + targetBuilder.getFrameQuality());
                return (targetBuilder.getFrameQuality() != ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_NONE) ? true
                    : false;
            }
        }
        
        return false;
    }
    
    
    void startBuild()
    {
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
            .getTracker(ObjectTracker.getClassType());
        
        if (objectTracker != null)
        {
            ImageTargetBuilder targetBuilder = objectTracker
                .getImageTargetBuilder();
            if (targetBuilder != null)
            {
                if (targetBuilder.getFrameQuality() == ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_LOW)
                {
                     showErrorDialogInUIThread();
                }
                
                String name;
                do
                {
                    name = "UserTarget-" + targetBuilderCounter;
                    Log.d(LOGTAG, "TRYING " + name);
                    targetBuilderCounter++;
                } while (!targetBuilder.build(name, 320.0f));
                
                refFreeFrame.setCreating();
                learnedObject = true;
            }
        }
    }
    
    
    void updateRendering()
    {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        refFreeFrame.initGL(metrics.widthPixels, metrics.heightPixels);
    }
    
    
    @Override
    public boolean doInitTrackers()
    {
        // Indicate if the trackers were initialized correctly
        boolean result = true;
        
        // Initialize the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        Tracker tracker = trackerManager.initTracker(ObjectTracker
            .getClassType());
        if (tracker == null)
        {
            Log.d(LOGTAG, "Failed to initialize ObjectTracker.");
            result = false;
        } else
        {
            Log.d(LOGTAG, "Successfully initialized ObjectTracker.");
        }
        
        return result;
    }
    
    
    @Override
    public boolean doLoadTrackersData()
    {
        // Get the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
            .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
        {
            Log.d(
                LOGTAG,
                "Failed to load tracking data set because the ObjectTracker has not been initialized.");
            return false;
        }
        
        // Create the data set:
        dataSetUserDef = objectTracker.createDataSet();
        if (dataSetUserDef == null)
        {
            Log.d(LOGTAG, "Failed to create a new tracking data.");
            return false;
        }
        
        if (!objectTracker.activateDataSet(dataSetUserDef))
        {
            Log.d(LOGTAG, "Failed to activate data set.");
            return false;
        }
        
        Log.d(LOGTAG, "Successfully loaded and activated data set.");
        return true;
    }
    
    
    @Override
    public boolean doStartTrackers()
    {
        // Indicate if the trackers were started correctly
        boolean result = true;
        
        Tracker objectTracker = TrackerManager.getInstance().getTracker(
            ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.start();
        
        return result;
    }
    
    
    @Override
    public boolean doStopTrackers()
    {
        // Indicate if the trackers were stopped correctly
        boolean result = true;
        
        Tracker objectTracker = TrackerManager.getInstance().getTracker(
            ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.stop();
        
        return result;
    }
    
    
    @Override
    public boolean doUnloadTrackersData()
    {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;
        
        // Get the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
            .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
        {
            result = false;
            Log.d(
                LOGTAG,
                "Failed to destroy the tracking data set because the ObjectTracker has not been initialized.");
        }
        
        if (dataSetUserDef != null)
        {
            if (objectTracker.getActiveDataSet() != null
                && !objectTracker.deactivateDataSet(dataSetUserDef))
            {
                Log.d(
                    LOGTAG,
                    "Failed to destroy the tracking data set because the data set could not be deactivated.");
                result = false;
            }
            
            if (!objectTracker.destroyDataSet(dataSetUserDef))
            {
                Log.d(LOGTAG, "Failed to destroy the tracking data set.");
                result = false;
            }
            
            Log.d(LOGTAG, "Successfully destroyed the data set.");
            dataSetUserDef = null;
        }
        
        return result;
    }
    
    
    @Override
    public boolean doDeinitTrackers()
    {
        // Indicate if the trackers were deinitialized correctly
        boolean result = true;
        
        if (refFreeFrame != null)
            refFreeFrame.deInit();
        
        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(ObjectTracker.getClassType());
        
        return result;
    }
    
    
    @Override
    public void onInitARDone(SampleApplicationException exception)
    {
        
        if (exception == null)
        {
            initApplicationAR();
            
            // Activate the renderer
            mRenderer.mIsActive = true;
            
            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
            
            // Sets the UILayout to be drawn in front of the camera
            mUILayout.bringToFront();
            
            // Hides the Loading Dialog
            loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
            
            // Sets the layout background to transparent
            mUILayout.setBackgroundColor(Color.TRANSPARENT);
            
            try
            {
                vuforiaAppSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);
            } catch (SampleApplicationException e)
            {
                Log.e(LOGTAG, e.getString());
            }
            
            boolean result = CameraDevice.getInstance().setFocusMode(
                CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);
            
            if (!result)
                Log.e(LOGTAG, "Unable to enable continuous autofocus");
            
            setSampleAppMenuAdditionalViews();
            mSampleAppMenu = new SampleAppMenu(this, this,
                "User Defined Targets", mGlView, mUILayout,
                mSettingsAdditionalViews);
            setSampleAppMenuSettings();
            
        } else
        {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }
    }
    
    
    // Shows initialization error messages as System dialogs
    public void showInitializationErrorMessage(String message)
    {
        final String errorMessage = message;
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                if (mErrorDialog != null)
                {
                    mErrorDialog.dismiss();
                }
                
                // Generates an Alert Dialog to show the error message
                AlertDialog.Builder builder = new AlertDialog.Builder(
                    UserDefinedTargets.this);
                builder
                    .setMessage(errorMessage)
                    .setTitle(getString(R.string.INIT_ERROR))
                    .setCancelable(false)
                    .setIcon(0)
                    .setPositiveButton(getString(R.string.button_OK),
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                finish();
                            }
                        });
                
                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });
    }
    
    
    @Override
    public void onVuforiaUpdate(State state)
    {
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
            .getTracker(ObjectTracker.getClassType());
        
        if (refFreeFrame.hasNewTrackableSource())
        {
            Log.d(LOGTAG,
                "Attempting to transfer the trackable source to the dataset");
            
            // Deactivate current dataset
            objectTracker.deactivateDataSet(objectTracker.getActiveDataSet());
            
            // Clear the oldest target if the dataset is full or the dataset
            // already contains five user-defined targets.
            if (dataSetUserDef.hasReachedTrackableLimit()
                || dataSetUserDef.getNumTrackables() >= 5)
                dataSetUserDef.destroy(dataSetUserDef.getTrackable(0));
            
            if (mExtendedTracking && dataSetUserDef.getNumTrackables() > 0)
            {
                // We need to stop the extended tracking for the previous target
                // so we can enable it for the new one
                int previousCreatedTrackableIndex = 
                    dataSetUserDef.getNumTrackables() - 1;
                
                objectTracker.resetExtendedTracking();
                dataSetUserDef.getTrackable(previousCreatedTrackableIndex)
                    .stopExtendedTracking();
            }
            
            // Add new trackable source
            Trackable trackable = dataSetUserDef
                .createTrackable(refFreeFrame.getNewTrackableSource());
            
            // Reactivate current dataset
            objectTracker.activateDataSet(dataSetUserDef);
            
            if (mExtendedTracking)
            {
                trackable.startExtendedTracking();
            }
            
        }
    }
    
    final public static int CMD_BACK = -1;
    final public static int CMD_EXTENDED_TRACKING = 1;
    
    // This method sets the additional views to be moved along with the GLView
    private void setSampleAppMenuAdditionalViews()
    {
        mSettingsAdditionalViews = new ArrayList<View>();
        mSettingsAdditionalViews.add(mBottomBar);
    }
    
    
    // This method sets the menu's settings
    private void setSampleAppMenuSettings()
    {
        SampleAppMenuGroup group;
        
        group = mSampleAppMenu.addGroup("", false);
        group.addTextItem(getString(R.string.menu_back), -1);
        
        group = mSampleAppMenu.addGroup("", true);
        group.addSelectionItem(getString(R.string.menu_extended_tracking),
            CMD_EXTENDED_TRACKING, false);
        
        mSampleAppMenu.attachMenu();
    }
    
    
    @Override
    public boolean menuProcess(int command)
    {
        boolean result = true;
        
        switch (command)
        {
            case CMD_BACK:
                finish();
                break;
            
            case CMD_EXTENDED_TRACKING:
                if (dataSetUserDef.getNumTrackables() > 0)
                {
                    int lastTrackableCreatedIndex = 
                        dataSetUserDef.getNumTrackables() - 1;
                    
                    Trackable trackable = dataSetUserDef
                        .getTrackable(lastTrackableCreatedIndex);
                    
                    if (!mExtendedTracking)
                    {
                        if (!trackable.startExtendedTracking())
                        {
                            Log.e(LOGTAG,
                                "Failed to start extended tracking target");
                            result = false;
                        } else
                        {
                            Log.d(LOGTAG,
                                "Successfully started extended tracking target");
                        }
                    } else
                    {
                        if (!trackable.stopExtendedTracking())
                        {
                            Log.e(LOGTAG,
                                "Failed to stop extended tracking target");
                            result = false;
                        } else
                        {
                            Log.d(LOGTAG,
                                "Successfully stopped extended tracking target");
                        }
                    }
                }
                
                if (result)
                    mExtendedTracking = !mExtendedTracking;
                
                break;
            
        }
        
        return result;
    }

   
    boolean foundSucess = false;
	public void sendRecognition(float[] data) {
		  if(learnedObject && data!=null){
			  float x = data[0];
			  float y = data[1];
			  if(data!=null && data.length==2){
				  
			    	  if(x>900){ //JoystickView.RIGHT:
			          	sendSerial("c");
			          	lastmessage = "c";
			          	Log.i("INFO","JoystickView.RIGHT ..: c" );
			    	  }else
			    		  if (x<300){//JoystickView.LEFT:
			    			  sendSerial("b");
			    			  lastmessage = "b";
			    			  Log.i("INFO","JoystickView.LEFT ..: b" );
			    		  }else{
			    			  sendSerial("a");
			    			  lastmessage = "a";
			    			  Log.i("INFO","JoystickView.FRONT ..: a" ); 
			    			  foundSucess = true;
			    			  if(y<60 || y>800){
			    				//  new RequestTask().execute("object here");
			    			  }
			    			  /* Intent datai = new Intent();
			    		        datai.putExtra("foundsucess", foundSucess);
			    		       if (getParent() == null) {
			    		           setResult(RESULT_OK, datai);
			    		       } else {
			    		           getParent().setResult(RESULT_OK, datai);
			    		       }
			    		       onBackPressed();*/
			    		  }
			    	 
			  }else{
				  sendSerial("g");
				  lastmessage = "g";
				  
			  }
			  //sendSerial(lastmessage);
		  }else{
			  sendSerial("g");
			  lastmessage = "g";
			  
		  }
		 
        
          
	}
	
	private String lastmessage = "";
	 
	private void sendSerial(String message){
		try {
			if(!lastmessage.equalsIgnoreCase(message)){
				mBTSocket.getOutputStream().write(message.getBytes());
				Log.i("INFO","Serial send ..: " + message);
			}
		} catch (IOException e) {
			Log.e("ERROR","Serial not found");
		}
	}
	
	
	
	
	private class ReadInput implements Runnable {

		private boolean bStop = false;
		private Thread t;

		public ReadInput() {
			t = new Thread(this, "Input Thread");
			t.start();
		}

		public boolean isRunning() {
			return t.isAlive();
		}

		@Override
		public void run() {
			InputStream inputStream;

			try {
				inputStream = mBTSocket.getInputStream();
				while (!bStop) {
					byte[] buffer = new byte[256];
					if (inputStream.available() > 0) {
						inputStream.read(buffer);
						int i = 0;
						
						for (i = 0; i < buffer.length && buffer[i] != 0; i++) {
						}
						final String strInput = new String(buffer, 0, i);

						

						/*if (chkReceiveText.isChecked()) {
							mTxtReceive.post(new Runnable() {
								@Override
								public void run() {
									mTxtReceive.append(strInput);
									//Uncomment below for testing
									//mTxtReceive.append("\n");
									//mTxtReceive.append("Chars: " + strInput.length() + " Lines: " + mTxtReceive.getLineCount() + "\n");
									
									int txtLength = mTxtReceive.getEditableText().length();  
									if(txtLength > mMaxChars){
										mTxtReceive.getEditableText().delete(0, txtLength - mMaxChars);
									}

									if (chkScroll.isChecked()) { // Scroll only if this is checked
										scrollView.post(new Runnable() { // Snippet from http://stackoverflow.com/a/4612082/1287554
													@Override
													public void run() {
														scrollView.fullScroll(View.FOCUS_DOWN);
													}
												});
									}
								}
							});
						}*/

					}
					Thread.sleep(500);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	
		public void stop() {
			bStop = true;
		}

	}

	private class DisConnectBT extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected Void doInBackground(Void... params) {

			if (mReadThread != null) {
				mReadThread.stop();
				while (mReadThread.isRunning())
					; // Wait until it stops
				mReadThread = null;

			}

			try {
				mBTSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			mIsBluetoothConnected = false;
			if (mIsUserInitiatedDisconnect) {
				finish();
			}
		}

	}

	private void msg(String s) {
		Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
	}


	

	@Override
	protected void onStop() {
		Log.d(LOGTAG, "Stopped");
		super.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}

	private class ConnectBT extends AsyncTask<Void, Void, Void> {
		private boolean mConnectSuccessful = true;

		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(UserDefinedTargets.this, "Por favor aguarde", "Conectando");// http://stackoverflow.com/a/11130220/1287554
		}

		@SuppressLint("NewApi")
		@Override
		protected Void doInBackground(Void... devices) {

			try {
				if (mBTSocket == null || !mIsBluetoothConnected) {
					mBTSocket = mDevice.createInsecureRfcommSocketToServiceRecord(mDeviceUUID);
					BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
					mBTSocket.connect();
				}
			} catch (IOException e) {
				// Unable to connect to device
				e.printStackTrace();
				mConnectSuccessful = false;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			if (!mConnectSuccessful) {
				Toast.makeText(getApplicationContext(), "Não é possível conectar ao dispositivo. Seu serial device existe? Também chque o UUID está conectado nas preferências", Toast.LENGTH_LONG).show();
				finish();
			} else {
				msg("Connected to device");
				mIsBluetoothConnected = true;
				mReadThread = new ReadInput(); // Kick off input reader
			}
			progressDialog.dismiss();
			/*progressDialog.dismiss();
			
			  Intent intent = new Intent(MainActivity.this, AboutScreen.class);
		        intent.putExtra("ABOUT_TEXT_TITLE", "User Defined Targets");
			
			  
			  intent.putExtra("ACTIVITY_TO_LAUNCH",
	                  "app.UserDefinedTargets.UserDefinedTargets");
	              intent.putExtra("ABOUT_TEXT",
	                  "UserDefinedTargets/UD_about.html");
			 
				
				intent.putExtra(DEVICE_EXTRA, mDevice);
				intent.putExtra(DEVICE_UUID, mDeviceUUID.toString());
				intent.putExtra(BUFFER_SIZE, mMaxChars);
				startActivity(intent);
			  
			  
			  startActivity(intent);*/
		}

	}
	
	class RequestTask extends AsyncTask<String, String, Void>{

	    @Override
	    protected Void doInBackground(String... uri) {
	        try {
	        	 /*try
	             {
	                 vuforiaAppSession.stopAR();
	             } catch (SampleApplicationException e)
	             {
	                 Log.e(LOGTAG, e.getString());
	             }
	             */
	             // Unload texture:
	             //mTextures.clear();
	             //mTextures = null;
	             
	             //System.gc();
				new Thread().sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
	        
	    }

	    @Override
	    protected void onPostExecute(Void result) {
	        super.onPostExecute(result);
	        
	       
	       Intent data = new Intent();
	        data.putExtra("foundsucess", foundSucess);
	       if (getParent() == null) {
	    	   setResult(RESULT_OK, data);
	       } else {
	    	   getParent().setResult(RESULT_OK, data);
	       }
	       onBackPressed();
	    }
	}
    
}
