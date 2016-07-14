package com.hex.riplay.Adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hex.riplay.Items.Song;
import com.hex.riplay.R;
import com.hex.riplay.SettingsActivity;
import com.hex.riplay.Utils.Downloader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created on 03/07/16.
 */
public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.ViewHolder> {
	private final Context context;
	private final List<Song> items;
	private int selection = -1;


	public CollectionAdapter(Context context) {
		this.context = context;
		this.items = new ArrayList<>();
	}


	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(context).inflate(R.layout.item_index, parent, false);
		return new ViewHolder(view);
	}


	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		holder.title.setText(items.get(position).name);
		holder.itemView.setActivated(position == selection);
	}


	@Override
	public int getItemCount() {
		return items.size();
	}


	public void addAll(List<Song> items) {
		this.items.clear();
		Collections.sort(items);
		this.items.addAll(items);
		notifyDataSetChanged();
	}


	public void clearSelection() {
		if (this.selection >= 0) {
			int prevSelection = this.selection;
			this.selection = -1;
			notifyItemChanged(prevSelection);
		}
	}


	public void setSelection(int selection) {
		clearSelection();
		notifyItemChanged(this.selection = selection);
		System.out.println("Selected: " + this.selection);
	}


	public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
		final TextView title;


		public ViewHolder(View itemView) {
			super(itemView);
			title = (TextView) itemView.findViewById(R.id.textView);

			itemView.setOnClickListener(this);
			itemView.setOnLongClickListener(this);
		}


		@Override
		public void onClick(View v) {
			new RequestTask(String.format(
					"%s/api/play/%d", SettingsActivity.getHost(), items.get(getAdapterPosition()).index
			)).execute();
//			Snackbar.make(v, String.format("Playing %s", items.get(getAdapterPosition()).name), Snackbar.LENGTH_SHORT).show();
		}


		@Override
		public boolean onLongClick(View v) {
			new RequestTask(String.format(
					"%s/api/queue/%d", SettingsActivity.getHost(), items.get(getAdapterPosition()).index
			)).execute();
			Snackbar.make(v, String.format("Queued %s", items.get(getAdapterPosition()).name), Snackbar.LENGTH_SHORT).show();
			return true;
		}
	}


	private class RequestTask extends AsyncTask<Void, Void, String> {
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
			super.onPostExecute(s);
		}


		@Override
		protected void onCancelled(String s) {
			super.onCancelled(s);
		}
	}
}
