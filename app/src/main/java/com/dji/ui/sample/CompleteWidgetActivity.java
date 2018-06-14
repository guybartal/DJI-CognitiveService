package com.dji.ui.sample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
//import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.NumberFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.microsoft.bing.speech.SpeechClientStatus;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionStatus;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;
import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.contract.*;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.simulator.InitializationData;
import dji.common.flightcontroller.simulator.SimulatorState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.ui.widget.FPVWidget;
//implements SurfaceTextureListener,OnClickListener
/** Activity that shows all the UI elements together */
public class CompleteWidgetActivity extends Activity implements View.OnClickListener,OnCSTaskCompleted, TextToSpeech.OnInitListener, ISpeechRecognitionServerEvents {

    ImageButton detectBtn, proxyBtn, faceBtn, voiceBtn;
    TextView textResult, textStatus;
    FPVWidget fpv;
    Bitmap mBitmap;
    boolean mLoop = false;
    boolean mProxy = false;
    boolean mPhoneCamera = false;
    int detectCount = 0;
    long detectTime = 0;
    Date dStart;
    MediaPlayer player, recordingPlayer;
    TextToSpeech tts;
    String sttSubscriptionKey, luisAppId, luisSubscriptionId,speechLocale;
    DataRecognitionClient dataClient = null;
    MicrophoneRecognitionClient micClient = null;
    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;
    private FlightController mFlightController;
    private boolean flightControllerInited = false;
    //FinalResponseStatus isReceivedResponse = FinalResponseStatus.NotReceived;
    private static final int CAMERA_REQUEST = 1888; // field
    private String irisUrl,irisKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_widgets);

        initUI();

        //Speech to Text Subscription Key
        sttSubscriptionKey = "da6d8e3f-688a-4d75-a5f0-412b7a223422";
        //luis App Id & Subscription Key (get from luis.ai portal)
        luisAppId="481d442d-ea1a-4274-a212-b18c9378d63a";
        luisSubscriptionId = "da6d8e3f-688a-4d75-a5f0-412b7a223422";
        speechLocale = "en-US";
        //Custom Vision Prediction URL (get from Custom vision portal)
        irisUrl = "https://southcentralus.api.cognitive.microsoft.com/customvision/v2.0/Prediction/41765292-196d-4060-8519-9379a48be1e1/image?iterationId=72caf992-a276-40f0-b253-ebfb40a418fe";

        //Custom Vision Key
        irisKey = "6d600c50e8bc44618e3229ad06a695de";

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.CHINESE);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "This Language is not supported");
                    }
                    long time = System.currentTimeMillis();
                    final Calendar mCalendar = Calendar.getInstance();
                    mCalendar.setTimeInMillis(time);

                    int hour = mCalendar.get(Calendar.HOUR);
                    int apm = mCalendar.get(Calendar.AM_PM);

                    speak((apm==0?"good morning":"good afternoon"));

                } else {
                    Log.e("TTS", "Initialization Failed!");
                }
            }
        });
    }

    private void speak(String text){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }else{
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Hide both the navigation bar and the status bar.
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void initUI() {
        player = MediaPlayer.create(getApplicationContext(), R.raw.w_found);
        recordingPlayer = MediaPlayer.create(getApplicationContext(), R.raw.w_startrecording);
        detectBtn = (ImageButton) findViewById(R.id.detectBtn);
        detectBtn.setOnClickListener(this);
        proxyBtn = (ImageButton) findViewById(R.id.proxyBtn);
        proxyBtn.setOnClickListener(this);
        faceBtn = (ImageButton) findViewById(R.id.faceBtn);
        faceBtn.setOnClickListener(this);
        voiceBtn = (ImageButton) findViewById(R.id.voiceBtn);
        voiceBtn.setOnClickListener(this);

        textResult = (TextView) findViewById(R.id.textResult);
        textStatus = (TextView) findViewById(R.id.textStatus);
        fpv = (FPVWidget) findViewById(R.id.fpv);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.detectBtn:{
                //textResult.setText((new Date()).toString());
                mLoop = !mLoop;

                if(mLoop) {
                    textStatus.setText("checking");
                    detectBtn.setImageResource(R.mipmap.stopaction);
                    detectObject();
                }
                else {
                    String status = textStatus.getText().toString();
                    if(status.indexOf("checking") >= 0)
                        status = status.replace("checking","stopped");
                    else
                        status = "stopped";
                    textStatus.setText(status);
                    detectBtn.setImageResource(R.mipmap.findobj);
                }
                break;
            }
            case R.id.proxyBtn:{
                /*
                mProxy = !mProxy;
                if(mProxy)
                    proxyBtn.setImageResource(R.mipmap.proxyon);
                else
                    proxyBtn.setImageResource(R.mipmap.proxyoff);
                break;
                */
                mPhoneCamera = !mPhoneCamera;
                if(mPhoneCamera)
                    proxyBtn.setImageResource(R.mipmap.proxyon);
                else
                    proxyBtn.setImageResource(R.mipmap.proxyoff);
                break;
            }
            case R.id.faceBtn:{
                //Face Detection
                if(!mPhoneCamera)
                {
                    DetectFace(fpv.getBitmap());
                }else{
                    getBitmapAndExecute("DetectFace");
                }
                break;
            }

            case R.id.voiceBtn:{
                //LUIS
                voiceControl();
                break;
            }
            default:
                break;
        }
    }

    private void getBitmapAndExecute(String action){
        int CAMERA_REQUEST = 1888; // field

        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap picture = (Bitmap) data.getExtras().get("data");//this is your bitmap image and now you can do whatever you want with this
            //picture); //for example I put bmp in an ImageView
            DetectFace(picture);
        }
    }

    private void voiceControl() {
        voiceBtn.setImageResource(R.mipmap.ic_voicecontrol_on);
        voiceBtn.setEnabled(false);
        //textResult.setText("正在聆听...");
        if(recordingPlayer.isPlaying())
            recordingPlayer.stop();
        recordingPlayer.start();

        try {
            if (micClient == null) {
                micClient =
                        SpeechRecognitionServiceFactory.createMicrophoneClientWithIntent(
                                this,
                                speechLocale,
                                this,
                                sttSubscriptionKey,
                                luisAppId,
                                luisSubscriptionId);
            }
            micClient.startMicAndRecognition();
        }catch (Exception ex){
            //System.out.println(ex.getMessage());
            showToast("Error:" + ex.getMessage());
            Log.d("DjiDemo", ex.getMessage());
        }
    }

    private void detectObject(){
        dStart = new Date();
        mBitmap = fpv.getBitmap();

        CSIRISDetectTask detectObjTask = new CSIRISDetectTask(this);
        detectObjTask.execute();
    }

    @Override
    public void onDetectObjectCompleted(String result){
        detectCount++;

        Date dEnd = new Date();
        long diffInSeconds = (dEnd.getTime() - dStart.getTime()) / 1000;
        detectTime += diffInSeconds;
        int avgLatency = (int) (detectTime/detectCount);

        if(mLoop)
            textStatus.setText("检测中, " + detectCount + "Time delay:" + diffInSeconds + ",Mean:" + avgLatency);
        else
            textStatus.setText("已停止, " + detectCount + "Time delay:" + diffInSeconds + ",Mean:" + avgLatency);

        //textStatus.setText("Found Objects: " + result);
        textResult.setText(result);


        if (mLoop) {
            dStart = new Date();
            mBitmap = fpv.getBitmap();

            CSIRISDetectTask detectObjTask = new CSIRISDetectTask(this);
            detectObjTask.execute();
        }
    }

    @Override
    public void onInit(int status) {
        Log.d("Speech", "OnInit - Status ["+status+"]");

        if (status == TextToSpeech.SUCCESS) {
            Log.d("Speech", "Success!");
            //engine.setLanguage(Locale.UK);
        }
    }

    public void DetectFace(Bitmap imageBitmap) {

        //Cognitive Service - Face API subscription Key
        String subscriptionKey = "b758a95fb4df4686804b4af57b9f7972";
        String uriBase = "https://southcentralus.api.cognitive.microsoft.com/face/v1.0";
        String requestParameters = "returnFaceId=true&returnFaceLandmarks=false&returnFaceAttributes=age,gender,smile,emotion";

        final FaceServiceClient faceServiceClient = new FaceServiceRestClient(uriBase,subscriptionKey);

        //final Bitmap imageBitmap = fpv.getBitmap();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        AsyncTask<InputStream, String, Face[]> detectTask = new AsyncTask<InputStream, String, Face[]>() {
                    @Override
                    protected Face[] doInBackground(InputStream... params) {
                        try {
                            publishProgress("Detecting faces in photos...");
                            //String faceAttr = "age,gender,smile,emotion";
                            FaceServiceClient.FaceAttributeType[] faceAttr = new FaceServiceClient.FaceAttributeType[]{
                                    FaceServiceClient.FaceAttributeType.Age,
                                    FaceServiceClient.FaceAttributeType.Gender,
                                    FaceServiceClient.FaceAttributeType.Emotion,
                                    FaceServiceClient.FaceAttributeType.Smile
                            };
                            Face[] result = faceServiceClient.detect(
                                    inputStream,
                                    true,         // returnFaceId
                                    false,        // returnFaceLandmarks
                                    faceAttr           // returnFaceAttributes: a string like "age, gender"
                            );
                            if (result == null || result.length == 0)
                            {
                                publishProgress("No one is found in the photo");
                                return null;
                            }
                            else{
                                String text = "The end of the test, there are photos" + ((result.length == 2) ? "Two":result.length) + "personal";
                                List<UUID> faceIds = new ArrayList<UUID>();
                                int smile = 0;
                                for(int i=0; i< result.length; i++){
                                    Face f = result[i];
                                    if(f.faceAttributes.smile > 0.5)
                                        smile++;

                                    faceIds.add(f.faceId);
                                }
                                if(smile == result.length)
                                {
                                    if(smile > 1)
                                        text+= "，All smiled very happy";
                                    else
                                        text+= "，I am very happy";
                                }

                                else if(smile/result.length > 0.5)
                                    text+= "，Most people laugh very happily";
                                else if(smile>0)
                                    text+= "，Have" + ((smile == 2) ? "Two":smile) + "Personally happy smiling";

                                //publishProgress(text);

                                UUID[] faceIdArr = new UUID[faceIds.size()];
                                faceIdArr = faceIds.toArray(faceIdArr);

                                //Get Pre-Trained Personal Groups from Cognitive Service
                                String identifiedPerson = "，";
                                IdentifyResult[] identifyResults = faceServiceClient.identity("[[Group GUID]]",faceIdArr,1);
                                for (int i=0; i<identifyResults.length;i++){
                                    if(identifyResults[i].candidates.size() > 0) {
                                        Candidate candidate = identifyResults[i].candidates.get(0);

                                        //Person 1
                                        if (candidate.personId.toString().equals("[[Personal Guid 1]]"))
                                            identifiedPerson += "[[Person 1 Name]]";

                                        //Person 2
                                        if(candidate.personId.toString().equals("[[Personal Guid 2]]"))
                                            identifiedPerson += "[[Person 2 Name]]";

                                        //Persona 3 ....
                                    }
                                }
                                if(!identifiedPerson.equals("，"))
                                    identifiedPerson += "In the photo";

                                publishProgress(text + identifiedPerson);
                            }

                            //publishProgress(String.format("Detection Finished. %d face(s) detected", result.length));
                            return result;
                        } catch (Exception e) {
                            publishProgress("Failed to detect");
                            showToast(e.getMessage());
                            return null;
                        }
                    }
                    @Override
                    protected void onPreExecute() {
                        //TODO: show progress dialog
                    }
                    @Override
                    protected void onProgressUpdate(String... progress) {
                        //TODO: update progress
                        textStatus.setText(progress[0].toString());
                        speak(progress[0].toString());
                    }
                    @Override
                    protected void onPostExecute(Face[] result) {
                        //TODO: update face frames
                    }
                };
            detectTask.execute(inputStream);
    }

    public void onFinalResponseReceived(final RecognitionResult response) {
        micClient.endMicAndRecognition();
        voiceBtn.setEnabled(true);
        voiceBtn.setImageResource(R.mipmap.ic_voicecontrol_off);

        this.WriteLine("********* Final n-BEST Results *********");
        for (int i = 0; i < response.Results.length; i++) {
            this.WriteLine("[" + i + "]" + " Confidence=" + response.Results[i].Confidence +
                    " Text=\"" + response.Results[i].DisplayText + "\"");
        }

        this.WriteLine();
    }

    /**
     * Called when a final response is received and its intent is parsed
     */
    public void onIntentReceived(final String payload) {
        this.WriteLine("--- Intent received by onIntentReceived() ---");
        this.WriteLine(payload);
        this.WriteLine();

        //Parse Intent
        try {
            JSONObject jObject = new JSONObject(payload);
            String query = jObject.getString("query");
            JSONArray intents = jObject.getJSONArray("intents");
            String intent = intents.getJSONObject(0).getString("intent");

            textResult.setText(query + "-" + intent);

            controlFlight(intent,"");
        }catch (Exception ex){
            textResult.setText("Speech recognition error");
            showToast(ex.getMessage());
        }
    }

    public void onPartialResponseReceived(final String response) {
        this.WriteLine("--- Partial result received by onPartialResponseReceived() ---");
        this.WriteLine(response);
        this.WriteLine();
    }

    public void onError(final int errorCode, final String response) {
        voiceBtn.setEnabled(true);
        voiceBtn.setImageResource(R.mipmap.ic_voicecontrol_off);
        this.WriteLine("--- Error received by onError() ---");
        this.WriteLine("Error code: " + SpeechClientStatus.fromInt(errorCode) + " " + errorCode);
        this.WriteLine("Error text: " + response);
        this.WriteLine();
    }

    /**
     * Called when the microphone status has changed.
     * @param recording The current recording state
     */
    public void onAudioEvent(boolean recording) {
        this.WriteLine("--- Microphone status change received by onAudioEvent() ---");
        this.WriteLine("********* Microphone status: " + recording + " *********");
        if (recording) {
            this.WriteLine("Please start speaking.");
        }

        WriteLine();
        if (!recording) {
            micClient.endMicAndRecognition();
            voiceBtn.setEnabled(true);
            voiceBtn.setImageResource(R.mipmap.ic_voicecontrol_off);
        }
    }

    /**
     * Writes the line.
     */
    private void WriteLine() {
        this.WriteLine("");
    }

    /**
     * Writes the line.
     * @param text The line to write.
     */
    private void WriteLine(String text) {
        //this._logText.append(text + "\n");
        Log.d("Yanzhi", text + "\n");
        //textStatus.setText(textStatus.getText() + text + "\n");
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(CompleteWidgetActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void controlFlight(String command, String params){
        if(DJISDKManager.getInstance().getProduct() != null) {
            Aircraft aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();

            if(!flightControllerInited)
            {
                flightControllerInited = true;
                mFlightController = aircraft.getFlightController();
                mFlightController.getSimulator()
                        .start(InitializationData.createInstance(new LocationCoordinate2D(23, 113), 10, 10),
                                new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError != null) {
                                            showToast(djiError.getDescription());
                                        }else
                                        {
                                            showToast("Start Simulator Success");
                                        }
                                    }
                                });

                mFlightController.getSimulator().setStateCallback(new SimulatorState.Callback() {
                    @Override
                    public void onUpdate(final SimulatorState stateData) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                String yaw = String.format("%.2f", stateData.getYaw());
                                String pitch = String.format("%.2f", stateData.getPitch());
                                String roll = String.format("%.2f", stateData.getRoll());
                                String positionX = String.format("%.2f", stateData.getPositionX());
                                String positionY = String.format("%.2f", stateData.getPositionY());
                                String positionZ = String.format("%.2f", stateData.getPositionZ());
                                textStatus.setText("Yaw : " + yaw + ", Pitch : " + pitch + ", Roll : " + roll + "\n" + ", PosX : " + positionX +
                                        ", PosY : " + positionY +
                                        ", PosZ : " + positionZ);
                            }
                        });
                    }
                });
            }
            if(command.equals("Photo")){
                speak("Start taking pictures");
                final dji.sdk.camera.Camera camera = aircraft.getCamera();
                if (camera != null) {
                    SettingsDefinitions.CameraMode mode = SettingsDefinitions.CameraMode.SHOOT_PHOTO;
                    camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO,
                            new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {

                                    camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {
                                            if (null == djiError) {
                                                showToast("Photographed successfully");
                                            } else {
                                                showToast(djiError.getDescription());
                                            }
                                        }
                                    });

                                }
                            });
                } else {
                    showToast("Unable to obtain aircraft camera");
                }
            }else if(command.equals("Landing")){
                speak("Start landing");
                aircraft.getFlightController().startLanding(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            showToast(djiError.getDescription());
                        }else{
                            showToast("Landing Success");
                        }
                    }
                });
            }else if(command.equals("TakeOff")){
                speak("Start to take off");
                aircraft.getFlightController().startTakeoff(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            showToast(djiError.getDescription());
                        }else{
                            showToast("TakeOff Success");
                        }
                    }
                });
            }else if(command.equals("Up")) {
                speak("Rise height");
                float throttleJoyStickControlMaxSpeed = 3;
                //mYaw = (float)(yawJoyStickControlMaxSpeed * pX);
                //mThrottle = (float)(yawJoyStickControlMaxSpeed * pY);
                mPitch = 0; mRoll = 0; mYaw = 0;
                mThrottle = (float)(throttleJoyStickControlMaxSpeed * 0.05);

                aircraft.getFlightController().sendVirtualStickFlightControlData(
                        new FlightControlData(mPitch, mRoll, mYaw, mThrottle), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                            }
                        }
                );
            }else if(command.equals("Down")) {
                speak("Drop height");
                float throttleJoyStickControlMaxSpeed = 3;
                //mYaw = (float)(yawJoyStickControlMaxSpeed * pX);
                //mThrottle = (float)(yawJoyStickControlMaxSpeed * pY);
                mThrottle = (float)(throttleJoyStickControlMaxSpeed * -0.05);
                mPitch = 0; mRoll = 0; mYaw = 0;

                aircraft.getFlightController().sendVirtualStickFlightControlData(
                        new FlightControlData(mPitch, mRoll, mYaw, mThrottle), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                            }
                        }
                );
            }else if(command.equals("Rotate")) {
                speak("Rotating body");
                showToast("under implementation");
            }else if(command.equals("Video")) {
                speak("Start recording video");
                showToast("under implementation");
            }
        }
        else
            showToast("No connection to the aircraft");
    }

    public class CSIRISDetectTask extends AsyncTask<String, Void, String> {
        private OnCSTaskCompleted listener;
        private String result = "";

        public CSIRISDetectTask(OnCSTaskCompleted listener){
            this.listener=listener;
        }

        @Override
        protected String doInBackground(String... params) {
            HttpClient httpclient = new DefaultHttpClient();
            try {
                //Pre-Trained Object (such as Fanta)
                String predictionUrl = irisUrl;

                HttpPost request = new HttpPost(predictionUrl);

                request.setHeader("Content-Type", "application/octet-stream");
                request.setHeader("Prediction-Key", irisKey);

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 80, output);

                request.setEntity(new ByteArrayEntity(output.toByteArray()));

                HttpResponse response = httpclient.execute(request);
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    String jsonStr = EntityUtils.toString(entity);

                    double probility = 0.0;
                    JSONObject jObject = new JSONObject(jsonStr);
                    JSONArray tags = jObject.getJSONArray("Predictions");
                    for(int i=0; i<tags.length(); i++)
                    {
                        JSONObject tag = tags.getJSONObject(i);
                        String tagName = tag.getString("Tag");
                        if(tagName.equals("bottle"))
                        {
                            probility = tag.getDouble("Probability");
                            break;
                        }
                    }

                    NumberFormat fmt = NumberFormat.getPercentInstance();
                    fmt.setMaximumFractionDigits(2);//Up to two decimal places, such as 25.23%
                    if(probility > 0.3) {
                        result = "Find object: " + fmt.format(probility);
                        if(player.isPlaying())
                            player.stop();
                        player.start();
                        speak("Object detected in the photo");
                    }
                    else
                        result = "Can't find object: " + fmt.format(probility);
                }
                else{
                    result = "Error Prediction Response.";
                }


            } catch (Exception e) {
                e.printStackTrace();
                return "Failed:" + e.getMessage();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            listener.onDetectObjectCompleted(result.toString());
        }
    }
}
