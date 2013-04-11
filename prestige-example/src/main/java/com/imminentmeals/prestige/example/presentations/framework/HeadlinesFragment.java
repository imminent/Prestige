package com.imminentmeals.prestige.example.presentations.framework;

import static com.imminentmeals.prestige.SegueController.sendMessage;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ListFragment;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.imminentmeals.prestige.example.R.layout;
import com.imminentmeals.prestige.example.models.NewsCategory;
import com.imminentmeals.prestige.example.presentations.Messages.NewsReaderPresentation.HeadlineSelected;

/**
 *
 * @author Dandre Allison
 */
public class HeadlinesFragment extends ListFragment {

	/**
	 * <p>Constructs a {@link HeadlinesFragment}.</p>
	 */
	public HeadlinesFragment() { }
	
/* Lifecycle */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		_headlines_adapter = new ArrayAdapter<String>(getActivity(),
				layout.headline_item, _headlines) {

					@Override
					public View getView(int position, View convertView,
							ViewGroup parent) {
						final TextView text = (TextView) super.getView(position, convertView, parent);
						text.setTypeface(redacted_font);
						return text;
					}
			
		};
	}
	
	@Override 
	public void onStart() {
		super.onStart();
		setListAdapter(_headlines_adapter);
	}

/* ListFragment Callbacks */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		redacted_font = Typeface.createFromAsset(activity.getAssets(), "fonts/redacted-script-light.ttf");
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		sendMessage(this, new HeadlineSelected(position));
	}

/* Public API */
	/**
	 * @param category
	 */
	public void loadCategory(NewsCategory category) {
		_headlines.clear();
		for (String headline : category.getArticleHeadlines())
			_headlines.add(headline);
	    _headlines_adapter.notifyDataSetChanged();
	}


	public void setSelectable(boolean selectable) {
	      getListView().setChoiceMode(selectable? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
	}
	
	// The list of headlines that we are displaying
	private List<String> _headlines = new ArrayList<String>();
	// The list adapter for the list we are displaying
	private ArrayAdapter<String> _headlines_adapter;
	/* inner-class */Typeface redacted_font;
}
