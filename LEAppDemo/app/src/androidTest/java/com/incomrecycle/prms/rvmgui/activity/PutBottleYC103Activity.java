package com.incomrecycle.prms.rvmgui.activity;

import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.incomrecycle.common.SysConfig;
import com.incomrecycle.common.task.TimeoutAction;
import com.incomrecycle.common.task.TimeoutTask;
import com.incomrecycle.common.utils.StringUtils;
import com.incomrecycle.prms.rvm.gui.BaseActivity;
import com.incomrecycle.prms.rvm.gui.CommonServiceHelper;
import com.incomrecycle.prms.rvm.gui.CommonServiceHelper.GUICommonService;
import com.incomrecycle.prms.rvm.gui.GUIAction;
import com.incomrecycle.prms.rvm.gui.action.GUIActionRecycleDone;
import com.incomrecycle.prms.rvm.gui.action.GUIActionRecycleEnd;
import com.incomrecycle.prms.rvm.gui.action.GUIActionRecycleStart;
import com.incomrecycle.prms.rvmgui.R;
import com.incomrecycle.prms.rvmgui.RVMMainActivity;

public class PutBottleYC103Activity extends BaseActivity {
	private ImageView imageView1 = null;
	@Override
	protected void onStart() {
		super.onStart();
        //open door
	    executeGUIAction(false, new GUIActionRecycleStart(), null);
		TextView noPut = (TextView) findViewById(R.id.puthint);
		noPut.setVisibility(View.GONE);
		TextView noPutL = (TextView) findViewById(R.id.puthintLarge);
		noPutL.setVisibility(View.GONE);
	    //Timer
		final int timeoutOnPutBottle = Integer.valueOf(SysConfig.get("RVM.TIMEOUT.PUTBOTTLE"));
		final int timeoutOnBottleScan = Integer.valueOf(SysConfig.get("RVM.TIMEOUT.BOTTLESCAN"));
	  	TimeoutTask.getTimeoutTask().addTimeoutAction(timeoutAction, timeoutOnPutBottle, false);
	  	TimeoutTask.getTimeoutTask().addTimeoutAction(timeoutActionForBottleScan, timeoutOnBottleScan, false);
	    TimeoutTask.getTimeoutTask().reset(timeoutAction);
		TimeoutTask.getTimeoutTask().setEnabled(timeoutAction, true);
		TimeoutTask.getTimeoutTask().setEnabled(timeoutActionForBottleScan, false);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		((TextView) findViewById(R.id.time)).setText("");
		TimeoutTask.getTimeoutTask().setEnabled(timeoutAction, false);
		TimeoutTask.getTimeoutTask().setEnabled(timeoutActionForBottleScan, false);
		if(imageView1 != null){
			AnimationDrawable animaition = (AnimationDrawable)imageView1.getBackground(); 
		    animaition.stop();
		}
	}
	public void finish() {
		super.finish();
		TimeoutTask.getTimeoutTask().removeTimeoutAction(timeoutAction);
		TimeoutTask.getTimeoutTask().removeTimeoutAction(timeoutActionForBottleScan);
	}
	
