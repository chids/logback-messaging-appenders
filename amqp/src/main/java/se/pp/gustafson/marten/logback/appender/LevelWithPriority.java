/**
 * 
 */
package se.pp.gustafson.marten.logback.appender;

import ch.qos.logback.classic.Level;

public enum LevelWithPriority
{
    INFO(1, Level.INFO.toString()), DEBUG(3, Level.DEBUG.toString()), WARN(7, Level.WARN.toString()), ERROR(9, Level.ERROR.toString());

    final int intValue;
    private final String name;

    private LevelWithPriority(final int value, final String name)
    {
        this.intValue = value;
        this.name = name;
    }

    public int intValue()
    {
        return this.intValue;
    }

    public String stringValue()
    {
        return this.name;
    }
}