// Decompiled by Jad v1.5.8e2. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://kpdus.tripod.com/jad.html
// Decompiler options: packimports(3) fieldsfirst ansi space 
// Source File Name:   HeaderElementIterator.java

package org.apache.http;

import java.util.Iterator;

// Referenced classes of package org.apache.http:
//			HeaderElement

/**
 * @deprecated Interface HeaderElementIterator is deprecated
 */

public interface HeaderElementIterator
	extends Iterator
{

	public abstract boolean hasNext();

	public abstract HeaderElement nextElement();
}
