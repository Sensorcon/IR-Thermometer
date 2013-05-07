package com.sensorcon.irthermometer;

import java.util.EventObject;

import com.sensorcon.sdhelper.ConnectionBlinker;
import com.sensorcon.sdhelper.SDHelper;
import com.sensorcon.sdhelper.SDStreamer;
import com.sensorcon.sensordrone.Drone;
import com.sensorcon.sensordrone.Drone.DroneEventListener;
import com.sensorcon.sensordrone.Drone.DroneStatusListener;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.view.ViewGroup.MarginLayoutParams;

public class MainActivity extends Activity {
	
	private ImageView lightGreen;
	private ImageView lightRed;
	private ImageView lightBlue;
	private ImageView lightOff;
	private ImageButton setRefReleasedLarge;
	private ImageButton setRefPressedLarge;
	private ImageButton setRefReleasedSmall;
	private ImageButton setRefPressedSmall;
	private ImageButton inputRefReleased;
	private ImageButton inputRefPressed;
	private ImageButton maxHoldReleased;
	private ImageButton maxHoldPressed;
	private ImageView iv_directions;
	private TextView tv_valueRef;
	private TextView labelRef;
	private TextView labelUnit;
	private TextView labelScan;
	private TextView labelMax;
	private TextView tv_valueIr;
	private RadioGroup radioGroupUnits;
	private RadioGroup radioGroupRef;
	private RadioButton radioButtonOff;
	private RadioButton radioButton2f;
	private RadioButton radioButton10f;
	private RadioButton radioButton20f;
	private RadioButton radioButtonF;
	private RadioButton radioButtonC;
	
	private float valueRef;
	private float valueIr;
	private float reference;
	private float max;
	private boolean checkRef;
	private boolean showRef;
	private boolean celcius;
	private boolean maxHold;
	
	private int mode;
	private boolean modeChange;
	private final int NORMAL_MODE = 0;
	private final int ADVANCED_MODE = 1;
	
	private Handler displayIrHandler = new Handler();
	private Handler displayRefHandler = new Handler();
	private Handler modeHandler = new Handler();
	
	private Typeface lcdFont;

	protected Drone myDrone;
	public Storage box;
	public SDHelper myHelper;
	
	private boolean on;
	private boolean maxHoldOn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		lightGreen = (ImageView)findViewById(R.id.light_green);
		lightRed = (ImageView)findViewById(R.id.light_red);
		lightBlue = (ImageView)findViewById(R.id.light_blue);
		lightOff = (ImageView)findViewById(R.id.light_off);
		setRefReleasedLarge = (ImageButton)findViewById(R.id.set_ref_released_large);
		setRefPressedLarge = (ImageButton)findViewById(R.id.set_ref_pressed_large);
		setRefReleasedSmall = (ImageButton)findViewById(R.id.set_ref_released_small);
		setRefPressedSmall = (ImageButton)findViewById(R.id.set_ref_pressed_small);
		inputRefReleased = (ImageButton)findViewById(R.id.input_ref_released);
		inputRefPressed = (ImageButton)findViewById(R.id.input_ref_pressed);
		maxHoldReleased = (ImageButton)findViewById(R.id.max_hold_released);
		maxHoldPressed = (ImageButton)findViewById(R.id.max_hold_pressed);
		iv_directions = (ImageView)findViewById(R.id.directions);
		tv_valueRef = (TextView)findViewById(R.id.value_ref);
		tv_valueIr = (TextView)findViewById(R.id.value_ir);
		labelRef = (TextView)findViewById(R.id.label_ref);
		labelUnit = (TextView)findViewById(R.id.label_unit);
		labelScan = (TextView)findViewById(R.id.label_scan);
		labelMax = (TextView)findViewById(R.id.label_max);
		radioGroupRef = (RadioGroup)findViewById(R.id.radio_group_ref);
		radioGroupUnits = (RadioGroup)findViewById(R.id.radio_group_units);
		
