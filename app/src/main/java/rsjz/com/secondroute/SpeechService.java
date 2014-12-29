package rsjz.com.secondroute;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SpeechService extends Service {
    GoogleSpeechRecognizer speechRecognizer;
    public SpeechService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        speechRecognizer = new GoogleSpeechRecognizer(this);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        speechRecognizer.stop();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
