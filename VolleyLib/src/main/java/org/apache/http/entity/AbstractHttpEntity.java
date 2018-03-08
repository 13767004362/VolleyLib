// Decompiled by Jad v1.5.8e2. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://kpdus.tripod.com/jad.html
// Decompiler options: packimports(3) fieldsfirst ansi space 
// Source File Name:   AbstractHttpEntity.java

package org.apache.http.entity;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

import java.io.IOException;

/**
 * @deprecated Class AbstractHttpEntity is deprecated
 */

public abstract class AbstractHttpEntity
	implements HttpEntity
{

	protected boolean chunked;
	protected Header contentEncoding;
	protected Header contentType;

	protected AbstractHttpEntity()
	{
		throw new RuntimeException("Stub!");
	}

	public Header getContentType()
	{
		throw new RuntimeException("Stub!");
	}

	public Header getContentEncoding()
	{
		throw new RuntimeException("Stub!");
	}

	public boolean isChunked()
	{
		throw new RuntimeException("Stub!");
	}

	public void setContentType(Header contentType)
	{
		throw new RuntimeException("Stub!");
	}

	public void setContentType(String ctString)
	{
		throw new RuntimeException("Stub!");
	}

	public void setContentEncoding(Header contentEncoding)
	{
		throw new RuntimeException("Stub!");
	}

	public void setContentEncoding(String ceString)
	{
		throw new RuntimeException("Stub!");
	}

	public void setChunked(boolean b)
	{
		throw new RuntimeException("Stub!");
	}

	public void consumeContent()
		throws IOException, UnsupportedOperationException
	{
		throw new RuntimeException("Stub!");
	}
}
