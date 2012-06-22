/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.winograd.mp3remote;

import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.Gson;

/**
 * This is the main Activity that displays the current chat session.
 */
public class BluetoothChat extends Activity {
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;
    
    private static final char END_CMD_CHAR = '!';

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    // Layout Views
    private TextView mTitle;
    private ListView mConversationView;    
    private LinearLayout playerLayout = null;
    private Button playButton = null;
    private Button pauseButton = null;
    private Button prevButton = null;
    private Button nextButton = null;
    private Button connectButton = null;
    private SeekBar volumeSlider = null;
    private SeekBar seekSlider = null;
    private TextView trackTextView = null;
    private TextView artistTextView = null;
    private TextView albumTextView = null;
    
    private ArrayList<Song> songs = new ArrayList<Song>();
    private ArrayAdapter<Song> mAdapter;
    private ListView library;
    
    Gson gson = null;
    
    String mReceived = "";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        playerLayout = (LinearLayout) findViewById(R.id.playerLayout);
        
        connectButton = (Button) findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				scanForDevices();
			}
		});        
        
        playButton = (Button) findViewById(R.id.playButton);
        playButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				sendMessage(Command.PLAY.toString());
			}
		});
        
        pauseButton = (Button) findViewById(R.id.pauseButton);
        pauseButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				sendMessage(Command.PAUSE.toString());
			}
		});
        
        nextButton = (Button) findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				sendMessage(Command.NEXT_TRACK.toString());
			}
		});
        
        prevButton = (Button) findViewById(R.id.prevButton);
        prevButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				sendMessage(Command.PREV_TRACK.toString());
			}
		});
        
        volumeSlider = (SeekBar) findViewById(R.id.volumeSlider);
        volumeSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser){
					sendMessage(Command.VOLUME.toString() + "," + progress);
				}
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
        });
        
        seekSlider = (SeekBar) findViewById(R.id.seekSlider);
        seekSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser){
					sendMessage(Command.SEEK.toString() + "," + progress);
				}
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
        });        
        
        trackTextView = (TextView) findViewById(R.id.trackTitle);
        artistTextView = (TextView) findViewById(R.id.artistTextView);
        albumTextView = (TextView) findViewById(R.id.albumTitle);
        
        //String[] from = new String[] { ReviewsDB.KEY_TITLE, ReviewsDB.KEY_CATEGORY, ReviewsDB.KEY_RATING };
		//int[] to = new int[] { R.id.nameText, R.id.categoryText, R.id.ratingBar };
        Song s = new Song();
        s.title = "Title";
        s.album = "Album";
        s.artist = "Artist";
        songs.add(s);
		ArrayList<String> data = new ArrayList<String>();
		data.add("The Prolific Oven");
		data.add("Fraiche Yogurt");
		data.add("Patxis Pizza");
		data.add("Jing Jing");
		data.add("St. Michael's Alley");        
        mAdapter = new ArrayAdapter<Song>(
    			this,
    			android.R.layout.simple_list_item_1,
    			songs
    	);
        mAdapter.notifyDataSetChanged();
		//mCursorAdapter = new SimpleCursorAdapter(this, R.layout.row3, cursor, from, to);
        
        
        library = (ListView)findViewById(R.id.library);
        library.setAdapter(mAdapter);

        library.setOnItemClickListener(new OnItemClickListener(){
			public void onItemClick(AdapterView<?> av, View view, int pos,
					long arg3) {
				ListView lv = (ListView)av;
				Song s = (Song)lv.getAdapter().getItem(pos);
				sendMessage("SONG,"+s.songNumber);
			}
        });
        
        
        
        gson = new Gson();
        updateInterfaceState();
    }
    
    private void updateInterfaceState(){
    	boolean enabled = mChatService != null && mChatService.getState() == BluetoothChatService.STATE_CONNECTED;
    	if (enabled){
    		playerLayout.setVisibility(View.VISIBLE);
    		connectButton.setVisibility(View.GONE);
    	}
    	else{
    		playerLayout.setVisibility(View.GONE);
    		connectButton.setVisibility(View.VISIBLE);
    	}
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
        updateInterfaceState();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
        updateInterfaceState();
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setVisibility(View.GONE);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
    	message += END_CMD_CHAR;
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            if(D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };

	private void handleCommand(String commandJSON){
		try{
			CommandArgs cmd = gson.fromJson(commandJSON, CommandArgs.class);
			
			if (cmd.title != null){
				trackTextView.setText(cmd.state + ": " + cmd.title);
			}
			if (cmd.artist != null){
				artistTextView.setText(cmd.artist);
			}
			if (cmd.album != null){
				albumTextView.setText(cmd.album);
			}
			if (cmd.position != -1){
				seekSlider.setProgress(cmd.position);
			}

			switch(Command.valueOf(cmd.command)){
			case CONNECTED:
				volumeSlider.setProgress(cmd.volume);
				break;
			case PLAY:
				break;
			case PAUSE:
				break;
			case NEXT_TRACK:
				break;
			case PREV_TRACK:
				break;
			case VOLUME:
				break;
			case SEEK:
				break;
			case MESSAGE:
				 //shown in catch all case
			case LIBRARY:
				songs = cmd.songs;
				mAdapter = new ArrayAdapter(
						this,
						android.R.layout.simple_list_item_1,
						songs);
				mAdapter.notifyDataSetChanged();
				library.setAdapter(mAdapter);
				mConversationArrayAdapter.add("numSongs : " + cmd.songs.size());
				mConversationArrayAdapter.add("first : " + songs.get(0).title);
				break;
			default:
				mConversationArrayAdapter.add("UErr: "+ cmd.command.toString());
				Toast.makeText(this, "Unknown Error", Toast.LENGTH_SHORT).show();
				break;
			}
			
			if(cmd.message != null){
				Toast.makeText(this, cmd.message, Toast.LENGTH_LONG).show();
			}
		}
		catch(Exception e){
			mConversationArrayAdapter.add("ERROR: "+e.toString());
		}
		
		mConversationArrayAdapter.add("received : " + commandJSON);
	}    

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
    	
    	private void processReceivedMessage(String readMessage){
    		String allReceived = mReceived.concat(readMessage);
    		int index;
    		while( (index=allReceived.indexOf('!')) != -1){
    			String command = allReceived.substring(0, index);
    			allReceived = allReceived.substring(index+1);
    			handleCommand(command);
    		}
    		
    		mReceived = allReceived;
    	}
    	
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
                    mConversationArrayAdapter.clear();
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    break;
                }
                updateInterfaceState();
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                // construct a string from the valid bytes in the buffer
                String readMessage = (String)msg.obj;
                processReceivedMessage(readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mChatService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }
    
    public void scanForDevices(){
        // Launch the DeviceListActivity to see devices and do scan
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
        	scanForDevices();
            return true;
        case R.id.debug:
            // Ensure this device is discoverable by others
            if(mConversationView.getVisibility() == View.GONE){
            	mConversationView.setVisibility(View.VISIBLE);
            }
            else{
            	mConversationView.setVisibility(View.GONE);
            }
            return true;
        }
        return false;
    }

}