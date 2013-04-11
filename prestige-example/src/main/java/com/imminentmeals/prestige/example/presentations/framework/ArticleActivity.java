package com.imminentmeals.prestige.example.presentations.framework;

import static com.imminentmeals.prestige.SegueController.sendMessage;
import static com.imminentmeals.prestige.example.presentations.framework.PresentationUtilities.EXTRA;
import android.app.Activity;
import android.os.Bundle;

import com.imminentmeals.prestige.annotations.PresentationImplementation;
import com.imminentmeals.prestige.example.R.bool;
import com.imminentmeals.prestige.example.models.NewsArticle;
import com.imminentmeals.prestige.example.presentations.ArticlePresentation;
import com.imminentmeals.prestige.example.presentations.Messages.ArticlePresentation.WillCreatePresentation;

@PresentationImplementation
public class ArticleActivity extends Activity implements ArticlePresentation {
	public static final String EXTRA_CATEGORY_INDEX = EXTRA + "ArticleActivity.CATEGORY_INDEX";
	public static final String EXTRA_ARTICLE_INDEX = EXTRA + "ArticleActivity.ARTICLE_INDEX";

/* Lifecycle */
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		// The news category index and the article index for the article we are to display
	    final int category_index, article_index;
	    category_index = getIntent().getExtras().getInt(EXTRA_CATEGORY_INDEX, 0);
	    article_index = getIntent().getExtras().getInt(EXTRA_ARTICLE_INDEX, 0);
	    final boolean has_two_panes = getResources().getBoolean(bool.has_two_panes);
	    sendMessage(this, new WillCreatePresentation(has_two_panes, category_index, article_index));
	}

/* ArticlePresentation Contract */
	@Override
	public void displayArticle(NewsArticle article) {
		// Place an ArticleFragment as our content pane
	    ArticleFragment fragment = new ArticleFragment();
	    getFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();
	    fragment.displayArticle(article);
	}
	
	@Override
	public void stop() {
		finish();
	}
}
