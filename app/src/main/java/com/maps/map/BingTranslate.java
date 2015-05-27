package com.maps.map;

import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;

/**
 * Some messages include information for which we can't hardcode the translation.
 * For example, while we can hardcode the translation for the word "map", we can't
 * do this for the "addition" message type which adds unstructured text to an event
 * (e.g. directing users to go to a certain website for more info).
 * 
 * Google Translate is no longer free, so we have to use Bing Translate. Which is kind
 * of funny considering this is an Android project...
 * 
 * The library being used to enable this is:
 * https://code.google.com/p/microsoft-translator-java-api/
 *
 * Please check out the link, it has some good information about using the library and
 * it has instructions for getting your own key!
 */

public class BingTranslate {
	
	static public String bingTranslate(String query, Language langTo)
	{
		
		if(langTo.equals(Language.ENGLISH))
			return query;

		/*Please replace this with your own client id and client secret. Follow the first
		 * two steps do this: http://msdn.microsoft.com/en-us/library/hh454950.aspx */
		
		Translate.setClientId("deadbeef");
	    Translate.setClientSecret("XCZ1uWkyW1RWv1P0KElJL4D26ME4jFynoQEgY422Qk0=");
	    String translatedText = "";
		try {
			translatedText = Translate.execute(query, Language.ENGLISH, langTo);
		} catch (Exception e) {
			e.printStackTrace();
		}
	    return translatedText;
	}
}
