package com.llm.toon.log;

import java.util.Map;

public class Log {
	
	public static void logComparison(String mode, long timeMs, Map<String, Object> usage) {
	    System.out.println("\n====================");
	    System.out.println("   " + mode + " RESULTS");
	    System.out.println("====================");
	    System.out.println("Time taken:     " + timeMs + " ms");
	    
	    if (usage != null) {
	        System.out.println("Prompt tokens:  " + usage.get("prompt_tokens"));
	        System.out.println("Completion:     " + usage.get("completion_tokens"));
	        System.out.println("Total tokens:   " + usage.get("total_tokens"));
	    }
	    System.out.println("====================\n");
	}


}
