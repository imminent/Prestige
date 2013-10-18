package com.imminentmeals.prestige.example.controllers.implementations;

import com.imminentmeals.prestige.annotations.ControllerImplementation;
import com.imminentmeals.prestige.annotations.InjectModel;
import com.imminentmeals.prestige.annotations.InjectPresentation;
import com.imminentmeals.prestige.example.controllers.NewsReaderController;
import com.imminentmeals.prestige.example.models.NewsCategory;
import com.imminentmeals.prestige.example.models.NewsSource;
import com.imminentmeals.prestige.example.presentations.Messages;
import com.imminentmeals.prestige.example.presentations.NewsReaderPresentation;
import com.squareup.otto.Subscribe;

/**
 *
 * @author Dandre Allison
 */
@ControllerImplementation
/* package */class _NewsReaderController implements NewsReaderController, Messages.NewsReaderPresentation {
	@InjectModel /* package */NewsSource news_source;
	@InjectPresentation /* package */NewsReaderPresentation presentation;

/* News Reader Controller Contract */
	@Override
	public int categoryIndex() {
		return _category_index;
	}

	@Override
	public int articleIndex() {
		return _article_index;
	}

/* News Reader Presentation Messages */
	@Override
	@Subscribe
	public void willCreatePresentation(WillCreatePresentation message) {
		_has_two_panes = message.has_two_panes;
	    presentation.setupActionBar(_CATEGORIES, _has_two_panes, message.category_index);
	}

	@Override
	@Subscribe
	public void willRestorePresentation(WillRestorePresentation message) {
		category(message.category_index, message.article_index);
	}

	@Override
	@Subscribe
	public void willStartPresentation(WillStartPresentation message) {
		category(_category_index, _article_index);
	}

	@Override
	@Subscribe
	public void onCategorySelected(CategorySelected message) {
		category(message.category_index, _NO_ARTICLE);
	}

	@Override
	@Subscribe
	public void onHeadlineSelected(HeadlineSelected message) {
		_article_index = message.article_index;
	    if (_has_two_panes)
	      presentation.article(getCurrentCategory().getArticle(_article_index));
	    else
	      presentation.showArticleActivity(_category_index, _article_index);
	}

/* Helpers */
	private void category(int category_index, int article_index) {
		_category_index = category_index;
		_article_index = article_index;
		final NewsCategory category = getCurrentCategory();
		presentation.category(category);
		// If we are displaying the article on the right, we have to update that too
		if (_has_two_panes)
			presentation.article(article_index == _NO_ARTICLE 
				? category.getArticle(0) 
				: category.getArticle(article_index));
	}
	
	private NewsCategory getCurrentCategory() {
	    return news_source.categoryForIndex(_category_index);
	}
	
	private static final int _NO_ARTICLE = -1;
	// List of category titles
	private final String _CATEGORIES[] = { "Top Stories", "Politics", "Economy", "Technology" };
	// Whether or not we are in dual-pane mode
	private boolean _has_two_panes;
	// The news category and article index currently being displayed
	private int _category_index;
	private int _article_index;
}
