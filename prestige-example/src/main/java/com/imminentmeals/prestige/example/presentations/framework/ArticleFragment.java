package com.imminentmeals.prestige.example.presentations.framework;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import com.imminentmeals.prestige.example.models.NewsArticle;

/**
 * @author Dandre Allison
 */
public class ArticleFragment extends Fragment {

  /**
   * <p>Constructs an {@link ArticleFragment}.</p>
   */
  public ArticleFragment() { }

/* Lifecyle */
  @Override public View onCreateView(LayoutInflater _, ViewGroup __, Bundle ___) {
    _web_view = new WebView(getActivity());
    loadWebView();
    return _web_view;
  }

/* Public API */
  /**
   * @param article
   */
  public void displayArticle(NewsArticle article) {
    _news_article = article;
    loadWebView();
  }
	
/* Helpers */

  /**
   * Loads article data into the webview.
   *
   * This method is called internally to update the webview's contents to the appropriate article's
   * text.
   */
  private void loadWebView() {
    if (_web_view != null) {
      _web_view.loadData(_news_article == null ? "" : _news_article.getBody(), "text/html", "utf-8");
    }
  }

  // The webview where we display the article (our only view)
  private WebView _web_view;

  // The article we are to display
  private NewsArticle _news_article = null;
}
