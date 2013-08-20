package com.sensorcon.irthermometer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.view.View.OnTouchListener;
import android.widget.*;
import com.sensorcon.sensordrone.DroneEventListener;
import com.sensorcon.sensordrone.DroneEventObject;
import com.sensorcon.sensordrone.DroneStatusListener;
import com.sensorcon.sensordrone.android.Drone;
import com.sensorcon.sensordrone.android.tools.DroneConnectionHelper;
import com.sensorcon.sensordrone.android.tools.DroneQSStreamer;

@SuppressLint("NewApi")
public class MainActivity extends Activity {
	
	static String LAST_MAC = "LAST_MAC";
	static String DISABLE_INTRO = "DISABLE_INTRO";
	private SharedPreferences preferences;
	
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
	private RelativeLayout refLayout;
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
	private RadioButton rbNorm;
	private RadioButton rbAdv;
	
	private RadioGroup radioGroupMode;
	private ToggleButton toggleMode;
	
	private float valueRef;
	private float valueIr;
	private float reference;
	private float max;
	private boolean checkRef;
	private boolean showRef;
	private boolean celcius;
	private boolean maxHold;
	private boolean rEnabled;
	private boolean gEnabled;
	private boolean bEnabled;
	
	public AlertInfo myInfo;
	
	private int api;
	private final int NEW_API = 0;
	private final int OLD_API = 1;
	
	private int mode;
	private boolean modeChange;
	private final int NORMAL_MODE = 0;
	private final int ADVANCED_MODE = 1;
	
	private Handler displayIrHandler = new Handler();
	private Handler displayRefHandler = new Handler();
	private Handler modeHandler = new Handler();
	
	private Typeface lcdFont;

	protected Drone myDrone;
	public DroneConnectionHelper myHelper;
	
	private boolean on;



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
    public DroneQSStreamer streamer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Check to see if API supports swipe views and fragments
		if (android.os.Build.VERSION.SDK_INT < 14) {
		    api = OLD_API;
		} else {
			api = NEW_API;
		}
		
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
		refLayout = (RelativeLayout)findViewById(R.id.ref_layout);
		
		if(api == NEW_API) {
			toggleMode = (ToggleButton)findViewById(R.id.toggle_mode);
		}
		
		labelUnit.setText("F" + (char) 0x00B0 );
		
		mode = NORMAL_MODE;
		modeChange = false;
		rEnabled = false;
		gEnabled = false;
		bEnabled = false;
		
		valueIr = 0;
		valueRef = 0;
		reference = 9999;
		checkRef = false;
		showRef = false;
		celcius = false;
		
		lightGreen.setVisibility(View.INVISIBLE);
		lightBlue.setVisibility(View.INVISIBLE);
		lightRed.setVisibility(View.INVISIBLE);
		tv_valueRef.setVisibility(View.INVISIBLE);
		tv_valueIr.setVisibility(View.INVISIBLE);
		labelRef.setVisibility(View.INVISIBLE);
		labelUnit.setVisibility(View.INVISIBLE);
		labelScan.setVisibility(View.INVISIBLE);
		labelMax.setVisibility(View.INVISIBLE);
		setRefPressedLarge.setVisibility(View.INVISIBLE);
		setRefPressedSmall.setVisibility(View.INVISIBLE);
		setRefReleasedSmall.setVisibility(View.INVISIBLE);
		inputRefPressed.setVisibility(View.INVISIBLE);
		inputRefReleased.setVisibility(View.INVISIBLE);
		maxHoldReleased.setVisibility(View.INVISIBLE);
		maxHoldPressed.setVisibility(View.INVISIBLE);
		
		on = false;
		
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
		
		myInfo = new AlertInfo(this);
		
		// Initialize SharedPreferences
		preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		String disableIntro = preferences.getString(DISABLE_INTRO, "");
		
		if(!disableIntro.equals("DISABLE")) {
			showIntroDialog();
		}
				
