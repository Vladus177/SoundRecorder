package vladus177.ru.soundrecorder;

import android.app.ProgressDialog;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    String filePath = Environment.getExternalStorageDirectory() + "/sound.pcm";
    private CountDownTimer countDownTimer = null;
    private ProgressDialog progressDialog;
    private AudioRecord recorder = null;
    AudioTrack at = null;
    int BufferElements2Rec = 2048;
    int BytesPerElement = 2;
    private boolean isRecording = false;

    private onRecord mOnRecord;
    private onPlay mOnPlay;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setButtonHandlers();
        enableButtons(false);
        progressDialog = new ProgressDialog(this);
        long interval = 1000;
        long startTime = 10 * 1000;
        countDownTimer = new CountDownTimerActivity(startTime, interval);

    }

    private void setButtonHandlers() {
        findViewById(R.id.btnStart).setOnClickListener(btnClick);
        findViewById(R.id.btnPlayRecord).setOnClickListener(btnClick);
    }

    private void enableButton(int id, boolean isEnable) {
        findViewById(id).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.btnStart, !isRecording);
        enableButton(R.id.btnPlayRecord, isRecording);
    }


    //short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    //bytes to short
    private short[] toShortArray(byte[] byteArray) {
        short[] shortArray = new short[byteArray.length / 2];
        ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);
        return shortArray;
    }

    //Reverse
    public void reverse(short[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        short tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    private void writeAudioDataToFile() {
        short sData[] = new short[BufferElements2Rec];
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while (isRecording) {
            recorder.read(sData, 0, BufferElements2Rec);
            try {
                byte bData[] = short2byte(sData);
                if (os != null) {
                    os.write(bData, 0, BufferElements2Rec * BytesPerElement);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            if (os != null) {
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStart: {
                    enableButtons(true);
                    mOnRecord = new onRecord();
                    mOnRecord.execute();
                    break;
                }
                case R.id.btnPlayRecord: {
                    enableButtons(true);
                    mOnPlay = new onPlay();
                    mOnPlay.execute();
                }
            }
        }
    };

    private void playAudio(String filePath) throws IOException {
        if (filePath == null)
            return;
        byte[] byteData;
        File file;
        file = new File(filePath);
        byteData = new byte[(int) file.length()];
        FileInputStream in;
        try {
            in = new FileInputStream(file);
            in.read(byteData);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (at != null) {
            short tempData[] = toShortArray(byteData);
            reverse(tempData);
            byte tempByte[] = short2byte(tempData);
            at.play();
            at.write(tempByte, 0, tempByte.length);
            isRecording = false;
            at.stop();
            at.release();
            at = null;
        } else {
            Toast toast = Toast.makeText(this, "Track not found", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public class CountDownTimerActivity extends CountDownTimer {
        public CountDownTimerActivity(long startTime, long interval) {
            super(startTime, interval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            int progress = (int) millisUntilFinished;
            progressDialog.setMessage("Record..." + " " + progress / 1000);

        }

        @Override
        public void onFinish() {

            if (recorder != null) {
                isRecording = false;
                recorder.stop();
                recorder.release();
                recorder = null;
                progressDialog.dismiss();
                if (mOnRecord != null) {
                    mOnRecord.cancel(true);
                }
            }
        }
    }

    public class onRecord extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] params) {
            int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                    RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLERATE,
                    RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING,
                    bufferSize);
            countDownTimer.start();
            recorder.startRecording();
            isRecording = true;
            writeAudioDataToFile();
            return null;
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
            super.onPreExecute();
        }
    }

    public class onPlay extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] params) {
            int intSize = android.media.AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE,
                    RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
            at = new AudioTrack(AudioManager.STREAM_MUSIC,
                    RECORDER_SAMPLERATE,
                    RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING,
                    intSize, AudioTrack.MODE_STREAM);

            try {
                playAudio(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Object o) {
            enableButtons(false);
            if (mOnPlay != null) {
                mOnPlay.cancel(true);
            }
            super.onPostExecute(o);
        }
    }
}
