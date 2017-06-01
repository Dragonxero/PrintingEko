package common.init;

import com.google.code.microlog4android.config.PropertyConfigurator;

import java.util.HashMap;

public class LoggerInit implements InitInterface {

	private static String micrologfile = null;
	@Override
	public void Init(HashMap hsmp) {
		// TODO Auto-generated method stub
		if(micrologfile == null) {
			if(hsmp != null) {
				micrologfile = (String)hsmp.get("microlog.properties");
			}
			if(micrologfile == null) {
				micrologfile = "assets/microlog.properties";
			}
			PropertyConfigurator.getConfigurator().configure(micrologfile);
		}
	}
}
