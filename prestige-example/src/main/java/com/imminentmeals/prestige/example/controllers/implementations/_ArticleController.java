package com.imminentmeals.prestige.example.controllers.implementations;

import com.imminentmeals.prestige.annotations.ControllerImplementation;
import com.imminentmeals.prestige.annotations.InjectModel;
import com.imminentmeals.prestige.annotations.InjectPresentation;
import com.imminentmeals.prestige.example.controllers.ArticleController;
import com.imminentmeals.prestige.example.models.NewsArticle;
import com.imminentmeals.prestige.example.models.NewsSource;
import com.imminentmeals.prestige.example.presentations.ArticlePresentation;
import com.imminentmeals.prestige.example.presentations.Messages;
import com.squareup.otto.Subscribe;

/**
 * @author Dandre Allison
 */
@ControllerImplementation
/* package */class _ArticleController implements ArticleController, Messages.ArticlePresentation {
  @InjectModel /* package */NewsSource news_source;
  @InjectPresentation /* package */ArticlePresentation presentation;

  @Override @Subscribe
  public void willShowPresentation(WillShowPresentation message) {
    if (message.has_two_panes) {
      presentation.stop();
      return;
    }

    // Display the correct news article.
    final NewsArticle article =
        news_source.categoryForIndex(message.category_index).getArticle(message.article_index);
    presentation.displayArticle(article);
  }
}