	private TimeoutAction timeoutActionForBottleScan = new TimeoutAction() {

		@Override
		public void apply(int forwardSeconds, final int remainedSeconds) {
			if(remainedSeconds == 0) {
				stopPutBottlePhase();
			}else{
				final TextView textTime= (TextView) findViewById(R.id.time);
				textTime.post(new Runnable() {					
					@Override
					public void run() {
						textTime.setText(""+remainedSeconds);	
					}
			   	  });
			}
		}
	};
	private void stopPutBottlePhase() {
		executeGUIAction(true, new GUIActionRecycleEnd(), null);
		Intent intent = new Intent(this,RVMMainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(intent);
		executeGUIAction(true, new GUIActionRecycleDone(), null);
		finish();
	}
	private TimeoutAction timeoutAction = new TimeoutAction() {
		@Override
		public void apply(int forwardSeconds, int remainedSeconds) {
			GUIAction guiAction = new GUIAction() {
				@Override
				protected void doAction(Object[] paramObjs) {
					int remainedSeconds = (Integer)paramObjs[1];
			    	if(remainedSeconds == 0) {
			    		stopPutBottlePhase();
			    	 } else {
						  TextView textTime= (TextView) findViewById(R.id.time);
						  textTime.setText(""+remainedSeconds);
					}
				}
			};
			executeGUIAction(false,guiAction,new Object[]{forwardSeconds,remainedSeconds});
		}
	};
		
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().getDecorView().setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
		setContentView(R.layout.activity_put_103);
		TextView serialNumber = (TextView)findViewById(R.id.serial_number);
		serialNumber.setText(SysConfig.get("RVM.CODE"));
	    Button btnClose = (Button)findViewById(R.id.close);
	    btnClose.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				stopPutBottlePhase();
			}
	    });
	}    

	@Override
	public void updateLanguage() {
		Button btnClose = (Button)findViewById(R.id.close);
		btnClose.setText(R.string.close);
		TextView noPut = (TextView) findViewById(R.id.puthint);
		noPut.setText(R.string.puthint);
		TextView noPutL = (TextView) findViewById(R.id.puthintLarge);
		noPutL.setText(R.string.puthint);
	}

	@Override
	public void doEvent(HashMap hsmpEvent) {
		GUICommonService guiCommonService = CommonServiceHelper.getGUICommonService();
		String EVENT = (String)hsmpEvent.get("EVENT");
		if("INFORM".equalsIgnoreCase(EVENT)) {
			String INFORM = (String)hsmpEvent.get("INFORM");
			//the first photoelectricity and timeout stop
			if("BOTTLE_SCANNING".equalsIgnoreCase(INFORM)) {			
				//Disable timeout
				TimeoutTask.getTimeoutTask().setEnabled(timeoutAction, false);
				TimeoutTask.getTimeoutTask().reset(timeoutActionForBottleScan);
				TimeoutTask.getTimeoutTask().setEnabled(timeoutActionForBottleScan, true);
				//ImageView
				if(imageView1 != null){
				    imageView1 =(ImageView)findViewById(R.id.ImageView);
					imageView1.setVisibility(View.VISIBLE);
				    imageView1.setBackgroundResource(R.anim.scanbarcode);
				    AnimationDrawable animaition = (AnimationDrawable)imageView1.getBackground();
				    animaition.setOneShot(false);
				    animaition.start();
				}

			    TextView textView = (TextView)findViewById(R.id.hint);
			    String scanHint = getString(R.string.hintScanBarcode);
			    textView.setText(scanHint);
			    textView.setVisibility(View.VISIBLE);
				TextView noPut = (TextView) findViewById(R.id.puthint);
				noPut.setVisibility(View.VISIBLE);
				TextView noPutL = (TextView) findViewById(R.id.puthintLarge);
				noPutL.setVisibility(View.VISIBLE);
			    Button btnClose = (Button)findViewById(R.id.close);
			    btnClose.setVisibility(View.GONE);
			}
			//bar code not found
			if("BAR_CODE_NOT_FOUND".equalsIgnoreCase(INFORM)) {			
				TimeoutTask.getTimeoutTask().setEnabled(timeoutActionForBottleScan, false);
				TimeoutTask.getTimeoutTask().reset(timeoutAction);
				TimeoutTask.getTimeoutTask().setEnabled(timeoutAction, true);
				//Hint: Bottle bar code is not found, please put bottle bar code up and continuet.
				if(imageView1 != null){
					imageView1 =(ImageView)findViewById(R.id.ImageView);
					imageView1.setVisibility(View.VISIBLE);
				    imageView1.setBackgroundResource(R.anim.throwbackground);
				    AnimationDrawable animaition = (AnimationDrawable)imageView1.getBackground(); 
				    animaition.setOneShot(false);
				    animaition.start();
				}
			    TextView textView = (TextView)findViewById(R.id.hint);
			    String notFound =getString(R.string.NoScanBarcodePer) + "<font color=\"#ff0000\">"+ getString(R.string.NoScanBarcode) +"</font>";
			    textView.setText(Html.fromHtml(notFound));
				TextView noPut = (TextView) findViewById(R.id.puthint);
				noPut.setVisibility(View.GONE);
				TextView noPutL = (TextView) findViewById(R.id.puthintLarge);
				noPutL.setVisibility(View.GONE);
			    Button btnClose = (Button)findViewById(R.id.close);
			    btnClose.setVisibility(View.VISIBLE);

			}
			//bar code was rejected
			if("BAR_CODE_REJECT".equalsIgnoreCase(INFORM)) {			
				TimeoutTask.getTimeoutTask().setEnabled(timeoutActionForBottleScan, false);
				TimeoutTask.getTimeoutTask().reset(timeoutAction);
				TimeoutTask.getTimeoutTask().setEnabled(timeoutAction, true);
				//Hint: Bottle bar code is recognized, please withdraw it.
				if(imageView1 != null){
					imageView1 =(ImageView)findViewById(R.id.ImageView);
					imageView1.setVisibility(View.VISIBLE);
				    imageView1.setBackgroundResource(R.anim.throwbackground);
				    AnimationDrawable animaition = (AnimationDrawable)imageView1.getBackground(); 
				    animaition.setOneShot(false);
				    animaition.start();
				}
			    TextView textView = (TextView)findViewById(R.id.hint);
			    String scanHint = getString(R.string.RecognizedScanBarcode);
			    textView.setText(scanHint);
			    textView.setVisibility(View.VISIBLE);
				TextView noPut = (TextView) findViewById(R.id.puthint);
				noPut.setVisibility(View.GONE);
				TextView noPutL = (TextView) findViewById(R.id.puthintLarge);
				noPutL.setVisibility(View.GONE);
			    Button btnClose = (Button)findViewById(R.id.close);
			    btnClose.setVisibility(View.VISIBLE);
			}
			//bottle is ready to receive
			if("RECYCLE_ENABLE".equalsIgnoreCase(INFORM)) {	
				TimeoutTask.getTimeoutTask().setEnabled(timeoutAction, false);
				TimeoutTask.getTimeoutTask().setEnabled(timeoutActionForBottleScan, true);
			    TextView textView = (TextView)findViewById(R.id.hint);
			    String scanHint = getString(R.string.bottleSureAccept);
			    textView.setText(scanHint);
			    textView.setVisibility(View.VISIBLE);
				TextView noPut = (TextView) findViewById(R.id.puthint);
				noPut.setVisibility(View.VISIBLE);
				TextView noPutL = (TextView) findViewById(R.id.puthintLarge);
				noPutL.setVisibility(View.VISIBLE);
			    Button btnClose = (Button)findViewById(R.id.close);
			    btnClose.setVisibility(View.GONE);
			}
			if("RECYCLE_ENABLE_FORCE".equalsIgnoreCase(INFORM)) {			
				TimeoutTask.getTimeoutTask().setEnabled(timeoutAction, false);
				TimeoutTask.getTimeoutTask().reset(timeoutActionForBottleScan);
				TimeoutTask.getTimeoutTask().setEnabled(timeoutActionForBottleScan, true);
				//ImageView
				if(imageView1 != null){
				    imageView1 =(ImageView)findViewById(R.id.ImageView);
					imageView1.setVisibility(View.VISIBLE);
				    imageView1.setBackgroundResource(R.anim.scanbarcode);
				    AnimationDrawable animaition = (AnimationDrawable)imageView1.getBackground();
				    animaition.setOneShot(false);
				    animaition.start();
				}

				TextView textView = (TextView)findViewById(R.id.hint);
				textView.setText(R.string.ForceRecycle);
				textView.setVisibility(View.VISIBLE);
				TextView noPut = (TextView) findViewById(R.id.puthint);
				noPut.setVisibility(View.GONE);
				TextView noPutL = (TextView) findViewById(R.id.puthintLarge);
				noPutL.setVisibility(View.GONE);
			    Button btnClose = (Button)findViewById(R.id.close);
			    btnClose.setVisibility(View.VISIBLE);
			}
			if("REACH_MAX_BOTTLES".equalsIgnoreCase(INFORM)) {			
				TimeoutTask.getTimeoutTask().setEnabled(timeoutAction, false);
				TimeoutTask.getTimeoutTask().setEnabled(timeoutActionForBottleScan, false);
				TextView textViewHint = (TextView)findViewById(R.id.hint);
				textViewHint.setText(R.string.ReachMaxOfStorage);
				executeGUIAction(3,new GUIAction() {
					@Override
					protected void doAction(Object[] paramObjs) {
			    		stopPutBottlePhase();
					}
				},null);
			}
			if("REACH_MAX_BOTTLES_PER_OPT".equalsIgnoreCase(INFORM)) {			
				TimeoutTask.getTimeoutTask().setEnabled(timeoutAction, false);
				TimeoutTask.getTimeoutTask().setEnabled(timeoutActionForBottleScan, false);
				TextView textViewHint = (TextView)findViewById(R.id.hint);
				textViewHint.setText(R.string.ReachMaxPerOpt);
				executeGUIAction(3,new GUIAction() {
					@Override
					protected void doAction(Object[] paramObjs) {
			    		stopPutBottlePhase();
					}
				},null);
			}
			//bar code was rejected finish
			if("BOTTLE_REJECTED".equalsIgnoreCase(INFORM)) {			
				//Enable timeout
				TimeoutTask.getTimeoutTask().setEnabled(timeoutActionForBottleScan, false);
				TimeoutTask.getTimeoutTask().reset(timeoutAction);
				TimeoutTask.getTimeoutTask().setEnabled(timeoutAction, true);				
				//Hint: Please put bottle in hole and continue or put the end button for stop
				if(imageView1 != null){
				    //ImageView
				    imageView1 =(ImageView)findViewById(R.id.ImageView);
					imageView1.setVisibility(View.VISIBLE);
				    imageView1.setBackgroundResource(R.anim.throwbackground);
				    AnimationDrawable animaition = (AnimationDrawable)imageView1.getBackground(); 
				    animaition.setOneShot(false);
				    animaition.start();
				}

			    TextView textView = (TextView)findViewById(R.id.hint);
			    String scanHint = getString(R.string.RejectedFinish);
			    textView.setText(scanHint);
			    textView.setVisibility(View.VISIBLE);
				TextView noPut = (TextView) findViewById(R.id.puthint);
				noPut.setVisibility(View.GONE);
				TextView noPutL = (TextView) findViewById(R.id.puthintLarge);
				noPutL.setVisibility(View.GONE);
			    Button btnClose = (Button)findViewById(R.id.close);
			    btnClose.setVisibility(View.VISIBLE);
			}
			//bottle was accepted finish and the third photoelectrycity
			if("BOTTLE_RECYCLED".equalsIgnoreCase(INFORM) || "RESET".equalsIgnoreCase(INFORM)) {			
				//Enable timeout
				TimeoutTask.getTimeoutTask().setEnabled(timeoutActionForBottleScan, false);
				TimeoutTask.getTimeoutTask().reset(timeoutAction);
				TimeoutTask.getTimeoutTask().setEnabled(timeoutAction, true);				
				//Hint: Please put bottle in hole or put the end button for stop
				if(imageView1 != null){
				    //ImageView
				    imageView1 =(ImageView)findViewById(R.id.ImageView);
					imageView1.setVisibility(View.VISIBLE);
				    imageView1.setBackgroundResource(R.anim.throwbackground);
				    AnimationDrawable animaition = (AnimationDrawable)imageView1.getBackground(); 
				    animaition.setOneShot(false);
				    animaition.start();
				}
			    TextView textView = (TextView)findViewById(R.id.hint);
				TextView noPut = (TextView) findViewById(R.id.puthint);
				noPut.setVisibility(View.GONE);
				TextView noPutL = (TextView) findViewById(R.id.puthintLarge);
				noPutL.setVisibility(View.GONE);
			    Button btnClose = (Button)findViewById(R.id.close);
			    btnClose.setVisibility(View.VISIBLE);
			    int bottleNumber =0;
			    try {
					HashMap<String,Object> hsmpResult = guiCommonService.execute("GUIRecycleCommonService", "recycledBottleSummary", null);
					if(hsmpResult != null) {
						List<HashMap<String,String>> listHsmpRecycleBottle = (List<HashMap<String,String>>)hsmpResult.get("RECYCLED_BOTTLE_SUMMARY");
						if(listHsmpRecycleBottle != null && listHsmpRecycleBottle.size() > 0) {
							for(int i=0;i<listHsmpRecycleBottle.size();i++) {
								HashMap<String,String> hsmpRecycleBottle = listHsmpRecycleBottle.get(i);
								String BOTTLE_BAR_CODE = hsmpRecycleBottle.get("BOTTLE_BAR_CODE");
								String BOTTLE_AMOUNT = hsmpRecycleBottle.get("BOTTLE_AMOUNT");
								String BOTTLE_VOL = hsmpRecycleBottle.get("BOTTLE_VOL");
								String BOTTLE_STUFF = hsmpRecycleBottle.get("BOTTLE_STUFF");
								String BOTTLE_COUNT = hsmpRecycleBottle.get("BOTTLE_COUNT");
								int bottleCount = Integer.parseInt(BOTTLE_COUNT);
								bottleNumber += bottleCount;
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			    int maxBotleNumber = Integer.parseInt(SysConfig.get("MAX.BOTTLES.PER.OPT"));
			    int canPutNumber = maxBotleNumber - bottleNumber;
			    String AcceptedFinish = getString(R.string.AcceptedFinish);
			    String AcceptedFinishText = StringUtils.replace(StringUtils.replace(AcceptedFinish,"$PUT_BOTTLE_NUM$","<font color=\"#ff0000\">"+bottleNumber+"</font>"),"$REMAIN_BOTTLE_NUM$","<font color=\"#ff0000\">"+canPutNumber+"</font>");
			    textView.setText(Html.fromHtml(AcceptedFinishText));
			}
			if("BOTTLE_RECYCLING_EXCEPTION".equalsIgnoreCase(INFORM)){
				TimeoutTask.getTimeoutTask().setEnabled(timeoutAction, false);
				TimeoutTask.getTimeoutTask().setEnabled(timeoutActionForBottleScan, false);
				TextView noPut = (TextView) findViewById(R.id.puthint);
				noPut.setVisibility(View.GONE);
				TextView noPutL = (TextView) findViewById(R.id.puthintLarge);
				noPutL.setVisibility(View.GONE);
				TextView textViewHint = (TextView)findViewById(R.id.hint);
				textViewHint.setText(R.string.bottle_state_excepiton);
				executeGUIAction(3,new GUIAction() {
					@Override
					protected void doAction(Object[] paramObjs) {
			    		stopPutBottlePhase();
					}
				},null);
			}
		}
	}
}