		// Initialize drone variables
		myDrone = new Drone();
		myHelper = new DroneConnectionHelper();

        // Initialize sensor
        sensor = myDrone.QS_TYPE_IR_TEMPERATURE;



        streamer = new DroneQSStreamer(myDrone, sensor);
			
			/*
			 * Let's set up our Drone Event Listener.
			 * 
			 * See adcMeasured for the general flow for when a sensor is measured.
			 * 
			 */
        droneEventListener = new DroneEventListener() {

            @Override
            public void connectEvent(DroneEventObject arg0) {


                Editor prefEditor = preferences.edit();
                prefEditor.putString(LAST_MAC, myDrone.lastMAC);
                prefEditor.commit();

                quickMessage("Connected!");

                streamer.enable();
                myDrone.quickEnable(sensor);

                myDrone.setLEDs(0,126,0);

                tv_valueRef.setVisibility(View.VISIBLE);
                tv_valueIr.setVisibility(View.VISIBLE);
                labelRef.setVisibility(View.VISIBLE);
                labelUnit.setVisibility(View.VISIBLE);
                labelScan.setVisibility(View.VISIBLE);

                displayIrHandler.post(displayIRRunnable);
            }


            @Override
            public void connectionLostEvent(DroneEventObject arg0) {


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
            public void disconnectEvent(DroneEventObject arg0) {
                quickMessage("Disconnected!");
            }

            @Override
            public void irTemperatureMeasured(DroneEventObject arg0) {
                if(celcius) {
                    valueIr = myDrone.irTemperature_Celsius;
                } else {
                    valueIr = myDrone.irTemperature_Fahrenheit;
                }

                if(maxHold) {
                    if(valueIr > max) {
                        max = valueIr;
                    }
                }
                else {
                    max = 0;
                }

                streamer.streamHandler.postDelayed(streamer, 250);
            }

            /*
             * Unused events
             */
            @Override
            public void precisionGasMeasured(DroneEventObject arg0) {}
            @Override
            public void customEvent(DroneEventObject arg0) {}
            @Override
            public void adcMeasured(DroneEventObject arg0) {}
            @Override
            public void altitudeMeasured(DroneEventObject arg0) {}
            @Override
            public void capacitanceMeasured(DroneEventObject arg0) {}
            @Override
            public void humidityMeasured(DroneEventObject arg0) {}
            @Override
            public void i2cRead(DroneEventObject arg0) {}
            @Override
            public void oxidizingGasMeasured(DroneEventObject arg0) {}
            @Override
            public void pressureMeasured(DroneEventObject arg0) {}
            @Override
            public void reducingGasMeasured(DroneEventObject arg0) {}
            @Override
            public void rgbcMeasured(DroneEventObject arg0) {}
            @Override
            public void temperatureMeasured(DroneEventObject arg0) {}
            @Override
            public void uartRead(DroneEventObject arg0) {}
            @Override
            public void unknown(DroneEventObject arg0) {}
            @Override
            public void usbUartRead(DroneEventObject arg0) {}
        };
			
			/*
			 * Set up our status listener
			 * 
			 * see adcStatus for the general flow for sensors.
			 */
        droneStatusListener = new DroneStatusListener() {

            @Override
            public void irStatus(DroneEventObject arg0) {
                streamer.run();
            }

            /*
             * Unused statuses
             */
            @Override
            public void precisionGasStatus(DroneEventObject arg0) {}
            @Override
            public void adcStatus(DroneEventObject arg0) {}
            @Override
            public void altitudeStatus(DroneEventObject arg0) {}
            @Override
            public void batteryVoltageStatus(DroneEventObject arg0) {}
            @Override
            public void capacitanceStatus(DroneEventObject arg0) {}
            @Override
            public void chargingStatus(DroneEventObject arg0) {}
            @Override
            public void customStatus(DroneEventObject arg0) {}
            @Override
            public void humidityStatus(DroneEventObject arg0) {}
            @Override
            public void lowBatteryStatus(DroneEventObject arg0) {}
            @Override
            public void oxidizingGasStatus(DroneEventObject arg0) {}
            @Override
            public void pressureStatus(DroneEventObject arg0) {}
            @Override
            public void reducingGasStatus(DroneEventObject arg0) {}
            @Override
            public void rgbcStatus(DroneEventObject arg0) {}
            @Override
            public void temperatureStatus(DroneEventObject arg0) {}
            @Override
            public void unknownStatus(DroneEventObject arg0) {}
        };

        // Register the listeners
        myDrone.registerDroneEventListener(droneEventListener);
        myDrone.registerDroneStatusListener(droneStatusListener);
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
			myDrone.unregisterDroneEventListener(droneEventListener);
			myDrone.unregisterDroneStatusListener(droneStatusListener);

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
		case R.id.reconnect:
			if (!myDrone.isConnected) {
				String prefLastMAC = preferences.getString(LAST_MAC, "");
				// This option is used to re-connect to the last connected MAC
				if (!prefLastMAC.equals("")) {
					if (!myDrone.btConnect(prefLastMAC)) {
						myInfo.connectFail();
					}
				} else {
					// Notify the user if no previous MAC was found.
					quickMessage("Last MAC not found... Please scan");
				} 
			} else {
				quickMessage("Already connected...");
			}
			break;
		case R.id.disconnect:
			doOnDisconnect();
			break;
		case R.id.instructions:
			if(api == OLD_API) {
				Intent myIntent = new Intent(getApplicationContext(), InstructionsActivityOld.class);
				startActivity(myIntent);
			}
			else {
				Intent myIntent = new Intent(getApplicationContext(), InstructionsActivity.class);
				startActivity(myIntent);
			}
			break;
		}
			
