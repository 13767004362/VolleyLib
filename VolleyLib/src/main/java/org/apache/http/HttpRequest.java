// Decompiled by Jad v1.5.8e2. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://kpdus.tripod.com/jad.html
// Decompiler options: packimports(3) fieldsfirst ansi space 
// Source File Name:   HttpRequest.java

package org.apache.http;


// Referenced classes of package org.apache.http:
//			HttpMessage, RequestLine

/**
 * @deprecated Interface HttpRequest is deprecated
 */

public interface HttpRequest
	extends HttpMessage
{

	public abstract RequestLine getRequestLine();
}
