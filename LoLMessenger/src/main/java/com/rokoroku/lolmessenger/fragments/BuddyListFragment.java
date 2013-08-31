package com.rokoroku.lolmessenger.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.rokoroku.lolmessenger.ClientActivity;
import com.rokoroku.lolmessenger.R;
import com.rokoroku.lolmessenger.classes.ParcelableRoster;
import com.rokoroku.lolmessenger.utilities.RosterAdapter;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Youngrok Kim on 13. 8. 22.
 */
public class BuddyListFragment extends Fragment implements ExpandableListView.OnChildClickListener {

    private final String TAG = "com.example.lolmessenger.BuddyListFragment";

    private String mUserID = null;
    private ClientActivity mActivity = null;
    protected Map<String, ParcelableRoster> mRosterMap = null;
    private ArrayList<String> mRosterGroupList = null;

    private boolean isReady = false;

    ExpandableListView mExpandbleListView = null;

    public BuddyListFragment() {
    }

    public BuddyListFragment(ClientActivity mActivity, String mUserID) {
        this.mUserID = mUserID;
        this.mActivity = mActivity;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_buddylist, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mExpandbleListView = (ExpandableListView) getView().findViewById(R.id.expandableListView);

        if(mActivity.isReady()) {
            refreshBuddyList();
            isReady = true;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        //if(isVisibleToUser) {
        //    if(isReady) refreshBuddyList();
        //}
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
                                int groupPosition, int childPosition, long id) {
        Toast.makeText(mActivity, "Clicked On Child", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG,"onResume()");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG,"onStart()");
    }

    public void refreshBuddyList() {

        mRosterMap = ((ClientActivity) mActivity).getRosterMap();
        mRosterGroupList = ((ClientActivity) mActivity).getRosterGroupList();

        RosterAdapter mNewAdapter = new RosterAdapter( mRosterMap, mRosterGroupList );

        mNewAdapter.setInflater((LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE), mActivity);
        mExpandbleListView = (ExpandableListView) getView().findViewById(R.id.expandableListView);
        mExpandbleListView.setAdapter(mNewAdapter);
        mExpandbleListView.setOnChildClickListener(this);
        for(int i=0; i<mRosterGroupList.size()-1; i++) {
            mExpandbleListView.expandGroup(i);
        }
    }

    public boolean isReady() {
        return isReady;
    }
}