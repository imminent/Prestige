package com.imminentmeals.prestige.example.presentations;

import com.imminentmeals.prestige.annotations.Presentation;
import com.imminentmeals.prestige.example.models.NewsArticle;
import com.imminentmeals.prestige.example.models.NewsCategory;
import com.imminentmeals.prestige.example.presentations.protocols.NewsReaderProtocol;

/**
 *
 * @author Dandre Allison
 */
@Presentation(protocol = NewsReaderProtocol.class)
public interface NewsReaderPresentation {

	void category(NewsCategory category);

	void setupActionBar(String[] categories, boolean show_tabs, int selected_index);

	void article(NewsArticle article);

	void showArticleActivity(int category_index, int article_index);

	void showCategoryDialog(String[] categories);
}
