package com.hex.riplay;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.MainThread;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.hex.riplay.Adapters.CollectionAdapter;
import com.hex.riplay.Items.Song;
import com.hex.riplay.Server.UpdateChecker;
import com.hex.riplay.Utils.Downloader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

	private static final int NO_CONFIG = 1;
	private static final int NORMAL = 0;
	private final UpdateChecker server = new UpdateChecker(this);

	@BindView(R.id.recyclerView)
	RecyclerView recyclerView;
	@BindView(R.id.play)
	ImageButton play;
	@BindView(R.id.stop)
	ImageButton stop;
	@BindView(R.id.toolbar)
	Toolbar toolbar;

	private CollectionAdapter adapter;
	private boolean subscribed = false;
	private boolean playing = false;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		SettingsActivity.load(this);
		ButterKnife.bind(this);

		setSupportActionBar(toolbar);

		adapter = new CollectionAdapter(getApplicationContext());
		recyclerView.setAdapter(adapter);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));


		if (SettingsActivity.isConfigured()) {
			updateCollection(null);
		} else {
			startActivityForResult(new Intent(this, SettingsActivity.class), NO_CONFIG);
		}
	}


	@Override
	protected void onStop() {
		super.onStop();
		unsubscribe();
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		System.out.println("returned");
		if (SettingsActivity.isConfigured()) {
			updateCollection(null);
		} else
			finish();
	}


	@Override
	protected void onStart() {
		super.onStart();
		subscribe();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.settings:
				startActivityForResult(new Intent(this, SettingsActivity.class), NORMAL);
				break;
		}
		return true;
	}


	@OnClick({R.id.play, R.id.stop, R.id.skip, R.id.play_all, R.id.play_shuffle})
	@Override
	public void onClick(View view) {
		String url = null;
		switch (view.getId()) {
			case R.id.stop:
				if (!playing) return;
				url = String.format("%s/api/stop", SettingsActivity.getHost());
				break;
			case R.id.play:
				if (playing) return;
				url = String.format("%s/api/start", SettingsActivity.getHost());
				break;
			case R.id.skip:
				url = String.format("%s/api/skip", SettingsActivity.getHost());
				break;
			case R.id.play_all:
				url = String.format("%s/api/play/all", SettingsActivity.getHost());
				break;
			case R.id.play_shuffle:
				url = String.format("%s/api/play/shuffle", SettingsActivity.getHost());
				break;
		}
		if (url != null) {
			new RequestTask(url).execute();
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			// Vibrate for 500 milliseconds
			v.vibrate(200);
		} else
			System.out.println("Unknown click");
	}


	public void updateCollection(String json) {
		if (json == null) {
			new RequestTask(String.format("%s/api/collection", SettingsActivity.getHost())).execute();
		} else {
			try {
				JSONObject object = new JSONObject(json);
				Iterator<String> iter = object.keys();
				List<Song> items = new ArrayList<>();
				while (iter.hasNext()) {
					String key = iter.next();
					items.add(new Song(Integer.parseInt(key), object.getString(key)));
				}
				adapter.addAll(items);
			} catch (JSONException exeption) {
				exeption.printStackTrace();
			}
		}
	}


	//{"playing": false, "queued": 1}
	//{"index": 9, "queued": 0, "playing": true, "name": "MIRAI NIKKI (Cover espa\u00f1ol).webm"}
	@MainThread
	public void updateStatus(String json) {
		if (json == null) {
			new RequestTask(String.format("%s/api/status", SettingsActivity.getHost())).execute();
		} else
			try {
				JSONObject object = new JSONObject(json);
				if (playing = object.getBoolean("playing")) {
					play.setColorFilter(ContextCompat.getColor(this, R.color.green));
					stop.setColorFilter(ContextCompat.getColor(this, R.color.white));
					int selection = object.getInt("index") - 1;
					adapter.setSelection(selection);
					recyclerView.smoothScrollToPosition(selection);
				} else {
					play.setColorFilter(ContextCompat.getColor(this, R.color.white));
					stop.setColorFilter(ContextCompat.getColor(this, R.color.green));
					adapter.clearSelection();
				}
			} catch (JSONException exeption) {
				exeption.printStackTrace();
			}
	}


	private void subscribe() {
		if (!subscribed && adapter.getItemCount() > 0) {
			new RequestTask(String.format("%s/api/subscribe?port=8000", SettingsActivity.getHost())).execute();
			server.start();
		}
	}


	private void unsubscribe() {
		if (subscribed) {
			new RequestTask(String.format("%s/api/unsubscribe", SettingsActivity.getHost())).execute();
			server.stop();
		}
	}


	public class RequestTask extends AsyncTask<Void, Void, String> {
		private final String url;


		public RequestTask(String url) {
			this.url = url;
		}


		@Override
		protected String doInBackground(Void... params) {
			try {
				return Downloader.getPage(url);
			} catch (IOException e) {
				e.printStackTrace();
			}
			cancel(true);
			return null;
		}


		@Override
		protected void onPostExecute(String s) {
			if (s == null) return;

			if (url.endsWith("/collection")) {
				updateCollection(s);
				subscribe();
			} else if (url.endsWith("/status")) {
				updateStatus(s);
			} else if (url.contains("/subscribe")) {
				subscribed = true;
			} else if (url.endsWith("/unsubscribe")) {
				subscribed = false;
			}
		}


		@Override
		protected void onCancelled(String s) {
			if (url.endsWith("/collection")) {
				Snackbar.make(getWindow().getDecorView(), "No connection ...", Snackbar.LENGTH_INDEFINITE)
						.setAction("Try again", new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								updateCollection(null);
							}
						}).show();
			}
		}
	}
}
