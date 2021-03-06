package eu.kolimaa.dev.deturpstudio;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import com.squareup.otto.Bus;

import java.util.ArrayList;

public class MusicService extends Service implements IMusicServiceController {

    private static MusicService msrv;
    private static Bus bus;

    private ArrayList<Track> tracks;
    private Track currentPlayingTrack;

    private MediaPlayer mp;

    private int currentTrackIndex;
    private boolean firstStart;

    private final static int START_INDEX = 0;

    public static Bus getServiceBus() {
        if (bus == null) {
            bus = new Bus();
        }
        return bus;
    }

    public MusicService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        msrv = this;

        firstStart = true;
        currentTrackIndex = START_INDEX;

        getServiceBus().post(new ServiceEvent());

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static IMusicServiceController getControllerInstance() {
        return msrv;
    }

    @Override
    public void setTracks(ArrayList<Track> tracks) {
        this.tracks = tracks;
    }

    @Override
    public synchronized void play(boolean switchTrack) {

        if (!tracks.isEmpty()) {

            if (switchTrack || firstStart) {
                switchTrack = false;
                firstStart = false;
                currentPlayingTrack = tracks.get(currentTrackIndex);

                mp = MediaPlayer.create(getApplicationContext(),
                        currentPlayingTrack.getTrackPath());

            }

            if (!switchTrack) {
                if (mp != null) {
                    if (mp.isPlaying()) {
                        mp.pause();

                    } else {
                        mp.start();
                    }
                }
            }

            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.stop();
                    mp.reset();
                    mp.release();

                    if (tracks.listIterator(currentTrackIndex+1).hasNext()) {
                        currentTrackIndex++;
                    } else {
                        currentTrackIndex = START_INDEX;
                    }

                    play(true);

                }

            });

        }
    }

    @Override
    public void stop() {
        if (mp != null) {
            if (!tracks.isEmpty()) {
                mp.seekTo(0);
            }
        }
    }

    @Override
    public synchronized void onRemove(int position) {

        getServiceBus().post(new TrackRemoveEvent());

        //decrement a currentTrackIndex if the removed track was upon in the list
        if (position < currentTrackIndex) {
            currentTrackIndex--;
        }

        if (tracks.isEmpty()) {
            firstStart = true;
        }

        //stop current track if it has been removed
        if (tracks.get(position) == currentPlayingTrack) {
            mp.reset();
            mp.release();
            mp = null;
            currentTrackIndex = START_INDEX;
            firstStart = true;
        }

    }

    @Override
    public synchronized void onAdd() {

    }

    @Override
    public void onDestroy() {
        mp = null;
        msrv = null;

        super.onDestroy();
    }

}
