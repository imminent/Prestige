package com.imminentmeals.prestige.example.presentations.framework;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import butterknife.InjectView;
import butterknife.Optional;
import com.imminentmeals.prestige.Prestige;
import com.imminentmeals.prestige.annotations.InjectDataSource;
import com.imminentmeals.prestige.annotations.PresentationImplementation;
import com.imminentmeals.prestige.example.R.id;
import com.imminentmeals.prestige.example.R.layout;
import com.imminentmeals.prestige.example.models.NewsArticle;
import com.imminentmeals.prestige.example.models.NewsCategory;
import com.imminentmeals.prestige.example.presentations.Messages;
import com.imminentmeals.prestige.example.presentations.Messages.NewsReaderPresentation.CategorySelected;
import com.imminentmeals.prestige.example.presentations.Messages.NewsReaderPresentation.WillRestorePresentation;
import com.imminentmeals.prestige.example.presentations.Messages.NewsReaderPresentation.WillStartPresentation;
import com.imminentmeals.prestige.example.presentations.NewsReaderPresentation;
import com.imminentmeals.prestige.example.presentations.protocols.NewsReaderProtocol;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import static com.imminentmeals.prestige.example.presentations.framework.PresentationUtilities.KEY;

/**
 * Main activity: shows headlines list and articles, if layout permits.
 *
 * This is the main activity of the application. It can have several different layouts depending on
 * the SDK version, screen size and orientation. The configurations are divided in two large groups:
 * single-pane layouts and dual-pane layouts.
 *
 * In single-pane mode, this activity shows a list of headlines using a {@link HeadlinesFragment}.
 * When the user clicks on a headline, a separate activity (a {@link ArticleActivity}) is launched
 * to show the news article.
 *
 * In dual-pane mode, this activity shows a {@link HeadlinesFragment} on the left side and an {@link
 * ArticleFragment} on the right side. When the user selects a headline on the left, the
 * corresponding article is shown on the right.
 */
@PresentationImplementation
public class NewsReaderActivity extends Activity implements NewsReaderPresentation,
    NewsReaderNavigationCallback {
  @Optional @InjectView(id.article) /* package */ View article;
  @InjectDataSource /* package */ NewsReaderProtocol data_source;
  private boolean _is_dual_pane;
  private int _category_index;
  private int _article_index;

  /* Lifecycle */
  @Override protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(layout.main_layout);

    // Finds the Fragment minions
    _headlines_fragment = (HeadlinesFragment) getFragmentManager().findFragmentById(id.headlines);
    _article_fragment = (ArticleFragment) getFragmentManager().findFragmentById(id.article);

    _is_dual_pane = article != null && article.getVisibility() == View.VISIBLE;
    restoreSelection(icicle);
  }

  @Override protected void onResume() {
    super.onResume();
    Prestige.sendMessage(
        new Messages.NewsReaderPresentation.WillShowPresentation(_is_dual_pane, _category_index));

    // Sets up Headlines
    _headlines_fragment.setSelectable(_is_dual_pane);
    if (_article_index != -1) {
      Prestige.sendMessage(new WillRestorePresentation(_category_index, _article_index));
    }
  }

  @Override protected void onStart() {
    super.onStart();
    Prestige.sendMessage(WillStartPresentation.DID_START);
  }

  /* Activity Callbacks */
  @Override public void onRestoreInstanceState(@Nonnull Bundle icicle) {
    restoreSelection(icicle);
    Prestige.sendMessage(new WillRestorePresentation(_category_index, _article_index));
  }

  /** Save instance state. Saves current category/article index. */
  @Override protected void onSaveInstanceState(@Nonnull Bundle icicle) {
    icicle.putInt(_KEY_CATEGORY_INDEX, data_source.categoryIndex());
    icicle.putInt(_KEY_ARTICLE_INDEX, data_source.articleIndex());
    super.onSaveInstanceState(icicle);
  }

/* NewsReaderPresentation Contract */
  @Override public void category(NewsCategory category) {
    _headlines_fragment.loadCategory(category);
  }

  @Override public void setupActionBar(String[] categories, boolean show_tabs, int selected_tab) {
    Log.d("NewsReaderActivity", "set up action bar");
    final ActionBar action_bar = getActionBar();
    assert action_bar != null;
    // Sets up an ActionBarNavigationObserver to deliver us the Action Bar navigation events
    final ActionBarNavigationObserver handler = new ActionBarNavigationObserver(this);
    if (show_tabs) {
      action_bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
      for (String category : categories)
        action_bar.addTab(action_bar.newTab().setText(category).setTabListener(handler));
    } else {
      action_bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
      final ArrayAdapter<String> adapter =
          new ArrayAdapter<>(this, layout.actionbar_list_item, categories);
      action_bar.setListNavigationCallbacks(adapter, handler);
    }
    action_bar.setSelectedNavigationItem(selected_tab);
  }

  @Override public void article(NewsArticle article) {
    _article_fragment.displayArticle(article);
  }

  @Override public void showArticleActivity(int category_index, int article_index) {
    final Intent intent = new Intent(this, ArticleActivity.class);
    intent.putExtra(ArticleActivity.EXTRA_CATEGORY_INDEX, category_index);
    intent.putExtra(ArticleActivity.EXTRA_ARTICLE_INDEX, article_index);
    startActivity(intent);
  }

  @Override public void showCategoryDialog(String[] categories) {
    new AlertDialog.Builder(this)
        .setTitle("Select a Category")
        .setItems(categories, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            Prestige.sendMessage(new CategorySelected(which));
          }
        })
        .create()
        .show();
  }

  /* NewsReaderNavigationCallback */
  @Override public void onCategorySelected(int category_index) {
    Prestige.sendMessage(new CategorySelected(category_index));
  }

/* Helpers */

  /** Restore category/article selection from saved state. */
  private void restoreSelection(Bundle icicle) {
    _category_index = icicle == null ? 0 : icicle.getInt(_KEY_CATEGORY_INDEX, 0);
    _article_index = icicle == null ? -1 : icicle.getInt(_KEY_ARTICLE_INDEX, -1);
  }

  private static final String _KEY_CATEGORY_INDEX = KEY + "NewsReaderActivity.CATEGORY_INDEX";
  private static final String _KEY_ARTICLE_INDEX = KEY + "NewsReaderActivity.ARTICLE_INDEX";
  private HeadlinesFragment _headlines_fragment;
  private ArticleFragment _article_fragment;
}
