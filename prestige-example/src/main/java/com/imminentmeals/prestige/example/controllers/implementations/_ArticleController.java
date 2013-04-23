package com.imminentmeals.prestige.example.controllers.implementations;

import static com.imminentmeals.prestige.annotations.meta.Implementations.PRODUCTION;

import javax.inject.Inject;

import com.imminentmeals.prestige.annotations.ControllerImplementation;
import com.imminentmeals.prestige.example.controllers.ArticleController;
import com.imminentmeals.prestige.example.models.NewsArticle;
import com.imminentmeals.prestige.example.models.NewsSource;
import com.imminentmeals.prestige.example.presentations.ArticlePresentation;
import com.imminentmeals.prestige.example.presentations.Messages;
import com.squareup.otto.Subscribe;

/**
 *
 * @author Dandre Allison
 */
@ControllerImplementation(PRODUCTION)
class _ArticleController implements ArticleController, Messages.ArticlePresentation {
	@Inject /* package */NewsSource news_source;
	
	@Override
	public void attachPresentation(Object presentation) {
		_presentation = (ArticlePresentation) presentation;
	}

	@Override
	@Subscribe
	public void willCreatePresentation(WillCreatePresentation message) {
		if (message.has_two_panes) {
			_presentation.stop();
			return;
		}
		
		// Display the correct news article.
	    final NewsArticle article = news_source.categoryForIndex(message.category_index).getArticle(message.article_index);
	    _presentation.displayArticle(article);
	}

	private ArticlePresentation _presentation;
}
