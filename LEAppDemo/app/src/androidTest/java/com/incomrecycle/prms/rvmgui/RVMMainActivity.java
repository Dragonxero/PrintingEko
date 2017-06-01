package com.incomrecycle.prms.rvmgui;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.code.microlog4android.Logger;
import com.incomrecycle.common.SysConfig;
import com.incomrecycle.common.SysGlobal;
import com.incomrecycle.common.task.DelayTaskMgr;
import com.incomrecycle.common.task.TaskAction;
import com.incomrecycle.common.task.TimeoutAction;
import com.incomrecycle.common.task.TimeoutTask;
import com.incomrecycle.common.utils.IOUtils;
import com.incomrecycle.common.utils.StringUtils;
import com.incomrecycle.prms.rvm.comm.CommEntity;
import com.incomrecycle.prms.rvm.comm.entity.BasePrinter;
import com.incomrecycle.prms.rvm.gui.BaseActivity;
import com.incomrecycle.prms.rvm.gui.CommonServiceHelper;
import com.incomrecycle.prms.rvm.gui.CommonServiceHelper.GUICommonService;
import com.incomrecycle.prms.rvm.gui.GUIAction;
import com.incomrecycle.prms.rvm.gui.GUIGlobal;
import com.incomrecycle.prms.rvm.gui.action.GUIActionLaunchMaintainGUI;
import com.incomrecycle.prms.rvm.gui.activity.view.MySbarView;
import com.incomrecycle.prms.rvm.gui.activity.view.MySbarView.OnScrollEvent;
import com.incomrecycle.prms.rvmgui.activity.PutBottleYC103Activity;

public class RVMMainActivity extends BaseActivity{
	private final static Logger logger = Logger.getLogger("RVMMainActivity");
	private SurfaceView surfaceView = null;
	private TextView contentView = null;
	public MySbarView sbarView = null;
	private MediaPlayer mplayer = null;
	private int index = 0;
	private List<HashMap<String, String>> listVideo = new ArrayList<HashMap<String, String>>();
	public static int indexOfListAdText = 0;
	public String selectRvmMode, selectHDVersion;
	TextView textViewHint = null;
	private TextView rvm_time = null;
	private boolean hasSurface,isVisibleHint;
	boolean hasHomePic = false;
	@Override
	protected void onStart() {
		super.onStart();
		try {
			initSettings();
			initDoorOperateButton();

			enableRecycleBottle();
			SysConfig.set("UPDATE_ENABLE", "TRUE");
			TextView serialNumber = (TextView) findViewById(R.id.serial_number);
			serialNumber.setText(SysConfig.get("RVM.CODE"));
			final int timeoutForQueryLightState = Integer.valueOf(SysConfig.get("RVM.QUERY.LIGHT.STATE.TIME"));
			String isCheckLight = SysConfig.get("RVM.QUERY.LIGHT.STATE");
			if(!StringUtils.isBlank(isCheckLight)){
				if("TRUE".equalsIgnoreCase(isCheckLight)){
					TimeoutTask.getTimeoutTask().addTimeoutAction(timeoutActionForQueryLightState,timeoutForQueryLightState, false);
					TimeoutTask.getTimeoutTask().reset(timeoutActionForQueryLightState);
					TimeoutTask.getTimeoutTask().setEnabled(timeoutActionForQueryLightState, true);
				}
			}
			
			systemTime();
			decideCountry();
		} catch(Exception e) {
			logger.debug("Exception on logger onStart", e);
		}
	}
	public void initSettings() {
		try {
			String[][] valueNames = {
				{"RVM.CODE","RVM.CODE"},
				{"RVM.MODE","RVM.MODE"},
				{"RVM.VERSION.ID","RVM.VERSION.ID"},
				{"RVM.POWER.ON.TIME","RVM.POWER.ON.TIME"},
				{"RVM.POWER.OFF.TIME","RVM.POWER.OFF.TIME"},
				{"SERVICE.DISABLED.SET","SERVICE.DISABLED.SET"},
				{"RECYCLE.SERVICE.SET","RECYCLE.SERVICE.SET"},
			};
			GUICommonService guiCommonService = CommonServiceHelper.getGUICommonService();
			HashMap hsmpSettings = guiCommonService.execute("GUIRecycleCommonService", "querySettings", null);
			if(hsmpSettings != null) {
				for(int i=0;i<valueNames.length;i++) {
					String value = (String)hsmpSettings.get(valueNames[i][0]);
					if(!StringUtils.isBlank(value)) {
						SysConfig.set(valueNames[i][1], value);
					}
				}
			}
		} catch (Exception e) {
		}
		
	}