		labelUnit.setText("F" + (char) 0x00B0 );
		
		mode = NORMAL_MODE;
		modeChange = false;
		
		valueIr = 0;
		valueRef = 0;
		reference = 9999;
		checkRef = false;
		showRef = false;
		celcius = false;
		
		lightGreen.setVisibility(View.GONE);
		lightBlue.setVisibility(View.GONE);
		lightRed.setVisibility(View.GONE);
		tv_valueRef.setVisibility(View.GONE);
		tv_valueIr.setVisibility(View.GONE);
		labelRef.setVisibility(View.GONE);
		labelUnit.setVisibility(View.GONE);
		labelScan.setVisibility(View.GONE);
		labelMax.setVisibility(View.GONE);
		setRefPressedLarge.setVisibility(View.GONE);
		setRefPressedSmall.setVisibility(View.GONE);
		setRefReleasedSmall.setVisibility(View.GONE);
		inputRefPressed.setVisibility(View.GONE);
		inputRefReleased.setVisibility(View.GONE);
		maxHoldReleased.setVisibility(View.GONE);
		maxHoldPressed.setVisibility(View.GONE);
		
		on = false;
		maxHoldOn = false;
		
		lcdFont = Typeface.createFromAsset(this.getAssets(), "DS-DIGI.TTF");	
		tv_valueRef.setTypeface(lcdFont);
		tv_valueIr.setTypeface(lcdFont);
		labelRef.setTypeface(lcdFont);
		labelUnit.setTypeface(lcdFont);
		labelScan.setTypeface(lcdFont);
		labelMax.setTypeface(lcdFont);
		
