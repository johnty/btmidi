/**
 * 
 * For information on usage and redistribution, and for a DISCLAIMER OF ALL WARRANTIES, see the
 * file, "LICENSE.txt," in this distribution.
 * 
 */

package com.noisepages.nettoyeur.miditest;

import java.io.IOException;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.noisepages.nettoyeur.bluetooth.util.*;
import com.noisepages.nettoyeur.bluetooth.BluetoothSppConnection;
import com.noisepages.nettoyeur.bluetooth.BluetoothSppObserver;
import com.noisepages.nettoyeur.bluetooth.midi.BluetoothMidiDevice;
import com.noisepages.nettoyeur.midi.MidiReceiver;
import com.noisepages.nettoyeur.midi.util.SystemMessageDecoder;
import com.noisepages.nettoyeur.midi.util.SystemMessageReceiver;


public class BluetoothMidiTest extends Activity implements OnClickListener {

  private static final String TAG = "Midi Test";

  private static final int CONNECT = 1;

  private Button connect;
  private Button play;
  private Button reset;
  private TextView logs;

  private BluetoothMidiDevice midiService = null;
  private SystemMessageDecoder midiSysDecoder;

  private Toast toast = null;

  private void toast(final String msg) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (toast == null) {
          toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
        }
        toast.setText(TAG + ": " + msg);
        toast.show();
      }
    });
  }

  private void post(final String s) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        logs.append(s + ((s.endsWith("\n")) ? "" : "\n"));
        if (logs.length() >= 300) {
        	logs.setText("");
        }
      }
    });
  }

  private final BluetoothSppObserver observer = new BluetoothSppObserver() {
    @Override
    public void onDeviceConnected(BluetoothDevice device) {
      post("device connected: " + device);
    }

    @Override
    public void onConnectionLost() {
      post("connection lost");
    }

    @Override
    public void onConnectionFailed() {
      post("connection failed");
    }
  };
  
  private final SystemMessageReceiver sysMsgReceiver = new SystemMessageReceiver() {
	
	@Override
	public void onTuneRequest() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onTimingClock() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onTimeCode(int value) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSystemReset() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSystemExclusive(byte[] sysex) {
		// TODO Auto-generated method stub
		StringBuilder sb = new StringBuilder();
	    for (byte b : sysex) {
	        sb.append(String.format("%02X ", b));
	    }
	    post(sb.toString());
	}
	
	@Override
	public void onStop() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSongSelect(int index) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSongPosition(int pointer) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onContinue() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onActiveSensing() {
		// TODO Auto-generated method stub
		
	}
};

  private final MidiReceiver receiver = new MidiReceiver() {
	  
    @Override
    public void onNoteOff(int channel, int key, int velocity) {
      post("note off: " + channel + ", " + key + ", " + velocity);
    }

    @Override
    public void onNoteOn(int channel, int key, int velocity) {
      post("note on: " + channel + ", " + key + ", " + velocity);
    }

    @Override
    public void onAftertouch(int channel, int velocity) {
      post("aftertouch: " + channel + ", " + velocity);
    }

    @Override
    public void onControlChange(int channel, int controller, int value) {
      post("control change: " + channel + ", " + controller + ", " + value);
    }

    @Override
    public void onPitchBend(int channel, int value) {
      post("pitch bend: " + channel + ", " + value);
    }

    @Override
    public void onPolyAftertouch(int channel, int key, int velocity) {
      post("polyphonic aftertouch: " + channel + ", " + key + ", " + velocity);
    }

    @Override
    public void onProgramChange(int channel, int program) {
      post("program change: " + channel + ", " + program);
    }

    @Override
    public void onRawByte(byte value) {
      //post("raw byte: " + Integer.toHexString(value));
    	midiSysDecoder.decodeByte(value);
    }

    @Override
    public boolean beginBlock() {
      return false;
    }

    @Override
    public void endBlock() {}
  };

  @Override
  protected void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initGui();
    try {
      midiService = new BluetoothMidiDevice(observer, receiver);
      midiSysDecoder = new SystemMessageDecoder(sysMsgReceiver);
    } catch (IOException e) {
      toast("MIDI not available");
      finish();
    }
  };

  @Override
  protected void onDestroy() {
    super.onDestroy();
    cleanup();
  }

  private void initGui() {
    setContentView(R.layout.main);
    connect = (Button) findViewById(R.id.connect_button);
    connect.setOnClickListener(this);
    play = (Button) findViewById(R.id.play_button);
    play.setOnClickListener(this);
    reset = (Button) findViewById(R.id.reset_button);
    reset.setOnClickListener(this);
    
    logs = (TextView) findViewById(R.id.log_box);
    logs.setMovementMethod(new ScrollingMovementMethod());
  }

  private void cleanup() {
    if (midiService != null) {
      midiService.close();
      midiService = null;
    }
  }

  @Override
  public void finish() {
    cleanup();
    super.finish();
  }

  private int note = 60;
  private boolean on = false;

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.connect_button:
        if (midiService.getConnectionState() == BluetoothSppConnection.State.NONE) {
          startActivityForResult(new Intent(this, DeviceListActivity.class), CONNECT);
        } else {
          midiService.close();
        }
        break;
      case R.id.reset_button:
    	  post("switching to host mode and turning on s1");
    	  //byte data[] = new byte[] { (byte) 0xF0, (byte) 0x7D, (byte) 0x00, (byte) 0x22, (byte) 0xF7 };

    	  //host mode
    	  byte data[] = new byte[] { (byte) 0xF0, (byte) 0x7D, (byte) 0x00, (byte) 0x5A, (byte) 0x00, (byte) 0xF7 };
    	  midiService.getMidiOut().beginBlock();
    	  
    	  for (int i=0; i<data.length; i++) {
    		  midiService.getMidiOut().onRawByte(data[i]);
    	  }
    	  midiService.getMidiOut().endBlock();
    	  //set interval to 10ms
    	  data = new byte[] { (byte) 0xF0, (byte) 0x7D, (byte) 0x00, (byte) 0x03, (byte) 0x00,(byte) 0x0A, (byte) 0xF7 };
    	  midiService.getMidiOut().beginBlock();
    	  for (int i=0; i<data.length; i++) {
    		  midiService.getMidiOut().onRawByte(data[i]);
    	  }
    	  midiService.getMidiOut().endBlock();
    	  //start stream port 0
    	  data = new byte[] { (byte) 0xF0, (byte) 0x7D, (byte) 0x00, (byte) 0x01, (byte) 0x41, (byte) 0xF7 };
    	  midiService.getMidiOut().beginBlock();
    	  for (int i=0; i<data.length; i++) {
    		  midiService.getMidiOut().onRawByte(data[i]);
    	  }
    	  midiService.getMidiOut().endBlock();
    	  break;
      case R.id.play_button:
        if (!on) {
          midiService.getMidiOut().onNoteOn(0, note, 80);
        } else {
          midiService.getMidiOut().onNoteOff(0, note, 64);
          note++;
        }
        on = !on;
      default:
        break;
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case CONNECT:
        if (resultCode == Activity.RESULT_OK) {
          String address = data.getExtras().getString(DeviceListActivity.DEVICE_ADDRESS);
          try {
            midiService.connect(address);
          } catch (IOException e) {
            toast(e.getMessage());
          }
        }
        break;
    }
  }
}
