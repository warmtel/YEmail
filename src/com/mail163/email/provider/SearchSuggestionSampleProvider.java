package com.mail163.email.provider;

import android.content.SearchRecentSuggestionsProvider;

public class SearchSuggestionSampleProvider extends
		SearchRecentSuggestionsProvider {

	public final static String AUTHORITY = "com.mail163.email.provider.SearchSuggestionSampleProvider";
	public final static int MODE = DATABASE_MODE_QUERIES;

	public SearchSuggestionSampleProvider() {
		super();
		setupSuggestions(AUTHORITY, MODE);
	}
}
