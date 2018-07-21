package jmri.enginedriver.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import jmri.enginedriver.R;

/**
 * Represents an on-device flashlight.
 *
 * Provides different methods to operate depending on the device API level
 *
 * @author Matthew Harris  Copyright (C) 2018.
 */

public abstract class Flashlight {

    private static Context flashlightContext;

    public static Flashlight newInstance(Context context) {
        flashlightContext = context;
        final int sdkVersion = Build.VERSION.SDK_INT;
        Flashlight flashlight = null;
        if (sdkVersion < Build.VERSION_CODES.FROYO) {
            flashlight = new EclairFlashlight();
        } else if (sdkVersion < Build.VERSION_CODES.LOLLIPOP) {
            flashlight = new FroyoFlashlight();
        } else {
            flashlight = new LollipopFlashlight();
        }
        flashlight.init();
        Log.d("Engine_Driver", "Created new " + flashlight.getClass());
        return flashlight;
    }

    /**
     * Allow for any needed initialisation for concrete implementations
     */
    protected abstract void init();

    /**
     * Allow for any needed teardown for concrete implementations
     */
    public abstract void teardown();

    /**
     * Check to see if a flashlight is available in this context
     *
     * @return true if a flashlight is available; false if not
     */
    public boolean isFlashlightAvailable() {
        return flashlightContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    /**
     * Switch on the flashlight
     *
     * @param activity the requesting activity
     * @return true if the flashlight successfully switch on; false if unsuccessful
     */
    public abstract boolean setFlashlightOn(Activity activity);

    /**
     * Switch off the flashlight
     */
    public abstract void setFlashlightOff();

    /**
     * Concrete implementation for Eclair API7 (and later) devices
     *
     * This uses the legacy {@link android.hardware.Camera} API.
     */
    @TargetApi(Build.VERSION_CODES.ECLAIR_MR1)
    private static class EclairFlashlight extends Flashlight {
        protected static Camera camera;

        @Override
        protected void init() {
            // No specific initialisation needed - do nothing
        }

        @Override
        public void teardown() {
            // No specific teardown needed - do nothing
        }

        @Override
        public boolean setFlashlightOn(Activity activity) {
            try {
                camera = Camera.open();
                Camera.Parameters parameters = camera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(parameters);
                camera.startPreview();
                Log.d("Engine_Driver", "Flashlight switched on");
                return true;
            } catch (Exception ex) {
                Log.e("Engine_Driver", "Error switching on flashlight: " + ex.getMessage());
                Toast.makeText(flashlightContext, flashlightContext.getResources().getString(R.string.toastFlashlightOnFailed), Toast.LENGTH_LONG).show();
                return false;
            }
        }

        @Override
        public void setFlashlightOff() {
            try {
                if (camera != null) {
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }
                Log.d("Engine_Driver", "Flashlight switched off");
            } catch (Exception ex) {
                Log.e("Engine_Driver", "Error switching off flashlight: " + ex.getMessage());
                Toast.makeText(flashlightContext, flashlightContext.getResources().getString(R.string.toastFlashlightOffFailed), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Concrete implementation for Froyo API8 (and later) devices
     *
     * This uses the legacy {@link android.hardware.Camera} API.
     *
     * On certain devices, we need to ensure that the orientation of the camera preview
     * matches that of the activity, otherwise 'bad things happen' using the newly available
     * {@link android.hardware.Camera#setDisplayOrientation(int)} method.
     */
    @TargetApi(Build.VERSION_CODES.FROYO)
    private static class FroyoFlashlight extends Flashlight {
        protected static Camera camera;

        @Override
        protected void init() {
            // No specific initialisation needed - do nothing
        }

        @Override
        public void teardown() {
            // No specific teardown needed - do nothing
        }

        @Override
        public boolean setFlashlightOn(Activity activity) {
            try {
                camera = Camera.open();
                Camera.Parameters parameters = camera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(parameters);
                camera.setDisplayOrientation(getDisplayOrientation(activity));
                camera.startPreview();
                Log.d("Engine_Driver", "Flashlight switched on");
                return true;
            } catch (Exception ex) {
                Log.e("Engine_Driver", "Error switching on flashlight: " + ex.getMessage());
                Toast.makeText(flashlightContext, flashlightContext.getResources().getString(R.string.toastFlashlightOnFailed), Toast.LENGTH_LONG).show();
                return false;
            }
        }

        @Override
        public void setFlashlightOff() {
            try {
                if (camera != null) {
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }
                Log.d("Engine_Driver", "Flashlight switched off");
            } catch (Exception ex) {
                Log.e("Engine_Driver", "Error switching off flashlight: " + ex.getMessage());
                Toast.makeText(flashlightContext, flashlightContext.getResources().getString(R.string.toastFlashlightOffFailed), Toast.LENGTH_LONG).show();
            }
        }

        /**
         * Retrieves the screen orientation for the specified activity
         *
         * @param activity the requesting activity
         * @return screen orientation as integer number of degrees
         */
        private int getDisplayOrientation(Activity activity) {
            switch (activity.getWindowManager().getDefaultDisplay().getRotation()) {
                case Surface.ROTATION_0: return 0;
                case Surface.ROTATION_90: return 90;
                case Surface.ROTATION_180: return 180;
                case Surface.ROTATION_270: return 270;
                default: return 90;
            }
        }
    }

    /**
     * Concrete implementation for Lollipop API 21 (and later) devices
     *
     * This uses the new {@link android.hardware.camera2.CameraManager} API due to deprectation of
     * the previous {@link android.hardware.Camera} API.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static class LollipopFlashlight extends Flashlight {

        private static CameraManager cameraManager;
        private static CameraCaptureSession captureSession;
        private static CameraDevice cameraDevice;
        private static CaptureRequest.Builder builder;
        private static String cameraId;
        private static SurfaceTexture surfaceTexture;
        private static Surface surface;

        @Override
        protected void init() {
            cameraManager = (CameraManager) flashlightContext.getSystemService(Context.CAMERA_SERVICE);
            try {
                cameraId = cameraManager.getCameraIdList()[0];
                cameraManager.openCamera(cameraId, new CameraDeviceStateCallBack(), null);
            } catch (CameraAccessException|SecurityException ex) {
                Log.e("Engine_Driver", "Error initiating camera manager: " + ex.getMessage());
            }
        }

        @Override
        public void teardown() {
            if (cameraDevice == null || captureSession == null) {
                return;
            }
            captureSession.close();
            cameraDevice.close();
            cameraDevice = null;
            captureSession = null;
        }

        private class CameraDeviceStateCallBack extends CameraDevice.StateCallback {

            @Override
            public void onOpened(CameraDevice camera) {
                cameraDevice = camera;
                try {
                    builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                    List<Surface> list = new ArrayList<Surface>();
                    surfaceTexture = new SurfaceTexture(1);
                    Size size = getSmallestSize(cameraId);
                    surfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
                    surface = new Surface(surfaceTexture);
                    list.add(surface);
                    builder.addTarget(surface);
                    camera.createCaptureSession(list, new CameraCaptureSessionStateCallback(), null);
                } catch (CameraAccessException ex) {
                    Log.e("Engine_Driver", "Error initiating camera device: " + ex.getMessage());
                }
            }

            @Override
            public void onDisconnected(CameraDevice cameraDevice) {
                // Do nothing
            }

            @Override
            public void onError(CameraDevice cameraDevice, int i) {
                // Do nothing
            }
        }

        private class CameraCaptureSessionStateCallback extends CameraCaptureSession.StateCallback {

            @Override
            public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                captureSession = cameraCaptureSession;
                try {
                    captureSession.setRepeatingRequest(builder.build(), null, null);
                } catch (CameraAccessException ex) {
                    Log.e("Engine_Driver", "Error configuring camera capture session: " + ex.getMessage());
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                // Do nothing
            }
        }

        public boolean setFlashlightOn(Activity activity) {
            builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
            try {
                captureSession.setRepeatingRequest(builder.build(), null, null);
                Log.d("Engine_Driver", "Flashlight switched on");
                return true;
            } catch (CameraAccessException ex) {
                Log.e("Engine_Driver", "Error switching on flashlight: " + ex.getMessage());
                Toast.makeText(flashlightContext, flashlightContext.getResources().getString(R.string.toastFlashlightOnFailed), Toast.LENGTH_LONG).show();
                return false;
            }
        }

        @Override
        public void setFlashlightOff() {
            builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
            try {
                captureSession.setRepeatingRequest(builder.build(), null, null);
                Log.d("Engine_Driver", "Flashlight switched off");
            } catch (CameraAccessException ex) {
                Log.e("Engine_Driver", "Error switching off flashlight: " + ex.getMessage());
                Toast.makeText(flashlightContext, flashlightContext.getResources().getString(R.string.toastFlashlightOffFailed), Toast.LENGTH_LONG).show();
            }
        }

        private Size getSmallestSize(String cameraId) throws CameraAccessException {
            Size[] outputSizes = cameraManager.getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(SurfaceTexture.class);
            if (outputSizes == null || outputSizes.length == 0) {
                throw new IllegalStateException("Camera " + cameraId + " doesn't support any outputSize.");
            }

            Size chosenSize = outputSizes[0];
            for (Size size: outputSizes) {
                if (chosenSize.getWidth() >= size.getWidth() && chosenSize.getHeight() >= size.getHeight()) {
                    chosenSize = size;
                }
            }

            return chosenSize;
        }
    }
}