		/*
		 * Controls the program flow for when the left button is pressed/released
		 */
		setRefReleasedLarge.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				/*
				 * If button is pressed down
				 */
				if(event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
					refButtonLargePressed();
				}
				/*
				 * If button is released
				 */
				else if(event.getAction() == android.view.MotionEvent.ACTION_UP) {
					refButtonLargeReleased();
				}
				return true;
			}
		});
		
		setRefReleasedSmall.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				/*
				 * If button is pressed down
				 */
				if(event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
					refButtonSmallPressed();
				}
				/*
				 * If button is released
				 */
				else if(event.getAction() == android.view.MotionEvent.ACTION_UP) {
					refButtonSmallReleased();
				}
				return true;
			}
		});
		
		inputRefReleased.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				/*
				 * If button is pressed down
				 */
				if(event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
					inputRefButtonPressed();
				}
				/*
				 * If button is released
				 */
				else if(event.getAction() == android.view.MotionEvent.ACTION_UP) {
					inputRefButtonReleased();
				}
				return true;
			}
		});
		
		maxHoldReleased.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				/*
				 * If button is pressed down
				 */
				if(event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
					maxHoldButtonPressed();
				}
				/*
				 * If button is released
				 */
				else if(event.getAction() == android.view.MotionEvent.ACTION_UP) {
					maxHoldButtonReleased();
				}
				return true;
			}
		});
		
		// Initialize drone variables
		myDrone = new Drone();
		box = new Storage(this);
		myHelper = new SDHelper();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();

		if (isFinishing()) {
			// Try and nicely shut down
			doOnDisconnect();
			// A brief delay
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// Unregister the listener
			myDrone.unregisterDroneEventListener(box.droneEventListener);
			myDrone.unregisterDroneStatusListener(box.droneStatusListener);

		} else { 
			//It's an orientation change.
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.connect:
			scan();
			break;
		case R.id.disconnect:
			doOnDisconnect();
			break;
		case R.id.normal_mode:
			modeChange = true;
			mode = NORMAL_MODE;
			modeHandler.post(changeModeRunnable);
			break;
		case R.id.advanced_mode:
			modeChange = true;
			mode = ADVANCED_MODE;
			modeHandler.post(changeModeRunnable);
			break;
		case R.id.directions:
			break;
		}
			
		return true;
	}
	
	public void refButtonLargePressed() {
		if(!on) {
			setRefReleasedLarge.setVisibility(View.GONE);
			setRefPressedLarge.setVisibility(View.VISIBLE);
			
			if(myDrone.isConnected) {
				on = true;
				
				lightGreen.setVisibility(View.VISIBLE);
				lightOff.setVisibility(View.GONE);
				
				checkRef = true;
				setRef();
			}
		}
		else {
			on = false;
			
			setRefReleasedLarge.setVisibility(View.GONE);
			setRefPressedLarge.setVisibility(View.VISIBLE);
			
			lightGreen.setVisibility(View.GONE);
			lightOff.setVisibility(View.VISIBLE);
			
			checkRef = false;
		}
	}
	
	public void refButtonLargeReleased() {
		setRefReleasedLarge.setVisibility(View.VISIBLE);
		setRefPressedLarge.setVisibility(View.GONE);
	}
	
	public void refButtonSmallPressed() {
		if(!on) {
			setRefReleasedSmall.setVisibility(View.GONE);
			setRefPressedSmall.setVisibility(View.VISIBLE);
			
			if(myDrone.isConnected) {
				on = true;
				
				lightGreen.setVisibility(View.VISIBLE);
				lightOff.setVisibility(View.GONE);
				
				checkRef = true;
				setRef();
			}
		}
		else {
			on = false;
			
			setRefReleasedSmall.setVisibility(View.GONE);
			setRefPressedSmall.setVisibility(View.VISIBLE);
			
			lightGreen.setVisibility(View.GONE);
			lightOff.setVisibility(View.VISIBLE);
			
			checkRef = false;
		}
	}
	
	public void refButtonSmallReleased() {
		setRefReleasedSmall.setVisibility(View.VISIBLE);
		setRefPressedSmall.setVisibility(View.GONE);
	}
	
	public void inputRefButtonPressed() {
		inputRefReleased.setVisibility(View.GONE);
		inputRefPressed.setVisibility(View.VISIBLE);
		
		if(myDrone.isConnected) {
			on = true;
			getInputVal();
			
			lightGreen.setVisibility(View.VISIBLE);
			lightOff.setVisibility(View.GONE);
		}
	}
	
	public void inputRefButtonReleased() {
		inputRefReleased.setVisibility(View.VISIBLE);
		inputRefPressed.setVisibility(View.GONE);
	}
	
	public void maxHoldButtonPressed() {
		maxHoldReleased.setVisibility(View.GONE);
		maxHoldPressed.setVisibility(View.VISIBLE);
		
		if(myDrone.isConnected) {
			if(!maxHold) {
				maxHold = true;
				labelMax.setVisibility(View.VISIBLE);
			}
			else {
				maxHold = false;
				labelMax.setVisibility(View.GONE);
				max = 0;
			}
		}
	}
	
	public void maxHoldButtonReleased() {
		maxHoldReleased.setVisibility(View.VISIBLE);
		maxHoldPressed.setVisibility(View.GONE);
	}
	
	/**
	 * Scans for nearby sensordrones and brings up list
	 */
	public void scan() {
		myHelper.scanToConnect(myDrone, MainActivity.this , this, false);
	}
	
	/**
	 * Actions to do when drone is disconnected
	 */
	public void doOnDisconnect() {
		
		// Shut off any sensors that are on
		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				
				displayRefHandler.removeCallbacksAndMessages(null);
				displayIrHandler.removeCallbacksAndMessages(null);
				
				tv_valueRef.setVisibility(View.GONE);
				tv_valueIr.setVisibility(View.GONE);
				labelRef.setVisibility(View.GONE);
				labelUnit.setVisibility(View.GONE);
				labelScan.setVisibility(View.GONE);
				
				valueIr = 0;
				valueRef = 0;
				
				lightGreen.setVisibility(View.GONE);
				lightBlue.setVisibility(View.GONE);
				lightRed.setVisibility(View.GONE);
				lightOff.setVisibility(View.VISIBLE);
				
				// Turn off myBlinker
				box.myBlinker.disable();
				
				// Make sure the LEDs go off
				if (myDrone.isConnected) {
					myDrone.setLEDs(0, 0, 0);
				}
				
				// Only try and disconnect if already connected
				if (myDrone.isConnected) {
					myDrone.disconnect();
				}
			}
		});
	}
		
	/**
	 * A function to display Toast Messages.
	 * 
	 * By having it run on the UI thread, we will be sure that the message
	 * is displays no matter what thread tries to use it.
	 * 
	 * @param msg	Message to be displayed
	 */
	public void quickMessage(final String msg) {
		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void getInputVal() {
		LayoutInflater li = LayoutInflater.from(this);
		View promptView = li.inflate(R.layout.prompt, null);
		
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

		// set prompts.xml to alertdialog builder
		alertDialogBuilder.setView(promptView);

		final EditText userInput = (EditText) promptView.findViewById(R.id.editTextDialogUserInput);

		// set dialog message
		alertDialogBuilder.setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog,int id) {
			    	valueRef = Float.valueOf(userInput.getText().toString());
			    	showRef = true;
			    	checkRef = true;
					displayRefHandler.post(displayRefRunnable);
			    }
			  })
			.setNegativeButton("Cancel",
			  new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog,int id) {
				dialog.cancel();
			    }
			  });

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();
	}
	
	public void setRef() {
		valueRef = valueIr;
		showRef = true;
		displayRefHandler.post(displayRefRunnable);
	}
	
	public int pxToDp(int px) {
	
		float d = this.getResources().getDisplayMetrics().density;
		int ret = (int)(px * d); // margin in pixels
		return ret;
	}
	
	public void onRadioButtonClicked(View view) {
		
		boolean checked = ((RadioButton)view).isChecked();
		
		switch(view.getId()) {
		case R.id.radio_button_off:
			if(checked) {
				reference = 9999;
			}
			break;
		case R.id.radio_button_2f:
			if(checked) {
				reference = 2;
			}
			break;
		case R.id.radio_button_10f:
			if(checked) {
				reference = 10;
			}
			break;
		case R.id.radio_button_20f:
			if(checked) {
				reference = 20;
			}
			break;
		case R.id.radio_button_F:
			if(checked) {
				celcius = false;
				max = 0;
			}
			break;
		case R.id.radio_button_C:
			if(checked) {
				celcius = true;
				max = 0;
			}
			break;
		}
		
	}
	
	public Runnable displayIRRunnable = new Runnable() {

		@Override
		public void run() {
			if(myDrone.isConnected) {
				if(maxHold) {
					tv_valueIr.setText(String.format("%.1f", max));
				}
				else {
					tv_valueIr.setText(String.format("%.1f", valueIr));
				}
				
				if(celcius) {
					labelUnit.setText("C" + (char) 0x00B0);
				} else {
					labelUnit.setText("F" + (char) 0x00B0);
				}
				
				displayIrHandler.postDelayed(this, 1000);
			}
			else {
				displayIrHandler.removeCallbacksAndMessages(null);
			}
		}
	};
	
	public Runnable displayRefRunnable = new Runnable() {

		@Override
		public void run() {
			if(myDrone.isConnected) {
				
				if(showRef) {
					tv_valueRef.setText(String.format("%.1f", valueRef));
					showRef = false;
				}
				
				Log.d("chris", "Value: " + Float.toString(valueIr));
				Log.d("chris", "Ref value: " + Float.toString(valueRef));
				Log.d("chris", "Reference: " + Float.toString(reference));
				
				if(checkRef) {
					if((valueIr + reference) < valueRef) {
						lightGreen.setVisibility(View.GONE);
						lightBlue.setVisibility(View.VISIBLE);
						lightRed.setVisibility(View.GONE);
					}
					else if((valueIr - reference) > valueRef) {
						lightGreen.setVisibility(View.GONE);
						lightBlue.setVisibility(View.GONE);
						lightRed.setVisibility(View.VISIBLE);
					}
					else {
						lightGreen.setVisibility(View.VISIBLE);
						lightBlue.setVisibility(View.GONE);
						lightRed.setVisibility(View.GONE);
					}
				}
				else {
					displayRefHandler.removeCallbacksAndMessages(null);
				}
				
				displayRefHandler.postDelayed(this, 1000);
			}
			else {
				
			}
		}
	};
	
	public Runnable changeModeRunnable = new Runnable() {

		@Override
		public void run() {
			
			if(modeChange == true) {
				if(mode == NORMAL_MODE) {
					setRefReleasedLarge.setVisibility(View.VISIBLE);
					setRefPressedLarge.setVisibility(View.GONE);
					setRefPressedSmall.setVisibility(View.GONE);
					setRefReleasedSmall.setVisibility(View.GONE);
					inputRefPressed.setVisibility(View.GONE);
					inputRefReleased.setVisibility(View.GONE);
					maxHoldReleased.setVisibility(View.GONE);
					maxHoldPressed.setVisibility(View.GONE);
					
					RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)iv_directions.getLayoutParams();
					params.setMargins(pxToDp(150), pxToDp(340), 0, 0); //substitute parameters for left, top, right, bottom
					iv_directions.setLayoutParams(params);
					
					maxHold = false;
				}
				else {
					setRefReleasedLarge.setVisibility(View.GONE);
					setRefPressedLarge.setVisibility(View.GONE);
					setRefPressedSmall.setVisibility(View.GONE);
					setRefReleasedSmall.setVisibility(View.VISIBLE);
					inputRefPressed.setVisibility(View.GONE);
					inputRefReleased.setVisibility(View.VISIBLE);
					maxHoldReleased.setVisibility(View.VISIBLE);
					maxHoldPressed.setVisibility(View.GONE);
					
					RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)iv_directions.getLayoutParams();
					params.setMargins(pxToDp(150), pxToDp(450), 0, 0); //substitute parameters for left, top, right, bottom
					iv_directions.setLayoutParams(params);
				}
				modeChange = false;
			}
		}
	};
	
	/*
	 * Because Android will destroy and re-create things on events like orientation changes,
	 * we will need a way to store our objects and return them in such a case. 
	 * 
	 * A simple and straightforward way to do this is to create a class which has all of the objects
	 * and values we want don't want to get lost. When our orientation changes, it will reload our
	 * class, and everything will behave as normal! See onRetainNonConfigurationInstance in the code
	 * below for more information.
	 * 
	 * A lot of the GUI set up will be here, and initialized via the Constructor
	 */
	public final class Storage {
		
		// A ConnectionBLinker from the SDHelper Library
		public ConnectionBlinker myBlinker;
		
		// Holds the sensor of interest - the CO precision sensor
		public int sensor;
		
		// Our Listeners
		public DroneEventListener droneEventListener;
		public DroneStatusListener droneStatusListener;
		public String MAC = "";
		
		// GUI variables
		public TextView statusView;
		public TextView tvConnectionStatus;
		public TextView tvConnectInfo;
		
		// Streams data from sensor
		public SDStreamer streamer;
		
		public Storage(Context context) {
			
			// Initialize sensor
			sensor = myDrone.QS_TYPE_IR_TEMPERATURE;
			
			// This will Blink our Drone, once a second, Blue
			myBlinker = new ConnectionBlinker(myDrone, 1000, 0, 255, 0);
			
			streamer = new SDStreamer(myDrone, sensor);
			
			/*
			 * Let's set up our Drone Event Listener.
			 * 
			 * See adcMeasured for the general flow for when a sensor is measured.
			 * 
			 */
			droneEventListener = new DroneEventListener() {
				
				@Override
				public void connectEvent(EventObject arg0) {

					quickMessage("Connected!");
					
					streamer.enable();
					myDrone.quickEnable(sensor);
					
					// Flash teh LEDs green
					myHelper.flashLEDs(myDrone, 3, 100, 0, 0, 22);
					// Turn on our blinker
					myBlinker.enable();
					myBlinker.run();
					
					tv_valueRef.setVisibility(View.VISIBLE);
					tv_valueIr.setVisibility(View.VISIBLE);
					labelRef.setVisibility(View.VISIBLE);
					labelUnit.setVisibility(View.VISIBLE);
					labelScan.setVisibility(View.VISIBLE);
					
					displayIrHandler.post(displayIRRunnable);
				}

				
				@Override
				public void connectionLostEvent(EventObject arg0) {
					// Turn off the blinker
					myBlinker.disable();
					
					quickMessage("Connection lost! Trying to re-connect!");

					// Try to reconnect once, automatically
					if (myDrone.btConnect(myDrone.lastMAC)) {
						// A brief pause
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						
					} else {
						quickMessage("Re-connect failed");
						doOnDisconnect();
					}
				}

				@Override
				public void disconnectEvent(EventObject arg0) {
					quickMessage("Disconnected!");
				}

				@Override
				public void irTemperatureMeasured(EventObject arg0) {
					if(celcius) {
						valueIr = myDrone.irTemperature_Celcius;
					} else {
						valueIr = myDrone.irTemperature_Farenheit;
					}
					
					if(valueIr > max) {
						max = valueIr;
					}
					
					streamer.streamHandler.postDelayed(streamer, 250);
				}
				
				/*
				 * Unused events
				 */
				@Override
				public void precisionGasMeasured(EventObject arg0) {}
				@Override
				public void customEvent(EventObject arg0) {}
				@Override
				public void adcMeasured(EventObject arg0) {}
				@Override
				public void altitudeMeasured(EventObject arg0) {}
				@Override
				public void capacitanceMeasured(EventObject arg0) {}
				@Override
				public void humidityMeasured(EventObject arg0) {}
				@Override
				public void i2cRead(EventObject arg0) {}
				@Override
				public void oxidizingGasMeasured(EventObject arg0) {}
				@Override
				public void pressureMeasured(EventObject arg0) {}
				@Override
				public void reducingGasMeasured(EventObject arg0) {}
				@Override
				public void rgbcMeasured(EventObject arg0) {}
				@Override
				public void temperatureMeasured(EventObject arg0) {}
				@Override
				public void uartRead(EventObject arg0) {}
				@Override
				public void unknown(EventObject arg0) {}
				@Override
				public void usbUartRead(EventObject arg0) {}
			};
			
			/*
			 * Set up our status listener
			 * 
			 * see adcStatus for the general flow for sensors.
			 */
			droneStatusListener = new DroneStatusListener() {

				@Override
				public void irStatus(EventObject arg0) {
					streamer.run();
				}
				
				/*
				 * Unused statuses
				 */
				@Override
				public void precisionGasStatus(EventObject arg0) {}
				@Override
				public void adcStatus(EventObject arg0) {}
				@Override
				public void altitudeStatus(EventObject arg0) {}
				@Override
				public void batteryVoltageStatus(EventObject arg0) {}
				@Override
				public void capacitanceStatus(EventObject arg0) {}
				@Override
				public void chargingStatus(EventObject arg0) {}
				@Override
				public void customStatus(EventObject arg0) {}
				@Override
				public void humidityStatus(EventObject arg0) {}
				@Override
				public void lowBatteryStatus(EventObject arg0) {}
				@Override
				public void oxidizingGasStatus(EventObject arg0) {}
				@Override
				public void pressureStatus(EventObject arg0) {}
				@Override
				public void reducingGasStatus(EventObject arg0) {}
				@Override
				public void rgbcStatus(EventObject arg0) {}
				@Override
				public void temperatureStatus(EventObject arg0) {}
				@Override
				public void unknownStatus(EventObject arg0) {}
			};
			
			// Register the listeners
			myDrone.registerDroneEventListener(droneEventListener);
			myDrone.registerDroneStatusListener(droneStatusListener);
		}
	}
}
