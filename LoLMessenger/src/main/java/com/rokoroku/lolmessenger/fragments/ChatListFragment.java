package com.rokoroku.lolmessenger.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.rokoroku.lolmessenger.ClientActivity;
import com.rokoroku.lolmessenger.LolMessengerApplication;
import com.rokoroku.lolmessenger.R;
import com.rokoroku.lolmessenger.classes.ChatInformation;
import com.rokoroku.lolmessenger.utilities.ChatlistViewAdapter;

import java.util.ArrayList;

/**
 * Created by Youngrok Kim on 13. 8. 20.
 */
public class ChatListFragment extends Fragment {

    private String mUserID = null;
    private ClientActivity mActivity = null;
    private ArrayList<ChatInformation> mChatList = new ArrayList<ChatInformation>();

    private ListView mChatListView = null;
    private ChatlistViewAdapter mAdapter = null;

    private boolean isReady=false;


    public ChatListFragment() {
        super();
    }

    public ChatListFragment(ClientActivity mActivity, String mUserID) {
        this.mUserID = mUserID;
        this.mActivity = mActivity;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chatlist, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        //Called immediately after onCreateView(LayoutInflater, ViewGroup, Bundle) has returned
        super.onViewCreated(view, savedInstanceState);

        this.mActivity = (ClientActivity)getActivity();
        this.mUserID = mActivity.getUserID();
        this.mChatListView = (ListView) mActivity.findViewById(R.id.chatListView);

        if(mActivity.isReady()) {
            refreshChatList();
            isReady = true;
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (isVisibleToUser) {
            // 보인다.
            // if(isReady) refreshChatList();
        } else {
            // 안보인다.
        }
        super.setUserVisibleHint(isVisibleToUser);
    }

    public void refreshChatList() {
        this.mUserID = mActivity.getUserID();
        this.mChatListView = (ListView) mActivity.findViewById(R.id.chatListView);

        //열린 대화 개수
        mChatList = mActivity.getSQLiteDbAdapter().getChatList(mUserID);

        if(mChatList != null) {

            if(mChatListView.getAdapter() == null) {
                mAdapter = new ChatlistViewAdapter( mChatList,  mActivity.getRosterMap() );
                mAdapter.setInflater((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE), getActivity());
                mChatListView.setAdapter(mAdapter);
            } else {
                mAdapter.setItem(mChatList, mActivity.getRosterMap());
                mAdapter.notifyDataSetChanged();
            }

        } else {
            Toast.makeText(mActivity.getApplicationContext(), mActivity.getString(R.string.error_failure_retrieving_chatting_list), Toast.LENGTH_SHORT).show();
            mActivity.doLogout(true);
        }
    }
}
