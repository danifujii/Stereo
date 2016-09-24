package com.example.daniel.podcastplayer;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.example.daniel.podcastplayer.adapter.ImageAdapter;
import com.example.daniel.podcastplayer.data.DbHelper;


/**
 * A simple {@link Fragment} subclass.
 */
public class SubscriptionsFragment extends Fragment {


    public SubscriptionsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_subscriptions, container, false);
        GridView gv = (GridView)v.findViewById(R.id.subs_gv);
        gv.setAdapter(new ImageAdapter(getContext(), DbHelper.getInstance(getContext()).getPodcasts()));
        return v;
    }

}
