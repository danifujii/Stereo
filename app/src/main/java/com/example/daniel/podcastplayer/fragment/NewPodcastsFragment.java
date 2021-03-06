package com.example.daniel.podcastplayer.fragment;


import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.daniel.podcastplayer.R;
import com.example.daniel.podcastplayer.adapter.EpisodeAdapter;
import com.example.daniel.podcastplayer.data.DbHelper;
import com.example.daniel.podcastplayer.data.Episode;
import com.example.daniel.podcastplayer.data.FileManager;
import com.example.daniel.podcastplayer.download.Downloader;
import com.example.daniel.podcastplayer.player.PlayerQueue;
import com.example.daniel.podcastplayer.player.PodcastPlayerService;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class NewPodcastsFragment extends Fragment {

    private RecyclerView rv;
    private List<Episode> deletedEpisodes = new ArrayList<>();
    private List<Integer> posEpisodes = new ArrayList<>();

    public NewPodcastsFragment() {} // Required empty public constructor

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_new_podcasts, container, false);

        rv = (RecyclerView)v.findViewById(R.id.new_episodes_rv);
        rv.setLayoutManager(new LinearLayoutManager(v.getContext()));
        configSwipe();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(PodcastPlayerService.ACTION_FINISH);
        filter.addAction(Downloader.ACTION_DOWNLOADED);
        filter.addAction(FileManager.ACTION_DELETE);
        getActivity().registerReceiver(receiver,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(localReceiver,filter);

        //Do this in case a download finishes while app is not active,
        //avoiding the broadcast receivers to work
        Downloader.updateDownloads(getActivity());
        setRecyclerViewInfo();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(receiver);
        LocalBroadcastManager.getInstance(getContext())
                .unregisterReceiver(localReceiver);
    }

    public void setRecyclerViewInfo(){
        List<Episode> latest = DbHelper.getInstance(getContext()).getLatestEpisodes();
        if (latest.isEmpty()) {
            getView().findViewById(R.id.np_message_tv).setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        }
        else {
            rv.setAdapter(new EpisodeAdapter(latest, true));
            rv.getAdapter().notifyDataSetChanged();
        }
    }

    private void configSwipe(){
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                EpisodeAdapter adapter = (EpisodeAdapter)rv.getAdapter();
                Episode ep = adapter.getItem(position);

                deletedEpisodes.add(ep);
                posEpisodes.add(position);
                showSnackbar();
                adapter.removeItem(position);
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE){
                    Episode e = ((EpisodeAdapter) rv.getAdapter()).getItem(viewHolder.getAdapterPosition());
                    boolean exists = false;
                    if (e != null)
                        exists =  FileManager.getEpisodeFile(getActivity(), e).exists() && !Downloader.isDownloading(e.getEpURL());
                    View itemView = viewHolder.itemView;
                    float height = (float)itemView.getBottom() - (float)itemView.getTop();
                    float width = height / 3;

                    if (dX > 0){
                        Paint p = new Paint();
                        if (dX > c.getWidth() / 3 || !exists){
                            p.setColor(ContextCompat.getColor(getContext(),R.color.red_delete));
                            RectF background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX,(float) itemView.getBottom());
                            c.drawRect(background,p);
                            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_delete_white_24dp);
                            RectF icon_dest = new RectF((float) itemView.getLeft() + width ,(float) itemView.getTop() + width,(float) itemView.getLeft()+ 2*width,(float)itemView.getBottom() - width);
                            c.drawBitmap(icon,null,icon_dest,p);
                        }
                        else{
                            p.setColor(ContextCompat.getColor(getContext(),R.color.mediumGray));
                            RectF background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX,(float) itemView.getBottom());
                            c.drawRect(background,p);
                            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_queue_white_24dp);
                            RectF icon_dest = new RectF((float) itemView.getLeft() + width ,(float) itemView.getTop() + width,(float) itemView.getLeft()+ 2*width,(float)itemView.getBottom() - width);
                            c.drawBitmap(icon,null,icon_dest,p);
                        }

                        if (!isCurrentlyActive && dX < c.getWidth() / 3 && exists) {
                            PlayerQueue.getInstance(getContext()).addEpisode(e, getContext());
                        }
                    }

                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

        };
        ItemTouchHelper helper = new ItemTouchHelper(simpleCallback);
        helper.attachToRecyclerView(rv);
    }

    private void showSnackbar(){
        Snackbar snackbar = Snackbar.make(
                getView().findViewById(R.id.new_podcasts_layout), getString(R.string.deleted), Snackbar.LENGTH_SHORT)
                .setAction(getString(R.string.undo), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //UNDO latest episode swiped
                        ((EpisodeAdapter)rv.getAdapter()).addItem(deletedEpisodes.get(deletedEpisodes.size()-1),
                                posEpisodes.get(posEpisodes.size()-1));
                        deletedEpisodes.remove(deletedEpisodes.size()-1);
                        posEpisodes.remove(posEpisodes.size()-1);
                        //The rest are removed
                        for (Episode ep: deletedEpisodes){
                            FileManager.deleteFile(getContext(), ep);
                            //DbHelper.getInstance(getContext()).updateEpisodeNew(ep.getEpURL(), false);
                        }
                    }
                });
        snackbar.setActionTextColor(ContextCompat.getColor(getContext(), R.color.green_done));
        snackbar.setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                if (event == DISMISS_EVENT_TIMEOUT || event == DISMISS_EVENT_SWIPE){
                    for (Episode ep : deletedEpisodes){
                        if (!FileManager.deleteFile(getContext(), ep))
                            DbHelper.getInstance(getContext()).updateEpisodeNew(ep.getEpURL(), false);
                    }
                }
                super.onDismissed(snackbar, event);
            }
        });
        snackbar.show();
    }

    private BroadcastReceiver localReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()){
                case (Downloader.ACTION_DOWNLOADED): {
                    setRecyclerViewInfo();
                    break;
                }
                case (PodcastPlayerService.ACTION_FINISH):{
                    rv.getAdapter().notifyDataSetChanged();
                    break;
                }
                case (FileManager.ACTION_DELETE):{
                    rv.getAdapter().notifyDataSetChanged();
                    break;
                }
            }
        }
    };

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                rv.getAdapter().notifyDataSetChanged();
                Downloader.removeDownload(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1));
            }
        }
    };
}
