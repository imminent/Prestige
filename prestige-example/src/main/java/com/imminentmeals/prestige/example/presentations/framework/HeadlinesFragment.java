package com.imminentmeals.prestige.example.presentations.framework;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.imminentmeals.prestige.Prestige;
import com.imminentmeals.prestige.example.R.layout;
import com.imminentmeals.prestige.example.models.NewsCategory;
import com.imminentmeals.prestige.example.presentations.Messages.NewsReaderPresentation.HeadlineSelected;
import java.util.ArrayList;
import java.util.List;

import static android.widget.ListView.CHOICE_MODE_NONE;
import static android.widget.ListView.CHOICE_MODE_SINGLE;

/**
 * @author Dandre Allison
 */
public class HeadlinesFragment extends ListFragment {

  /**
   * <p>Constructs a {@link HeadlinesFragment}.</p>
   */
  public HeadlinesFragment() { }

/* Lifecycle */
  @Override public void onCreate(Bundle icicle) {
    assert getActivity() != null;
    super.onCreate(icicle);
    _headlines_adapter = new ArrayAdapter<>(getActivity(), layout.headline_item, _headlines);
  }

  @Override public void onStart() {
    super.onStart();
    setListAdapter(_headlines_adapter);
  }

/* ListFragment Callbacks */
  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
  }

  @Override public void onListItemClick(ListView l, View v, int position, long id) {
    Prestige.sendMessage(new HeadlineSelected(position));
  }

/* Public API */
  /**
   * @param category
   */
  public void loadCategory(NewsCategory category) {
    _headlines.clear();
    for (String headline : category.getArticleHeadlines()) _headlines.add(headline);
    _headlines_adapter.notifyDataSetChanged();
  }

  public void setSelectable(boolean selectable) {
    if (getListView() != null) {
      getListView().setChoiceMode(selectable ? CHOICE_MODE_SINGLE : CHOICE_MODE_NONE);
    }
  }

  // The list of headlines that we are displaying
  private List<String> _headlines = new ArrayList<>();
  // The list adapter for the list we are displaying
  private ArrayAdapter<String> _headlines_adapter;
}
