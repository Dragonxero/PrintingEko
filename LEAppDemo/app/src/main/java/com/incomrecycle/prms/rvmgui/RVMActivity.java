package com.incomrecycle.prms.rvmgui;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.incomrecycle.common.SysConfig;
import com.incomrecycle.common.SysGlobal;
import com.incomrecycle.common.init.LoggerInit;
import com.incomrecycle.common.task.TimeoutAction;
import com.incomrecycle.common.task.TimeoutTask;
import com.incomrecycle.common.utils.IOUtils;
import com.incomrecycle.common.utils.PropUtils;
import com.incomrecycle.common.utils.ShellUtils;
import com.incomrecycle.common.utils.SocketUtils;
import com.incomrecycle.common.utils.StringUtils;
import com.incomrecycle.prms.rvm.comm.CommEntity;
import com.incomrecycle.prms.rvm.gui.BaseActivity;
import com.incomrecycle.prms.rvm.gui.GUIAction;
import com.incomrecycle.prms.rvm.gui.GUIGlobal;
import com.incomrecycle.prms.rvm.gui.init.GUIInit;

public class RVMActivity extends BaseActivity{
	private static boolean hasInited = false;
	private EditText pwdEdit = null;
	boolean enableEvent = true;
	public void checkOnStart() {
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().getDecorView().setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
		setContentView(R.layout.activity_rvm);
		SysGlobal.execute(new Thread() {
			public void run() {
				if(!hasInited) {
					hasInited = true;
					init();
					enableEvent = false;
					GUIGlobal.setGUIReady(true);
				}
			}
		});
		Button tuichuBtn = (Button)findViewById(R.id.tuichuBtn);
		tuichuBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				View inputPWDView = (View)findViewById(R.id.inputPWDLayout);
				inputPWDView.setVisibility(View.VISIBLE);
				TimeoutTask.getTimeoutTask().reset(timeoutAction);
			}
		});
		pwdEdit = (EditText)findViewById(R.id.passwordText);
		pwdEdit.addTextChangedListener(mTextWatcher);
		Button quedingBtn = (Button)findViewById(R.id.quedingBtn);
		quedingBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				 TimeoutTask.getTimeoutTask().reset(timeoutAction);
				 String passWord = pwdEdit.getText().toString().trim();
				 String pwd = SysConfig.get("EXIT.PWD");
				 if(pwd == null || pwd.length() == 0 || pwd.equalsIgnoreCase(passWord)){
					GUIGlobal.exit();
				 }else{
					 Toast.makeText(RVMActivity.this, R.string.password_error, Toast.LENGTH_SHORT).show();
				 }
				
			}
		});
		TimeoutTask.getTimeoutTask().addTimeoutAction(timeoutAction, Integer.valueOf(SysConfig.get("RVM.TIMEOUT.INPUTPWD")), false);
		TimeoutTask.getTimeoutTask().reset(timeoutAction);
		TimeoutTask.getTimeoutTask().setEnabled(timeoutAction, true);
	}
	private void init() {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
		.permitAll()
		.build());

		//Check if sdcard is mounted.
		int errorTimes = 0;
		while(true) {
			String dfResult = ShellUtils.shell("df");
			if(!StringUtils.isBlank(dfResult)) {
				dfResult = dfResult.replaceAll("\t", " ");
				if(dfResult.indexOf("/sdcard ") != -1) {
					break;
				}
			} else {
				errorTimes ++;
				if(errorTimes >= 5)
					break;
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		SysConfig.init(null);
		String RVM_VERSION = SysConfig.get("RVM.VERSION");
		String RVM_VERSION_ID = SysConfig.get("RVM.VERSION.ID");
		String EXTERNAL_PROP_UPDATE_FIELD_SET = SysConfig.get("EXTERNAL.PROP.UPDATE.FIELD.SET");
		Properties propUpdateFields = new Properties();
		if(!StringUtils.isBlank(EXTERNAL_PROP_UPDATE_FIELD_SET)) {
			String[] fields = EXTERNAL_PROP_UPDATE_FIELD_SET.split(";");
			for(int i=0;i<fields.length;i++) {
				fields[i] = fields[i].trim();
				if(fields[i].length() == 0)
					continue;
				propUpdateFields.put(fields[i], StringUtils.nullToEmpty(SysConfig.get(fields[i])));
			}
		}
		Properties propExternalConfig = PropUtils.loadFile(SysConfig.get("EXTERNAL.FILE"));
		String OLD_RVM_VERSION_ID=propExternalConfig.getProperty("RVM.VERSION.ID");
		propExternalConfig.putAll(propUpdateFields);
		if(!StringUtils.isBlank(RVM_VERSION_ID)) {
			if(StringUtils.isBlank(propExternalConfig.getProperty("RVM.VERSION.ID"))) {
				propExternalConfig.put("RVM.VERSION.ID", RVM_VERSION_ID);
				propExternalConfig.put("RVM.VERSION", RVM_VERSION);
			}
		}
		SysConfig.set(propExternalConfig);
		PropUtils.update(SysConfig.get("EXTERNAL.FILE"), propExternalConfig);
		try {
			String ipAddress = SocketUtils.getIpAddress();
			if(ipAddress == null) {
				ipAddress = "127.0.0.1";
			}
			SysConfig.set("LOCAL.IP", ipAddress);
		} catch (Exception e) {
		}
		if(!StringUtils.isBlank(SysConfig.get("SUB.CONFIG.RESOURCE"))) {
			try {
				SysConfig.set(PropUtils.loadResource(SysConfig.get("SUB.CONFIG.RESOURCE")));
			} catch(Exception e) {
			}
		}
		String VERSION_UPDATE_FIELDS = SysConfig.get("VERSION.UPDATE.FIELDS");
		Properties propVersionUpdateFields = new Properties();
		if(!StringUtils.isBlank(VERSION_UPDATE_FIELDS)) {
			String[] fields = VERSION_UPDATE_FIELDS.split(";");
			for(int i=0;i<fields.length;i++) {
				fields[i] = fields[i].trim();
				if(fields[i].length() == 0)
					continue;
				propVersionUpdateFields.put(fields[i], StringUtils.nullToEmpty(SysConfig.get(fields[i])));
			}
		}
		try {
			String init_shell = SysConfig.get("INIT.SHELL");
			if(!StringUtils.isBlank(init_shell)) {
				InputStream is = getClass().getClassLoader().getResourceAsStream(init_shell);
				DataInputStream dataIS = new DataInputStream(is);
				String cmd = null;
				List<String> listCmd = new ArrayList<String>();
				while((cmd = dataIS.readLine()) != null) {
					cmd = cmd.trim();
					if(cmd.startsWith("#"))
						continue;
					if(cmd.length() == 0)
						continue;
					listCmd.add(cmd);
				}
				is.close();
				if(listCmd.size() > 0) {
					ShellUtils.shell(listCmd);
				}
			}
		} catch(Exception e) {
		}
		
		SysGlobal.execute(new Thread() {
			public void run() {
				try {
					String init_shell_file = SysConfig.get("INIT.SHELL.FILE");
					if(!StringUtils.isBlank(init_shell_file)) {
						File file = new File(init_shell_file);
						if(file.isFile()) {
							FileInputStream fis = new FileInputStream(file);
							DataInputStream dataIS = new DataInputStream(fis);
							String cmd = null;
							List<String> listCmd = new ArrayList<String>();
							while((cmd = dataIS.readLine()) != null) {
								cmd = cmd.trim();
								if(cmd.startsWith("#"))
									continue;
								if(cmd.length() == 0)
									continue;
								listCmd.add(cmd);
							}
							fis.close();
							if(listCmd.size() > 0) {
								ShellUtils.shell(listCmd);
							}
						}
					}
				} catch(Exception e) {
				}
			}
		});
		//Init PLATFORM
		try {
			/* Missing read/write permission, trying to chmod the file */
			Process su;
			su = Runtime.getRuntime().exec("su");
			String cmd = "busybox uname -m\n"
					+ "exit\n";
			su.getOutputStream().write(cmd.getBytes());
			InputStream is = su.getInputStream();
			String output = new String(IOUtils.read(is));
			String OS_PLATFORM = null;
			if(!StringUtils.isBlank(SysConfig.get("PLATFORM.SET"))) {
			String[] PLATFORM_SET = SysConfig.get("PLATFORM.SET").split(";");
				for(int i=0;i<PLATFORM_SET.length;i++) {
					String[] PLATFORM_INFO = PLATFORM_SET[i].split(":");
					if(PLATFORM_INFO.length < 2)
						continue;
					String PLATFORM = PLATFORM_INFO[0];
					String[] PLATFORM_NAME = PLATFORM_INFO[1].split(",");
					if(PLATFORM_NAME.length < 1)
						continue;
					for(int n=0;n<PLATFORM_NAME.length;n++) {
						if(output.indexOf(PLATFORM_NAME[n]) != -1) {
							OS_PLATFORM = PLATFORM;
							break;
						}
					}
					if(OS_PLATFORM != null)
						break;
				}
			}
			if(OS_PLATFORM != null)
				SysConfig.set("PLATFORM", OS_PLATFORM);
		} catch (Exception e) {
			e.printStackTrace();
		}
		(new LoggerInit()).Init(null);
		(new GUIInit()).Init(null);
		CommEntity.init();
	}

	@Override
	public String getName() {
		return "RVMWelcome";
	}

	@Override
	public void updateLanguage() {
		TextView contentView = (TextView) findViewById(R.id.fullscreen_content);
		if(contentView != null)
			contentView.setText(R.string.welcome);
		TextView startRemindTextView = (TextView) findViewById(R.id.starting_remind);
		if(startRemindTextView != null)
			startRemindTextView.setText(R.string.starting_remind);
	}
	public boolean doEventFilter(HashMap hsmpEvent) {
		if(enableEvent) {
			return super.doEventFilter(hsmpEvent);
		}
		return false;
	}
	@Override
	public void doEvent(HashMap hsmpEvent) {
	}
	
	private TimeoutAction timeoutAction = new TimeoutAction() {
		@Override
		public void apply(int forwardSeconds, final int remainedSeconds) {
			GUIAction guiAction = new GUIAction() {
				@Override
				protected void doAction(Object[] paramObjs) {
					int remainedSeconds = (Integer) paramObjs[1];
					if (remainedSeconds == 0) {
						startNextActivity();
					}
				}
			};
			executeGUIAction(true, guiAction, new Object[] { forwardSeconds,
					remainedSeconds });
		}
	};
	@Override
	public void onStart() {
		super.onStart();
		Intent intent = getIntent();
		if(intent != null) {
			String param = intent.getStringExtra("PARAM");
			if(!StringUtils.isBlank(param)) {
				Properties prop = new Properties();
				prop.setProperty("TIME", (new Date()).toString());
				prop.setProperty("PARAM", param);
				PropUtils.save(new File("/sdcard/rvmgui.properties"), prop);
			}
		}
	}
	@Override
	public void finish() {
		super.finish();
		TimeoutTask.getTimeoutTask().setEnabled(timeoutAction, false);
		TimeoutTask.getTimeoutTask().removeTimeoutAction(timeoutAction);
	}
	
	TextWatcher mTextWatcher = new TextWatcher() {
        private CharSequence temp;
        @Override
        public void beforeTextChanged(CharSequence s, int arg1, int arg2,
                int arg3) {
            temp = s;
        }
       
        @Override
        public void onTextChanged(CharSequence s, int arg1, int arg2,
                int arg3) {
   
        	TimeoutTask.getTimeoutTask().reset(timeoutAction);
        }
       
        @Override
        public void afterTextChanged(Editable s) {

        }
    };
   
	private void startNextActivity(){
		if(!hasInited) {
	    	Intent intent = new Intent(RVMActivity.this,RVMMainActivity.class);
	    	intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
			finish();
		}else{
			String baseActivityName = null;
			BaseActivity baseActivity = null;
			int idx = 0;
			while((baseActivityName = GUIGlobal.getTopBaseActivity(idx)) != null) {
				//Find the other activity
				if(baseActivity == null && !RVMActivity.this.getName().equals(baseActivityName)) {
					baseActivity = GUIGlobal.getBaseActivity(baseActivityName);
				}
				if(baseActivity != null)
					break;
				idx ++;
			}
			if(baseActivity != null) {
		    	Intent intent = new Intent(RVMActivity.this,baseActivity.getClass());
		    	intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);
			} else {
		    	Intent intent = new Intent(RVMActivity.this,RVMMainActivity.class);
		    	intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);
			}
			finish();
		}
	}
}
