/**
 * For information on usage and redistribution, and for a DISCLAIMER OF ALL WARRANTIES, see the
 * file, "LICENSE.txt," in this distribution.
 */

package com.noisepages.nettoyeur.usbmiditest;

import java.util.List;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.noisepages.nettoyeur.midi.MidiReceiver;
import com.noisepages.nettoyeur.midi.util.SystemMessageDecoder;
import com.noisepages.nettoyeur.midi.util.SystemMessageReceiver;
import com.noisepages.nettoyeur.usb.ConnectionFailedException;
import com.noisepages.nettoyeur.usb.DeviceNotConnectedException;
import com.noisepages.nettoyeur.usb.InterfaceNotAvailableException;
import com.noisepages.nettoyeur.usb.UsbBroadcastHandler;
import com.noisepages.nettoyeur.usb.midi.UsbMidiDevice;
import com.noisepages.nettoyeur.usb.midi.UsbMidiDevice.UsbMidiInput;
import com.noisepages.nettoyeur.usb.midi.UsbMidiDevice.UsbMidiOutput;
import com.noisepages.nettoyeur.usb.midi.util.UsbMidiInputSelector;
import com.noisepages.nettoyeur.usb.midi.util.UsbMidiOutputSelector;
import com.noisepages.nettoyeur.usb.util.AsyncDeviceInfoLookup;
import com.noisepages.nettoyeur.usb.util.UsbDeviceSelector;

public class UsbMidiTest extends Activity {

	private TextView mainText;
	private UsbMidiDevice midiDevice = null;
	private MidiReceiver midiOut = null;
	private Handler handler;
	private SystemMessageDecoder midiSysDecoder;
	private SystemMessageReceiver midiSysMsgReceiver;

	int valBefore;

	private Toast toast = null;

