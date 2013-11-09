package com.imminentmeals.prestige.example.presentations;

import com.imminentmeals.prestige.annotations.Presentation;
import com.imminentmeals.prestige.example.models.NewsArticle;

/**
 *
 * @author Dandre Allison
 */
@Presentation
public interface ArticlePresentation {

	void displayArticle(NewsArticle article);

	void stop();
}
