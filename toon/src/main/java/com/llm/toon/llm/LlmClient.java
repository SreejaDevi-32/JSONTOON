package com.llm.toon.llm;

import com.llm.toon.dto.ParseResponse;

public interface LlmClient {
	
	 ParseResponse parseSalesText(String text) throws Exception;

}
