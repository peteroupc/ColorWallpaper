package com.upokecenter.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.json.JSONArray;
import org.json.JSONException;


public class JsonArraySerializer implements IStreamObjectSerializer<JSONArray> {


	@Override
	public JSONArray readObjectFromStream(InputStream stream) throws IOException {
		try {
			return new JSONArray(StreamUtility.streamToString(stream));
		} catch (JSONException e) {
			return null;
		}
	}

	@Override
	public void writeObjectToStream(JSONArray obj, OutputStream file)
			throws IOException {
		StreamUtility.stringToStream(obj.toString(),file);
	}
	
}