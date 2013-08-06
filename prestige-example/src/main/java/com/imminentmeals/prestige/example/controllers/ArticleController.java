package com.imminentmeals.prestige.example.controllers;

import com.imminentmeals.prestige.annotations.Controller;
import com.imminentmeals.prestige.example.presentations.ArticlePresentation;

/**
 *
 * @author Dandre Allison
 */
@Controller(presentation = ArticlePresentation.class)
public interface ArticleController { }
