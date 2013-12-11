/* Copyright (C) 2013 M. Steve Todd mstevetodd@enginedriver.rrclubs.org

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jmri.enginedriver;

import java.util.HashMap;
import jmri.enginedriver.logviewer.ui.LogViewerActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;

public class about_page extends Activity {

	private threaded_application mainapp; // hold pointer to mainapp
	private Menu AMenu;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mainapp=(threaded_application)this.getApplication();

//		SharedPreferences prefs  = getSharedPreferences("jmri.enginedriver_preferences", 0);
//		String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
//		setTitle(getApplicationContext().getResources().getString(R.string.app_name_about) + "    |    Throttle Name: " + 
//				prefs.getString("throttle_name_preference", defaultName));

		setContentView(R.layout.about_page);

		// format and show version info
		TextView v = (TextView) findViewById(R.id.about_info);
		String s;
		// ED version info
		try {
			s = "Engine Driver: v" 
				+ mainapp.getPackageManager().getPackageInfo(mainapp.getPackageName(), 0 ).versionName;
		}
		catch(Exception e) {
			s = "";
		}
		if(mainapp.host_ip != null) {
			// JMRI version info
			HashMap<String, String> metadata = threaded_application.metadata;
			if (metadata != null && metadata.size() > 0)
				s += "\nJMRI v: " + metadata.get("JMRIVERCANON") + "    build: " + metadata.get("JMRIVERSION");
			// WiT info
			if(mainapp.withrottle_version != 0.0) {
				s += "\nWiThrottle: v" + mainapp.withrottle_version;
				s += String.format("    Heartbeat: %d secs\n", mainapp.heartbeatInterval);
			}
		}
		// show info
		v.setText(s);

		// show ED webpage
		WebView webview = (WebView) findViewById(R.id.about_webview);
		webview.loadUrl("file:///android_asset/about_page.html");

	};

	@Override
	public void onResume() {
		super.onResume();
		mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
		mainapp.cancelRunningNotify();

		if(AMenu != null)
		{
			mainapp.displayEStop(AMenu);
		}
	};

	@Override
	public void onPause()
	{
		super.onPause();
		if(this.isFinishing()) {		//if finishing, expedite it and don't invoke setContentIntentNotification
			return;
		}
		mainapp.setContentIntentNotification(this.getIntent());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.about_menu, menu);
		AMenu = menu;
		mainapp.displayEStop(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		Intent in;
		switch (item.getItemId()) {
		case R.id.throttle_mnu:
			this.finish();
			connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
			break;
		case R.id.turnouts_mnu:
			this.finish();
			in=new Intent().setClass(this, turnouts.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
			break;
		case R.id.routes_mnu:
			in = new Intent().setClass(this, routes.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
			break;
		case R.id.web_mnu:
			this.finish();
			in=new Intent().setClass(this, web_activity.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.preferences_mnu:
			in=new Intent().setClass(this, preferences.class);
			startActivityForResult(in, 0);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.power_control_mnu:
			this.finish();
			in=new Intent().setClass(this, power_control.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.logviewer_menu:
			this.finish();
			Intent logviewer=new Intent().setClass(this, LogViewerActivity.class);
			startActivity(logviewer);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.EmerStop:
			mainapp.sendEStopMsg();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	//Handle pressing of the back button to end this activity
	@Override
	public boolean onKeyDown(int key, KeyEvent event) {
		if(key==KeyEvent.KEYCODE_BACK)
		{
			this.finish();  //end this activity
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
		}
		return(super.onKeyDown(key, event));
	};


}
