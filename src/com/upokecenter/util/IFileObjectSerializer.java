package com.upokecenter.util;

import java.io.File;
import java.io.IOException;

public interface IFileObjectSerializer<T> {
	public T readObjectFromFile(File file) throws IOException;
	public void writeObjectToFile(T obj, File file) throws IOException;
}
