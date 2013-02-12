package com.upokecenter.util;

public class HeaderParser {
	
	private static int skipQuotedString(String v, int index){
		// assumes index points to quotation mark
		index++;
		int length=v.length();
		char c=0;
		while(index<length){ // skip whitespace
			c=v.charAt(index);
			if(c=='"'){
				if(index+1>=length || v.charAt(index+1)!='"'){
					return index+1;
				} else {
					index++;
				}
			}
			index++;
		}
		return index;
	}
	
	@SuppressWarnings("unused")
	private static int getPositiveNumber(String v, int index){
		int length=v.length();
		char c=0;
		boolean haveNumber=false;
		int startIndex=index;
		while(index<length){ // skip whitespace
			c=v.charAt(index);
			if(c<'0' || c>'9'){
				if(!haveNumber)return -1;
				try {
					return Integer.parseInt(v.substring(startIndex,index),10);
				} catch(NumberFormatException e){
					return -1;
				}
			} else haveNumber=true;
			index++;
		}
		try {
			return Integer.parseInt(v.substring(startIndex,length),10);
		} catch(NumberFormatException e){
			return -1;
		}
	}
	
	private static String getQuotedString(String v, int index){
		// assumes index points to quotation mark
		index++;
		int startIndex=index;
		int length=v.length();
		char c=0;
		while(index<length){ // skip whitespace
			c=v.charAt(index);
			if(c=='"'){
				if(index+1>=length || v.charAt(index+1)!='"'){
					// we have no inner quotes
					return v.substring(startIndex,index);
				} else {
					// we have an inner quote
					break;
				}
			}
			index++;
		}
		index=startIndex;
		StringBuilder sb=new StringBuilder();
		while(index<length){ // skip whitespace
			c=v.charAt(index);
			if(c=='"'){
				if(index+1>=length || v.charAt(index+1)!='"'){
					return sb.toString();
				} else {
					// we have an inner quote
					index++;
				}
			}
			sb.append(c);
			index++;
		}
		return sb.toString();
	}

	private static String getDefaultCharset(String contentType){
		if(contentType.length()>=5){
			char c;
			c=contentType.charAt(0);
			if(c!='T' && c!='t')return "";
			c=contentType.charAt(1);
			if(c!='E' && c!='e')return "";
			c=contentType.charAt(2);
			if(c!='X' && c!='x')return "";
			c=contentType.charAt(3);
			if(c!='T' && c!='t')return "";
			c=contentType.charAt(4);
			if(c!='/')return "";
			return "US-ASCII";
		}
		return "";
	}
	
	public static String getCharset(String contentType){
		if(contentType==null){
			return "";
		}
		int io=contentType.indexOf(";");
		int length=contentType.length();
		char c=0;
		boolean skipData=false;
		if(io<0)return getDefaultCharset(contentType); // no charset
		io++;
		while(true){
			if(skipData){
				while(io<length){ // skip non-equal
					c=contentType.charAt(io);
					if(c=='=')break;
					io++;
				}
				io++;
				while(io<length){ // skip whitespace
					c=contentType.charAt(io);
					if(c!=' ' && c!='\t' && c!='\r' && c!='\n')break;
					io++;
				}
				if(io<length && contentType.charAt(io)=='"'){
					io=skipQuotedString(contentType,io);
					// skip semicolon if it exists
					if(io<length && contentType.charAt(io)==';'){
						io++;
					}
				} else {
					while(io<length){ // skip non-semicolon
						c=contentType.charAt(io);
						if(c==';')break;
						io++;
					}
					// skip semicolon
					io++;
				}
				while(io<length){ // skip non-whitespace
					c=contentType.charAt(io);
					if(c==' ' || c=='\t' || c=='\r' || c=='\n')break;
					io++;
				}
				skipData=false;
			}
			while(io<length){ // skip whitespace
				c=contentType.charAt(io);
				if(c!=' ' && c!='\t' && c!='\r' && c!='\n')break;
				io++;
			}
			if(io+8>length)return getDefaultCharset(contentType);
			skipData=true;
			// Find out if it's CHARSET
			c=contentType.charAt(io++);
			if(c!='C' && c!='c')continue;
			c=contentType.charAt(io++);
			if(c!='H' && c!='h')continue;
			c=contentType.charAt(io++);
			if(c!='A' && c!='a')continue;
			c=contentType.charAt(io++);
			if(c!='R' && c!='r')continue;
			c=contentType.charAt(io++);
			if(c!='S' && c!='s')continue;
			c=contentType.charAt(io++);
			if(c!='E' && c!='e')continue;
			c=contentType.charAt(io++);
			if(c!='T' && c!='t')continue;
			c=contentType.charAt(io);
			if(c=='*'){
				io++;
				if(io<length && contentType.charAt(io)>='0' && contentType.charAt(io)<='9'){
					// Rare continuation
					return getDefaultCharset(contentType);
				}
				return getDefaultCharset(contentType);// encoded data, which is not supported
			}
			while(io<length){ // skip whitespace
				c=contentType.charAt(io);
				if(c!=' ' && c!='\t' && c!='\r' && c!='\n')break;
				io++;
			}
			if(io>=length || contentType.charAt(io)!='='){
				// ill-formed content-type
				return getDefaultCharset(contentType);
			}
			io++;
			while(io<length){ // skip whitespace
				c=contentType.charAt(io);
				if(c!=' ' && c!='\t' && c!='\r' && c!='\n')break;
				io++;
			}
			if(io<length && contentType.charAt(io)=='"'){
				return getQuotedString(contentType,io);
			} else {
				int startIndex=io;
				while(io<length){ // skip non-semicolon
					c=contentType.charAt(io);
					if(c==';')break;
					io++;
				}
				return contentType.substring(startIndex,io);
			}
			
		}
	}
	
}
