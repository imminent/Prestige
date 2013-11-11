package com.imminentmeals.prestige.example.presentations.framework;

import android.app.Activity;
import com.imminentmeals.prestige.example.presentations.framework.NewsReaderActivity;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.junit.Before;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.robolectric.Robolectric.buildActivity;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class NewsReaderActivityTest {

  @Test public void testDefaultLayout() {
    final NewsReaderActivity activity = buildActivity(NewsReaderActivity.class).create().get();
    assertTrue(activity != null);
  }

  @Config(qualifiers="sw600dp-port")
  @Test public void testNexus7PortraitLayout() {
    final NewsReaderActivity activity = buildActivity(NewsReaderActivity.class).create().get();
    assertTrue(activity != null);
  }

  @Config(qualifiers="sw600dp-land")
  @Test public void testNexus7LandscapeLayout() {
    final NewsReaderActivity activity = buildActivity(NewsReaderActivity.class).create().get();
    assertTrue(activity != null);
  }

  @Config(qualifiers="sw720dp-port")
  @Test public void testNexus10PortraitLayout() {
    final NewsReaderActivity activity = buildActivity(NewsReaderActivity.class).create().get();
    assertTrue(activity != null);
  }

  @Config(qualifiers="sw720dp-land")
  @Test public void testNexus10LandscapeLayout() {
    final NewsReaderActivity activity = buildActivity(NewsReaderActivity.class).create().get();
    assertTrue(activity != null);
  }
}