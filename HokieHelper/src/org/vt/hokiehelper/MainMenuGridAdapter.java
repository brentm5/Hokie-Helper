/**
 * 
 */
package org.vt.hokiehelper;

import android.content.Context;
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MainMenuGridAdapter extends BaseAdapter {

	private static final String TAG = MainMenuGridAdapter.class.getName();

	private Context context_;
	private String[] text_ = { "Maps", "Dining", "News", "Football",
			"Rec Sports", "Information" };
	private Integer[] images_ = { R.drawable.maps, R.drawable.dining,
			R.drawable.news, R.drawable.football, R.drawable.recsports,
			R.drawable.information };

	public MainMenuGridAdapter(Context c) {
		this.context_ = c;
	}

	// @Override
	public int getCount() {
		// TODO Auto-generated method stub
		return text_.length;
	}

	// @Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View MyView = convertView;
		Log.d(TAG, "Setting up menu item " + text_[position]);
		// we define the view that will display on the grid
		// Inflate the layout
		LayoutInflater li = (LayoutInflater) context_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		MyView = li.inflate(R.layout.main_menu_grid_item, null);
		MyView.setPadding(8, 0, 8, 8);
		MyView.setOnClickListener((OnClickListener) context_);
		MyView.setId(position);
		TextView tv = (TextView) MyView.findViewById(R.id.grid_item_text);
		tv.setPadding(0, 0, 0, 4);
		ImageView iv = (ImageView) MyView.findViewById(R.id.grid_item_image);
		tv.setText(text_[position]);
		iv.setImageResource(images_[position]);
		return MyView;
	}

	// @Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	// @Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

}
