package com.hex.riplay;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

	private static MyPreferenceFragment fragment;
	private static SharedPreferences pref;


	protected void onCreate(Bundle savedInstanceState) {
		load(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		ActionBar actionBar = getSupportActionBar();
		assert actionBar != null;

		actionBar.setTitle("Settings");
		actionBar.setDisplayHomeAsUpEnabled(true);

		if (fragment == null) fragment = new MyPreferenceFragment();
		getFragmentManager().beginTransaction().replace(R.id.settings_container, fragment).commit();
	}


	public boolean onOptionsItemSelected(android.view.MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return onOptionsItemSelected(item);
	}


	public static String getHost() {
		if (SettingsActivity.isConfigured())
			return String.format("http://%s:%s", SettingsActivity.getAddress(), SettingsActivity.getPort());
		else
			return "";
	}


	public static boolean isConfigured() {
		String address = getAddress();
		String port = getPort();

		return !(address == null || address.isEmpty() || port == null || port.isEmpty());
	}


	public static void load(Context con) {
		pref = PreferenceManager.getDefaultSharedPreferences(con);
	}


	private static String getAddress() {
		return pref.getString("address", null);
	}


	private static String getPort() {
		return pref.getString("port", null);
	}


	public static class MyPreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

		@Override
		public void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.settings);
			bindPreferenceSummaryToValue(findPreference("address"));
			bindPreferenceSummaryToValue(findPreference("port"));
		}


		/**
		 * Called when a Preference has been changed by the user. This is
		 * called before the state of the Preference is about to be updated and
		 * before the state is persisted.
		 *
		 * @param preference The changed Preference.
		 * @param newValue   The new value of the Preference.
		 * @return True to update the state of the Preference with the new value.
		 */
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			preference.setSummary((String) newValue);
			return true;
		}


		/**
		 * Called when a Preference has been clicked.
		 *
		 * @param preference The Preference that was clicked.
		 * @return True if the click was handled.
		 */
		@Override
		public boolean onPreferenceClick(final Preference preference) {
			return true;
		}


		/**
		 * Initalize preference change listener on each preference
		 *
		 * @param preference to set onPreferenceChangeListener to
		 */
		private void bindPreferenceSummaryToValue(Preference preference) {
			preference.setOnPreferenceClickListener(this);
			preference.setOnPreferenceChangeListener(this);

			String value = pref.getString(preference.getKey(), null);
			if (!(value == null || value.isEmpty()))
				preference.setSummary(value);
		}
	}
}

