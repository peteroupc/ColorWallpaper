package com.upokecenter.android.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public interface IDownloadHandler<T> {
	/**
	 * Processes the HTTP response on a background thread.
	 * @param url
	 * @param stream
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public T processResponse(URL url, 
			InputStream stream, IHttpHeaders headers) throws IOException;
	/**
	 * Processes the data on the UI thread after it's downloaded.
	 * @param url URL of the data.
	 * @param value Data processed by 'processResponse'.
	 * @param exception If this value is non-null, an error has occurred
	 * and this exception contains further information on the error,
	 * and 'value' will be null.
	 * @param progress Number of URLs processed so far. This is
	 * not necessarily the index of the URL in the list passed to
	 * downloadUrls.
	 * @param total Total number of URLs to process. 
	 */
	public void onFinished(URL url, T value, IOException exception, int progress, int total);
}