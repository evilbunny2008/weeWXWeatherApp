package com.odiousapps.weewxweather;

@SuppressWarnings("unused")
class myIOException extends Exception
{
	public myIOException()
	{
		super();
	}

	public myIOException(String message)
	{
		super(message);
	}

	public myIOException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public myIOException(Throwable cause)
	{
		super(cause);
	}
}