		return true;
	}
	
	/**
	 * Loads the dialog shown at startup
	 */
	public void showIntroDialog() {

		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setCancelable(false);
		alert.setTitle("Introduction").setMessage("If you are new to the IR Thermometer app, you should read through the instructions. To access them, go to the main menu and select Instructions.");
		alert.setPositiveButton("Don't Show Again", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		        	Editor prefEditor = preferences.edit();
					prefEditor.putString(DISABLE_INTRO, "DISABLE");
					prefEditor.commit();
		        }
		     })
		    .setNegativeButton("Okay", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		            // do nothing
		        }
		     }).show();
	}
	
	public void refButtonLargePressed() {
		setRefReleasedLarge.setVisibility(View.INVISIBLE);
		setRefPressedLarge.setVisibility(View.VISIBLE);
		
		if(!on) {
			if(myDrone.isConnected) {
				on = true;
				
				lightGreen.setVisibility(View.VISIBLE);
				lightOff.setVisibility(View.INVISIBLE);
				
				checkRef = true;
				setRef();
			}
		}
		else {
			setRef();
		}
	}
	
	public void refButtonLargeReleased() {
		setRefReleasedLarge.setVisibility(View.VISIBLE);
		setRefPressedLarge.setVisibility(View.INVISIBLE);
	}
	
	public void refButtonSmallPressed() {
		setRefReleasedSmall.setVisibility(View.INVISIBLE);
		setRefPressedSmall.setVisibility(View.VISIBLE);
		
		if(!on) {
			if(myDrone.isConnected) {
				on = true;
				
				lightGreen.setVisibility(View.VISIBLE);
				lightOff.setVisibility(View.INVISIBLE);
				
				checkRef = true;
				setRef();
			}
		}
		else {
			setRef();
		}
	}
	
	public void refButtonSmallReleased() {
		setRefReleasedSmall.setVisibility(View.VISIBLE);
		setRefPressedSmall.setVisibility(View.INVISIBLE);
	}
	
	public void inputRefButtonPressed() {
		inputRefReleased.setVisibility(View.INVISIBLE);
		inputRefPressed.setVisibility(View.VISIBLE);
		
		if(myDrone.isConnected) {
			getInputVal();
		}
	}
	
	public void inputRefButtonReleased() {
		inputRefReleased.setVisibility(View.VISIBLE);
		inputRefPressed.setVisibility(View.INVISIBLE);
	}
	
	public void maxHoldButtonPressed() {
		maxHoldReleased.setVisibility(View.INVISIBLE);
		maxHoldPressed.setVisibility(View.VISIBLE);
		
		if(myDrone.isConnected) {
			if(!maxHold) {
				maxHold = true;
				labelMax.setVisibility(View.VISIBLE);
			}
			else {
				maxHold = false;
				labelMax.setVisibility(View.INVISIBLE);
				max = 0;
			}
		}
	}
	
	public void maxHoldButtonReleased() {
		maxHoldReleased.setVisibility(View.VISIBLE);
		maxHoldPressed.setVisibility(View.INVISIBLE);
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
				
				tv_valueRef.setVisibility(View.INVISIBLE);
				tv_valueIr.setVisibility(View.INVISIBLE);
				labelRef.setVisibility(View.INVISIBLE);
				labelMax.setVisibility(View.INVISIBLE);
				labelUnit.setVisibility(View.INVISIBLE);
				labelScan.setVisibility(View.INVISIBLE);
				
				valueIr = 0;
				on = false;
				maxHold = false;
				
				lightGreen.setVisibility(View.INVISIBLE);
				lightBlue.setVisibility(View.INVISIBLE);
				lightRed.setVisibility(View.INVISIBLE);
				lightOff.setVisibility(View.VISIBLE);

				
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

	public void rangeErrorMessage() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setCancelable(false);
		alert.setTitle("Error").setMessage("Please enter a value in between -273" + (char)0x00B0 + "C (-459.4" + (char)0x00B0 + "F) and 999" + (char)0x00B0 + "C (1830.2" + (char)0x00B0 + "F)");
		alert.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		        	getInputVal();
		        }
		     }).show();
	}
	
	public void getInputVal() {
		
		LayoutInflater li = LayoutInflater.from(this);
		View promptView = li.inflate(R.layout.prompt, null);
		
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

		// set prompts.xml to alertdialog builder
		alertDialogBuilder.setView(promptView);
		
		TextView tv = (TextView)promptView.findViewById(R.id.promptMessage);
		
		if(celcius) {
			tv.setText("Enter Reference Value (" + (char) 0x00B0 + "C): ");
		}
		else {
			tv.setText("Enter Reference Value (" + (char) 0x00B0 + "F): ");
		}

		final EditText userInput = (EditText) promptView.findViewById(R.id.editTextDialogUserInput);

		// set dialog message
		alertDialogBuilder.setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog,int id) {
			    	if(userInput.getText().toString().length() == 0 ) {
			    		
			    	}
			    	else {
			    		
			    		boolean error = false;
			    		float userRef = 0;
			    		
			    		try {
			    			userRef = Float.valueOf(userInput.getText().toString());
			    		}
			    		catch(NumberFormatException e) {
			    			error = true;
			    		}
				    	
				    	
				    	if(celcius) {
				    		if((userRef < -273) || (userRef > 999)) {
				    			rangeErrorMessage();
				    			error = true;
				    		}
				    	}
				    	else {
				    		if((userRef < -459.4) || (userRef > 1030.2)) {
				    			rangeErrorMessage();
				    			error = true;
				    		}
				    	}
	
				    	if(error == false) {
				    		if(!on) {
				    			if(myDrone.isConnected) {
				    				on = true;
				    				
				    				lightGreen.setVisibility(View.VISIBLE);
				    				lightOff.setVisibility(View.INVISIBLE);
				    				
				    				checkRef = true;
				    			}
				    		}
				    		
				    		valueRef = userRef;
					    	showRef = true;
							displayRefHandler.post(displayRefRunnable);
				    	}
				    	else {
				    		dialog.cancel();
				    	}
			    	}
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
	
	public float F(float c) {
		return (float)(c*1.8) + 32;
	}
	
	public float C(float f) {
		return (f - 32)*(float)(0.555555556);
	}
	
	public void onRadioButtonClicked(View view) {
		
		boolean checked = ((RadioButton)view).isChecked();
		
		switch(view.getId()) {
		case R.id.radio_button_off:
			if(checked) {
				reference = Float.MAX_VALUE;
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
				if(celcius) {
					valueRef = F(valueRef);
					showRef = true;
				}
				
				celcius = false;
				max = F(max);
			}
			break;
		case R.id.radio_button_C:
			if(checked) {
				if(!celcius) {
					valueRef = C(valueRef);
					showRef = true;
				}
				
				celcius = true;
				max = C(max);
			}
			break;
		}
	}
	
	public void onToggleClicked(View view) {
		
		    boolean on = ((ToggleButton)view).isChecked();
			
		    if(on) {
		    	modeChange = true;
				mode = ADVANCED_MODE;
				modeHandler.post(changeModeRunnable);
		    } else {
		    	 modeChange = true;
				mode = NORMAL_MODE;
				modeHandler.post(changeModeRunnable);
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
					labelUnit.setText((char) 0x00B0 + "C");
				} else {
					labelUnit.setText((char) 0x00B0 + "F");
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
						lightGreen.setVisibility(View.INVISIBLE);
						lightBlue.setVisibility(View.VISIBLE);
						lightRed.setVisibility(View.INVISIBLE);
						
						if(bEnabled == false) {
							bEnabled = true;
							rEnabled = false;
							gEnabled = false;

                            myDrone.setLEDs(0,0,126);
						}
					}
					else if((valueIr - reference) > valueRef) {
						lightGreen.setVisibility(View.INVISIBLE);
						lightBlue.setVisibility(View.INVISIBLE);
						lightRed.setVisibility(View.VISIBLE);
						
						if(rEnabled == false) {
							rEnabled = true;
							bEnabled = false;
							gEnabled = false;

                            myDrone.setLEDs(126,0,0);
						}
					}
					else {
						lightGreen.setVisibility(View.VISIBLE);
						lightBlue.setVisibility(View.INVISIBLE);
						lightRed.setVisibility(View.INVISIBLE);
						
						if(gEnabled == false) {
							gEnabled = true;
							bEnabled = false;
							rEnabled = false;

                            myDrone.setLEDs(0,126,0);
						}
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
					setRefPressedLarge.setVisibility(View.INVISIBLE);
					setRefPressedSmall.setVisibility(View.INVISIBLE);
					setRefReleasedSmall.setVisibility(View.INVISIBLE);
					inputRefPressed.setVisibility(View.INVISIBLE);
					inputRefReleased.setVisibility(View.INVISIBLE);
					maxHoldReleased.setVisibility(View.INVISIBLE);
					maxHoldPressed.setVisibility(View.INVISIBLE);
					
					RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)refLayout.getLayoutParams();
					params.addRule(RelativeLayout.BELOW, R.id.lcd_layout);
					params.setMargins(0, pxToDp(10), 0, 0);
					refLayout.setLayoutParams(params);
				}
				else {
					setRefReleasedLarge.setVisibility(View.INVISIBLE);
					setRefPressedLarge.setVisibility(View.INVISIBLE);
					setRefPressedSmall.setVisibility(View.INVISIBLE);
					setRefReleasedSmall.setVisibility(View.VISIBLE);
					inputRefPressed.setVisibility(View.INVISIBLE);
					inputRefReleased.setVisibility(View.VISIBLE);
					maxHoldReleased.setVisibility(View.VISIBLE);
					maxHoldPressed.setVisibility(View.INVISIBLE);
			
					RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)refLayout.getLayoutParams();
					params.addRule(RelativeLayout.BELOW, R.id.max_hold_released);
					params.setMargins(0, pxToDp(10), 0, 0);
					refLayout.setLayoutParams(params);
				}
				modeChange = false;
			}
		}
	};
	

}
