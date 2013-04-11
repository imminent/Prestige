package com.imminentmeals.prestige.example.controllers.implementations;

import static com.imminentmeals.prestige.annotations.meta.Implementations.PRODUCTION;

import javax.inject.Inject;
import javax.inject.Named;

import com.imminentmeals.prestige.annotations.ControllerImplementation;
import com.imminentmeals.prestige.example.controllers.ArticleController;
import com.imminentmeals.prestige.example.models.NewsArticle;
import com.imminentmeals.prestige.example.models.NewsSource;
import com.imminentmeals.prestige.example.presentations.ArticlePresentation;
import com.imminentmeals.prestige.example.presentations.Messages;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

/**
 *
 * @author Dandre Allison
 */
@ControllerImplementation(PRODUCTION)
class _ArticleController implements ArticleController, Messages.ArticlePresentation {

	@Inject
	/* package */_ArticleController(@Named(BUS) Bus bus) {
		bus.register(this);
	}
	
	@Override
	public void attachPresentation(Object presentation) {
		_presentation = (ArticlePresentation) presentation;
	}

	/**
	 *
	 * @see com.imminentmeals.prestige.example.presentations.Messages.ArticlePresentation#willCreatePresentation(com.imminentmeals.prestige.example.presentations.Messages.ArticlePresentation.WillCreatePresentation)
	 */
	@Override
	@Subscribe
	public void willCreatePresentation(WillCreatePresentation message) {
		if (message.has_two_panes) {
			_presentation.stop();
			return;
		}
		
		// Display the correct news article.
	    final NewsArticle article = NewsSource.getInstance().getCategory(message.category_index)
	        .getArticle(message.article_index);
	    _presentation.displayArticle(article);
	}

	private ArticlePresentation _presentation;
}