	@Override
	public void finish() {
		super.finish();
	}

	private void initDoorOperateButton() {
		try {
			Button btnDoorOperate = (Button)findViewById(R.id.doorOperate);
			GUICommonService guiCommonService = CommonServiceHelper.getGUICommonService();
			HashMap hsmpDoorState = guiCommonService.execute("GUIRecycleCommonService", "doorState", null);
			boolean isDoorOn = false;
			if(hsmpDoorState != null) {
				if("ON".equalsIgnoreCase((String)hsmpDoorState.get("STATE"))) {
					isDoorOn = true;
				}
			}
			if(isDoorOn) {
				btnDoorOperate.setText("Close Door");
			} else {
				btnDoorOperate.setText("Open Door");
			}
		} catch (Exception e) {
		}
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().getDecorView()
					.setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
			setContentView(R.layout.activity_rvm_main);
			rvm_time = (TextView) findViewById(R.id.rvm_time);
			textViewHint = (TextView) findViewById(R.id.rvmHint);
			sbarView = (MySbarView) findViewById(R.id.mySbarView);
			sbarView.setOnScrollEvent(onScrollEvent);
			contentView = (TextView) findViewById(R.id.fullscreen_content);
			hasSurface = false;
			surfaceView = (SurfaceView) findViewById(R.id.firstVideo);
			surfaceView.getHolder().addCallback(new SurfaceListener());
			surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			surfaceView.getHolder().setKeepScreenOn(true);
			// Upon interacting with UI controls, delay any scheduled hide()
			// operations to prevent the jarring behavior of controls going away
			// while interacting with the UI.
			final GUICommonService guiCommonService = CommonServiceHelper
					.getGUICommonService();
			Button btnChinese = (Button) findViewById(R.id.btnChinese);
			btnChinese.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					GUIGlobal.updateLanguage(getApplication(),
							Locale.SIMPLIFIED_CHINESE);
				}
			});
			Button btnEnglish = (Button) findViewById(R.id.btnEnglish);
			btnEnglish.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					GUIGlobal.updateLanguage(getApplication(), Locale.ENGLISH);
				}
			});
	
			Button btnPortuguese = (Button) findViewById(R.id.btnPortuguese);
			btnPortuguese.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					GUIGlobal.updateLanguage(getApplication(), new Locale("pt",
							"BR"));
				}
			});
	
			// start throw bottles
			Button btnThrowBottles = (Button) findViewById(R.id.throwBottle);
			btnThrowBottles.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// Added by Zhihai
					startRecycleBottle();
				}
			});
			Button btnLaunchMaintain = (Button) findViewById(R.id.launchMaintain);
			btnLaunchMaintain.setOnClickListener(new View.OnClickListener() {
				private boolean isEnable = true;
				private TaskAction delayTaskAction = new TaskAction() {
					@Override
					public void execute() {
						isEnable = true;
					}
				};
				@Override
				public void onClick(View v) {
					if(!isEnable)
						return;
					executeGUIAction(false, new GUIActionLaunchMaintainGUI(), null);
					DelayTaskMgr.getDelayTaskMgr().addDelayTask(delayTaskAction, 2000);
				}
			});
	
			Button btnPrint = (Button) findViewById(R.id.btnPrint);
			btnPrint.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					try {
						String textModel = new String(IOUtils.readResource("assets/model/voucher_inst.txt"));
						HashMap<String,String> hsmpParam = new HashMap<String,String>();
						hsmpParam.put("$BARCODE$","123456789012");
						BasePrinter basePrinter = CommEntity.getPrinter(1);
						basePrinter.print(textModel, hsmpParam);
					} catch (Exception e) {
					}
				}
			});
			Button btnCamera = (Button) findViewById(R.id.btnCamera);
			btnCamera.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					try {
						int numCamera = Camera.getNumberOfCameras();
						List<Integer> listIdx = new ArrayList<Integer>();
						for(int i=0;i<numCamera;i++) {
							Camera camera = Camera.open(i);
							if(camera != null) {
								camera.release();
								listIdx.add(i);
							}
						}
						StringBuffer sb = new StringBuffer();
						sb.append("Camera number is " + numCamera + ". The usable index is[");
						for(int i=0;i<listIdx.size();i++) {
							if(i > 0) {
								sb.append(",");
							}
							sb.append(listIdx.get(i).toString());
						}
						sb.append("].");
						System.out.println(sb.toString());
						textViewHint.setText(sb.toString());
					} catch (Exception e) {
					}
				}
			});
			Button exitSys = (Button) findViewById(R.id.exitSys);
			exitSys.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					try {
						GUICommonService guiCommonService = CommonServiceHelper.getGUICommonService();
						guiCommonService.execute("GUIRecycleCommonService", "exit", null);
					} catch (Exception e) {
					}
				}
			});
			Button btnDoorOperate = (Button)findViewById(R.id.doorOperate);
			btnDoorOperate.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					try {
						Button btnDoorOperate = (Button)findViewById(R.id.doorOperate);
						GUICommonService guiCommonService = CommonServiceHelper.getGUICommonService();
						HashMap hsmpDoorState = guiCommonService.execute("GUIRecycleCommonService", "doorState", null);
						boolean isDoorOn = false;
						if(hsmpDoorState != null) {
							if("ON".equalsIgnoreCase((String)hsmpDoorState.get("STATE"))) {
								isDoorOn = true;
							}
						}
						if(isDoorOn) {
							guiCommonService.execute("GUIRecycleCommonService", "closeDoor", null);
						} else {
							guiCommonService.execute("GUIRecycleCommonService", "openDoor", null);
						}
					} catch (Exception e) {
					}

				}
			});
	
			String str = SysConfig.get("COUNTRIES_REGIONS");
			String[] strs = new String[2];
			if (StringUtils.isBlank(str)) {
				strs[0] = Locale.getDefault().getLanguage();
				strs[1] = Locale.getDefault().getCountry();
			} else {
				String[] strs1 = str.split(",");
				if (strs1.length < 2) {
					strs[0] = strs1[0];
					strs[1] = "";
				} else {
					strs = strs1;
				}
			}
			GUIGlobal.updateLanguage(getApplication(), new Locale(strs[0], strs[1]));
	
			String VisibleLanguageButton = SysConfig.get("RVM.LANGUAGE.STATE");
			String[] VisibleLanguageButtons = VisibleLanguageButton.split(",");
			for (int i = 0; i < VisibleLanguageButtons.length; i++) {
				if (VisibleLanguageButtons[i].equalsIgnoreCase("CHINESE")) {
					btnChinese.setVisibility(View.VISIBLE);
				}
				;
				if (VisibleLanguageButtons[i].equalsIgnoreCase("ENGLISH")) {
					btnEnglish.setVisibility(View.VISIBLE);
				}
				;
				if (VisibleLanguageButtons[i].equalsIgnoreCase("PORTUGUESE")) {
					btnPortuguese.setVisibility(View.VISIBLE);
				}
				;
			}
		} catch(Exception e) {
			logger.debug("Exception on RVMMainActivity onCreate",e);
		}
	}

	public void systemTime() {
		Date now = new Date();
		DateFormat time = DateFormat.getDateInstance();
		String nowTime = time.format(now);
		rvm_time.setText(nowTime);
	}


	public void enableRecycleBottle() {
		Button btnThrowBottles = (Button) findViewById(R.id.throwBottle);
		boolean hasPLCLightDectorOn = "ON".equalsIgnoreCase((String)SysGlobal.get("PLC_LIGHTDECTOR_STATE"));
		boolean isPLCError = "TRUE".equalsIgnoreCase((String)SysGlobal.get("PLC_ERROR"));
		if (!isPLCError && !hasPLCLightDectorOn) {
			btnThrowBottles.setEnabled(true);
			btnThrowBottles.setText(R.string.btnthrowBottle);
			btnThrowBottles.setBackgroundResource(R.drawable.rvm_btn);
		} else {
			btnThrowBottles.setEnabled(false);
			btnThrowBottles.setBackgroundColor(Color.GRAY);
			btnThrowBottles.setText(R.string.maintaining);
			textViewHint.setText(R.string.maintaining);
		}
	}

	@Override
	public String getName() {
		return "RVMMain";
	}

	@Override
	public void updateLanguage() {
		// The center display
		if(!hasHomePic){
			TextView contentView = (TextView) findViewById(R.id.fullscreen_content);
			contentView.setText(R.string.welcome);
		}
		// Began to throw bottles
		Button btn = (Button) findViewById(R.id.throwBottle);
		if (btn.isEnabled()) {
			btn.setText(R.string.btnthrowBottle);
		} else {
			if(isVisibleHint){
				btn.setText(R.string.ReachMaxOfBottle);
			}else{
				btn.setText(R.string.maintaining);
			}
			
		}
		TextView rvmCode = (TextView) findViewById(R.id.rvm_code);
		rvmCode.setText(R.string.setupCfgTimerNumText);
		if(isVisibleHint){
			textViewHint.setText(R.string.ReachMaxOfStorage);
		}else{
			textViewHint.setText(R.string.maintaining);
		}
		TextView now_time = (TextView) findViewById(R.id.now_time);
		now_time.setText(R.string.nowTime);
	}

	boolean enableCallStartRecycleBottle = true;
	private void startRecycleBottle() {
		Button btnThrowBottles = (Button) findViewById(R.id.throwBottle);
		if (btnThrowBottles == null || !btnThrowBottles.isEnabled())
			return;
		if(!enableCallStartRecycleBottle)
			return;
		enableCallStartRecycleBottle = false;
		Intent intent = new Intent(RVMMainActivity.this,
				PutBottleYC103Activity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(intent);
		DelayTaskMgr.getDelayTaskMgr().addDelayTask(new TaskAction() {
			@Override
			public void execute() {
				enableCallStartRecycleBottle = true;
			}
		}, 2000);
	}

	@Override
	public void doEvent(HashMap hsmpEvent) {
		String EVENT = (String)hsmpEvent.get("EVENT");
		if("INFORM".equalsIgnoreCase(EVENT)) {
			String CMD = (String)hsmpEvent.get("INFORM");
			if("BUTTON_PUSH".equalsIgnoreCase(CMD)) {
				this.startRecycleBottle();
			}
			if("BOTTLE_DETECT".equalsIgnoreCase(CMD)) {
				this.startRecycleBottle();
			}
			if("DOOR_OPEN".equalsIgnoreCase(CMD)) {
				Button btnDoorOperate = (Button)findViewById(R.id.doorOperate);
				btnDoorOperate.setText("Close Door");
			}
			if("DOOR_CLOSE".equalsIgnoreCase(CMD)) {
				Button btnDoorOperate = (Button)findViewById(R.id.doorOperate);
				btnDoorOperate.setText("Open Door");
			}
			if ("PLC_ERROR".equalsIgnoreCase(CMD)) {
				enableRecycleBottle();
			}
			if ("PLC_ERROR_RECOVERY".equalsIgnoreCase(CMD)) {
				enableRecycleBottle();
			}
            if("EXCEPTION_RECOVERY".equalsIgnoreCase(CMD)){
    			enableRecycleBottle();
            }
            if("EXCEPTION".equalsIgnoreCase(CMD)){
    			enableRecycleBottle();
            }
		}
	}

	private void next() throws IOException {
		index++;
		if (index < listVideo.size()) {
			play();
		} else {
			index = 0;
			play();
		}
	}

	private void play() throws IOException {
		HashMap<String, String> hsmpVideo;
		if (listVideo.size() > 0 && index < listVideo.size()) {
			hsmpVideo = listVideo.get(index);
			String path = hsmpVideo.get("FILE_PATH");
			try {
				if(mplayer != null)
					mplayer.reset();
				if (mplayer != null && path != null && hasSurface) {
					mplayer.setDataSource(path);
					mplayer.setDisplay(surfaceView.getHolder());
					mplayer.prepare();
					//mplayer.start();
				}
			} catch (Exception e) {
				logger.debug("MediaPlay Error", e);
				e.printStackTrace();
			}
		}
	}
	private class SurfaceListener implements SurfaceHolder.Callback{
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
		holder.setFixedSize(800, 500);
		}
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			try {
				if (!hasSurface) {
					hasSurface = true;
					play();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder arg0) {
			hasSurface = false;
/*			if (mplayer != null) {
				mplayer.release();
				mplayer = null;
			}*/
		}
	}




	private OnScrollEvent onScrollEvent = new OnScrollEvent() {
		@Override
		public void onChange(View view) {
			indexOfListAdText++;
			String text = "";
			sbarView.setText(text,
					Integer.parseInt(SysConfig.get("RVM.SCROLLTEXT.SIZE")),
					SysConfig.get("RVM.SCROLLTEXT.COLOR"),
					Integer.parseInt(SysConfig.get("RVM.SCROLLTEXT.STEP")));
		}
	};

	@Override
	protected void onPause() {
		super.onPause();
		if (mplayer != null) {
			mplayer.stop();
		}
		SysConfig.set("UPDATE_ENABLE", "FALSE");
	}

	@Override
	protected void onStop() {
		super.onStop();
		String isCheckLight = SysConfig.get("RVM.QUERY.LIGHT.STATE");
		if(!StringUtils.isBlank(isCheckLight)){
			if("TRUE".equalsIgnoreCase(isCheckLight)){
				TimeoutTask.getTimeoutTask().setEnabled(timeoutActionForQueryLightState, false);
				TimeoutTask.getTimeoutTask().removeTimeoutAction(timeoutActionForQueryLightState);
			}
		}
		if (mplayer != null) {
			mplayer.release();
			mplayer = null;
		}
	}

	public void decideCountry(){
		String PICTURE_URL = SysConfig.get("HOME_PAGE_URL");
		String HOME_PICTURE = SysConfig.get("HOME_PAGE_PICTURE");
		if(!StringUtils.isBlank(HOME_PICTURE)){
			if("TRUE".equalsIgnoreCase(HOME_PICTURE)){
				hasHomePic = true;
				if(!StringUtils.isBlank(PICTURE_URL)){
					File newurlpic = new File(PICTURE_URL);
					if (newurlpic.isFile()) {
						Bitmap bitmap = BitmapFactory.decodeFile(PICTURE_URL); 
						if(bitmap != null){
							BitmapDrawable bd = new BitmapDrawable(bitmap);
							contentView.setBackgroundDrawable(bd);
							contentView.setText("");
						}else{
							contentView.setBackgroundResource(R.drawable.home);
							contentView.setText("");	
						}
				}else{
			    	contentView.setBackgroundResource(R.drawable.home);
					contentView.setText("");
			    }
			  }else{
				  contentView.setBackgroundResource(R.drawable.home);
				  contentView.setText("");
			  }
			}
		}
	}
	private TimeoutAction timeoutActionForQueryLightState = new TimeoutAction() {
		@Override
		public void apply(int forwardSeconds, int remainedSeconds) {
			GUIAction guiAction = new GUIAction() {
				@Override
				protected void doAction(Object[] paramObjs) {
					int forwardSeconds = (Integer) paramObjs[0];
					int remainedSeconds = (Integer) paramObjs[1];
					if (remainedSeconds == 0) {
						TimeoutTask.getTimeoutTask().reset(timeoutActionForQueryLightState);
						TimeoutTask.getTimeoutTask().setEnabled(timeoutActionForQueryLightState, true);
					}
				}
			};
			executeGUIAction(false, guiAction, new Object[] { forwardSeconds,remainedSeconds });
		}
	};
}
