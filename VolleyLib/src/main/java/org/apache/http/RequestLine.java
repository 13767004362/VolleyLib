// Decompiled by Jad v1.5.8e2. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://kpdus.tripod.com/jad.html
// Decompiler options: packimports(3) fieldsfirst ansi space 
// Source File Name:   RequestLine.java

package org.apache.http;


// Referenced classes of package org.apache.http:
//			ProtocolVersion

/**
 * @deprecated Interface RequestLine is deprecated
 */

public interface RequestLine
{

	public abstract String getMethod();

	public abstract ProtocolVersion getProtocolVersion();

	public abstract String getUri();
}