	private void toast(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (toast == null) {
					toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
				}
				toast.setText("USBMIDI" + ": " + msg);
				toast.show();
			}
		});
	}

	private final SystemMessageReceiver midiSysExReceiver = new SystemMessageReceiver() {

		@Override
		public void onSystemExclusive(byte[] sysex) {
			// TODO Auto-generated method stub
			StringBuilder sb = new StringBuilder();
		    for (byte b : sysex) {
		        sb.append(String.format("%02X ", b));
		    }
			update("sysex: " + sb);
			//Log.v("USBMIDI", "sysex");
		}

		@Override
		public void onTimeCode(int value) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSongPosition(int pointer) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSongSelect(int index) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onTuneRequest() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onTimingClock() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStart() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onContinue() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStop() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onActiveSensing() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSystemReset() {
			// TODO Auto-generated method stub

		}

	};

	private final MidiReceiver midiReceiver = new MidiReceiver() {
		@Override
		public void onRawByte(byte value) {
			//update("raw byte: " + (value & 0xFF));
			if (midiSysDecoder != null) {
				midiSysDecoder.decodeByte(value);
			}
			if ( (value & 0xFF) == 247)
				toast("val = " + valBefore);
			valBefore = (value & 0xFF);
		}

		@Override
		public void onProgramChange(int channel, int program) {
			update("program change: " + channel + ", " + program);
		}

		@Override
		public void onPolyAftertouch(int channel, int key, int velocity) {
			update("poly aftertouch: " + channel + ", " + key + ", " + velocity);
		}

		@Override
		public void onPitchBend(int channel, int value) {
			update("pitch bend: " + channel + ", " + value);
		}

		@Override
		public void onNoteOn(int channel, int key, int velocity) {
			update("note on: " + channel + ", " + key + ", " + velocity);
		}

		@Override
		public void onNoteOff(int channel, int key, int velocity) {
			update("note off: " + channel + ", " + key + ", " + velocity);
		}

		@Override
		public void onControlChange(int channel, int controller, int value) {
			update("control change: " + channel + ", " + controller + ", " + value);
		}

		@Override
		public void onAftertouch(final int channel, final int velocity) {
			update("aftertouch: " + channel + ", " + velocity);
		}

		@Override
		public boolean beginBlock() {
			return false;
		}

		@Override
		public void endBlock() {}

	};

	private void update(final String n) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				mainText.setText(mainText.getText() + "\n" + n);
				if (mainText.getText().length() > 400) //clear it once in a while...
					mainText.setText("");
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handler = new Handler();
		setContentView(R.layout.activity_main);
		mainText = (TextView) findViewById(R.id.mainText);
		mainText.setMovementMethod(new ScrollingMovementMethod());


		midiSysDecoder = new SystemMessageDecoder(midiSysExReceiver);

		UsbMidiDevice.installBroadcastHandler(this, new UsbBroadcastHandler() {

			@Override
			public void onPermissionGranted(UsbDevice device) {
				if (midiDevice == null || !midiDevice.matches(device)) return;
				try {
					midiDevice.open(UsbMidiTest.this);
				} catch (ConnectionFailedException e1) {
					update("\n\nConnection failed.");
					midiDevice = null;
					return;
				}
				new UsbMidiInputSelector(midiDevice) {

					@Override
					protected void onInputSelected(UsbMidiInput input, UsbMidiDevice device, int iface,
							int index) {
						update("\n\nInput: Interface " + iface + ", Index " + index);
						input.setReceiver(midiReceiver);
						try {
							input.start();
						} catch (DeviceNotConnectedException e) {
							mainText.setText("MIDI device has been disconnected.");
						} catch (InterfaceNotAvailableException e) {
							update("\n\nMIDI interface is unavailable.");
						}
					}

					@Override
					protected void onNoSelection(UsbMidiDevice device) {
						update("\n\nNo inputs available.");
					}
				}.show(getFragmentManager(), null);
			}

			@Override
			public void onPermissionDenied(UsbDevice device) {
				if (midiDevice == null || !midiDevice.matches(device)) return;
				mainText.setText("Permission denied for device " + midiDevice.getCurrentDeviceInfo() + ".");
				midiDevice = null;
			}

			@Override
			public void onDeviceDetached(UsbDevice device) {
				if (midiDevice == null || !midiDevice.matches(device)) return;
				midiDevice.close();
				midiDevice = null;
				mainText.setText("USB MIDI device detached.");
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (midiDevice != null) {
			midiDevice.close();
		}
		UsbMidiDevice.uninstallBroadcastHandler(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.connect_item:
			if (midiDevice == null) {
				chooseMidiDevice();
			} else {
				midiDevice.close();
				midiDevice = null;
				mainText.setText("USB MIDI connection closed.");
			}
			return true;
		case R.id.stop_stream:
			//a reset stops things...
			byte datar[] = new byte[] { (byte) 0xF0, (byte) 0x7D, (byte) 0x00, (byte) 0x22, (byte) 0xF7 };
			midiOut.beginBlock();

			for (int i=0; i<datar.length; i++) {
				midiOut.onRawByte(datar[i]);
			}
			midiOut.endBlock();
			return true;
		case R.id.reset_item: {
			//set to host mode
			byte data[] = new byte[] { (byte) 0xF0, (byte) 0x7D, (byte) 0x00, (byte) 0x5A, (byte) 0x00, (byte) 0xF7 };
			midiOut.beginBlock();

			for (int i=0; i<data.length; i++) {
				midiOut.onRawByte(data[i]);
			}
			midiOut.endBlock();
			//set interval to 20ms
			data = new byte[] { (byte) 0xF0, (byte) 0x7D, (byte) 0x00, (byte) 0x03, (byte) 0x00,(byte) 0x14, (byte) 0xF7 };
			midiOut.beginBlock();
			for (int i=0; i<data.length; i++) {
				midiOut.onRawByte(data[i]);
			}
			midiOut.endBlock();
			//start stream port 0
			data = new byte[] { (byte) 0xF0, (byte) 0x7D, (byte) 0x00, (byte) 0x01, (byte) 0x40, (byte) 0xF7 };
			midiOut.beginBlock();
			for (int i=0; i<data.length; i++) {
				midiOut.onRawByte(data[i]);
			}
			midiOut.endBlock();
		}
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void chooseMidiDevice() {
		final List<UsbMidiDevice> devices = UsbMidiDevice.getMidiDevices(this);
		new AsyncDeviceInfoLookup() {

			@Override
			protected void onLookupComplete() {
				new UsbDeviceSelector<UsbMidiDevice>(devices) {

					@Override
					protected void onDeviceSelected(UsbMidiDevice device) {
						midiDevice = device;
						mainText.setText("Selected device: " + device.getCurrentDeviceInfo());
						midiDevice.requestPermission(UsbMidiTest.this);

						UsbMidiOutputSelector outputSelector = new UsbMidiOutputSelector(midiDevice) {

							@Override
							protected void onOutputSelected(UsbMidiOutput output, UsbMidiDevice device, int iface,
									int index) {
								toast("Output selection: Interface " + iface + ", Output " + index);
								try {
									midiOut = output.getMidiOut();
								} catch (DeviceNotConnectedException e) {
									toast("MIDI device has been disconnected");
								} catch (InterfaceNotAvailableException e) {
									toast("MIDI interface is unavailable");
								}
							}

							@Override
							protected void onNoSelection(UsbMidiDevice device) {
								toast("No output selected");
							}
						};

						outputSelector.show(getFragmentManager(), "");

					}

					@Override
					protected void onNoSelection() {
						mainText.setText("No USB MIDI device selected.");
					}
				}.show(getFragmentManager(), null);

			}
		}.execute(devices.toArray(new UsbMidiDevice[devices.size()]));
	}
}
