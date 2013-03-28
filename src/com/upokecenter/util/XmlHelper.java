
package com.upokecenter.util;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class XmlHelper
{

	private XmlHelper(){}

	private static boolean isNullOrEmpty(String s){
		return s==null || s.length()==0;
	}

	public static String GetFullName(XmlPullParser reader){
		if(reader.getPrefix()!=null)
			return reader.getPrefix()+":"+reader.getName();
		return reader.getName();
	}
	private static boolean StringEquals(String s1, String s2){
		if(s1==s2)return true;
		if(s1==null||s2==null)return false;
		return s1.equals(s2);
	}
	public static boolean IsName(XmlPullParser reader, String namespaceName, String localName){
		if(reader==null)throw new NullPointerException();
		String ns=reader.getNamespace();
		if(isNullOrEmpty(ns)) {
			ns=null;
		}
		if(isNullOrEmpty(namespaceName)) {
			namespaceName=null;
		}
		return (StringEquals(ns,namespaceName)) &&
				(isNullOrEmpty(localName) ||
						StringEquals(reader.getName(),localName));
	}
	public static boolean IsEndElement(XmlPullParser reader) throws XmlPullParserException{
		if(reader==null)throw new NullPointerException();
		return IsEndElement(reader,null);
	}
	public static boolean IsEndElement(XmlPullParser reader, String name) throws XmlPullParserException{
		if(reader==null)throw new NullPointerException();
		return (reader.getEventType()==XmlPullParser.END_TAG) &&
				(isNullOrEmpty(name) || StringEquals(GetFullName(reader),name));
	}
	public static boolean IsEndElement(XmlPullParser reader, String namespaceName, String localName) throws XmlPullParserException{
		if(reader==null)throw new NullPointerException();
		return (reader.getEventType()==XmlPullParser.END_TAG)
				&& IsName(reader,namespaceName,localName);
	}
	public static boolean IsElement(XmlPullParser reader) throws XmlPullParserException{
		if(reader==null)throw new NullPointerException();
		return reader.getEventType()==XmlPullParser.START_TAG;
	}
	public static boolean IsElement(XmlPullParser reader, String name) throws XmlPullParserException{
		if(reader==null)throw new NullPointerException();
		return reader.getEventType()==XmlPullParser.START_TAG &&
				(isNullOrEmpty(name) || StringEquals(GetFullName(reader),name));
	}
	public static boolean IsElement(XmlPullParser reader, String namespaceName, String localName)
			throws XmlPullParserException{
		if(reader==null)throw new NullPointerException();
		return reader.getEventType()==XmlPullParser.START_TAG && IsName(reader,namespaceName,localName);
	}
	public static boolean IsStartElement(XmlPullParser reader) throws XmlPullParserException, IOException{
		if(reader==null)throw new NullPointerException();
		MoveToContent(reader);
		return reader.getEventType()==XmlPullParser.START_TAG;
	}
	public static boolean IsStartElement(XmlPullParser reader, String name) throws XmlPullParserException, IOException{
		if(reader==null)throw new NullPointerException();
		MoveToContent(reader);
		return reader.getEventType()==XmlPullParser.START_TAG &&
				(isNullOrEmpty(name) || StringEquals(GetFullName(reader),name));
	}
	public static boolean IsStartElement(XmlPullParser reader, String namespaceName, String localName) throws XmlPullParserException, IOException{
		if(reader==null)throw new NullPointerException();
		MoveToContent(reader);
		return reader.getEventType()==XmlPullParser.START_TAG && IsName(reader,namespaceName,localName);
	}

	public static int GetDepth(XmlPullParser reader){
		if(reader==null)throw new NullPointerException();
		return Math.max(reader.getDepth()-1,0);
	}

	// Similar to ReadElementString, but reads up to the
	// end element tag instead of past it.
	public static String ReadSubtreeString(XmlPullParser reader)
			throws XmlPullParserException, IOException {
		if(reader==null)throw new NullPointerException();
		// Return if not on a start element tag
		if(reader.getEventType()!=XmlPullParser.START_TAG)
			return "";
		StringBuilder builder=new StringBuilder();
		int depth=GetDepth(reader);
		while(GetDepth(reader)!=depth || reader.getEventType()!=XmlPullParser.END_TAG){
			reader.next();
			if(reader.getEventType()!=XmlPullParser.END_TAG){
				if(reader.getEventType()==XmlPullParser.ENTITY_REF||
						reader.getEventType()==XmlPullParser.CDSECT||
						reader.getEventType()==XmlPullParser.TEXT){
					builder.append(reader.getText());
				}
			}
		}
		return builder.toString();
	}

	public static boolean Read(XmlPullParser parser) throws XmlPullParserException, IOException{
		if(parser==null)throw new NullPointerException();
		if(parser.getEventType()==XmlPullParser.END_DOCUMENT)
			return false;
		parser.nextToken();
		return true;
	}
	public static int MoveToContent(XmlPullParser parser) throws XmlPullParserException, IOException{
		if(parser==null)throw new NullPointerException();
		while(parser.getEventType()==XmlPullParser.PROCESSING_INSTRUCTION ||
				parser.getEventType()==XmlPullParser.DOCDECL ||
				parser.getEventType()==XmlPullParser.IGNORABLE_WHITESPACE ||
				parser.getEventType()==XmlPullParser.START_DOCUMENT){
			if(!Read(parser)){
				break;
			}
		}
		return parser.getEventType();
	}
	public static String XmlEscape(String value){
		if(value==null)return null;
		value=value.replaceAll("&","&amp;");
		value=value.replaceAll("<","&lt;");
		value=value.replaceAll(">","&gt;");
		return value;
	}

	public static boolean findChildToDepth(
			XmlPullParser parser, int parentDepth, String namespaceName, String name)
					throws XmlPullParserException, IOException {
		if(GetDepth(parser)<parentDepth)return false;
		while(Read(parser)){
			int currentDepth=GetDepth(parser);
			if(currentDepth==parentDepth && IsEndElement(parser))
				return false;
			if(currentDepth>parentDepth && IsStartElement(parser,namespaceName,name))
				return true;
		}
		return false;
	}


	public static boolean moveToElement(XmlPullParser parser, String namespaceName, String name)
			throws XmlPullParserException, IOException {
		while(Read(parser)){
			if(IsStartElement(parser,namespaceName,name))
				return true;
		}
		return false;
	}
}
