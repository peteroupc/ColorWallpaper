package com.upokecenter.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.json.JSONArray;
import org.json.JSONException;


public class JsonArraySerializer implements IFileObjectSerializer<JSONArray> {

	private static void stringToFile(File file, String s) throws IOException{
		Writer writer=null;
		try {
			writer=new FileWriter(file);
			writer.write(s);
		} finally {
			if(writer!=null)writer.close();
		}
	}

	private static String fileToString(File file) 
			throws IOException {
		Reader reader = new FileReader(file);
		try {
			StringBuilder builder=new StringBuilder();
			char[] buffer = new char[4096];
			while(true){
				int count=reader.read(buffer);
				if(count<0)break;
				builder.append(buffer,0,count);
			}
			return builder.toString();
		} finally {
			if(reader!=null)reader.close();
		}
	}

	@Override
	public JSONArray readObjectFromFile(File file) throws IOException {
		// TODO Auto-generated method stub
		try {
			return new JSONArray(fileToString(file));
		} catch (JSONException e) {
			return null;
		}
	}

	@Override
	public void writeObjectToFile(JSONArray obj, File file)
			throws IOException {
		// TODO Auto-generated method stub
		stringToFile(file,obj.toString());
	}
	
}